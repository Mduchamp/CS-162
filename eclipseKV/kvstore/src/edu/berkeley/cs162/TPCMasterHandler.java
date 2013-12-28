/**
 * Handle TPC connections over a socket interface
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

import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;

/**
 * Implements NetworkHandler to handle 2PC operation requests from the Master/
 * Coordinator Server
 *
 */
public class TPCMasterHandler implements NetworkHandler {

    public KVServer kvServer = null;
    public ThreadPool threadpool = null;
    public TPCLog tpcLog = null;
    public long slaveID = -1;

    // Used to handle the "ignoreNext" message
    public boolean ignoreNext = false;

    // Stored phase-1 request message from TPCMaster
    public KVMessage originalMessage = null;

    // Whether we sent back an abort decision in phase 1. Used and checked by
    // autograder. Is not used for any other logic.
    private boolean aborted = true;
    
    private final String ACK  = "ack";
    private final String IGNORE_MESSAGE_RESPONSE = "IgnoreNext Error: SlaveServer " + slaveID + " has ignored this 2PC request during the first phase";
    private final String ABORTED = "abort";
    private final String READY = "ready";
    private final String COMMIT = "commit";
    private final String RESP = "resp";
    private final String GET_MESSAGE = "getreq";
	private final String PUT_MESSAGE = "putreq";
	private final String DEL_MESSAGE = "delreq";
	private final String SUCCESS = "Success";
    
    public TPCMasterHandler(KVServer keyserver) {
        this(keyserver, 1);
    }

    public TPCMasterHandler(KVServer keyserver, long sID) {
        kvServer = keyserver;
        slaveID = sID;
        threadpool = new ThreadPool(1);
    }

    public TPCMasterHandler(KVServer kvS, long sID, int connections) {
        kvServer = kvS;
        slaveID = sID;
        threadpool = new ThreadPool(1);
    }


    /**
     * Set TPCLog after it has been rebuilt.
     * @param tpcLog
     */
    public void setTPCLog(TPCLog tpcLog) {
        this.tpcLog = tpcLog;
    }

    /**
     * Registers the slave server with the master.
     *
     * @param masterHostName
     * @param server SocketServer used by this slave server (contains the hostName and a random port)
     * @throws UnknownHostException
     * @throws IOException
     * @throws KVException
     */
    public void registerWithMaster(String masterHostName, SocketServer server)
            throws UnknownHostException, IOException, KVException {
        AutoGrader.agRegistrationStarted(slaveID);

        Socket master = new Socket(masterHostName, 9090);
        KVMessage regMessage = new KVMessage(
            "register", slaveID + "@" + server.getHostname() + ":" + server.getPort());
        regMessage.sendMessage(master);

        // Receive master response. Response should always be success.

        master.close();
        AutoGrader.agRegistrationFinished(slaveID);
    }

    @Override
    public void handle(Socket client) throws IOException {
        AutoGrader.agReceivedTPCRequest(slaveID);
        Runnable r = new MasterHandler(kvServer, client);
        try {
            threadpool.addToQueue(r);
        } catch (InterruptedException e) {
            return; // ignore this error
        }
        AutoGrader.agFinishedTPCRequest(slaveID);
    }

    public class MasterHandler implements Runnable {

        public KVServer keyserver = null;
        public Socket master = null;

        public void closeConn() {
            try {
                master.close();
            } catch (IOException e) {}
        }

        public MasterHandler(KVServer keyserver, Socket client) {
            this.keyserver = keyserver;
            master = client;
        }

        @Override
        public void run() {
           
            KVMessage masterMessage = null;
            
            try {
            	masterMessage = new KVMessage(master);
            	String key = masterMessage.getKey();
            	String msgType = masterMessage.getMsgType();
            	if (msgType.equals("getreq")) {
	                handleGet(masterMessage, key);
	            } else if (msgType.equals("putreq")) {
	            	if (!ignoreNext)
	            		handlePut(masterMessage, key);
	            	else 
	            		resetIgnoreNext();
	            } else if (msgType.equals("delreq")) {
	            	if (!ignoreNext)
	            		handleDel(masterMessage, key);
	            	else 
	            		resetIgnoreNext();
	            } else if (msgType.equals("ignoreNext")) {
	            		setIgnoreNext();
	            } else if (msgType.equals("commit") || msgType.equals("abort")) {
	            	if (tpcLog.hasInterruptedTpcOperation()){
	            		originalMessage = tpcLog.getInterruptedTpcOperation();
	            	}
            	     if (msgType.equals(ABORTED))
            	    	 aborted = true;
            	     else 
            	    	 aborted = false;
            	     handleMasterResponse(masterMessage, originalMessage, 
         	            	aborted);
	            	aborted = false;
	            	originalMessage = null;
	            }
            }
            catch (KVException e){
            	abortMessage(masterMessage.tpcOpId);
            }
            finally {
            	closeConn();
            }
        }
        
       

