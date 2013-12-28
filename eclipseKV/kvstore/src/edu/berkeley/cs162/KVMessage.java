/**
 * XML Parsing library for the key-value store
 *
 * @author Mosharaf Chowdhury (http://www.mosharaf.com)
 * @author Prashanth Mohan (http://www.cs.berkeley.edu/~prmohan)
 *
 * Copyright (c) 2012, University of California at Berkeley
 * All rights reserved.
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *  * Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *  * Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 *  * Neither the name of University of California, Berkeley nor the
 *    names of its contributors may be used to endorse or promote products
 *    derived from this software without specific prior written permission.
 *
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 *  ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 *  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 *  DISCLAIMED. IN NO EVENT SHALL THE AUTHORS BE LIABLE FOR ANY
 *  DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 *  (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 *  LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 *  ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 *  SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package edu.berkeley.cs162;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStreamWriter;
import java.io.Serializable;
import java.io.StringWriter;
import java.io.Writer;
import java.net.Socket;
import java.net.SocketException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.w3c.dom.*;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSOutput;
import org.w3c.dom.ls.LSSerializer;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.*;

/**
 * This is the object that is used to generate messages the XML based messages
 * for communication between clients and servers.
 */
@SuppressWarnings("serial")
public class KVMessage implements Serializable {
	private String msgType = null;
	private String key = null;
	private String value = null;
	private String message = null;
	public String tpcOpId = null;

	private final String GET_MESSAGE = "getreq";
	private final String PUT_MESSAGE = "putreq";
	private final String DEL_MESSAGE = "delreq";
	private final String RESP_MESSAGE = "resp";
	private final String RESP_GET_MESSAGE = "resp_get";
	private final String READY_MESSAGE = "ready";
	private final String ABORT_MESSAGE = "abort";
	private final String COMMIT_MESSAGE = "commit";
	private final String ACK_MESSAGE = "ack";
    private final String IGNORE_NEXT = "ignoreNext";
    private final String REGISTER_MESSAGE = "register";
	
	private boolean get_response = false;
	private boolean tpcOperation = false;

	public final String getKey() {
		return key;
	}

	public final void setKey(String key) {
		this.key = key;
	}

	public final String getValue() {
		return value;
	}

	public final void setValue(String value) {
		this.value = value;
	}

	public final String getMessage() {
		return message;
	}

	public final void setMessage(String message) {
		this.message = message;
	}

	public final void setMsgType(String msgType) {
		this.msgType = msgType;
	}

	public String getMsgType() {
		return msgType;
	}
	
	public String getTpcOpId() {
	        return tpcOpId;
	}

	public void setTpcOpId(String id) {
		this.tpcOpId = id;
	}

	/*
	 * Solution from
	 * http://weblogs.java.net/blog/kohsuke/archive/2005/07/socket_xml_pitf.html
	 */
	private class NoCloseInputStream extends FilterInputStream {
		public NoCloseInputStream(InputStream in) {
			super(in);
		}

		public void close() {
		} // ignore close
	}

	/***
	 * 
	 * @param msgType
	 * @throws KVException
	 *             of type "resp" with message "Message format incorrect" if
	 *             msgType is unknown
	 */
	public KVMessage(String msgType) throws KVException {
		if (msgType.equals(PUT_MESSAGE) || msgType.equals(DEL_MESSAGE)
				|| msgType.equals(ABORT_MESSAGE) || msgType.equals(COMMIT_MESSAGE) || msgType.equals(ACK_MESSAGE)
				|| msgType.equals(READY_MESSAGE)) {
			this.msgType = msgType;
			this.tpcOperation = true;
		} else if (msgType.equals(RESP_MESSAGE) || msgType.equals(RESP_GET_MESSAGE) || msgType.equals(GET_MESSAGE)
				|| msgType.equals(IGNORE_NEXT) || msgType.equals(REGISTER_MESSAGE)) {
			if (msgType.equals(RESP_GET_MESSAGE)) {
				this.get_response = true;
			}
			this.msgType = msgType;
			
		} else {
			KVMessage exceptionMessage = new KVMessage(RESP_MESSAGE,
					"Message format incorrect");
			KVException exception = new KVException(exceptionMessage);
			throw exception;
		}
	}

