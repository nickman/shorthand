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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;

/**
 * <p>Title: MemoryURLConnection</p>
 * <p>Description: A {@link URLConnection} implementation for in memory buffers. Copied from <a href="http://tika.apache.org/">Apache Tika</a></p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.shorthand.util.net.MemoryURLConnection</code></p>
 */

public class MemoryURLConnection extends URLConnection {

	
	/** The content length header */
	public static final String CONTENT_LENGTH = "content-length";
	/** The content type header */
	public static final String CONTENT_TYPE = "content-type";
	/** The default content type value */
	public static final String TEXT_PLAIN = "text/plain";
	/** The last modified timestamp header */
	public static final String LAST_MODIFIED = "last-modified";
	
	/** The system property key for the property that specifies URL stream handler factory names */
	public static final String PKGS = "java.protocol.handler.pkgs";
	/** The package specification */
	public static final String MEM_URL_PKG = "com.theice.clearing.eventcaster.io";
    /** The buffered data */
    private final MemBuffer data;
    
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
    	URL.setURLStreamHandlerFactory(new MemoryURLStreamHandlerFactory());    	
    }

    /**
     * Creates a new MemoryURLConnection
     * @param url The URL that represents this connection
     * @param data The buffered data
     */
    public MemoryURLConnection(URL url, MemBuffer data) {
        super(url);
        this.data = data;
    }

    /**
     * {@inheritDoc}
     * @see java.net.URLConnection#connect()
     */
    @Override
    public void connect() {
    }

    /**
     * {@inheritDoc}
     * @see java.net.URLConnection#getInputStream()
     */
    @Override
    public InputStream getInputStream() {
        return data.getInputStream();
    }
    
    /**
     * {@inheritDoc}
     * @see java.net.URLConnection#getLastModified()
     */
    @Override
    public long getLastModified() {
    	return data.getLastModifiedTime();
    }
    
    /**
     * {@inheritDoc}
     * @see java.net.URLConnection#getOutputStream()
     */
    @Override
    public OutputStream getOutputStream() throws IOException {    
    	return data.getOutputStream();
    }
    
    /**
     * {@inheritDoc}
     * @see java.net.URLConnection#getHeaderFieldDate(java.lang.String, long)
     */
    @Override
    public long getHeaderFieldDate(String name, long Default) {
    	if(LAST_MODIFIED.equalsIgnoreCase(name)) {
    		return data.getLastModifiedTime();
    	} 
    	return super.getHeaderFieldDate(name, Default);
    }


}
