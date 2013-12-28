package edu.berkeley.cs162;

import java.io.IOException;

import edu.berkeley.cs162.KVClientHandler;
import edu.berkeley.cs162.KVServer;
import edu.berkeley.cs162.NetworkHandler;
import edu.berkeley.cs162.SocketServer;

public class TestServer {
    static KVServer key_server = null;
    static SocketServer server = null;

    /**
     * @param args
     * @throws IOException
     * @throws KVException 
     */
    public static void main(String[] args) throws IOException {
        /*System.out.println("Binding Server:");
        key_server = new KVServer(100, 10);
        server = new SocketServer("localhost", 8080);
        NetworkHandler handler = new KVClientHandler(key_server);
        server.addHandler(handler);
        server.connect();
        System.out.println("Starting Server");
        server.run();*/
    }

}
