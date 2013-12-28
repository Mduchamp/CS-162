package test;

import static org.junit.Assert.*;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import edu.berkeley.cs162.*;

public class TestTPCLog {
	
	public TPCLog tpcLog = null;
    public String logPath = null;
    public KVServer server = null;
    public SocketServer serverSocket = null;
    public KVClient client = null;
    public ArrayList<KVMessage> entries = null;
    public static long slaveId = -1;
    
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
	
	@Before
	public void setUp() throws IOException, KVException {
		//set up code for testing
		server = new KVServer(100, 10);
		serverSocket = new SocketServer(InetAddress.getLocalHost().getHostAddress());
		TPCMasterHandler handler = new TPCMasterHandler(server, slaveId);
        serverSocket.addHandler(handler);
        serverSocket.connect();
		logPath = slaveId + "@" + serverSocket.getHostname() + "8";
		tpcLog = new TPCLog(logPath, server);
		tpcLog.rebuildKeyServer();
		handler.setTPCLog(tpcLog);
		handler.registerWithMaster("localhost", serverSocket);
		serverSocket.run();
	}
	
	@After
	public void tearDown() {
		//tear down code for testing
	}
	
	@Test
	public void tpcTesting() throws KVException {

		KVMessage entry1 = new KVMessage(GET_MESSAGE);
		entry1.setKey("keyA");
		KVMessage entry2 = new KVMessage(GET_MESSAGE);
		entry2.setKey("keyB");
		KVMessage entry3 = new KVMessage(GET_MESSAGE);
		entry3.setKey("keyC");

		tpcLog.loadFromDisk();
		tpcLog.appendAndFlush(entry1);		
		tpcLog.appendAndFlush(entry2);
		tpcLog.appendAndFlush(entry3);
		
		
		assertEquals(3, tpcLog.entries.size());
		assertEquals("keyA", tpcLog.entries.get(0).getKey());
		assertEquals("keyB", tpcLog.entries.get(1).getKey());
		assertEquals("keyC", tpcLog.entries.get(2).getKey());
		
		
		KVMessage entry4 = new KVMessage(PUT_MESSAGE);
		entry4.setKey("key1");
		entry4.setValue("value1");
		KVMessage entry5 = new KVMessage(PUT_MESSAGE);
		entry5.setKey("key2");
		entry5.setValue("value2");
		KVMessage entry6 = new KVMessage(PUT_MESSAGE);
		entry6.setKey("key3");
		entry6.setValue("value3");

		tpcLog.appendAndFlush(entry4);		
		tpcLog.appendAndFlush(entry5);
		tpcLog.appendAndFlush(entry6);		
		
		assertEquals(6, tpcLog.entries.size());
		assertEquals("key1", tpcLog.entries.get(3).getKey());
		assertEquals("value1", tpcLog.entries.get(3).getValue());
		assertEquals("key2", tpcLog.entries.get(4).getKey());
		assertEquals("value2", tpcLog.entries.get(4).getValue());
		assertEquals("key3", tpcLog.entries.get(5).getKey());
		assertEquals("value3", tpcLog.entries.get(5).getValue());
	}
}
