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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.concurrent.atomic.AtomicLong;

import javax.annotation.Resource;

/**
 * <p>Title: MemBuffer</p>
 * <p>Description: A container for mem url data buffer</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.shorthand.util.net.MemBuffer</code></p>
 */

public class MemBuffer {
	/** The memory buffer pointer */
	private long address;
	/** The size of the current buffer */
	private long size;
	/** The buffer output stream */
	final TimestampedByteBufOutputStreamImpl os;
	/** The buffer input stream */
	InputStream is;
	
	
	private final int initialSize;
	private final int nextSegSize;
	
	
	/** A counter of the number of MemBuffer finalizations */
	private static final AtomicLong fCount = new AtomicLong(0L);
	
	/** The default size of the initial memory space */
	public static final int DEFAULT_SIZE = 1024;
	/** The default size to grow the memory space by when space is exhausted */
	public static final int DEFAULT_NEXT_SEG = 1024;
	
	/**
	 * Returns the count of MemBuffers that have been finalized
	 * @return the count of MemBuffers that have been finalized
	 */
	public static long getFinalizationCount() {
		return fCount.get();
	}
	
	/**
	 * Creates a new Record of the defalt initial size and growth segment size
	 */
	public MemBuffer() {
		initialSize = DEFAULT_SIZE;
		nextSegSize = DEFAULT_NEXT_SEG;
		os = new TimestampedByteBufOutputStreamImpl(this);
	}
	
	
	/**
	 * Returns the timestamp of the last modification to this record's data
	 * @return the timestamp of the last modification to this record's data
	 */
	public long getLastModifiedTime() {
		return os.getLastModifiedTimestamp();
	}
	
	/**
	 * Returns the buffer's output stream
	 * @return the buffer's output stream
	 */
	public OutputStream getOutputStream() {
		return os;
	}
	
	/**
	 * Returns the buffer's input stream
	 * @return the buffer's intput stream
	 */
	public synchronized InputStream getInputStream() {
		if(is==null) is = new MemBufferInputStream(this);
		return is;
	}

	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (int) (address ^ (address >>> 32));
		return result;
	}

	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		MemBuffer other = (MemBuffer) obj;
		if (address != other.address)
			return false;
		return true;
	}
	
//	
//	/**
//	 * Writes the readable bytes from the passed URL's input stream to the buffer
//	 * @param url the URL to read the bytes from
//	 * @return this buffer
//	 */
//	public MemBuffer write(URL url) {
//		if(url==null) throw new IllegalArgumentException("The passed value was null", new Throwable());
//		try {
//			InputStream input = url.openStream();
//			buf.writeBytes(input, input.available());
//		} catch (IOException e) {
//			throw new RuntimeException("Failed to write bytes from input stream", e);
//		}		
//		return this;
//	}
	
//	/**
//	 * Writes the readable bytes from the passed {@link Resource}'s URL input stream to the buffer
//	 * @param resource the Spring resource to read the bytes from
//	 * @return this buffer
//	 */
//	public MemBuffer write(Resource resource) {
//		if(resource==null) throw new IllegalArgumentException("The passed value was null", new Throwable());
//		try {
//			
//			InputStream input = resource.getInputStream();
//			buf.writeBytes(input, input.available());
//		} catch (IOException e) {
//			throw new RuntimeException("Failed to write bytes from input stream", e);
//		}		
//		return this;
//	}
	
//	/**
//	 * Writes the  bytes from the passed {@link File} to the buffer
//	 * @param file The file system file to read bytes from
//	 * @return this buffer
//	 */
//	public MemBuffer write(File file) {
//		if(file==null || !file.canRead()) throw new IllegalArgumentException("The passed file cannot be read", new Throwable());
//		FileInputStream fis = null;		
//		try {			
//			fis = new FileInputStream(file);
//			write(fis);			
//		} catch (IOException e) {
//			throw new RuntimeException("Failed to write bytes from input stream", e);
//		} finally {
//			if(fis!=null) try { fis.close(); } catch (Exception x) {}
//		}
//		return this;
//	}	
	

}