	public KVMessage(String msgType, String message) throws KVException {
		System.out.println("Msgtype = " + msgType);
		if (msgType.equals(PUT_MESSAGE) || msgType.equals(DEL_MESSAGE)
				|| msgType.equals(ABORT_MESSAGE) || msgType.equals(COMMIT_MESSAGE) || msgType.equals(ACK_MESSAGE)
				|| msgType.equals(READY_MESSAGE)) {
			this.msgType = msgType;
			this.tpcOperation = true;
			setMessage(message);
		} else if (msgType.equals(RESP_MESSAGE) || msgType.equals(RESP_GET_MESSAGE) || msgType.equals(GET_MESSAGE)
				|| msgType.equals(IGNORE_NEXT) || msgType.equals(REGISTER_MESSAGE)) {
			if (msgType.equals(RESP_GET_MESSAGE)) {
				this.get_response = true;
			}
			this.msgType = msgType;
			setMessage(message);
		} else {
			KVMessage exceptionMessage = new KVMessage(RESP_MESSAGE,
					"Message format incorrect");
			KVException exception = new KVException(exceptionMessage);
			throw exception;
		}
	}
	
	public KVMessage(Socket sock, int timeout) throws KVException {
		try {
			sock.setSoTimeout(timeout);
		} catch (SocketException e) {
			KVMessage exceptionMessage = new KVMessage(RESP_MESSAGE, "Setting timeout on socket exception");
			KVException exception = new KVException(exceptionMessage);
			throw exception;
		}
		NoCloseInputStream inputData;
		try {
			inputData = new NoCloseInputStream(sock.getInputStream());
			
		} catch (IOException e1) {
			KVMessage exceptionMessage = new KVMessage(RESP_MESSAGE,
					"Timeout Error: Could not receive data");
			KVException exception = new KVException(exceptionMessage);
			throw exception;
		}

		org.w3c.dom.Document document = null;
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db = null;

		try {
			db = dbf.newDocumentBuilder();
		} catch (ParserConfigurationException e1) {
			e1.printStackTrace();
		}

		try {
			document = db.parse(new InputSource(inputData));
		} catch (IOException e) {
			System.out.println("In the catch for IOException");
			KVMessage exceptionMessage = new KVMessage(RESP_MESSAGE,
					"XML Error: Received unparseable message");
			KVException exception = new KVException(exceptionMessage);
			throw exception;
		} catch (SAXException e) {
			System.out.println("In the SAXException");
			KVMessage exceptionMessage = new KVMessage(RESP_MESSAGE,
					"XML Error: Received unparseable message");
			KVException exception = new KVException(exceptionMessage);
			throw exception;
		}
		
		try {
			NodeList nList = document.getElementsByTagName("KVMessage");
			Node nNode = nList.item(0);
			String messageType = null;
			Element eElement = null;
			if (nNode.getNodeType() == Node.ELEMENT_NODE) {
				eElement = (Element) nNode;
				messageType = eElement.getAttribute("type");
			}
			if (messageType.equals(GET_MESSAGE) || messageType.equals(DEL_MESSAGE)) {
				String keyString = eElement.getElementsByTagName("Key").item(0).getTextContent();
				if(keyString == null || keyString.length() < 1) {
					KVMessage exceptionMessage = new KVMessage(RESP_MESSAGE,
							"Unknown Error: invalid key");
					KVException exception = new KVException(exceptionMessage);
					throw exception;
				}
				setMsgType(messageType);
				System.out.println("Setting key in KVMessage constructor");
				setKey(keyString);
				if (messageType.equals(DEL_MESSAGE)) {
					this.tpcOperation = true;
					String tpcString = eElement.getElementsByTagName("TPCOpId").item(0).getTextContent();
					setTpcOpId(tpcString);
				}
			}else if (messageType.equals(PUT_MESSAGE)) {
				//System.out.println("In put request");
				setMsgType(messageType);
				setKeyValueForSocket(eElement);
				this.tpcOperation = true;
				String tpcString = eElement.getElementsByTagName("TPCOpId").item(0).getTextContent();
				setTpcOpId(tpcString);
			} else if (messageType.equals(READY_MESSAGE) || messageType.equals(ABORT_MESSAGE) || messageType.equals(ACK_MESSAGE)
					|| messageType.equals(COMMIT_MESSAGE)) {
				setMsgType(messageType);
				this.tpcOperation = true;
				String tpcString = eElement.getElementsByTagName("TPCOpId").item(0).getTextContent();
				setTpcOpId(tpcString);
			} else if (messageType.equals(IGNORE_NEXT) || messageType.equals(REGISTER_MESSAGE)) {
				setMsgType(messageType);
				if (messageType.equals(REGISTER_MESSAGE) && eElement.getElementsByTagName("Message").item(0).getTextContent() != null) {
					setMessage(eElement.getElementsByTagName("Message").item(0).getTextContent());
				}
			} else if (messageType.equals(RESP_MESSAGE)) {
				boolean hasValue = true;
				boolean hasKey = true;
				try {
					String keyString = eElement.getElementsByTagName("Key").item(0).getTextContent();
				}
				catch (Exception e){
					hasKey = false;
				}
				try {
					String valueString = eElement.getElementsByTagName("Value").item(0).getTextContent();
				}catch(Exception e){
					hasValue = false;
				}
				if (hasKey || hasValue){
					System.out.println("True");
					System.out.println("New place holder");
					setKeyValueForSocket(eElement);
				}else 
				{
					setMessage(eElement.getElementsByTagName("Message").item(0).getTextContent());
				}
				setMsgType(messageType);
				
			}
				
		} catch (Exception e) {
			System.out.println("Exception in KVMessage bottom nNode");
			KVMessage exceptionMessage = new KVMessage(RESP_MESSAGE,
					"Message format incorrect");
			KVException exception = new KVException(exceptionMessage);
			throw exception;
		}
	}
	   
//	public KVMessage(String msgType, boolean tpcMessage) throws KVException {
//		this.tpcOperation = tpcMessage;
//    	if (msgType.equals(GET_MESSAGE) || msgType.equals(PUT_MESSAGE)
//				|| msgType.equals(DEL_MESSAGE) || msgType.equals(RESP_MESSAGE) || msgType.equals(RESP_GET_MESSAGE)) {
//			if (msgType.equals(RESP_GET_MESSAGE)) {
//				this.get_response = true;
//			}
//			this.msgType = msgType;
//		} else {
//			KVMessage exceptionMessage = new KVMessage(RESP_MESSAGE,
//					"Message format incorrect");
//			KVException exception = new KVException(exceptionMessage);
//			throw exception;
//		}
//	}
	
