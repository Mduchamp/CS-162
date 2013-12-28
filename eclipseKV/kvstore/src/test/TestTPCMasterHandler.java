package test;

import static org.junit.Assert.assertTrue;

import java.net.InetAddress;

import org.junit.Before;
import org.junit.Test;

import edu.berkeley.cs162.KVCache;
import edu.berkeley.cs162.KVClient;
import edu.berkeley.cs162.KVServer;
import edu.berkeley.cs162.SocketServer;
import edu.berkeley.cs162.TPCLog;
import edu.berkeley.cs162.TPCMasterHandler;

public class TestTPCMasterHandler {
    static String logPath = null;
    static TPCLog tpcLog = null;

    static KVServer keyServer = null;
    static SocketServer server = null;

    static long slaveID = -1;
    static String masterHostName = null;
    static int masterPort = 8080;
    static int registrationPort = 9090;



@Before
public void setUp() {
// This will run before every test, not just once for the file!!!!
	 /*slaveID = Long.parseLong(args[0]);
     masterHostName = args[1];

     // Create TPCMasterHandler
     System.out.println("Binding SlaveServer:");
     keyServer = new KVServer(100, 10);
     server = new SocketServer(InetAddress.getLocalHost().getHostAddress());
     TPCMasterHandler handler = new TPCMasterHandler(keyServer, slaveID);
     server.addHandler(handler);
     server.connect();

     // Create TPCLog
     logPath = slaveID + "@" + server.getHostname();
     tpcLog = new TPCLog(logPath, keyServer);

     // Load from disk and rebuild logs
     tpcLog.rebuildKeyServer();

     // Set log for TPCMasterHandler
     handler.setTPCLog(tpcLog);

     // Register with the Master. Assuming it always succeeds (not catching).
     handler.registerWithMaster(masterHostName, server);

     System.out.println("Starting SlaveServer at " + server.getHostname() + ":" + server.getPort());
     server.run();*/

}

@Test
public void simpleTest() {
// put some condition where it says "true" to do the actual
// testing of some functionality
assertTrue("Testing simpleMethod", true);
}
}
