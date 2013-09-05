/**
 * Helios, OpenSource Monitoring
 * Brought to you by the Helios Development Group
 *
 * Copyright 2013, Helios Development Group and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org. 
 *
 */
package com.heliosapm.shorthand.util.net;

import java.net.URL;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.management.ObjectName;

import com.heliosapm.shorthand.util.jmx.JMXHelper;

/**
 * <p>Title: BufferManager</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.shorthand.util.net.BufferManager</code></p>
 */

public class BufferManager implements BufferManagerMBean, RefRemover {

	/** The singleton instance */
	private static volatile BufferManager instance = null;
	/** The singleton instance ctor lock */
	private static final Object lock = new Object();
	
	/** The BufferManager's JMX object name */
	private static final ObjectName OBJECT_NAME = JMXHelper.objectName(new StringBuilder(BufferManager.class.getPackage().getName()).append(":service=").append(BufferManager.class.getSimpleName()));
    
    /** Associative wek references from the URL to the data buffers they represent */
    private final Map<URL, MemBuffer> references =  Collections.synchronizedMap(new WeakHashMap<URL, MemBuffer>());
    		//Collections.synchronizedMap(new WeakHashMap<URL, MemBuffer>());
    		//new ConcurrentHashMap<URL, MemBuffer>();
	/** Flag indicating if the stream factory has been registered */
    private static final AtomicBoolean factoryRegistered = new AtomicBoolean(false);
    
	/** The mem URL protocol */
	public static final String MEM_PROTOCOL = "mem";
	/**
	 * Acquires the BufferManager singleton instance
	 * @return the BufferManager singleton instance
	 */
	public static BufferManager getInstance() {
		if(instance==null) {
			synchronized(lock) {
				if(instance==null) {
					instance = new BufferManager();
				}
			}
		}
		return instance;
	}
	
	/**
	 * Creates a new BufferManager
	 */
	private BufferManager() {
		try {
			JMXHelper.getHeliosMBeanServer().registerMBean(this, OBJECT_NAME);

		} catch (Exception ex) {

		}
	}
	
    /**
     * Registers the Memory URL stream factory
     */
    public static void register() {
    	// -Djava.protocol.handler.pkgs=com.theice.clearing.eventcaster.io
//    	synchronized(System.getProperties()) {
//	    	String value = System.getProperty(PKGS);
//	    	if(value!=null) {
//	    		if(!value.contains(MEM_URL_PKG)) {
//	    			value = "|" + MEM_URL_PKG;
//	    		}
//	    	} else {
//	    		value = "MEM_URL_PKG";
//	    	}
//	    	System.setProperty(PKGS, value);
//    	}
    	if(!factoryRegistered.get()) {
    		synchronized(factoryRegistered) {
    			if(!factoryRegistered.get()) {
    				URL.setURLStreamHandlerFactory(new MemoryURLStreamHandlerFactory());
    				factoryRegistered.set(true);
    			}
    		}
    	}    	    	
    }	
	
	/**
	 * Creates a new un-registered MemBuffer
	 * @param initialCapacity The initial capacity of the buffer to create. If 0 or less, the buffer will have no initial capacity.
	 * @param url the URL this mem buffer is being created for
	 * @return a new un-registered MemBuffer
	 */
	protected MemBuffer newMemBuffer(int initialCapacity, URL url) {
		return new MemBuffer(initialCapacity, MemBuffer.DEFAULT_NEXT_SEG, url, this);
	}
	
	/**
	 * Creates a new un-registered MemBuffer with no initial capacity
	 * @param url the URL this mem buffer is being created for
	 * @return a new un-registered MemBuffer
	 */
	protected MemBuffer newMemBuffer(URL url) {
		return new MemBuffer(url, this);
	}
	
