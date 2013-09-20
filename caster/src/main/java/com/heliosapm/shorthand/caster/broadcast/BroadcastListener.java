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

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

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