	/***
	 * Parse KVMessage from socket's input stream
	 * 
	 * @param sock
	 *            Socket to receive from
	 * @throws KVException
	 *             if there is an error in parsing the message. The exception
	 *             should be of type "resp and message should be : a.
	 *             "XML Error: Received unparseable message" - if the received
	 *             message is not valid XML. b.
	 *             "Network Error: Could not receive data" - if there is a
	 *             network error causing an incomplete parsing of the message.
	 *             c. "Message format incorrect" - if there message does not
	 *             conform to the required specifications. Examples include
	 *             incorrect message type.
	 */
	public KVMessage(Socket sock) throws KVException {
		System.out.println("In KVMessage Constructor socket");
		NoCloseInputStream inputData;
		try {
			inputData = new NoCloseInputStream(sock.getInputStream());
			
		} catch (IOException e1) {
			KVMessage exceptionMessage = new KVMessage(RESP_MESSAGE,
					"Timeout Error: Could not receive data");
			KVException exception = new KVException(exceptionMessage);
			throw exception;
		}

		org.w3c.dom.Document document = null;
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		DocumentBuilder db = null;

		try {
			db = dbf.newDocumentBuilder();
		} catch (ParserConfigurationException e1) {
			e1.printStackTrace();
		}

		try {
			document = db.parse(new InputSource(inputData));
		} catch (IOException e) {
			System.out.println("In the catch for IOException");
			KVMessage exceptionMessage = new KVMessage(RESP_MESSAGE,
					"XML Error: Received unparseable message");
			KVException exception = new KVException(exceptionMessage);
			throw exception;
		} catch (SAXException e) {
			System.out.println("In the SAXException");
			KVMessage exceptionMessage = new KVMessage(RESP_MESSAGE,
					"XML Error: Received unparseable message");
			KVException exception = new KVException(exceptionMessage);
			throw exception;
		}
		
		try {
			NodeList nList = document.getElementsByTagName("KVMessage");
			Node nNode = nList.item(0);
			String messageType = null;
			Element eElement = null;
			if (nNode.getNodeType() == Node.ELEMENT_NODE) {
				eElement = (Element) nNode;
				messageType = eElement.getAttribute("type");
			}
			if (messageType.equals(GET_MESSAGE) || messageType.equals(DEL_MESSAGE)) {
				String keyString = eElement.getElementsByTagName("Key").item(0).getTextContent();
				if(keyString == null || keyString.length() < 1) {
					KVMessage exceptionMessage = new KVMessage(RESP_MESSAGE,
							"Unknown Error: invalid key");
					KVException exception = new KVException(exceptionMessage);
					throw exception;
				}
				setMsgType(messageType);
				System.out.println("Setting key in KVMessage constructor");
				setKey(keyString);
				if (messageType.equals(DEL_MESSAGE)) {
					this.tpcOperation = true;
					String tpcString = eElement.getElementsByTagName("TPCOpId").item(0).getTextContent();
					setTpcOpId(tpcString);
				}
			}else if (messageType.equals(PUT_MESSAGE)) {
				//System.out.println("In put request");
				setMsgType(messageType);
				setKeyValueForSocket(eElement);
				this.tpcOperation = true;
				String tpcString = eElement.getElementsByTagName("TPCOpId").item(0).getTextContent();
				setTpcOpId(tpcString);
			} else if (messageType.equals(READY_MESSAGE) || messageType.equals(ABORT_MESSAGE) || messageType.equals(ACK_MESSAGE)
					|| messageType.equals(COMMIT_MESSAGE)) {
				setMsgType(messageType);
				this.tpcOperation = true;
				String tpcString = eElement.getElementsByTagName("TPCOpId").item(0).getTextContent();
				setTpcOpId(tpcString);
			}else if (messageType.equals(IGNORE_NEXT) || messageType.equals(REGISTER_MESSAGE)) {
				setMsgType(messageType);
				if (messageType.equals(REGISTER_MESSAGE) && eElement.getElementsByTagName("Message").item(0).getTextContent() != null) {
					setMessage(eElement.getElementsByTagName("Message").item(0).getTextContent());
				}
			} else if (messageType.equals(RESP_MESSAGE)) {
				System.out.println("TOXML resp_message");
				boolean hasValue = true;
				boolean hasKey = true;
				try {
					String keyString = eElement.getElementsByTagName("Key").item(0).getTextContent();
				}
				catch (Exception e){
					hasKey = false;
				}
				try {
					String valueString = eElement.getElementsByTagName("Value").item(0).getTextContent();
				}catch(Exception e){
					hasValue = false;
				}
				if (hasKey || hasValue){
					System.out.println("True");
					System.out.println("New place holder");
					setKeyValueForSocket(eElement);
				}else 
				{
					System.out.println("Else");
					setMessage(eElement.getElementsByTagName("Message").item(0).getTextContent());
				}
				setMsgType(messageType);
				
			}
				
		} catch (Exception e) {
			System.out.println("Exception in KVMessage bottom nNode");
			KVMessage exceptionMessage = new KVMessage(RESP_MESSAGE,
					"Message format incorrect");
			KVException exception = new KVException(exceptionMessage);
			throw exception;
		}
	}
	
