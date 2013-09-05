/**
 * Helios, OpenSource Monitoring
 * Brought to you by the Helios Development Group
 *
 * Copyright 2007, Helios Development Group and individual contributors
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
import java.io.OutputStream;

/**
 * <p>Title: MemBufferOutputStream</p>
 * <p>Description: An output stream impl for streaming data into a {@link MemBuffer}</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.shorthand.util.net.MemBufferOutputStream</code></p>
 */

public class MemBufferOutputStream extends OutputStream {
	/** The buffer we're writing to  */
	private final MemBuffer buf;
	/** The position in the buffer we're writing to */
	private long writePosition = 0;
	
	private boolean open = true;
	
	
	/**
	 * Creates a new MemBufferOutputStream
	 * @param buf the MemBuf to write to
	 */
	MemBufferOutputStream(MemBuffer buf) {
		this.buf = buf;
	}
	
	/**
	 * {@inheritDoc}
	 * @see java.io.OutputStream#close()
	 */
	public void close() {
		open = false;
	}

	/**
	 * {@inheritDoc}
	 * @see java.io.OutputStream#write(int)
	 */
	@Override
	public void write(int b) throws IOException {
		if(!open) throw new IOException("This OutputStream is closed. Nein, nein, nein");
		buf.write(writePosition, (byte)b);
		writePosition++;
	}
	
	/**
	 * {@inheritDoc}
	 * @see java.io.OutputStream#write(byte[])
	 */
	@Override
	public void write(byte[] b) throws IOException {
		if(!open) throw new IOException("This OutputStream is closed. Nein, nein, nein");
		buf.write(writePosition, b);
		writePosition += b.length;
	}
	
	/**
	 * {@inheritDoc}
	 * @see java.io.OutputStream#write(byte[], int, int)
	 */
	@Override
	public void write(byte[] b, int off, int len) throws IOException {
		if(!open) throw new IOException("This OutputStream is closed. Nein, nein, nein");
		byte[] arr = new byte[len];
		System.arraycopy(b, off, arr, 0, len);
		write(arr);		
	}

}
