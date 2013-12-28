package test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import org.hamcrest.core.Is;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import edu.berkeley.cs162.KVCache;
import edu.berkeley.cs162.KVCache.Entry;
import edu.berkeley.cs162.KVCache.setClass;
import edu.berkeley.cs162.KVClient;
import edu.berkeley.cs162.KVClientHandler;
import edu.berkeley.cs162.KVException;
import edu.berkeley.cs162.KVMessage;
import edu.berkeley.cs162.KVServer;
import edu.berkeley.cs162.KVStore;
import edu.berkeley.cs162.NetworkHandler;
import edu.berkeley.cs162.SocketServer;
import edu.berkeley.cs162.ThreadPool;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;

public class TestTemplate {

 // Sample, instantiate whatever you need here
 KVClient k;
 KVCache cache;
 KVServer key_server = null;
 SocketServer server = null;
    
 private final String GET_MESSAGE = "getreq";
 private final String PUT_MESSAGE = "putreq";
 private final String DEL_MESSAGE = "delreq";
 private final String RESP_MESSAGE = "resp";
 ReentrantReadWriteLock rw = new ReentrantReadWriteLock();
 public WriteLock testLock = rw.writeLock();
 public static int threadcounter = 0;
 
 @Before
    public void setUp() {
  // This will run before every test, not just once for the file!!!!
  k = new KVClient(null, 0);
  cache = new KVCache(10, 10);
  
    }
 
 @Test
 public void simpleTest() {
  // put some condition where it says "true" to do the actual
  // testing of some functionality
  assertTrue("Testing simpleMethod", true);
 }
 
 
    
 @Test
 public void task3SimplePutTest(){
  cache.put("Dummy", "69");
  setClass set = cache.getSetByKey("Dummy");
  Entry entry = set.list.peek();
  String value = entry.value;
  assertEquals("Value in set should be 69", "69", value);
 }
 
 @Test
 public void task3SimpleGetTest(){
  cache.put("Dummy", "69");
  String value = cache.get("Dummy");
  assertEquals("Value in set should be 69", "69", value);
 }
 
 @Test
 public void task3SimpleDelTest(){
  cache.put("Dummy", "69");
  setClass set = cache.getSetByKey("Dummy");
  cache.del("Dummy");
  Entry head = set.list.peek();
  assertEquals("Value in set should be null", null, head);
 }
 
 @Test
 public void task3EvictionPolictyTest1(){
  cache = new KVCache(1, 5);
  cache.put("d1", "1");
  setClass set = cache.getSetByKey("d1");
  cache.put("d2", "2");
  cache.put("d3", "3");
  cache.put("d4", "4");
  cache.put("d5", "5");
  cache.get("d1");
  assertEquals("D1 accessed", set.list.get(0).accessed, true);
  cache.put("d6", "6");
  boolean d2vacant = true;
  for (int i = 0; i < set.list.size(); i++){
   if (set.list.get(i).value == "2")
    d2vacant = false;
   //System.out.println("Cache at index " + i + " = " + set.list.get(i).value);
  }
  assertTrue("D2 is evicted", d2vacant);
 }
 
 @Test
 public void task3EvictionPolictyTest3(){
  cache = new KVCache(1, 5);
  cache.put("d1", "1");
  setClass set = cache.getSetByKey("d1");
  cache.put("d2", "2");
  cache.del("d1");
  
  cache.put("d4", "4");
  cache.put("d5", "5");
  cache.get("d2");
  checkSet(set);
  System.out.println("Value of d5= " + cache.get("d5"));
  assertEquals("D5 Value",  cache.get("d5"), "5");
  
 }
 
