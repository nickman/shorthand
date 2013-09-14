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
package com.heliosapm.shorthand.store;

import java.io.File;
import java.lang.management.ManagementFactory;

/**
 * <p>Title: ShorthandChronicleCleaner</p>
 * <p>Description: Cleans old inactive shorthand chronicle directories</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.shorthand.store.ShorthandChronicleCleaner</code></p>
 */

public class ShorthandChronicleCleaner extends Thread {
	/** The chronicle directory to clean */
	private final File chronicleDir;
	
	/** This JVM's Process ID */
	public static final String PID = ManagementFactory.getRuntimeMXBean().getName().split("@")[0];
	/**
	 * Creates a new ShorthandChronicleCleaner
	 * @param directoryName The chronicle directory to clean
	 */
	public ShorthandChronicleCleaner(String directoryName) {
		super("ShorthandChronicleCleaner");
		setDaemon(true);
		chronicleDir = new File(directoryName);
	}
	
	/**
	 * {@inheritDoc}
	 * @see java.lang.Thread#run()
	 */
	@Override
	public void run() {
		int filesDeleted = 0, dirsDeleted = 0;
		log("Cleaning Chronicle Dir [" + chronicleDir + "]");
		for(File pDir: chronicleDir.listFiles()) {
			if(pDir.isDirectory()) {
				log("Cleaning Chronicle Dir [%s] PID: [%s]", pDir, pDir.getName());
				if(PID.equals(pDir.getName())) continue;
				File lockFile = new File(pDir.getAbsoluteFile() + File.separator + "shorthand.lock");
				if(lockFile.exists()) {
					if(!lockFile.delete()) {
						log("Lock File [" + lockFile + "] is locked. Must be active");
						continue;
					}
				}
				filesDeleted++;
				for(File pFile: pDir.listFiles()) {
					pFile.delete();
					filesDeleted++;
				}
				if(!pDir.delete()) {
					log("Failed to delete Chronicle Dir [" + pDir + "]");
				} else {
					log("Cleaned Chronicle Dir [" + pDir + "]");
					dirsDeleted++;
				}
			}
		}
		log("Cleaned [%s] files and [%s] directories", filesDeleted, dirsDeleted);
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
