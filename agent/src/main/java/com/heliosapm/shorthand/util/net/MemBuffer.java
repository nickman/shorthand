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
import java.util.concurrent.atomic.AtomicLong;

import com.heliosapm.shorthand.util.ref.DeallocatingAction;
import com.heliosapm.shorthand.util.ref.RunnableReferenceQueue;
import com.heliosapm.shorthand.util.ref.RunnableReferenceQueue.DeallocatingPhantomReference;
import com.heliosapm.shorthand.util.unsafe.UnsafeAdapter;

/**
 * <p>Title: MemBuffer</p>
 * <p>Description: A container for mem url data buffer</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.shorthand.util.net.MemBuffer</code></p>
 */

public class MemBuffer implements DeallocatingAction {
	/** The memory buffer pointer */
	private long address;
	/** The offset of the last byte of data written in to the buffer */
	private long size;
	/** The total size of the memory currently allocated under the address */
	private long capacity;
	
	/** The buffer input stream */
	InputStream is;
	/** The timestamp of the last data change to this buffer */
	private long lastChange = System.currentTimeMillis();
	/** The URL this mem buffer was created for */
	private final URL memUrl;
	
	private final RefRemover remover;
	
	public String toString() {
		return String.format("[%s] size:%s  capacity:%s", memUrl.toString(), size, capacity);
	}
	/** The size increment of the memory allocation when the space is exhausted */
	private final int nextSegSize;
	
	private final DeallocatingPhantomReference<MemBuffer> phantomRef;
	
	
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
	 * Creates a new MemBuffer
	 * @param initialSize The initial memory size
	 * @param nextSegSize The size of the next segment allocated when space is exhausted
	 * @param memUrl The URL this mem buffer was created for
	 */
	public MemBuffer(int initialSize, int nextSegSize, URL memUrl, RefRemover remover) {
		this.memUrl = memUrl;
		this.remover = remover;
		this.nextSegSize = nextSegSize;
		address = UnsafeAdapter.allocateMemory(initialSize);
		size = 0;
		capacity = initialSize;
		phantomRef = RunnableReferenceQueue.getInstance().buildPhantomReference(this, address); 		
	}
	
	/**
	 * Creates a new Record of the defalt initial size and growth segment size
	 * @param memUrl The URL this mem buffer was created for
	 */
	public MemBuffer(URL memUrl, RefRemover remover) {
		this(DEFAULT_SIZE, DEFAULT_NEXT_SEG, memUrl, remover);
	}
	
	/**
	 * Writes the passed byte to the current offset in the buffer
	 * @param b The byte to write
	 * @throws IOException thrown if the position specified is greater than the current size
	 */
	void write(byte b) throws IOException {
		if(capacity-size<10) {
			resize();
		}
		UnsafeAdapter.putByte(null, address + size, b);
		size++;
		lastChange = System.currentTimeMillis();
	}
	
	/**
	 * Writes the passed byte array to the current offset in the buffer
	 * @param arr The byte array to write
	 * @throws IOException thrown if the position specified is greater than the current size
	 */
	void write(byte[] arr) throws IOException {
		if(arr==null) throw new IllegalArgumentException("The passed byte array was null");
		if(capacity-size<(arr.length + 10)) {
			resize(Math.max(DEFAULT_NEXT_SEG, arr.length + 10));
		}
		UnsafeAdapter.copyMemory(arr, UnsafeAdapter.BYTE_ARRAY_OFFSET, null, address + size, arr.length);
		lastChange = System.currentTimeMillis();
		size += arr.length;
	}
	
	/**
	 * Reads a byte from the allocated memory at the specified offset
	 * @param pos The offset off the address to read from
	 * @return The read byte
	 * @throws IOException thrown if the position specified is greater than the current size
	 */
	byte read(long pos) throws IOException {
		if(pos<0) throw new IllegalArgumentException("Invalid position [" + pos + "]");
		if(pos>size) throw new IOException("Invalid position [" + pos + "] for buffer of size ["  + size + "].");
		return UnsafeAdapter.getByte(address + pos);
	}
	
	/**
	 * Reads the allocated memory at the specified offset into the passed byte array
	 * @param pos The offset off the address to read from
	 * @param arr The aray to write into
	 * @return The number of bytes read
	 * @throws IOException thrown if the position specified is greater than the current size
	 */
	int read(long pos, byte[] arr) throws IOException {
		if(pos<0) throw new IllegalArgumentException("Invalid position [" + pos + "]");
		if(pos>size) throw new IOException("Invalid position [" + pos + "] for buffer of size ["  + size + "].");		
		if(size==pos) return 0;		
		int maxBytes = (int)size-(int)pos;
		int bytesToRead = (int)(Math.min(maxBytes, arr.length));
		UnsafeAdapter.copyMemory(null, address + pos, arr, UnsafeAdapter.BYTE_ARRAY_OFFSET, bytesToRead);
		return bytesToRead;
	}
	
	/**
	 * Reads the allocated memory at the specified offset into the passed byte array
	 * @param pos The offset off the address to read from
	 * @param arr The aray to write into
	 * @param off The offset in the array to start writing at
	 * @param len the number of bytes to write into the array
	 * @return the number of bytes written
	 * @throws IOException
	 */
	int read(long pos, byte[] arr, int off, int len) throws IOException {
		if(pos<0) throw new IllegalArgumentException("Invalid position [" + pos + "]");
		if(pos>size) throw new IOException("Invalid position [" + pos + "] for buffer of size ["  + size + "].");
		if(size==pos) return 0;
		int maxBytes = (int)size-(int)pos;
		int bytesToRead = (int)(Math.min(maxBytes, len));
		UnsafeAdapter.copyMemory(null, address + pos, arr, UnsafeAdapter.BYTE_ARRAY_OFFSET + off, bytesToRead);
		return bytesToRead;				
	}
	
	/**
	 * Grows the allocated memory space by {@link #nextSegSize} bytes.
	 */
	private void resize() {
		address = UnsafeAdapter.reallocateMemory(address, capacity + nextSegSize);
		capacity += nextSegSize; 
		phantomRef.setAddress(address);
	}
	
	/**
	 * Grows the allocated memory space by the passed size
	 * @param increase The number of bytes to increase the allocated memory by
	 */
	private void resize(int increase) {
		address = UnsafeAdapter.reallocateMemory(address, capacity + increase);
		capacity += increase;
		phantomRef.setAddress(address);
	}
	
	
	/**
	 * Returns the timestamp of the last modification to this record's data
	 * @return the timestamp of the last modification to this record's data
	 */
	public long getLastModifiedTime() {
		return lastChange;
	}
	
	/**
	 * Returns the buffer's output stream
	 * @return the buffer's output stream
	 */
	public OutputStream getOutputStream() {
		return new MemBufferOutputStream(this);
	}
	
	/**
	 * Returns the buffer's input stream
	 * @return the buffer's intput stream
	 */
	public InputStream getInputStream() {
		return new MemBufferInputStream(this);
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

	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.shorthand.util.ref.DeallocatingAction#getAction()
	 */
	@Override
	public Runnable getAction() {
		return new Runnable(){
			@Override
			public void run() {
				remover.unregister(memUrl);
				fCount.incrementAndGet();
			}
		};
	}

	/**
	 * Returns the total size of the allocated memory in bytes
	 * @return the allocated memory size
	 */
	public long getSize() {
		return size;
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
