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
package com.heliosapm.shorthand.caster;

import com.heliosapm.shorthand.caster.broadcast.BroadcastListener;

/**
 * <p>Title: Boot</p>
 * <p>Description: The bootstap main class for the shorthand agent watcher</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.shorthand.caster.Boot</code></p>
 */

public class Boot {
	/** The non daemon boot thread */
	static Thread bootThread = null;
	/** The boot instance */
	static Boot boot = null;
	
	Boot() {
		BroadcastListener.getInstance();
	}
	
	
	/**
	 * The boot entry point for the shorthand caster.
	 * With no arguments the default is to auto-attach to any located JVMs running the shorthand agent and watch them.
	 * @param args As follows:</ol>
	 * 
	 * </ol>
	 * 
	 */
	public static void main(String[] args) {
		bootThread = new Thread("ShorthandCasterBootThread") {
			public void run() {
				boot = new Boot();
				try {
					Thread.currentThread().join();
				} catch (Exception ex) {}
			}
		};
		bootThread.start();
		try {
			bootThread.join();
		} catch (Exception ex) {
			ex.printStackTrace(System.err);
		}
		

	}

}