	public void setKeyValueForSocket(Element eElement) throws KVException{
		String keyString = eElement.getElementsByTagName("Key").item(0).getTextContent();
		String valueString = eElement.getElementsByTagName("Value").item(0).getTextContent();
		setKey(keyString);
		setValue(valueString);
	}

	/**
	 * Generate the XML representation for this message.
	 * 
	 * @return the XML String
	 * @throws KVException
	 *             if not enough data is available to generate a valid KV XML
	 *             message
	 */
	public String toXML() throws KVException {
		String returnString = null;
		try {
			System.out.println("In to XML");
			DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder docBuilder  = docFactory.newDocumentBuilder();
    		Document doc = docBuilder.newDocument();
			Element rootElement = doc.createElement("KVMessage");
			doc.appendChild(rootElement);
			Attr type = doc.createAttribute("type");
			System.out.println("Message = " + this.msgType + " TCPOpId = " + this.tpcOpId);
			
			//System.out.println("Message Type = " + this.msgType);
			if (!this.msgType.equals(RESP_GET_MESSAGE)) {
				type.setValue(this.msgType);
			}
			rootElement.setAttributeNode(type);
			
			if (this.msgType.equals(RESP_MESSAGE)) {
				
			System.out.println("In reponse message");
				
				
				if(this.key != null || this.value != null) {
					System.out.println("Triggered new get resp");
					Element key = doc.createElement("Key");
		    		key.appendChild(doc.createTextNode(this.key));
		    		rootElement.appendChild(key);
		    		
		    		Element value = doc.createElement("Value");
		    		value.appendChild(doc.createTextNode(this.value));
		    		rootElement.appendChild(value);
				}
				else {
					System.out.println("Triggered normal get resp");
					Element message = doc.createElement("Message");
					message.appendChild(doc.createTextNode(this.message));
					rootElement.appendChild(message);
				}
	    		
	        }
			else if (this.msgType.equals(GET_MESSAGE) || this.msgType.equals(DEL_MESSAGE)) {
				//checkForValidInput();
				System.out.println("In get message toXML()");
				if(this.key == null || this.key.length() < 1) {
					KVMessage exceptionMessage = new KVMessage(RESP_MESSAGE,
							"Unknown Error: invalid key");
					KVException exception = new KVException(exceptionMessage);
					throw exception;
				}
	        	Element key = doc.createElement("Key");
	    		key.appendChild(doc.createTextNode(this.key));
	    		rootElement.appendChild(key);
	        	if (this.msgType.equals(DEL_MESSAGE)) {
	        		Element opId = doc.createElement("TPCOpId");
	        		opId.appendChild(doc.createTextNode(this.tpcOpId));
		    		rootElement.appendChild(opId);
	        	}
	        }  else if (this.msgType.equals(PUT_MESSAGE)) {
	        	System.out.println("In put message=" + this.key);
	        	Element key = doc.createElement("Key");
	    		key.appendChild(doc.createTextNode(this.key));
	    		rootElement.appendChild(key);
	    		
	    		Element value = doc.createElement("Value");
	    		value.appendChild(doc.createTextNode(this.value));
	    		rootElement.appendChild(value);
	    		
	    		Element opId = doc.createElement("TPCOpId");
        		opId.appendChild(doc.createTextNode(this.tpcOpId));
	    		rootElement.appendChild(opId);
	        } else if (this.msgType.equals(ACK_MESSAGE) || this.msgType.equals(ABORT_MESSAGE) || this.msgType.equals(READY_MESSAGE)
	        		|| this.msgType.equals(COMMIT_MESSAGE)) {
	        	if (this.msgType.equals(ABORT_MESSAGE) && this.message != null) {
	        		Element message = doc.createElement("Message");
	        		message.appendChild(doc.createTextNode(this.message));
		    		rootElement.appendChild(message);
	        	}
	        	Element opId = doc.createElement("TPCOpId");
        		opId.appendChild(doc.createTextNode(this.tpcOpId));
	    		rootElement.appendChild(opId);
	        } else if (this.msgType.equals(REGISTER_MESSAGE) && this.message != null) {
	        	Element message = doc.createElement("Message");
        		message.appendChild(doc.createTextNode(this.message));
	    		rootElement.appendChild(message);
	        } else if (this.msgType.equals(IGNORE_NEXT)){
	        
	        } else {
	        	System.out.println("Succesfully recognized error message");
				type.setValue(RESP_MESSAGE);
				Element message = doc.createElement("Message");
				message.appendChild(doc.createTextNode("Error Message"));
				rootElement.appendChild(message);
	        }
			
			DOMImplementationLS domImplementation = (DOMImplementationLS) doc.getImplementation();
 		    LSSerializer lsSerializer = domImplementation.createLSSerializer();
 		    LSOutput ls = domImplementation.createLSOutput();
 		    ls.setEncoding("UTF-8");
 		    Writer stringWriter = new StringWriter();
 		    ls.setCharacterStream(stringWriter);
 		    lsSerializer.write(doc, ls);
 		    returnString = stringWriter.toString();
 		    
		} catch (Exception e) {
			KVMessage exceptionMessage = new KVMessage(RESP_MESSAGE, "Unknown Error: Not enough Data");
			KVException exception = new KVException(exceptionMessage);
			throw exception;
		}
		System.out.println("ReturnString= " + returnString);
 
		return returnString;	 		
	}
	
	
	public void checkForValidInput() throws KVException{
		System.out.println("CHeck for valid input key= " + this.key);
		if(this.key == null || this.key.length() < 1) {
			
			KVMessage exceptionMessage = new KVMessage(RESP_MESSAGE,
					"Unknown Error: invalid key");
			KVException exception = new KVException(exceptionMessage);
			throw exception;
		} else if (this.key.length() > 256) {
			
			KVMessage exceptionMessage = new KVMessage(RESP_MESSAGE,
					"Oversized key");
			KVException exception = new KVException(exceptionMessage);
			throw exception;
		}else if (this.value == null || this.value.length() < 1) {
			
			KVMessage exceptionMessage = new KVMessage(RESP_MESSAGE,
					"Unknown Error: invalid value");
			KVException exception = new KVException(exceptionMessage);
			throw exception;
		} else if (this.value.length() > 262144) {
			
			KVMessage exceptionMessage = new KVMessage(RESP_MESSAGE,
					"Oversized value");
			KVException exception = new KVException(exceptionMessage);
			throw exception;
		}
	}

	public void sendMessage(Socket sock) throws KVException {
		OutputStreamWriter stream = null;
		try {
			stream = new OutputStreamWriter(sock.getOutputStream());
			stream.write(toXML());
			stream.flush();
			sock.shutdownOutput();
		}
		catch(Exception e){
			KVMessage exceptionMessage = new KVMessage(RESP_MESSAGE,
					"Network Error: Could not receive data");
			KVException exception = new KVException(exceptionMessage);
			throw exception;
		}
	}
}
