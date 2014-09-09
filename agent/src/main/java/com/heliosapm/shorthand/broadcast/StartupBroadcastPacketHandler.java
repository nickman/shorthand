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
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;

import com.heliosapm.shorthand.ShorthandProperties;
import com.heliosapm.shorthand.util.jmx.ShorthandJMXConnectorServer;

/**
 * <p>Title: StartupBroadcastPacketHandler</p>
 * <p>Description: Packet writer for the shorthand agent startup broadcast</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.shorthand.broadcast.StartupBroadcastPacketHandler</code></p>
 */

public class StartupBroadcastPacketHandler implements BroadcastPacketWriter, BroadcastPacketReader<BroadcastExecutable> {
	/** A static re-usable instance */
	public static final StartupBroadcastPacketHandler INSTANCE = new StartupBroadcastPacketHandler();
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
		buf.put((byte)BroadcastType.STARTUP.ordinal());
		buf.putInt(ShorthandProperties.IPID);
		buf.putInt(ShorthandJMXConnectorServer.getInstance().port);
		return buf.array();
	}
	
	/**
	 * <p>Title: AgentStartupBroadcast</p>
	 * <p>Description: An unmarshalled startup broadcast packet</p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * <p><code>com.heliosapm.shorthand.broadcast.StartupBroadcastPacketHandler.AgentStartupBroadcast</code></p>
	 */
	public static class AgentStartupBroadcast implements BroadcastExecutable {
		/** The process id of the JVM which started a shorthand agent */
		public final int pid;
		/** The JMXMP port the agent is listening on */
		public final int jmxmpPort;
		/** The address of the source agent */
		public final InetAddress  jmxmpHost;
		
		
		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			builder.append("AgentStartupBroadcast [pid=").append(pid)
					.append(", jmxmpPort=").append(jmxmpPort)
					.append(", jmxmpHost=").append(jmxmpHost).append("]");
			return builder.toString();
		}


		/**
		 * Creates a new AgentStartupBroadcast
		 * @param pid The process id of the JVM which started a shorthand agent
		 * @param jmxmpPort The JMXMP port the agent is listening on
		 * @param jmxmpHost The address of the source agent
		 */
		public AgentStartupBroadcast(int pid, int jmxmpPort, InetSocketAddress jmxmpSocketAddress) {			
			this.pid = pid;
			this.jmxmpPort = jmxmpPort;
			this.jmxmpHost = jmxmpSocketAddress.getAddress();
		}


		@Override
		public void run() {
			// TODO: Enroll the broadcasting JVM if not already enrolled
			
		}
	}

	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.shorthand.broadcast.BroadcastPacketReader#unmarshallPacket(java.nio.ByteBuffer, java.net.InetSocketAddress)
	 */
	@Override
	public AgentStartupBroadcast unmarshallPacket(ByteBuffer broadcast, InetSocketAddress sourceAddress) {		
		return new AgentStartupBroadcast(broadcast.getInt(), broadcast.getInt(), sourceAddress);
	}
	
}
