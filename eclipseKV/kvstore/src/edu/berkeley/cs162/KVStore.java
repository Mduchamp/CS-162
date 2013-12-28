/**
 * Persistent Key-Value storage layer. Current implementation is transient,
 * but assume to be backed on disk when you do your project.
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
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
import edu.berkeley.cs162.*;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSOutput;
import org.w3c.dom.ls.LSSerializer;
import org.xml.sax.SAXException;


/**
 * This is a dummy KeyValue Store. Ideally this would go to disk,
 * or some other backing store. For this project, we simulate the disk like
 * system using a manual delay.
 *
 *
 *
 */
public class KVStore implements KeyValueInterface {
    private Map<String, String> store     = null;

    public KVStore() {
        resetStore();
    }

    private void resetStore() {
        store = new HashMap<String, String>();
    }

    public void put(String key, String value) throws KVException {
        AutoGrader.agStorePutStarted(key, value);
        try {
            putDelay();
            store.put(key, value);
        } finally {
            AutoGrader.agStorePutFinished(key, value);
        }
    }
    
    public String get(String key) throws KVException {
        AutoGrader.agStoreGetStarted(key);

        try {
            getDelay();
            String retVal = this.store.get(key);
            System.out.println("RV= " + retVal);
            if (retVal == null) {
                KVMessage msg = new KVMessage("resp", "Does not exist");
                throw new KVException(msg);
            }
            return retVal;
        } finally {
            AutoGrader.agStoreGetFinished(key);
        }
    }

    public void del(String key) throws KVException {
        AutoGrader.agStoreDelStarted(key);

        try {
            delDelay();
            if (key != null) {
                if (this.store.containsKey(key)) {
                    this.store.remove(key);
                } else {
                    KVMessage msg = new KVMessage("resp", "key \"" + key + "\" does not exist");
                    throw new KVException(msg);
                }
            }
        } finally {
            AutoGrader.agStoreDelFinished(key);
        }
    }

    private void getDelay() {
        AutoGrader.agStoreDelay();
    }

    private void putDelay() {
        AutoGrader.agStoreDelay();
    }

    private void delDelay() {
        AutoGrader.agStoreDelay();
    }

    public String toXML() throws Exception {
        
    	String returnString = null;
		try {
			System.out.println("In to XML KVStore");
			DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder docBuilder  = docFactory.newDocumentBuilder();
    		Document doc = docBuilder.newDocument();
			Element rootElement = doc.createElement("KVStore");
			doc.appendChild(rootElement);
			
			for (String key : store.keySet()) {
				Element KVPairEle = doc.createElement("KVPair");
				rootElement.appendChild(KVPairEle);
				Element keyElement = doc.createElement("Key");
				keyElement.appendChild(doc.createTextNode(key));
				KVPairEle.appendChild(keyElement);
				Element valElement = doc.createElement("Value");
				valElement.appendChild(doc.createTextNode(store.get(key)));
				KVPairEle.appendChild(valElement);
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
			
			KVMessage exceptionMessage = new KVMessage("resp", "Unknown Error: Parser Configuration Exception");
			KVException exception = new KVException(exceptionMessage);
			throw exception;

		}
    	
		System.out.println("ReturnString= " + returnString);
		return returnString;
    }

    public void dumpToFile(String fileName) throws KVException {
    	String xmlString;
		try {
			xmlString = this.toXML();
		} catch (Exception e1) {
			KVMessage exceptionMessage = new KVMessage("resp", "Unknown Error: ToXML Failed");
			KVException exception = new KVException(exceptionMessage);
			throw exception;
		} 
    	FileWriter f;
		try {
			f = new FileWriter(fileName);
			f.write(xmlString); 
	    	f.close();
		} catch (IOException e) {
			KVMessage exceptionMessage = new KVMessage("resp", "IO Error");
			KVException exception = new KVException(exceptionMessage);
			throw exception;
		} 
    	
    }

    /**
     * Replaces the contents of the store with the contents of a file
     * written by dumpToFile; the previous contents of the store are lost.
     * @param fileName the file to be read.
     * @throws KVException 
     * @throws Exception
     */
    public void restoreFromFile(String fileName) throws KVException  {
        this.resetStore();
        
        File f = new File(fileName);
		DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder docBuilder;
		try {
			docBuilder = docFactory.newDocumentBuilder();
		} catch (ParserConfigurationException e1) {
			KVMessage exceptionMessage = new KVMessage("resp", "Unknown Error: Parser Configuration Exception");
			KVException exception = new KVException(exceptionMessage);
			throw exception;
		}
		Document doc;
		try {
			doc = docBuilder.parse(f);
		} catch (SAXException e1) {
			KVMessage exceptionMessage = new KVMessage("resp", "Unknown Error: SAX Exception");
			KVException exception = new KVException(exceptionMessage);
			throw exception;
		} catch (IOException e1) {
			KVMessage exceptionMessage = new KVMessage("resp", "IO Error");
			KVException exception = new KVException(exceptionMessage);
			throw exception;
		}
		doc.getDocumentElement().normalize();
		
		NodeList nList = doc.getElementsByTagName("KVPair");
		
		for (int i = 0; i < nList.getLength(); i++) {
			Node curNode = nList.item(i);
			
			if (curNode.getNodeType() == Node.ELEMENT_NODE) {
				Element e = (Element) curNode;
				String key = e.getElementsByTagName("Key").item(0).getTextContent();
				String val = e.getElementsByTagName("Value").item(0).getTextContent();
				store.put(key, val);
			}
		}
    }
}
