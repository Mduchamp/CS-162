package edu.berkeley.cs162;

import static org.junit.Assert.*;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class EndToEndRebuildKeyServer {

	private final Integer numSlaves = 2;

	private ServerRunner coordinatorRunner;

	private Map<String, ServerRunner> slaveRunners;
	private TPCMaster master;
	
	private TPCLog mrLog;

	@Before
	public void setUp() throws Exception {
		master = new TPCMaster(numSlaves);

		// Set up Coordinator Server
		SocketServer kvServer = new RobustSocketServer(
				InetAddress.getLocalHost().getHostAddress(), 8888);
		kvServer.addHandler(new KVClientHandler(master));

		coordinatorRunner = new ServerRunner(kvServer,
				"Coordinator Server",
				"Handles KVClient requests to the system.");
		coordinatorRunner.start();
		System.out.println("INFO EndToEnd.setUp: Coordinator server ready.");

		// Set up Registration Server
		System.out.print("INFO EndToEnd.setUp: Running registration server... ");
		master.run();
		System.out.println("ready.");
		Thread.sleep(3000);

		// Set up slaves
		slaveRunners = new HashMap<String, ServerRunner>();
		setUpSlave("Mr. Slave");
		setUpSlave("Mrs. Slave");
		Thread.sleep(3000);
	}

	private void setUpSlave(String name) throws UnknownHostException, IOException, KVException {
		System.out.format("INFO EndToEnd.setUp: Set up %s%n", name);
		SocketServer slave =
				new RobustSocketServer(InetAddress.getLocalHost().getHostAddress(), 0);
		long slaveId = hashTo64bit(name);
		KVServer slaveKvs = new KVServer(1, 3);
		TPCMasterHandler handler =
				new TPCMasterHandler(slaveKvs, slaveId);
		slave.addHandler(handler);


		// Create TPCLog
		String logPath = "slave" + slaveId + "@" + slave.getHostname();
		mrLog = new TPCLog(logPath, new KVServer(numSlaves, numSlaves));

		// Set log for TPCMasterHandler
		handler.setTPCLog(mrLog);
		
//		mrLog.rebuildKeyServer();

		ServerRunner slaveRunner = new ServerRunner(slave, name, "A Slave Server");
		slaveRunner.start();
		slaveRunners.put(name, slaveRunner);
	

		// Register with the Master. Assuming it always succeeds (not catching).
		handler.registerWithMaster(InetAddress.getLocalHost().getHostAddress(), slave);
	}

	@After
	public void tearDown() throws Exception {
		coordinatorRunner.stop();
		for (ServerRunner slaveRunner : slaveRunners.values()) {
			slaveRunner.stop();
		}
		master = null;
	}

	public static class ServerRunner implements Runnable {

		public static final int THREAD_STOP_TIMEOUT_MS = 1000 * 10; // Wait 10 seconds

		public ServerRunner(SocketServer socs, String name, String desc) {
			sockserver = socs;
			runnerName = name;
			runnerDesc = desc;
		}

		private final SocketServer sockserver;
		public final String runnerName;
		public final String runnerDesc;

		private Thread thread = null;
		private boolean isUp = false;

		@Override
		public void run() {
			try {
				sockserver.connect();
				System.out.format("Running %s...%n", runnerName);
				synchronized (this) {
					isUp = true;
					notifyAll();
				}
				sockserver.run();
				synchronized (this) {
					isUp = false;
					notifyAll();
				}
			} catch (Exception e) {
				System.out.println(String.format(
						"SERVER-SIDE: Error from %s", runnerName));
				e.printStackTrace();
			}
		}

		public void start() {
			if (thread == null) {
				thread = new Thread(this, runnerName);
				thread.setDaemon(true); // Allow JVM to exit if thread abandoned
				System.out.format("INFO ServerRunner.start: Starting %s: %s%n",
						runnerName, runnerDesc);
				thread.start();

				while (!isUp) {
					try {
						synchronized (this) {
							this.wait(100);
						}
					} catch (InterruptedException e) {}
				}
				System.out.format("INFO ServerRunner.start: %s is now up.%n", runnerName, runnerDesc);
			}
		}

		public void stop() {
			System.out.format("INFO ServerRunner: Stopping %s%n", runnerName);
			if (sockserver != null) {
				sockserver.stop();
			}
			if (thread != null) {
				try {
					thread.join(THREAD_STOP_TIMEOUT_MS);
				} catch (InterruptedException e) {
					System.out.format("ERROR ServerRunner: " +
							"Failed to stop Server (%s), giving up.%n", runnerName);
				}
			}
			isUp = false;
			thread = null;
		}

	}

	public class RobustSocketServer extends SocketServer {
		private boolean stopSocketServer;
		
		public RobustSocketServer(String hostname, int port) {
			super(hostname);
			this.hostname = hostname;
			this.port = port;
		}

		@Override
		public void connect() throws IOException {
			server = new ServerSocket(this.port);
			server.setReuseAddress(true);
			server.setSoTimeout(100); // Timeout after a while, instead of blocking forever
			if (this.port == 0) {
				this.port = server.getLocalPort();
			}
		}

		@Override
		public void run() throws IOException {
			while (!stopSocketServer) {
				try {
					Socket clientConn = server.accept();
					if (clientConn != null) {
						handler.handle(clientConn);
						// Don't close it here...it's queued for asynchronous handling!
					}
				} catch (SocketTimeoutException e) {
					// Do nothing, this is normal
				} catch (IOException e) {
					if (server.isClosed() || !server.isBound()) throw e;
				}
			}
			// Close the socket
			closeSocket();
		}

		@Override
		public void stop() {
			stopSocketServer = true;
		}

		public void closeSocket() {
			if (server.isClosed()) return;
			try {
				server.close();
			} catch (IOException e) {
				System.out.println("Could not close socket");
			}
		}
	}

	private static long hashTo64bit(String string) {
		// Take a large prime
		long h = 1125899906842597L;
		int len = string.length();

		for (int i = 0; i < len; i++) {
			h = 31*h + string.charAt(i);
		}
		return h;
	}
	
	/**
	 * Simple end-to-end put/get
	 * @throws InterruptedException
	 * @throws KVException
	 * @throws UnknownHostException 
	 */
	@Test(timeout = 100000)
	public void testPutGet() throws InterruptedException, KVException, UnknownHostException {
		System.out.println("INFO EndToEnd.testPutGet: Begin.");
		KVClient client = new KVClient(InetAddress.getLocalHost().getHostAddress(), 8888);

		//put in 4 keys
		try {
			client.put("key1", "value1");
			client.put("key2", "value2");
			client.put("key3", "value3");
			client.put("key4", "value4");
		} catch (Exception e) {
			e.printStackTrace();
			System.out.println("INFO EndToEnd.testPutGet: Finished.");
			fail("put failed");
		}
		
		//overwrite 4 put values
		try {
			client.put("key1", "value1new");
			client.put("key2", "value2new");
			client.put("key3", "value3new");
			client.put("key4", "value4new");
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		try {
			tearDown();
			master = new TPCMaster(numSlaves);

			// Set up Coordinator Server
			SocketServer kvServer = new RobustSocketServer(
					InetAddress.getLocalHost().getHostAddress(), 8080);
			kvServer.addHandler(new KVClientHandler(master));

			coordinatorRunner = new ServerRunner(kvServer,
					"Coordinator Server",
					"Handles KVClient requests to the system.");
			coordinatorRunner.start();
			System.out.println("INFO EndToEnd.setUp: Coordinator server ready.");

			// Set up Registration Server
			System.out.print("INFO EndToEnd.setUp: Running registration server... ");
			master.run();
			System.out.println("ready.");
			Thread.sleep(3000);

			// Set up slaves
			slaveRunners = new HashMap<String, ServerRunner>();
			setUpSlave("Mr. Slave");
			setUpSlave("Mrs. Slave");
			Thread.sleep(3000);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		mrLog.rebuildKeyServer();
		try {
			assertEquals("TEST 1new get failed", client.get("key1"), "value1new");
			assertEquals("TEST 2new get failed", client.get("key2"), "value2new");
			assertEquals("TEST 3new get failed", client.get("key3"), "value3new");
			assertEquals("TEST 4new get failed", client.get("key4"), "value4new");
		} catch (Exception e) {
			System.out.println("INFO EndToEnd.testPutGet: Finished.");
			e.printStackTrace();
		}
		
	}
}