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

import com.heliosapm.shorthand.util.unsafe.UnsafeAdapter;

/**
 * <p>Title: MemBufferInputStream</p>
 * <p>Description: An input stream impl for streaming data out of a {@link MemBuffer}</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.shorthand.util.net.MemBufferInputStream</code></p>
 */

public class MemBufferInputStream extends InputStream {
	/** The underlying mem buffer */
	private final BufferManager.MemBuffer mb;
	/** The position in the buffer we're reading from */
	private long readPosition = 0;
	
	/** The mark read limit */
	private int readlimit = Integer.MAX_VALUE;
	/** The bytes read since the mark limit was set */
	private int bytesSinceMark = 0;
	
	/** The current mark */
	private int mark = 0;
	
	private boolean open = true;
	
	
	
	/**
	 * Creates a new MemBufferInputStream
	 * @param mb The underlying buffer
	 */
	MemBufferInputStream(BufferManager.MemBuffer mb) {
		this.mb = mb;
	}

	/**
	 * {@inheritDoc}
	 * @see java.io.InputStream#read()
	 */
	@Override
	public int read() throws IOException {
		if(!open) throw new IOException("This InputStream is closed. Nein, nein, nein");
		byte b = mb.read(readPosition);		
		readPosition++;
		bytesSinceMark++;
		if(readlimit!=Integer.MAX_VALUE && bytesSinceMark>=readlimit) {
			mark = (int)readPosition;
			bytesSinceMark = 0;
		}
		return b;
	}
	
	/**
	 * {@inheritDoc}
	 * @see java.io.InputStream#read(byte[])
	 */
	@Override
	public int read(byte[] bytes) throws IOException {
		if(!open) throw new IOException("This InputStream is closed. Nein, nein, nein");
		if(readPosition==mb.getSize()) return 0;
		int bytesRead = mb.read(readPosition, bytes);
		readPosition += bytesRead;
		return bytesRead;
	}
	
	/**
	 * {@inheritDoc}
	 * @see java.io.InputStream#read(byte[], int, int)
	 */
	@Override
	public int read(byte[] b, int off, int len) throws IOException {
		if(!open) throw new IOException("This InputStream is closed. Nein, nein, nein");
		if(readPosition==mb.getSize()) return 0;
		int bytesRead = mb.read(readPosition, b, off, len);
		readPosition += bytesRead;
		return bytesRead;
	}
	
	/**
	 * {@inheritDoc}
	 * @see java.io.InputStream#skip(long)
	 */
	@Override
	public long skip(long n) throws IOException {
		int skipped = 0;
		for(long k = 0; k < n; k++) {
			if(readPosition == mb.getSize()) {
				break;
			}
			skipped++;
			readPosition++;
		}
		return skipped;
	}
	
	/**
	 * {@inheritDoc}
	 * @see java.io.InputStream#reset()
	 */
	@Override
	public synchronized void reset() throws IOException {
		readPosition = 0;
		readlimit = Integer.MAX_VALUE;
		mark = 0;
		bytesSinceMark = 0;
	}
	
	/**
	 * {@inheritDoc}
	 * @see java.io.InputStream#markSupported()
	 */
	@Override
	public boolean markSupported() {		
		return true;
	}
	
	/**
	 * {@inheritDoc}
	 * @see java.io.InputStream#available()
	 */
	@Override
	public int available() throws IOException {
		return (int)(mb.getSize()-readPosition);
	}
	
	/**
	 * {@inheritDoc}
	 * @see java.io.InputStream#close()
	 */
	@Override
	public void close() throws IOException {
		open = false;
	}
	
	/**
	 * {@inheritDoc}
	 * @see java.io.InputStream#mark(int)
	 */
	@Override
	public synchronized void mark(int readlimit) {
		this.readlimit = readlimit;
		mark = (int)readPosition;
	}
	

	
	

}
