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
import java.util.Set;

import com.heliosapm.shorthand.util.net.BufferManager.MemBuffer;

/**
 * <p>Title: BufferManagerMBean</p>
 * <p>Description: JMX MBean interface for {@link BufferManager}</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.shorthand.util.net.BufferManagerMBean</code></p>
 */

public interface BufferManagerMBean {
    /**
     * Returns the number of allocated and registered MemBuffers
     * @return the number of allocated and registered MemBuffers
     */
    public int getMemBufferCount();
    
    /**
     * Returns a set of the stringified URLs in the MemBuffer cache
     * @return a set of the stringified URLs in the MemBuffer cache
     */
    public Set<String> printKeys();
    
    /**
     * Returns the number of MemBuffers that have been expired due to their associated mem URL being garbage collected
     * @return the number of MemBuffers that have been expired
     */
    public long getMemBufferExpirations();
    
	/**
	 * Returns the URL created for the passed URL suffix
	 * @param bufferName The URL suffix for the URL
	 * @return the MemBuffer URL
	 */
	public URL getMemBufferURL(String bufferName);    
	
    /**
     * Returns the total number of MemBuffer instances that exist anywhere
     * @return the total number of MemBuffer instances that exist anywhere
     */
    public long getMemBufferInstances();
    
    /**
     * Returns the total number of MemBuffer instances that have been destroyed
     * @return the total number of MemBuffer instances that have been destroyed
     */
    public long getMemBufferDestroys();
    
    /**
     * Returns the highwater number of MemBuffer instances
     * @return the highwater number of MemBuffer instances
     */
    public long getMemBufferInstanceHighwater();


}
