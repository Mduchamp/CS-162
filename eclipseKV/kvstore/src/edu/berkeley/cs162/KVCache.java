/**
 * Implementation of a set-associative cache.
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

import java.io.StringWriter;
import java.io.Writer;
import java.util.LinkedList;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSOutput;
import org.w3c.dom.ls.LSSerializer;


/**
 * A set-associate cache which has a fixed maximum number of sets (numSets).
 * Each set has a maximum number of elements (MAX_ELEMS_PER_SET).
 * If a set is full and another entry is added, an entry is dropped based on the eviction policy.
 */
public class KVCache implements KeyValueInterface {
    private int numSets = 100;
    private int maxElemsPerSet = 10;
    private setClass[] sets;

    /**
     * Creates a new LRU cache.
     * @param cacheSize    the maximum number of entries that will be kept in this cache.
     */
    public KVCache(int numSets, int maxElemsPerSet) {
        this.numSets = numSets;
        this.maxElemsPerSet = maxElemsPerSet;
        sets = new setClass[this.numSets];
        for (int i = 0; i < sets.length; i++)
        	sets[i] = new setClass();
    }

    /**
     * Retrieves an entry from the cache.
     * Assumes the corresponding set has already been locked for writing.
     * @param key the key whose associated value is to be returned.
     * @return the value associated to this key, or null if no value with this key exists in the cache.
     */
    public String get(String key) {
        // Must be called before anything else
        AutoGrader.agCacheGetStarted(key);
        AutoGrader.agCacheGetDelay();

        String returnString = null;
        int setId = getSetId(key);
        LinkedList<Entry> set = sets[setId].list;
        for (Entry e : set){
        	if (key.equals(e.key)){
        		returnString = e.value;
        		e.accessed = true;
        	}		
        }
        // Must be called before returning
        AutoGrader.agCacheGetFinished(key);
        return returnString;
    }

    /**
     * Adds an entry to this cache.
     * If an entry with the specified key already exists in the cache, it is replaced by the new entry.
     * If the cache is full, an entry is removed from the cache based on the eviction policy
     * Assumes the corresponding set has already been locked for writing.
     * @param key    the key with which the specified value is to be associated.
     * @param value    a value to be associated with the specified key.
     * @return true is something has been overwritten
     */
    public void put(String key, String value) {
        // Must be called before anything else
        AutoGrader.agCachePutStarted(key, value);
        AutoGrader.agCachePutDelay();

        boolean found = false;
        int setId = getSetId(key);
        LinkedList<Entry> setlist = sets[setId].list;
        for (Entry e : setlist){
        	if (e.key.equals(key)){
        		found = true;
        		e.value = value;
        		e.accessed = false;
        		break;
        	}
        }
        if (setlist.size() < maxElemsPerSet && !found){
        	//System.out.println("Added String " + key);
        	setlist.add(new Entry(key, value));
        }
        else if (setlist.size() == maxElemsPerSet && !found){
        	//System.out.println("Second chance policy");
        	secondChancePolicy(setlist, key, value);
        }
        // Must be called before returning
        AutoGrader.agCachePutFinished(key, value);
    }
    
    public void secondChancePolicy(LinkedList<Entry> set, String newKey, String newValue){
    	boolean removed = false;
    	Entry e = set.removeFirst();
    	while (!removed){
    		if (!e.accessed){
    			removed = true;
    			break;
    		}
    		else 
    			e.accessed = false;
    		set.add(e);
    		e = set.removeFirst();
    	}
    	set.add(new Entry(newKey, newValue));
    }

    /**
     * Removes an entry from this cache.
     * Assumes the corresponding set has already been locked for writing.
     * @param key    the key with which the specified value is to be associated.
     */
    public void del (String key) {
        // Must be called before anything else
        AutoGrader.agCacheGetStarted(key);
        AutoGrader.agCacheDelDelay();
        
        int setId = getSetId(key);
        LinkedList<Entry> setlist = sets[setId].list;
        for (Entry e : setlist){
        	if (e.key.equals(key)){
        		setlist.remove(e);
        		break;
        	}
        }
        		

        // Must be called before returning
        AutoGrader.agCacheDelFinished(key);
    }

    /**
     * @param key
     * @return    the write lock of the set that contains key.
     */
    public WriteLock getWriteLock(String key) {
        setClass set = getSetByKey(key);
        return set.wl;
    }
    
    public setClass getSetByKey(String key){
    	int c = getSetId(key);
    	return sets[c];
    }

    /**
     *
     * @param key
     * @return    set of the key
     */
    private int getSetId(String key) {
        return Math.abs(key.hashCode()) % numSets;
    }
    
    public class setClass{
    	public LinkedList<Entry> list;
    	public WriteLock wl;
    	
    	public setClass(){
    		list = new LinkedList<Entry>();
    		ReentrantReadWriteLock rw = new ReentrantReadWriteLock();
    		wl = rw.writeLock();
    	}
    }
    
    public class Entry{
    	public String key;
    	public String value;
    	public boolean accessed;
    	
    	public Entry(String k, String v){
    		key = k;
    		value = v;
    		accessed = false;
    	}
    }

    public String toXML() throws Exception {

    	String returnString = null;
		try {
			System.out.println("In to XML KVStore");
			DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder docBuilder  = docFactory.newDocumentBuilder();
    		Document doc = docBuilder.newDocument();
			Element rootElement = doc.createElement("KVCache");
			doc.appendChild(rootElement);
			
			for (int setID = 0; setID < sets.length; setID++) {
				Element SetEle = doc.createElement("Set");
				rootElement.appendChild(SetEle);
				Attr attr = doc.createAttribute("Id");
				attr.setValue(Integer.toString(setID));
				SetEle.setAttributeNode(attr);
				
				for (Entry entry: sets[setID].list) {
					Element EntryEle = doc.createElement("CacheEntry");
					SetEle.appendChild(EntryEle);
					
					Attr attrR = doc.createAttribute("isReferenced");
					if (entry.accessed) {
						attrR.setValue("true");
					} else {
						attrR.setValue("false");
					}
					
					EntryEle.setAttributeNode(attrR);
					
					Attr attrV = doc.createAttribute("isValid");
					attrV.setValue("true");
					EntryEle.setAttributeNode(attrV);
					
					Element keyElement = doc.createElement("Key");
					keyElement.appendChild(doc.createTextNode(entry.key));
					EntryEle.appendChild(keyElement);
					Element valElement = doc.createElement("Value");
					valElement.appendChild(doc.createTextNode(entry.value));
					EntryEle.appendChild(valElement);
					
				}
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
    	
		//System.out.println("ReturnString= " + returnString);
		return returnString;
    }
}
