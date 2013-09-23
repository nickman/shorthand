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

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * <p>Title: BroadcastType</p>
 * <p>Description: Enumerates the broadcast types and the basic meta-data of the packet structure for each.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.shorthand.broadcast.BroadcastType</code></p>
 */

public enum BroadcastType implements BroadcastPacketWriter, BroadcastPacketReader<BroadcastExecutable> {
	/** Broadcast when a shorthand agent is started */
	STARTUP(StartupBroadcastPacketHandler.INSTANCE, StartupBroadcastPacketHandler.INSTANCE);
	
	/** A map of BroadcastTypes keyed by the ordinal */
	public static final Map<Integer, BroadcastType> ORD2ENUM;
	
	static {
		BroadcastType[] values = BroadcastType.values();
		Map<Integer, BroadcastType> tmpOrd2Enum = new HashMap<Integer, BroadcastType>(values.length);
		for(BroadcastType bt: values) {
			tmpOrd2Enum.put(bt.ordinal(), bt);
		}
		ORD2ENUM = Collections.unmodifiableMap(tmpOrd2Enum);
	}
	
	private BroadcastType(BroadcastPacketWriter packetWriter, BroadcastPacketReader<BroadcastExecutable> packetReader) {
		this.packetWriter = packetWriter;
		this.packetReader = packetReader;
		
	}
	
	private final BroadcastPacketWriter packetWriter;
	private final BroadcastPacketReader<BroadcastExecutable> packetReader;
	
	
	/**
	 * Returns the BroadcastType for the passed ordinal
	 * @param ordinal The ordinal of a BroadcastType
	 * @return the decoded BroadcastType
	 */
	public static BroadcastType ordinal(Number ordinal) {
		if(ordinal==null) throw new IllegalArgumentException("The passed ordinal was null");
		BroadcastType bt = ORD2ENUM.get(ordinal);
		if(bt==null) throw new IllegalArgumentException("The number [" + ordinal + "] is not a valid BroadcastType Ordinal");
		return bt;
	}
	
	/**
	 * Returns the BroadcastType for the passed key
	 * @param key The key which is deciphered into a BroadcastType
	 * @return the decoded BroadcastType
	 */
	public static BroadcastType forValue(Object key) {
		if(key==null) throw new IllegalArgumentException("The passed key was null");
		if(key instanceof Number) return ordinal((Number)key);
		try {
			int ord = -1;
			try {
				ord = new Double(key.toString().trim()).intValue();				
			} catch (Exception ex) {}
			if(ord!=-1) return ordinal(ord);
			return BroadcastType.valueOf(key.toString().trim().toUpperCase());
		} catch (Exception ex) {
			throw new IllegalArgumentException("The value [" + key + "] is not a valid BroadcastType name");
		}
	}

	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.shorthand.broadcast.BroadcastPacketWriter#buildPacket(java.lang.Object[])
	 */
	@Override
	public byte[] buildPacket(Object... args) {
		return packetWriter.buildPacket(args);
	}

	@Override
	public BroadcastExecutable unmarshallPacket(ByteBuffer broadcast, InetSocketAddress sourceAddress) {
		// TODO Auto-generated method stub
		return null;
	}
}
