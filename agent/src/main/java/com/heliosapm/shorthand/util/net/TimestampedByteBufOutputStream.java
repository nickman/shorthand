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

/**
 * <p>Title: TimestampedByteBufOutputStream</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.shorthand.util.net.TimestampedByteBufOutputStream</code></p>
 */

public interface TimestampedByteBufOutputStream   {
//	/** The last time data was written to this stream */
//	protected long lastModifiedTimestamp = System.currentTimeMillis();
//	/**
//	 * Creates a new TimestampedByteBufOutputStream
//	 * @param buffer the wrapped byte buffer
//	 */
//	public TimestampedByteBufOutputStream(MemBuffer mb);
//	
	/**
	 * Resets the timestamp
	 */
	void resetTimestamp();
//		lastModifiedTimestamp = System.currentTimeMillis();
//	}

	public void write(byte[] b, int off, int len) throws IOException;
//		super.write(b, off, len);
//		if(len>0) lastModifiedTimestamp = System.currentTimeMillis();
//	}

	public void write(byte[] b) throws IOException;
//		super.write(b);
//		if(b.length>0) lastModifiedTimestamp = System.currentTimeMillis();
//	}

	public void write(int b) throws IOException;
//		super.write(b);
//		lastModifiedTimestamp = System.currentTimeMillis();
//	}



	public void writeInt(int v) throws IOException;
//		super.writeInt(v);
//		lastModifiedTimestamp = System.currentTimeMillis();
//	}

	public void writeLong(long v) throws IOException;
//		super.writeLong(v);
//		lastModifiedTimestamp = System.currentTimeMillis();
//	}

	public void writeShort(int v) throws IOException;
//		super.writeShort(v);
//		lastModifiedTimestamp = System.currentTimeMillis();
//	}


	public void writeUTF(String s) throws IOException;
//		super.writeUTF(s);
//		if(!s.isEmpty()) lastModifiedTimestamp = System.currentTimeMillis();
//	}

	/**
	 * Returns the last time data was written to this stream
	 * @return the last time data was written to this stream
	 */
	public long getLastModifiedTimestamp();
//		return lastModifiedTimestamp;
//	}
	
}
