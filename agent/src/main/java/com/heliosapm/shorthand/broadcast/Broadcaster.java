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

import static com.heliosapm.shorthand.ShorthandProperties.AGENT_BROADCAST_NETWORK_PROP;
import static com.heliosapm.shorthand.ShorthandProperties.AGENT_BROADCAST_NIC_PROP;
import static com.heliosapm.shorthand.ShorthandProperties.AGENT_BROADCAST_PORT_PROP;
import static com.heliosapm.shorthand.ShorthandProperties.DEFAULT_BROADCAST_NETWORK;
import static com.heliosapm.shorthand.ShorthandProperties.DEFAULT_BROADCAST_NIC;
import static com.heliosapm.shorthand.ShorthandProperties.DEFAULT_BROADCAST_PORT;
import static com.heliosapm.shorthand.ShorthandProperties.DEFAULT_DISABLE_BROADCAST_NETWORK;
import static com.heliosapm.shorthand.ShorthandProperties.DISABLE_BROADCAST_NETWORK_PROP;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArraySet;

import com.heliosapm.shorthand.util.ConfigurationHelper;

/**
 * <p>Title: Broadcaster</p>
 * <p>Description: The event broadcaster service</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.shorthand.broadcast.Broadcaster</code></p>
 */

public class Broadcaster implements Runnable {
	/** The singleton instance */
	private static volatile Broadcaster instance = null;
	/** The singleton instance ctor lock */
	private static final Object lock = new Object();
	

	/** A set of activated broadcast sockets */
	protected final Set<DatagramSocket> broadcastSockets = new CopyOnWriteArraySet<DatagramSocket>();
	/** Indicates if broadcasting is enabled */
	protected final boolean enabled;
	/** The network interface name to bind multicast sockets to */
	protected final String nic;
	/** The broadcast execution thread */
	protected final Thread executionThread;
	/** The broadcast execution queue */
	protected final BlockingQueue<byte[]> executionQueue;
	
	private Broadcaster() {
		enabled = !ConfigurationHelper.getBooleanSystemThenEnvProperty(DISABLE_BROADCAST_NETWORK_PROP, DEFAULT_DISABLE_BROADCAST_NETWORK);
		if(enabled) {
			nic = ConfigurationHelper.getSystemThenEnvProperty(AGENT_BROADCAST_NIC_PROP, DEFAULT_BROADCAST_NIC);
			executionQueue = new ArrayBlockingQueue<byte[]>(128, false);
			executionThread = new Thread(this, "BroadcastExecutionThread");
			executionThread.setDaemon(true);
			initSockets();
			executionThread.start();
			log("\n\t=====================\n\tBroadcaster Started\n\t=====================");
		} else {
			nic = null;
			executionThread = null;
			executionQueue = null;
		}
	}
	
	/**
	 * Initializes the broadcast sockets 
	 */
	private void initSockets() {
		String[] addresses = ConfigurationHelper.getSystemThenEnvPropertyArray(AGENT_BROADCAST_NETWORK_PROP, DEFAULT_BROADCAST_NETWORK);
		int[] ports = ConfigurationHelper.getIntSystemThenEnvPropertyArray(AGENT_BROADCAST_PORT_PROP, "" + DEFAULT_BROADCAST_PORT);
		if(addresses.length!=ports.length) {
			throw new RuntimeException("Invalid broadcast configuration. Number of addresses != Number of ports. Addresses:" + Arrays.toString(addresses) + " Ports:" + Arrays.toString(ports));
		}
		for(int i = 0; i < addresses.length; i++) {
			try {
				InetAddress address = InetAddress.getByName(addresses[i]);
				if(address.isMulticastAddress()) {
					MulticastSocket msocket = new MulticastSocket(new InetSocketAddress(ports[i]));
					try {
						NetworkInterface ni = NetworkInterface.getByName(nic);
						msocket.setNetworkInterface(ni);
					} catch (Exception ex) {/* No Op */}
					msocket.joinGroup(address);
					broadcastSockets.add(msocket);
				} else {
					DatagramSocket dsocket = new DatagramSocket(ports[i], address);
					broadcastSockets.add(dsocket);
				}
				log("Connected broadcast socket [%s:%s]", addresses[i], ports[i]);
			} catch (Exception ex) {
				loge("Failed to connect broadcast socket [%s:%s]", ex, addresses[i], ports[i]);
			}
		}
	}
	
	/**
	 * Acquires the broadcaster singleton
	 * @return the broadcaster singleton
	 */
	public static Broadcaster getInstance() {
		if(instance==null) {
			synchronized(lock) {
				if(instance==null) {
					instance = new Broadcaster();
				}
			}
		}
		return instance;
	}
	
	/**
	 * Sends the passed broadcast message to all broadcast sockets
	 * @param packet The packet content to send
	 */
	public void send(byte[] packet) {
		if(packet!=null && enabled) {
			if(!executionQueue.offer(packet)) {
				loge("Failed to enqueue broadcast packet");
			}
		}
	}
	
	/**
	 * <p>The execution thread runnable</p>
	 * {@inheritDoc}
	 * @see java.lang.Runnable#run()
	 */
	public void run() {
		log("Started Broadcaster Execution Thread");
		while(true) {
			try {
				byte[] packet = executionQueue.take();
				if(packet!=null) {
					DatagramPacket dp = new DatagramPacket(packet, packet.length);
					for(DatagramSocket ds: broadcastSockets) {
						try {
							log("Interface [%s] Port [%s]", ds.getLocalSocketAddress() , ds.getLocalPort());
							if(ds instanceof MulticastSocket) {
								dp.setAddress(((MulticastSocket)ds).getInterface());
								dp.setPort((((InetSocketAddress)ds.getLocalSocketAddress()).getPort()));
							}
							ds.send(dp);
							log("Sent Broadcast to [%s]", ds);
						} catch (Exception ex) {
							loge("Failed to send broadcast to [%s]", ex, ds);
						}
					}
				}
			} catch (Exception ex) {
				/* No Op */
			}
		}
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
}
