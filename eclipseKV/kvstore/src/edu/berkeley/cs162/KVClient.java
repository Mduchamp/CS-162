/**
 * Client component for generating load for the KeyValue store.
 * This is also used by the Master server to reach the slave nodes.
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

import java.net.Socket;
import java.net.UnknownHostException;
import java.io.IOException;


/**
 * This class is used to communicate with (appropriately marshalling and unmarshalling)
 * objects implementing the {@link KeyValueInterface}.
 *
 * @param <K> Java Generic type for the Key
 * @param <V> Java Generic type for the Value
 */
public class KVClient implements KeyValueInterface {

    private String server = null;
    private int port = 0;
    private Socket finallyHolder = null;
    private static final int MAX_KEY_SIZE = 256;
    private final String IGNORE_NEXT = "ignoreNext";
    /**
     * @param server is the DNS reference to the Key-Value server
     * @param port is the port on which the Key-Value server is listening
     */
    public KVClient(String server, int port) {
        this.server = server;
        this.port = port;
    }

    private Socket connectHost() throws KVException {
      try
      {
        Socket socket = new Socket(this.server, this.port);
        System.out.println("Socket created! Close Status is: " + socket.isClosed() + " Connected Status is: " + socket.isConnected());
        return socket;
      }
      catch(Exception exception)
      {
        System.out.println("There was an exception in connectHost().");
        handleException(exception);
      }
      return new Socket();
    }

    private void closeHost(Socket sock) throws KVException {
      try
      {
        sock.close();
        System.out.println("Socket closed! Close Status is: " + sock.isClosed() + " Connected Status is: " + sock.isConnected());
      }
      catch(Exception exception)
      {
        System.out.println("There was an exception in closeHost().");
        handleException(exception);
      }
    }
    
    private void handleException(Exception exception) throws KVException{
      System.out.println("The exception was: " + exception.toString());
      if(exception.toString() == "edu.berkeley.cs162.KVException")
      {
        throw ((KVException) exception);
      }
      if(exception.toString().equals("java.net.UnknownHostException"))
      {
        throw new KVException(new KVMessage("resp", "Network Error: Could not connect"));
      }
      if(exception.toString().equals("java.net.IOException"))
      {
        throw new KVException(new KVMessage("resp", "Network Error: Could not create socket"));
      }
      throw new KVException(new KVMessage("resp", "Unknown Error: Unknown exception type"));
    }

    public void put(String key, String value) throws KVException {
      try
      {
    	
    	System.out.println("PUt key = " + key + " value = " + value);
        Socket socket = connectHost();
        finallyHolder = socket;
        KVMessage send = new KVMessage("putreq");
        send.setKey(key);
        send.setValue(value);
        send.sendMessage(socket);
        
        KVMessage recieve = new KVMessage(socket);
        
        if(!recieve.getMessage().equals("Success"))
        {
          throw new KVException(recieve);
        }
      }
      catch(Exception exception)
      {
        System.out.println("There was an exception in put.");
        handleException(exception);
      }
      finally
      {
        closeHost(finallyHolder);
      }
      return;
    }

    public String get(String key) throws KVException {
      try
      {
    	
    	System.out.println("Get key = " + key);
        Socket socket = connectHost();
        finallyHolder = socket;
        KVMessage send = new KVMessage("getreq");
        send.setKey(key);
        
        send.sendMessage(socket);
        System.out.println("Before KVMessage Constructor");
        KVMessage recieve = new KVMessage(socket);
        System.out.println("Get receive = " + recieve.getValue());
        if(recieve.getValue() != null && recieve.getKey() != null)
        {      
          return recieve.getValue();
        }
        else 
        {
          throw new KVException(recieve);
        }
      }
      catch(Exception exception)
      {
        System.out.println("There was an exception in get.");
        handleException(exception);
      }
      finally
      {
        closeHost(finallyHolder);
      }
      return "Error";
    }

    public void del(String key) throws KVException {
      try
      {
        Socket socket = connectHost();
        finallyHolder = socket;
        KVMessage send = new KVMessage("delreq");
        send.setKey(key);
        send.sendMessage(socket);
        KVMessage recieve = new KVMessage(socket);
        if(!recieve.getMessage().equals("Success"))
        {
          throw new KVException(recieve);
        }
        return;
      }
      catch(Exception exception)
      {
        System.out.println("There was an exception in del.");
        handleException(exception);
      }
      finally
      {
    	
    		closeHost(finallyHolder);
      }
    }
    
    public void ignoreNext() throws KVException {
    	KVMessage ignoreNext = new KVMessage(IGNORE_NEXT);
    	ignoreNext.sendMessage(connectHost());
    }
    
    /*@Test
    public void Test() {
      System.out.println("Starting KVClient Test!");
      runInNachos(new Runnable(){
        @Override
        public void run() {
          
        }
      })
    }
    
    public static void main(String[] args) throws Exception
    {
      KVException testexception = new KVException(new KVMessage("resp", "Error"));
      System.out.println(testexception.toString());
      throw testexception;
    }*/
}