        /* Handle a get request from the master */
        public void handleGet(KVMessage msg, String key) {
            AutoGrader.agGetStarted(slaveID);

            KVMessage failureMessage;
            try {
            	if (kvServer.hasKey(key)){
            		readyMessage(msg);
            	}
            	else {
            		failureMessage = new KVMessage(RESP, "KEY NOT HERE");
        			failureMessage.sendMessage(master);
            	}
            }
            catch (KVException e){
            	abortMessage(msg.tpcOpId);
            }

            AutoGrader.agGetFinished(slaveID);
        }

        /* Handle a phase-1 2PC put request from the master */
        public void handlePut(KVMessage msg, String key) {
            AutoGrader.agTPCPutStarted(slaveID, msg, key);
           
            try {
            	readyMessage(msg);
            }
            catch(KVException e){
            	abortMessage(msg.tpcOpId);
            }

            AutoGrader.agTPCPutFinished(slaveID, msg, key);
        }
        
        

        /* Handle a phase-1 2PC del request from the master */
        public void handleDel(KVMessage msg, String key) {
            AutoGrader.agTPCDelStarted(slaveID, msg, key);
            
            try {
            	if (kvServer.hasKey(key)){
            		readyMessage(msg);
            	}
            	else 
            		throw new KVException(msg);
            }
            catch(KVException e){
            	abortMessage(msg.tpcOpId);
            }

            AutoGrader.agTPCDelFinished(slaveID, msg, key);
        }

        /**
         * Second phase of 2PC
         *
         * @param masterResp Global decision taken by the master
         * @param origMsg Message from the actual client (received via the coordinator/master)
         * @param origAborted Did this slave server abort it in the first phase
         */
        public void handleMasterResponse(KVMessage masterResp, KVMessage origMsg, boolean origAborted) {
            AutoGrader.agSecondPhaseStarted(slaveID, origMsg, origAborted);
            
            try {
	            if (masterResp.getMsgType().equals(COMMIT)){
	            	if (origMsg.getMsgType().equals(DEL_MESSAGE)){
	            		String key = origMsg.getKey();
	            		kvServer.del(key);
	            	}
	            	else if (origMsg.getMsgType().equals(PUT_MESSAGE)){
	            		String value = origMsg.getValue();
	            		String key = origMsg.getKey();
	            		kvServer.put(key, value);
	            	}
	            	tpcLog.appendAndFlush(new KVMessage(COMMIT));
	            	sendAckMessage(masterResp.tpcOpId);
	            }
	            else if (aborted)
	            	sendAckMessage(masterResp.tpcOpId);
            }
            catch (KVException e){
            	abortMessage(masterResp.tpcOpId);
            }

            AutoGrader.agSecondPhaseFinished(slaveID, origMsg, origAborted);
        }
        
        
        private void sendAckMessage(String id){
        	try {
        		KVMessage ackMessage = new KVMessage(ACK);
        		ackMessage.setTpcOpId(id);
        		ackMessage.sendMessage(master);
        	} catch (KVException e1) {
        	               	//Fail silently due to connection error
        	}
        }
        
        private void abortMessage(String id){
        	aborted = true;
        	try {
        		KVMessage failureMessage = new KVMessage(ABORTED);
        		failureMessage.setTpcOpId(id);
        		failureMessage.sendMessage(master);
        	} catch (KVException e1) {
        	             //Fail silently due to connection error
        	}
        }
        
        private void setIgnoreNext(){
        	ignoreNext = true;
        	try {
        		KVMessage respMessage = new KVMessage(RESP, SUCCESS);
        		respMessage.sendMessage(master);
        	} catch (KVException e1) {
        		//Fail silently due to connection error
        	}
        }
        
        private void resetIgnoreNext(){
        	ignoreNext = false;
    		try {
    			KVMessage ignoreMessage = new KVMessage(RESP, IGNORE_MESSAGE_RESPONSE);
    			ignoreMessage.sendMessage(master);
    		}
    		catch(KVException e){
    			//Fail silently like a turd missing the toilet
    		}
        }
        
        public void readyMessage(KVMessage msg) throws KVException{
        	originalMessage = msg;
        	KVMessage successMessage = new KVMessage(READY);
        	successMessage.setTpcOpId(msg.tpcOpId);
        	successMessage.sendMessage(master);
        	tpcLog.appendAndFlush(originalMessage);
        }

    }

}
