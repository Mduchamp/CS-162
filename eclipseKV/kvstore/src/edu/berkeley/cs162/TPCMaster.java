/**
 * Master for Two-Phase Commits
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
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Comparator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TPCMaster {
	private final String GET_MESSAGE = "getreq";
	private final String PUT_MESSAGE = "putreq";
	private final String DEL_MESSAGE = "delreq";
	private final String RESP_MESSAGE = "resp";
	private final String ABORTED = "abort";
	private final String READY = "ready";
	private final String COMMIT = "commit";
	 private final String IGNORE_NEXT = "ignoreNext";

	// Timeout value used during 2PC operations
	public static final int TIMEOUT_MILLISECONDS = 5000;

	// Port on localhost to run registration server on
	private static final int REGISTRATION_PORT = 9090;

	// Cache stored in the Master/Coordinator Server
	public KVCache masterCache = new KVCache(100, 10);

	// Registration server that uses TPCRegistrationHandler
	public SocketServer regServer = null;

	// Number of slave servers in the system
	public int numSlaves = -1;

	// ID of the next 2PC operation
	public Long tpcOpId = 0L;
	
	public static boolean ignoreNextMessage = false;

	TreeMap<Long, SlaveInfo> registeredSlaves;
	Lock registeredSlavesLock; 

	/**
	 * Creates TPCMaster
	 * 
	 * @param numSlaves
	 *            number of slave servers expected to register
	 * @throws Exception
	 */
	public TPCMaster(int numSlaves) {
		this.numSlaves = numSlaves;
		try {
			regServer = new SocketServer(InetAddress.getLocalHost().getHostAddress(), REGISTRATION_PORT);
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}

		registeredSlaves = new TreeMap<Long, SlaveInfo>(new LongComparator());
		registeredSlavesLock = new ReentrantLock();
	}

	/**
	 * Calculates tpcOpId to be used for an operation. In this implementation it is a long variable that increases by one for each 2PC operation.
	 * 
	 * @return
	 */
	public String getNextTpcOpId() {
		tpcOpId++;
		return tpcOpId.toString();
	}

	class regServerRunnable implements Runnable {
		public void run() {

			regServer.addHandler(new TPCRegistrationHandler(numSlaves));
			try {

				// regServer.handl
				regServer.connect();
				regServer.run();

			} catch (IOException e) {
				
				e.printStackTrace();
			}
		}
	}

	/**
	 * Start registration server in a separate thread.
	 */
	public void run() {
		AutoGrader.agTPCMasterStarted();

		regServerRunnable myRunnable = new regServerRunnable();
		Thread myThread = new Thread(myRunnable);
		myThread.start();

		AutoGrader.agTPCMasterFinished();
	}

	/**
	 * Converts Strings to 64-bit longs. Borrowed from http://goo.gl/le1o0W, adapted from String.hashCode().
	 * 
	 * @param string
	 *            String to hash to 64-bit
	 * @return long hashcode
	 */
	public long hashTo64bit(String string) {
		long h = 1125899906842597L;
		int len = string.length();

		for (int i = 0; i < len; i++) {
			h = (31 * h) + string.charAt(i);
		}
		return h;
	}

	/**
	 * Compares two longs as if they were unsigned (Java doesn't have unsigned data types except for char). Borrowed from http://goo.gl/QyuI0V
	 * 
	 * @param n1
	 *            First long
	 * @param n2
	 *            Second long
	 * @return is unsigned n1 less than unsigned n2
	 */
	public boolean isLessThanUnsigned(long n1, long n2) {
		return (n1 < n2) ^ ((n1 < 0) != (n2 < 0));
	}

	public boolean isLessThanEqualUnsigned(long n1, long n2) {
		return isLessThanUnsigned(n1, n2) || (n1 == n2);
	}

	private class LongComparator implements Comparator<Long> {
		public int compare (Long a, Long b) {
			if (isLessThanUnsigned(a, b)) {
				return -1;
			} else if (a == b) {
				return 0;
			} else {
				return 1;
			}
		}
		
	}
	
	/**
	 * Find primary replica for a given key.
	 * 
	 * @param key
	 * @return SlaveInfo of first replica
	 */
	public SlaveInfo findFirstReplica(String key) {
		// 64-bit hash of the key
		long hashedKey = hashTo64bit(key.toString());
		registeredSlavesLock.lock();
		Map.Entry<Long, SlaveInfo> entry = registeredSlaves.higherEntry(hashedKey);
		SlaveInfo slave;
		System.out.println(registeredSlaves.size());
		if (entry == null) {
			// registeredSlaves.firstEntry().getValue();
			slave = registeredSlaves.firstEntry().getValue();
		} else {
			slave = entry.getValue();
		}
		registeredSlavesLock.unlock();
		return slave;
	}

	/**
	 * Find the successor of firstReplica.
	 * 
	 * @param firstReplica
	 *            SlaveInfo of primary replica
	 * @return SlaveInfo of successor replica
	 */
	public SlaveInfo findSuccessor(SlaveInfo firstReplica) {
		registeredSlavesLock.lock();
		Map.Entry<Long, SlaveInfo> entry = registeredSlaves.higherEntry(firstReplica.slaveID);
		SlaveInfo slave;
		if (entry == null) {
			slave = registeredSlaves.firstEntry().getValue();
		} else {
			slave = entry.getValue();
		}
		registeredSlavesLock.unlock();
		return slave;
	}

	/**
	 * Synchronized method to perform 2PC operations. This method contains the bulk of the two-phase commit logic.
	 * It performs phase 1 and phase 2 with appropriate timeouts and retries.
	 * See the spec for details on the expected behavior.
	 * 
	 * @param msg
	 * @param isPutReq
	 *            boolean to distinguish put and del requests
	 * @throws KVException
	 *             if the operation cannot be carried out
	 */
	public synchronized void performTPCOperation(KVMessage msg, boolean isPutReq) throws KVException {
		AutoGrader.agPerformTPCOperationStarted(isPutReq);
		WriteLock keyLock = null;
		String id = getNextTpcOpId();
		try {
			
			String key = msg.getKey();
			
			keyLock = masterCache.getWriteLock(key);
			
			keyLock.lock();
			
			/*for(Entry<Long, SlaveInfo> slaveServer :  registeredSlaves.entrySet()) {
				System.out.println("Registered Slaves = " + slaveServer.getValue().slaveID);
			}*/
			
			SlaveInfo firstServer = findFirstReplica(key);
			System.out.println("Find Successor = " + findSuccessor(firstServer).port);
			SlaveInfo secondServer = findSuccessor(firstServer);
			Socket firstSocket = null;
			Socket secondSocket = null;
			boolean isFirstSuccess = false;
			boolean isSecondSuccess = false;
			try {
				firstSocket = firstServer.connectHost();
				isFirstSuccess = true;
			} catch (KVException e) {
				//
			}
			try {
				secondSocket = secondServer.connectHost();
				isSecondSuccess = true;
			} catch (KVException e) {
				//
			}
			System.out.println("isFirstSuccess = " + isFirstSuccess + " isSecondSuccess " + isSecondSuccess);
			/** Does connectHost fail? If one fails, we must send ABORT to the other one and continue
			 *  1. Both succeed
			 *  2. #1 Success
			 *  3. #2 Success
			 *  4. Both fail
			 *  
			 *  If anything else happens besides a successful COMMIT, I throw an error that gets propagated to KVClientHandler.
			 */
			String type = setType(isPutReq);
			if (isFirstSuccess && isSecondSuccess) {
				sendRequestPhase1(type, firstSocket, secondSocket, msg, id);
				KVMessage first = readResponsePhase1(firstServer, firstSocket);
				KVMessage second = readResponsePhase1(secondServer, secondSocket);
				KVMessage decide = decidePhase2Response(first, second);

				decide.setTpcOpId(id);

				phase2SendRequest(firstServer, firstSocket, decide, true, key);
				phase2SendRequest(secondServer, secondSocket, decide, false, key);
				phase2CacheResponse(type, msg);
				
				if (decide.getMsgType().equals(ABORTED)) {
					throw buildKVException("Connected to both, but ABORTED");
				}
			} else if (isFirstSuccess) {
				System.out.println("Recognized first success");
				sendRequestPhase1Failure(type, firstSocket, msg, id);
				KVMessage abortDeciscion = new KVMessage(ABORTED);
				
				abortDeciscion.setTpcOpId(id);
				
				phase2SendRequest(firstServer, firstSocket, abortDeciscion, true, key);
				phase2CacheResponse(type, msg);
				
				throw buildKVException(secondServer.slaveID + ": Connection failed. " + firstServer.slaveID + ": Connection succeeded.");
			} else if(isSecondSuccess) {
				sendRequestPhase1Failure(type, secondSocket, msg, id);
				KVMessage abortDeciscion = new KVMessage(ABORTED);
				
				abortDeciscion.setTpcOpId(id);
				
				phase2SendRequest(secondServer, secondSocket, abortDeciscion, false, key);
				phase2CacheResponse(type, msg);
				
				throw buildKVException(firstServer.slaveID + ": Connection failed. " + secondServer.slaveID + ": Connection succeeded.");
			} else {
				throw buildKVException("Failed connected to both slaves." + firstServer.slaveID + ": Connection failed. " + secondServer.slaveID + ": Connection failed.");
			}
		} catch (KVException e) {
			throw e;
		} finally {
			keyLock.unlock();
			AutoGrader.agPerformTPCOperationFinished(isPutReq);
		}
		return;
	}

	public static void turnOnIgnoreNext(){
		ignoreNextMessage = true;
	}
	
	public static void turnOffIgnoreNext(){
		ignoreNextMessage = false;
	}
	
	private String setType(Boolean isPutReq) {
		if (ignoreNextMessage){
			return IGNORE_NEXT;
		}
		
		if (isPutReq)
			return PUT_MESSAGE; // make constant
		return DEL_MESSAGE;
	}

	private void sendRequestPhase1(String type, Socket firstSocket, Socket secondSocket, KVMessage msg, String id) throws KVException {
		KVMessage req = new KVMessage(type);
		req.setKey(msg.getKey());
		if (type.equals(PUT_MESSAGE)) {
			req.setValue(msg.getValue());
		}
		req.setTpcOpId(id);
		req.sendMessage(firstSocket);
		req.sendMessage(secondSocket);
	}
	
	private void sendRequestPhase1Failure(String type, Socket successSocket, KVMessage msg, String id) throws KVException {
		KVMessage req = new KVMessage(type);
		req.setKey(msg.getKey());
		if (type.equals(PUT_MESSAGE)) {
			req.setValue(msg.getValue());
		}
		req.setTpcOpId(id);
		req.sendMessage(successSocket);
	}

	private KVMessage readResponsePhase1(SlaveInfo server, Socket socket) throws KVException {
		KVMessage msg = null;
		try {
			msg = new KVMessage(socket, TIMEOUT_MILLISECONDS);
		} catch (KVException exception) {
			msg = new KVMessage(ABORTED, "Timeout Error: Could not receive data");
		} finally {
			server.closeHost(socket);
		}
		return msg;
	}


	private KVMessage decidePhase2Response(KVMessage first, KVMessage second) throws KVException {
		String decision = null;
		if (first.getMsgType().equals(READY) && second.getMsgType().equals(READY)) {
			decision = COMMIT;
		} else {
			decision = ABORTED;
		}
		return new KVMessage(decision);
	}

	private void phase2SendRequest(SlaveInfo server, Socket socket, KVMessage decide, boolean firstServer, String key) throws KVException {
		try {
			socket = server.connectHost();
			decide.sendMessage(socket);
			KVMessage msg = new KVMessage(socket, TIMEOUT_MILLISECONDS);
		} catch (KVException exception) {
			if (exception.getMsg().getMessage().equals("Timeout Error: Could not receive data") || exception.getMsg().getMessage().equals("Setting timeout on socket exception")) // placeholder
			{
				phase2Retry(key, decide, firstServer);
			}
		} finally {
			server.closeHost(socket);
		}
	}

	private void phase2Retry(String key, KVMessage decide, boolean firstServer) throws KVException {
		KVMessage msg = null;
		SlaveInfo server1 = null;
		SlaveInfo server2 = null;
		Socket socket = null;
		if (firstServer) {
			while (msg == null) {
				try {
					server1 = findFirstReplica(key);
					socket = server1.connectHost();
					decide.sendMessage(socket);
					msg = new KVMessage(socket, TIMEOUT_MILLISECONDS);
				} catch (KVException exception) {
					if (!exception.getMsg().getMessage().equals("Timeout Error: Could not receive data") || exception.getMsg().getMessage().equals("Setting timeout on socket exception")) {
						throw exception;
					}
				} finally {
					server1.closeHost(socket);
				}
			}
		} else {
			while (msg == null) {
				try {
					server1 = findFirstReplica(key);
					server2 = findSuccessor(server1);
					socket = server2.connectHost();
					decide.sendMessage(socket);
					msg = new KVMessage(socket, TIMEOUT_MILLISECONDS);
				} catch (KVException exception) {
					if (!exception.getMsg().getMessage().equals("Timeout Error: Could not receive data")) {
						throw exception;
					}
				} finally {
					server2.closeHost(socket);
				}
			}
		}
	}

	private void phase2CacheResponse(String type, KVMessage msg) {
		if (type.equals(PUT_MESSAGE)) {
			masterCache.put(msg.getKey(), msg.getValue());
		} else if (type.equals(DEL_MESSAGE)) {
			masterCache.del(msg.getKey());
		}
	}

	/**
	 * Put some useful comments here asshole
	 */
	public void handleIgnoreNext(KVMessage msg) throws KVException {
		KVMessage resp = null;
		for(Entry<Long, SlaveInfo> slaveServer :  registeredSlaves.entrySet()) {
			try {
				Socket firstSocket = slaveServer.getValue().connectHost();
				msg.sendMessage(firstSocket);
				resp = new KVMessage(firstSocket, TIMEOUT_MILLISECONDS);
				slaveServer.getValue().closeHost(firstSocket);
			}
			catch (KVException e) {
				KVMessage combinedErrorMessage = new KVMessage(RESP_MESSAGE, e.getMessage() + "\n" + e.getMessage());
				throw new KVException(combinedErrorMessage);
			}
		}
	}
	
	/**
	 * Perform GET operation in the following manner:
	 * - Try to GET from cache, return immediately if found
	 * - Try to GET from first/primary replica
	 * - If primary succeeded, return value
	 * - If primary failed, try to GET from the other replica
	 * - If secondary succeeded, return value
	 * - If secondary failed, return KVExceptions from both replicas Please see spec for more details.
	 * 
	 * @param msg
	 *            Message containing Key to get
	 * @return Value corresponding to the Key
	 * @throws KVException
	 */
	public String handleGet(KVMessage msg) throws KVException {
		AutoGrader.aghandleGetStarted();
		String key = msg.getKey();
		WriteLock keyLock = masterCache.getWriteLock(key);
		keyLock.lock();
		String value = masterCache.get(key);
		if (value != null) {
			keyLock.unlock();
			AutoGrader.aghandleGetFinished();
			return value;
		}
		SlaveInfo firstServer = findFirstReplica(key);
		SlaveInfo secondServer = findSuccessor(firstServer);
		KVMessage resp = null;
		Socket firstSocket = null;
		Socket secondSocket = null;
		while (resp == null) {
			try {
				firstSocket = firstServer.connectHost();
				msg.sendMessage(firstSocket);
				resp = new KVMessage(firstSocket, TIMEOUT_MILLISECONDS);
				firstServer.closeHost(firstSocket);
				// what if the server had shut off???
			} catch (KVException e1) {
				try {
					secondSocket = secondServer.connectHost();
					msg.sendMessage(secondSocket);
					resp = new KVMessage(secondSocket, TIMEOUT_MILLISECONDS);
					secondServer.closeHost(secondSocket);
				} catch (KVException e2) {
					/**
					 * Spec: "Multiple error messages in case of an abort should be placed in the same Message field 
					 * of a"resp" message prefixed by @SlaveServerID:= and separated by the newline character '\n'.
					 * These will be created by TPCMaster and returned to the client if multiple slaves return error messages.
					 * Use this as you deem necessary." It's never really supposed to happen though. -George
					 */
					throw buildKVException(firstServer.slaveID+":= "+e1.getMessage() + "\n" + secondServer.slaveID+":= "+e2.getMessage());
				}
			}
		}
		
		value = resp.getValue();
		masterCache.put(key, value);
		keyLock.unlock();
		AutoGrader.aghandleGetFinished();
		return value;
	}
	
	private KVException buildKVException(String errorMessage) throws KVException {
		KVException exception = new KVException(new KVMessage(RESP_MESSAGE, errorMessage));
		return exception;
	}

	/**
	 * Implements NetworkHandler to handle registration requests from SlaveServers.
	 * 
	 */
	public class TPCRegistrationHandler implements NetworkHandler {

		public ThreadPool threadpool = null;

		public TPCRegistrationHandler() {
			// Call the other constructor
			this(1);
		}

		public TPCRegistrationHandler(int connections) {
			System.out.println("TPCRegistrationHanlder is called");
			threadpool = new ThreadPool(connections);
		}

		@Override
		public void handle(Socket client) throws IOException {
			try {
				System.out.println("In the handle method ");
				threadpool.addToQueue(new RegistrationHandler(client));
			} catch (InterruptedException e) {
				return;
			}
		}

		public class RegistrationHandler implements Runnable {

			public Socket client = null;

			public RegistrationHandler(Socket client) {
				this.client = client;
			}

			@Override
			public void run() {
				KVMessage requestMsg;
				try {
					System.out.println("Before return statement in RegistrationHandler");
					requestMsg = new KVMessage(client);
					KVMessage responseMsg;
					registeredSlavesLock.lock(); 
					if (!requestMsg.getMsgType().equals("register")) {
						responseMsg = new KVMessage("resp", "Unknown Error: Not a register message.");
						responseMsg.sendMessage(client);
						return;
					}
					SlaveInfo newSlave = new SlaveInfo(requestMsg.getMessage()); 
					boolean found = false;
					SlaveInfo foundSlave = null;
					for(Entry<Long, SlaveInfo> slaveServer :  registeredSlaves.entrySet()) {
						if (slaveServer.getValue().slaveID == newSlave.slaveID){
							found = true;
							foundSlave = slaveServer.getValue();
						}
					}
					
					
					if (found) {
						System.out.println(" SHOULD GET here");
						//SlaveInfo existingSlave = registeredSlaves.get(newSlave.slaveID);
						SlaveInfo existingSlave = foundSlave;
						existingSlave.hostName = newSlave.hostName;
						existingSlave.port = newSlave.port;
					} else if (registeredSlaves.size() == numSlaves){
						responseMsg = new KVMessage("resp", "Unknown Error: Max amount of slaves already registered");
					} else {
						registeredSlaves.put(newSlave.slaveID, newSlave);
					}

					responseMsg = new KVMessage("resp", "Successfully registered " + newSlave.slaveID + "@" + newSlave.hostName + ":" + newSlave.port);
					responseMsg.sendMessage(client);

				} catch (KVException e) {
					try {
						e.getMsg().sendMessage(client);
					} catch (KVException e1) {
						try {
							client.close();
						} catch (IOException e2) {

						}
					}
				} finally {
					registeredSlavesLock.unlock();
				}
			}
		}
	}

	/**
	 * Data structure to maintain information about SlaveServers
	 * 
	 */
	public class SlaveInfo {
		// 64-bit globally unique ID of the SlaveServer
		public long slaveID = -1;
		// Name of the host this SlaveServer is running on
		public String hostName = null;
		// Port which SlaveServer is listening to
		public int port = -1;

		/**
		 * 
		 * @param slaveInfo
		 *            as "SlaveServerID@HostName:Port"
		 * @throws KVException
		 */
		public SlaveInfo(String slaveInfo) throws KVException {
			Pattern slaveInfoPattern = Pattern.compile("(-?\\d+)@(.+):(-?\\d+)"); 
			Matcher m = slaveInfoPattern.matcher(slaveInfo);
			if (!m.matches()) {
				throw new KVException(new KVMessage("resp", "Unknown Error: Unparseable SlaveInfo string"));
			}
			slaveID = Long.parseLong(m.group(1));
			hostName = m.group(2);
			port = Integer.parseInt(m.group(3));

		}

		public long getSlaveID() {
			
			return slaveID;
		}

		public Socket connectHost() throws KVException {
			try {
				Socket soc = new Socket();
				soc.setSoTimeout(TIMEOUT_MILLISECONDS);
				soc.connect(new InetSocketAddress(this.hostName, this.port), TIMEOUT_MILLISECONDS);
				return soc;
			} catch (UnknownHostException e) {
				throw new KVException(new KVMessage("resp", "Unknown Network Error: Unable to connect to Host"));
			} catch (IOException e) {
				throw new KVException(new KVMessage("resp", "Unknown Network Error: Unable to create socket"));
			}
		}

		public void closeHost(Socket sock) throws KVException {
			try {
				sock.close();
			} catch (IOException e) {
				throw new KVException(new KVMessage("resp", "Unknown Error: Unable to close socket"));
			}
		}
	}
}
