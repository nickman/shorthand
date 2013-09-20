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

import java.net.InetAddress;
import java.nio.ByteBuffer;

/**
 * <p>Title: BroadcastPacketReader</p>
 * <p>Description: A class that knows how to unmarshall a specific typed broadcast packet</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.shorthand.broadcast.BroadcastPacketReader</code></p>
 * @param <T> The type of the object the reader returns
 */

public interface BroadcastPacketReader<T> {
	/**
	 * Returns an unmarshalled broadcast packet
	 * @param broadcast The contents of the received DatagramPacket containing the broadcast 
	 * @param sourceAddress The address the DatagramPacket was received from
	 */
	public T unmarshallPacket(ByteBuffer broadcast, InetAddress sourceAddress);
}
