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
import java.io.OutputStream;

/**
 * <p>Title: TimestampedByteBufOutputStreamImpl</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.shorthand.util.net.TimestampedByteBufOutputStreamImpl</code></p>
 */

public class TimestampedByteBufOutputStreamImpl extends OutputStream implements  TimestampedByteBufOutputStream {

	/**
	 * Creates a new TimestampedByteBufOutputStreamImpl
	 */
	public TimestampedByteBufOutputStreamImpl(MemBuffer mb) {
		// TODO Auto-generated constructor stub
	}

	/**
	 * {@inheritDoc}
	 * @see java.io.OutputStream#write(int)
	 */
	@Override
	public void write(int b) throws IOException {
		// TODO Auto-generated method stub

	}

	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.shorthand.util.net.TimestampedByteBufOutputStream#resetTimestamp()
	 */
	@Override
	public void resetTimestamp() {
		// TODO Auto-generated method stub
		
	}

	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.shorthand.util.net.TimestampedByteBufOutputStream#writeInt(int)
	 */
	@Override
	public void writeInt(int v) throws IOException {
		// TODO Auto-generated method stub
		
	}

	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.shorthand.util.net.TimestampedByteBufOutputStream#writeLong(long)
	 */
	@Override
	public void writeLong(long v) throws IOException {
		// TODO Auto-generated method stub
		
	}

	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.shorthand.util.net.TimestampedByteBufOutputStream#writeShort(int)
	 */
	@Override
	public void writeShort(int v) throws IOException {
		// TODO Auto-generated method stub
		
	}

	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.shorthand.util.net.TimestampedByteBufOutputStream#writeUTF(java.lang.String)
	 */
	@Override
	public void writeUTF(String s) throws IOException {
		// TODO Auto-generated method stub
		
	}

	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.shorthand.util.net.TimestampedByteBufOutputStream#getLastModifiedTimestamp()
	 */
	@Override
	public long getLastModifiedTimestamp() {
		// TODO Auto-generated method stub
		return 0;
	}

}
