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
package com.heliosapm.shorthand.broadcast;

import java.nio.ByteBuffer;

import com.heliosapm.shorthand.ShorthandProperties;
import com.heliosapm.shorthand.util.unsafe.UnsafeAdapter;

/**
 * <p>Title: StartupBroadcastPacketWriter</p>
 * <p>Description: Packet writer for the shorthand agent startup broadcast</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.shorthand.broadcast.StartupBroadcastPacketWriter</code></p>
 */

public class StartupBroadcastPacketWriter implements BroadcastPacketWriter {
	/** A static re-usable instance */
	public static final BroadcastPacketWriter INSTANCE = new StartupBroadcastPacketWriter();
	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.shorthand.broadcast.BroadcastPacketWriter#buildPacket(java.lang.Object[])
	 */
	@Override
	public byte[] buildPacket(Object... args) {
		/*
		 * type: 1 (byte)
		 * pid: 4 (int)
		 * jmxmp port: 4 (int)
		 */
		ByteBuffer buf = ByteBuffer.allocate(9);
		buf.put((byte)1);
		buf.putInt(ShorthandProperties.IPID);
		buf.putInt(8006);
		return buf.array();
	}

}