	/**
	 * Registers a MemBuffer to associate with the passed mem URL
	 * @param url The mem URL to register with
	 * @param buffer The MemBuffer to register. If this is null, a new MemBuffer will be created.
	 * @return The registered MemBuffer
	 */
	public MemBuffer registerMemBuffer(URL url, MemBuffer buffer) {
		validateMemUrl(url);
		MemBuffer mb = references.get(url);
		if(mb==null) {
			synchronized(references) {
				mb = references.get(url);
				if(mb==null) {
					mb = buffer!=null ? buffer :  newMemBuffer(getInitialCapacity(url), url);
					references.put(url, mb);
				}
			}
		}
		return mb;
	}
	
	public void unregister(URL url) {
		references.remove(url);
	}
	
//	public URL getRegisteredURL(String protocol, String host, int port,
//            String authority, String userInfo, String path,
//            String query, String ref) {
//		
//		return null;
//	}
	
	/**
	 * Validates the passed URL to ensure that it is not null and has a <code>mem</code> protocol
	 * @param url The URL to validate
	 */
	public void validateMemUrl(URL url) {
		if(url==null) throw new IllegalArgumentException("The passed URL was null", new Throwable());
		if(!MEM_PROTOCOL.equalsIgnoreCase(url.getProtocol())) throw new IllegalArgumentException("The passed URL [" + url + "] did not have a mem protocol", new Throwable());		
	}
	
	/**
	 * Creates and registers a MemBuffer to associate with the passed mem URL
	 * @param url The mem URL to register with
	 * @return The registered and created MemBuffer
	 */
	public MemBuffer registerMemBuffer(URL url) {
		return registerMemBuffer(url, null);
	}
	
	/**
	 * Returns the MemBuffer associated to the passed URL
	 * @param url The URL to get the associated MemBuffer for
	 * @return the associated MemBuffer
	 */
	public MemBuffer getMemBuffer(URL url) {
		validateMemUrl(url);
		MemBuffer buffer = references.get(url);
		if(buffer==null) throw new RuntimeException("Invalid URL [" + url + "]. No MemBuffer found", new Throwable());
		return buffer;		
	}
	
    /**
     * Parses the named query parameter from a URL query segment
     * @param url the URL to get the query segment from
     * @param paramName The name of the parameter to get
     * @return the value of the named query parameter or null if it was not found
     */
    public String getParameterValue(URL url, String paramName) {
    	if(url==null || paramName==null || paramName.trim().isEmpty()) return null;
    	paramName = paramName.trim();
    	String query = url.getQuery();
    	if(query==null || query.trim().isEmpty()) return null;
    	String[] frags = query.trim().split("&");
    	for(String pair: frags) {
    		pair = pair.trim().toLowerCase();
    		String delim = paramName + "=";
    		int index = pair.indexOf(delim);
    		if(index!=-1) {
    			return pair.substring(index + delim.length());
    		}
    	}
    	return null;
    }    
	
	
    /**
     * Parses the initial capacity argument from a URL query segment
     * @param url the URL to get the query segment from
     * @return the initial capacity or -1
     */
    public int getInitialCapacity(URL url) {
    	String val = getParameterValue(url, "ic");
    	if(val==null) return MemBuffer.DEFAULT_SIZE;
    	return Integer.parseInt(val.trim());
    }
    
    /**
     * Returns the number of allocated and registered MemBuffers
     * @return the number of allocated and registered MemBuffers
     */
    @Override
	public int getMemBufferCount() {
    	return references.size();
    }
    
    /**
     * {@inheritDoc}
     * @see com.theice.clearing.eventcaster.io.mem.BufferManagerMBean#printKeys()
     */
    @Override
    public Set<String> printKeys() {
    	Set<String> keys = new TreeSet<String>();
    	Set<URL> urls = new HashSet<URL>(references.keySet());
    	for(URL url: urls) {
    		keys.add(url.toString());
    	}
    	urls.clear();
    	urls = null;
    	return keys;
    }
    
    /**
     * {@inheritDoc}
     * @see com.theice.clearing.eventcaster.io.mem.BufferManagerMBean#getMemBufferExpirations()
     */
    @Override
    public long getMemBufferExpirations() {
    	return MemBuffer.getFinalizationCount();
    }


}
