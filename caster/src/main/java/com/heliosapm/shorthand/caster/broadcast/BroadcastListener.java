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

import static com.heliosapm.shorthand.ShorthandProperties.AGENT_BROADCAST_NETWORK_PROP;
import static com.heliosapm.shorthand.ShorthandProperties.AGENT_BROADCAST_PORT_PROP;
import static com.heliosapm.shorthand.ShorthandProperties.DEFAULT_AGENT_BROADCAST_NETWORK;
import static com.heliosapm.shorthand.ShorthandProperties.DEFAULT_AGENT_BROADCAST_PORT;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.nio.NioEventLoop;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.util.concurrent.GlobalEventExecutor;

import java.lang.management.ManagementFactory;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

import com.heliosapm.shorthand.util.ConfigurationHelper;

/**
 * <p>Title: BroadcastListener</p>
 * <p>Description: Listens for broadcasts from shorthand agents</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.shorthand.caster.broadcast.BroadcastListener</code></p>
 */

public class BroadcastListener implements ThreadFactory {
	/** The singleton instance */
	private static volatile BroadcastListener instance = null;
	/** The singleton instance ctor lock */
	private static final Object lock = new Object();

	/** Serial number factory for created threads */
	private final AtomicInteger threadSerial = new AtomicInteger();
	
    /** The netty listener bootstrap */
    private final Bootstrap bootstrap;
    /** The netty listener event loop group */
    private final EventLoopGroup group;
    /** The non-eventloop task executor */
    private final ExecutorService taskThreadPool;
    /** The request router */
    private final BroadcastListenerRouter router;

    /** The channel group of bound channels */
    private final ChannelGroup boundChannels;
	
	
	/**
	 * Acquires the broadcast-listener singleton
	 * @return the broadcast-listener singleton
	 */
	public static BroadcastListener getInstance() {
		if(instance==null) {
			synchronized(lock) {
				if(instance==null) {
					instance = new BroadcastListener();
				}
			}
		}
		return instance;
	}
	
	private BroadcastListener() {
        group = new NioEventLoopGroup(0, this);
        bootstrap = new Bootstrap();
        taskThreadPool = Executors.newFixedThreadPool(ManagementFactory.getOperatingSystemMXBean().getAvailableProcessors(), new ThreadFactory(){
        	final AtomicInteger serial = new AtomicInteger();
        	@Override
        	public Thread newThread(Runnable r) {
        		Thread t = new Thread(r, "TaskExecutionThread#" + serial.incrementAndGet());
        		t.setDaemon(true);
        		return t;
        	}
        });
        router = new BroadcastListenerRouter(taskThreadPool);
        boundChannels = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
		String[] addresses = ConfigurationHelper.getSystemThenEnvPropertyArray(AGENT_BROADCAST_NETWORK_PROP, DEFAULT_AGENT_BROADCAST_NETWORK);
		int[] ports = ConfigurationHelper.getIntSystemThenEnvPropertyArray(AGENT_BROADCAST_PORT_PROP, "" + DEFAULT_AGENT_BROADCAST_PORT);
		if(addresses.length!=ports.length) {
			throw new RuntimeException("Invalid broadcast configuration. Number of addresses != Number of ports. Addresses:" + Arrays.toString(addresses) + " Ports:" + Arrays.toString(ports));
		}
		for(int i = 0; i < addresses.length; i++) {
			try {
				startListener(new InetSocketAddress(addresses[i], ports[i]));
			} catch (Exception ex) { 
				loge("Failed to start listener on [%s:%s]", ex, addresses[i], ports[i]);
			}
		}
	}
	
	/**
	 * Starts a listener on the passed socket address
	 * @param isa The socket address to listen on
	 */
	public void startListener(InetSocketAddress isa) {
		if(isa.getAddress().isMulticastAddress()) {
			startMulticastListener(isa);
		} else {
			Channel channel = bootstrap.group(group)
			        .channel(NioDatagramChannel.class)        
			        .option(ChannelOption.SO_BROADCAST, true)
			        .handler(new ChannelInitializer<Channel>() {
			            @Override
			            protected void initChannel(Channel channel) throws Exception {
			                ChannelPipeline pipeline = channel.pipeline();
			                pipeline.addLast(new LoggingHandler(BroadcastListener.class, LogLevel.DEBUG));
			                pipeline.addLast(router);
			            }
			        }).localAddress(isa).bind().syncUninterruptibly().channel();
			boundChannels.add(channel);
			log("Started Broadcast Listener on [%s]", isa);
 		}
	}
	
	
	/**
	 * Starts a listener on the passed socket address
	 * @param isa The socket address to listen on
	 */
	protected void startMulticastListener(InetSocketAddress isa) {
		bootstrap.group(group)
        .channel(NioDatagramChannel.class)        
        .option(ChannelOption.SO_BROADCAST, true)
        .handler(new ChannelInitializer<Channel>() {
            @Override
            protected void initChannel(Channel channel) throws Exception {
                ChannelPipeline pipeline = channel.pipeline();
                pipeline.addLast(new LoggingHandler(BroadcastListener.class, LogLevel.DEBUG));
                pipeline.addLast(router);
            }
        }).localAddress(isa);
		
		NioDatagramChannel channel = (NioDatagramChannel)bootstrap.bind(isa.getPort()).syncUninterruptibly().channel();
		channel.joinGroup(isa.getAddress());
		
		
        
        //.bind().syncUninterruptibly().channel();
        boundChannels.add(channel);
        log("Started Broadcast Listener on [%s]", isa);

	}
	
	/**
	 * Simple out formatted logger
	 * @param fmt The format of the message
	 * @param args The message arguments
	 */
	public static void log(String fmt, Object...args) {
		System.out.println(String.format(fmt, args));
	}
	
	/**
	 * Simple err formatted logger
	 * @param fmt The format of the message
	 * @param args The message arguments
	 */
	public static void loge(String fmt, Object...args) {
		System.err.println(String.format(fmt, args));
	}
	
	/**
	 * Simple err formatted logger
	 * @param fmt The format of the message
	 * @param t The throwable to print stack trace for
	 * @param args The message arguments
	 */
	public static void loge(String fmt, Throwable t, Object...args) {
		System.err.println(String.format(fmt, args));
		t.printStackTrace(System.err);
	}

	@Override
	public Thread newThread(Runnable r) {
		Thread t = new Thread(r, "BroadcastListenerThread#" + threadSerial.incrementAndGet());
		t.setDaemon(true);
		return t;
	}	
}
