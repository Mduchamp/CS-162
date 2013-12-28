package edu.berkeley.cs162;


import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSOutput;
import org.w3c.dom.ls.LSSerializer;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import edu.berkeley.cs162.KVClient;

public class TestClient {
    /**
     * @param args
     * @throws IOException
     */
	private final static String GET_MESSAGE = "getreq";
	private final static String PUT_MESSAGE = "putreq";
	private final static String DEL_MESSAGE = "delreq";
	private final static String RESP_MESSAGE = "resp";
	
    public static void main(String[] args) throws IOException {
        KVClient kc = new KVClient("localhost", 8080);
        try {
            System.out.println("PUT(3, 7)");
            kc.put("3", "7");
            
            System.out.println("PUT(hello, goodbye)");
            kc.put("hello", "goodboye");

            System.out.println("PUT(3, 8)");
            kc.put("3", "8");

            System.out.println("GET(3)");
            String value = kc.get("3");
            System.out.println("Should be 8: " + value);
            
            System.out.println("GET(hello)");
            value = kc.get("hello");
            System.out.println("Should be goodbye: " + value);
            
            kc.del("hello");
            kc.del("3");
        } catch (Exception e){
            e.printStackTrace();
        }
    }
}