 public void checkSet(setClass set){
	 for (Entry e: set.list)
		 System.out.println("Key= " + e.key + " Value= " + e.value + " Referenced= " + e.accessed);
 }
 
 
 @Test
 public void tast3EvictionPolicyTest2(){
  cache = new KVCache(1, 5);
  cache.put("d1", "1");
  setClass set = cache.getSetByKey("d1");
  cache.put("d2", "2");
  cache.put("d3", "3");
  cache.put("d4", "4");
  cache.put("d5", "5");
  cache.get("d1");
  cache.get("d2");
  cache.get("d3");
  cache.get("d4");
  cache.get("d5");
  
  assertEquals("D1 accessed", set.list.get(0).accessed, true);
  assertEquals("D2 accessed", set.list.get(1).accessed, true);
  assertEquals("D3 accessed", set.list.get(2).accessed, true);
  assertEquals("D4 accessed", set.list.get(3).accessed, true);
  assertEquals("D5 accessed", set.list.get(4).accessed, true);
  
  
  String head = set.list.peek().value;
  cache.put("d6", "6");
  
  String newhead = set.list.peek().value;
  /*for (int i = 0; i < set.list.size(); i++){
   System.out.println("Cache at index " + i + " = " + set.list.get(i).value);
  }*/
  assertEquals("Head should be changed since all were accessed","2" , newhead);
 }
 /*
 @Test
 public void kvclientTest() {
   try
   {
     ThreadPool pool = new ThreadPool(2);
     pool.addToQueue(new ThreadServer()); //see bottom of code
     pool.addToQueue(new ThreadTest());
   }
   catch(Exception exception)
   {
     assertTrue("kvclient test exception", false);
   }
 }    
     @Test
 public void threadpoolTest() {
       try
       {
         ThreadPool pool = new ThreadPool(2);
         for(int i = 0; i < 10; i++)
         {
           pool.addToQueue(new Runnable(){
             public void run(){
               testLock.lock();
               threadcounter++;
               //System.out.println(threadcounter);
               testLock.unlock();
             }});
         }
         assertTrue("ThreadPool threadcounter test", true);
       }
       catch(Exception exception)
       {
         assertTrue("ThreadPool test exception", false);
       }
 }
    
    @Test
    public void task1ConstructGETKVMessage() throws KVException{
        KVMessage message = new KVMessage(GET_MESSAGE);
        assertEquals(message.getMsgType(), GET_MESSAGE);
    }
    
    @Test
    public void task1ConstructPUTKVMessage() throws KVException{
        KVMessage message = new KVMessage(PUT_MESSAGE);
        assertEquals(message.getMsgType(), PUT_MESSAGE);
    }
    
    @Test
    public void task1ConstructDELKVMessage() throws KVException{
        KVMessage message = new KVMessage(DEL_MESSAGE);
        assertEquals(message.getMsgType(), DEL_MESSAGE);
    }
    
    @Test
    public void task1ConstructRESPKVMessage() throws KVException{
        KVMessage message = new KVMessage(RESP_MESSAGE);
        assertEquals(message.getMsgType(), RESP_MESSAGE);
    }
    
    @Test
    public void task1ConstructGetWithMessageKVMessage() throws KVException{
        KVMessage message = new KVMessage(GET_MESSAGE, "message");
        assertEquals(message.getMsgType(), GET_MESSAGE);
        assertEquals(message.getMessage(), "message");
    }
    
    @Test
    public void task1ConstructPutWithMessageKVMessage() throws KVException{
        KVMessage message = new KVMessage(PUT_MESSAGE, "message");
        assertEquals(message.getMsgType(), PUT_MESSAGE);
        assertEquals(message.getMessage(), "message");
    }
    
    @Test
    public void task1ConstructDelWithMessageKVMessage() throws KVException{
        KVMessage message = new KVMessage(DEL_MESSAGE, "message");
        assertEquals(message.getMsgType(), DEL_MESSAGE);
        assertEquals(message.getMessage(), "message");
    }
    
    @Test
    public void task1ConstructRespWithMessageKVMessage() throws KVException{
        KVMessage message = new KVMessage(RESP_MESSAGE, "message");
        assertEquals(message.getMsgType(), RESP_MESSAGE);
        assertEquals(message.getMessage(), "message");
    }
 
    @Test
    public void task1ToXmlTest1() throws KVException{
     KVMessage message = new KVMessage(GET_MESSAGE);
     message.setKey("keyValue");
     String xmlString = message.toXML();
     String expectedResponse = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<KVMessage type=\"getreq\"><Key>keyValue</Key></KVMessage>";
     assertEquals(expectedResponse, xmlString);
    }
    
    @Test
    public void task1ToXmlTest2() throws KVException{
     KVMessage message = new KVMessage(DEL_MESSAGE);
     message.setKey("keyValue");
     String xmlString = message.toXML();
     String expectedResponse = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<KVMessage type=\"delreq\"><Key>keyValue</Key></KVMessage>";
     assertEquals(expectedResponse, xmlString);
    }
    
    @Test
    public void task1ToXmlTest3() throws KVException{
     KVMessage message = new KVMessage(PUT_MESSAGE);
     message.setKey("keyValue");
     message.setValue("ValueString");
     String xmlString = message.toXML();
     String expectedResponse = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<KVMessage type=\"putreq\"><Key>keyValue</Key><Value>ValueString</Value></KVMessage>";
     assertEquals(expectedResponse, xmlString);
    }
    @Test
    public void task4ToXmlTest1() throws Exception{
     KVStore store = new KVStore();
     store.put("test1", "test2");
     String xmlString = store.toXML();
     String expectedResponse = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<KVStore><KVPair><Key>test1</Key><Value>test2</Value></KVPair></KVStore>";
     assertEquals(expectedResponse, xmlString);
    }
    
    @Test
    public void task4ToXmlTest2() throws Exception{
     KVStore store = new KVStore();
     store.put("test1", "test2");
     store.put("key2", "val2");
     store.put("key3", "val3");
     String xmlString = store.toXML();
     String expectedResponse = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<KVStore><KVPair><Key>key3</Key><Value>val3</Value></KVPair><KVPair><Key>test1</Key><Value>test2</Value></KVPair><KVPair><Key>key2</Key><Value>val2</Value></KVPair></KVStore>";
     assertEquals(expectedResponse, xmlString);
    }
    
    @Test
    public void task4ToXmlTest3() throws Exception{
     KVStore store = new KVStore();
     store.put("test1", "test2");
     store.put("key2", "val2");
     store.put("key3", "val3");
     store.dumpToFile("testFile");
     FileReader f = new FileReader("testFile");
     
     char[] read = new char[1000];
     f.read(read);
     String xmlString = new String (read);
     xmlString = xmlString.trim();
     f.close();
     String expectedResponse = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<KVStore><KVPair><Key>key3</Key><Value>val3</Value></KVPair><KVPair><Key>test1</Key><Value>test2</Value></KVPair><KVPair><Key>key2</Key><Value>val2</Value></KVPair></KVStore>";
     assertEquals(expectedResponse, xmlString);
    }
    
    @Test
    public void task4ToXmlTest4() throws Exception{
     KVStore store = new KVStore();
     store.put("test1", "test2");
     store.put("key2", "val2");
     store.put("key3", "val3");
     store.dumpToFile("testFile");
     store.put("key4", "val4");
     store.restoreFromFile("testFile");
     
     String xmlString = store.toXML();
     String expectedResponse = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<KVStore><KVPair><Key>key3</Key><Value>val3</Value></KVPair><KVPair><Key>test1</Key><Value>test2</Value></KVPair><KVPair><Key>key2</Key><Value>val2</Value></KVPair></KVStore>";
     assertEquals(expectedResponse, xmlString);
    }
 
    */
    @Test
    public void task4ToXmlTest5() throws Exception{
     KVCache cache = new KVCache(5, 10);
     cache.put("test1", "test2");
     cache.put("key2", "val3");
     cache.put("key2", "val2");
     cache.put("key3", "val3");
     cache.put("key4", "val4");
     
     String xmlString = cache.toXML();
     String expectedResponse = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<KVCache><Set Id=\"0\"><CacheEntry isReferenced=\"false\" isValid=\"true\"><Key>key3</Key><Value>val3</Value></CacheEntry></Set><Set Id=\"1\"><CacheEntry isReferenced=\"false\" isValid=\"true\"><Key>key4</Key><Value>val4</Value></CacheEntry></Set><Set Id=\"2\"><CacheEntry isReferenced=\"false\" isValid=\"true\"><Key>test1</Key><Value>test2</Value></CacheEntry></Set><Set Id=\"3\"/><Set Id=\"4\"><CacheEntry isReferenced=\"false\" isValid=\"true\"><Key>key2</Key><Value>val2</Value></CacheEntry></Set></KVCache>";
     System.out.println("expected Response = " + xmlString);
     
     assertEquals(expectedResponse, xmlString);
    }
    /*
 @Test
 public void Task6KVServerGetNothing() {
  KVServer server = new KVServer(5, 10);
  boolean isError = false;
  try {
   server.get("nothing");
  } catch (Exception e){
   isError = true;
  }
  assertTrue("Call GET on empty key, should error.", isError);
 }
 
 @Test
 public void Task6KVServerDelNothing() {
  KVServer server = new KVServer(5, 10);
  boolean isError = false;
  try {
   server.del("nothing");
  } catch (Exception e){
   isError = true;
  }
  assertTrue("Call DEL on empty key, should error.", isError);
 }
 
 @Test
 public void Task6KVServerMaxLength() {
  KVServer server = new KVServer(5, 10);
  boolean isErrorKey = false;
  boolean isErrorValue = false;
     
     String hundredWord = "vvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvvv";
     String largeKey = hundredWord + hundredWord + hundredWord;
     try {
   server.put(largeKey, "Irrelevant");
  } catch (KVException e) {
   isErrorKey = true;
  }
     String largeValue = hundredWord;
     for (int i = 0; i < 14; i++) {
      largeValue = largeValue + largeValue;
     }
     try {
   server.put("Irrelevant", largeValue);
  } catch (KVException e) {
   isErrorValue = true;
  }
     assertTrue("Call GET on large key, should error.", isErrorKey && isErrorValue);
 }
 
 @Test
 public void Task6KVServerPUTOverwrite() {
  KVServer server = new KVServer(5, 10);
  try {
   server.put("2", "200");
   server.put("2", "83");
   String value = server.get("2");
   assertTrue("Two PUTs on same key, should overwrite value with 83.", value == "83");
  } catch (KVException e){
   //System.out.println(e);
   assertTrue("Two PUTs on same key, should overwrite value with 83.", false);
  }
 }
 
 @Test
 public void Task6KVServerGeneral() {
  KVServer server = new KVServer(5, 10);
  try {
   server.put("1", "100");
   String value1 = server.get("1");
   server.put("2", "200");
   String value2 = server.get("2");
   server.put("2", "87");
   String valueOver = server.get("2");
   server.del("2");
   server.del("1");
   
   assertTrue("Series of PUTs and GETs with overwriting, check for correct values at correct times.", value1=="100" && value2=="200" && valueOver=="87");
  } catch (Exception e){
   assertTrue("Series of PUTs and GETs with overwriting, check for correct values at correct times.", false);
  }
 }

    /**
     * Tears down the test fixture. 
     * (Called after every test case method.)
     */
    @After
    public void tearDown() {
  // This will run after every test, not just once for the file!!!!
    }
}

class ThreadServer implements Runnable {
  public void run()
      {
        try
        {
          /*KVServer key_server = new KVServer(100, 10);  
          SocketServer server = new SocketServer("localhost", 8080);
          NetworkHandler handler = new KVClientHandler(key_server);
          server.addHandler(handler);
          server.connect();    
          System.out.println("Starting Server");
          server.run();*/
        }
        catch(Exception exception)
        {
          System.out.println(exception.toString());
        }
      }
}

class ThreadTest implements Runnable {
        public void run()
      {
        try
        {
          KVClient client = new KVClient("localhost", 8080);
          client.put("foo", "bar");
          assertTrue("put/get test", client.get("foo").equals("bar"));
          client.put("foo", "garple");
          assertTrue("put/put test", client.get("foo").equals("garple"));
          client.del("foo");
          client.get("foo");
        }
        catch(KVException exception)
        {
          try
          {
            assertTrue("KVClient test.", exception.getMsg().getMessage().equals("Does not exist"));
          }
          catch(Exception kvexception)
          {
            assertTrue(kvexception.toString(), false);
          }
        }
        catch(Exception exception)
        {
          assertTrue(exception.toString(), false);
        }
      }
}

