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
package com.heliosapm.shorthand.caster.broadcast;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.socket.DatagramPacket;
import io.netty.handler.codec.MessageToMessageDecoder;

import java.util.List;
import java.util.concurrent.ExecutorService;

import com.heliosapm.shorthand.broadcast.BroadcastExecutable;
import com.heliosapm.shorthand.broadcast.BroadcastType;

/**
 * <p>Title: BroadcastListenerRouter</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.shorthand.caster.broadcast.BroadcastListenerRouter</code></p>
 */


public class BroadcastListenerRouter extends MessageToMessageDecoder<DatagramPacket> {
	/** The task execution thread pool */
	private final ExecutorService taskThreadPool;
	
	
	/**
	 * Creates a new BroadcastListenerRouter
	 * @param taskThreadPool The task execution thread pool
	 */
	public BroadcastListenerRouter(ExecutorService taskThreadPool) {
		super();
		this.taskThreadPool = taskThreadPool;
	}


	@Override
	protected void decode(ChannelHandlerContext ctx, DatagramPacket msg, List<Object> out) throws Exception {
		ByteBuf data = msg.content();
		byte msgType = data.readByte();
		BroadcastType bt = BroadcastType.ORD2ENUM.get(msgType);
		log("Processing Broadcast [%s]", bt.name());
		BroadcastExecutable exec = bt.unmarshallPacket(data.nioBuffer(), msg.sender());
		taskThreadPool.execute(exec);
	}
	
	/**
	 * Simple out formatted logger
	 * @param fmt The format of the message
	 * @param args The message arguments
	 */
	public static void log(String fmt, Object...args) {
		System.out.println(String.format(fmt, args));
	}
}
