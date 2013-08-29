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
package com.heliosapm.shorthand.util.ref;

import java.io.File;
import java.lang.ref.PhantomReference;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import com.heliosapm.shorthand.util.jmx.JMXHelper;
import com.heliosapm.shorthand.util.ref.RunnableReferenceQueue.DeallocatingPhantomReference;
import com.heliosapm.shorthand.util.unsafe.UnsafeAdapter;

/**
 * <p>Title: MySlabbedClass</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.shorthand.util.ref.MySlabbedClass</code></p>
 */

public class MySlabbedClass {
	
	/** The address to the memory allocated for this instance */
	private final long address;
	
	
	private MySlabbedClass() {
		address = UnsafeAdapter.allocateMemory(100);
		RunnableReferenceQueue.getInstance().buildPhantomReference(this, address);		
	}


	private static final int HEAPDUMP = 100000;
	
	public static void main(String[] args) {
		log("Slabbed Memory Deallocation Test");
		for(File f: new File(System.getProperty("java.io.tmpdir")).listFiles()) {
			if(f.getName().contains("SlabHeapDump")) f.delete();
		}
		for(int i = 0; i < 10000000; i++) {
			if(i%10000==0) {
				log("Created [%s]  Uncleared: %s", i, RunnableReferenceQueue.getInstance().getUnclearedReferenceCount());
				System.gc();
				try { Thread.sleep(500); } catch (Exception ex) {}				
			}
			if(i!=0 && i%HEAPDUMP==0) {
				heapDump(i/HEAPDUMP);
			}
			MySlabbedClass instance = new MySlabbedClass();		
			instance = null;
		}
	}
	
	private static final ObjectName HOTSPOT = JMXHelper.objectName("com.sun.management:type=HotSpotDiagnostic");
	private static final MBeanServer SERVER = JMXHelper.getHeliosMBeanServer();
	private static final String[] SIG = new String[]{String.class.getName(), Boolean.TYPE.getName()}; 
	
	private static void heapDump(int seq) {
		try {
			String fileName = System.getProperty("java.io.tmpdir") + "SlabHeapDump" + seq + ".dmp";			
			SERVER.invoke(HOTSPOT, "dumpHeap", new Object[]{fileName, true}, SIG);
			log("Dumped heap to: " + fileName);
		} catch (Exception ex) {
			ex.printStackTrace(System.err);
		}
	}
	
	public static void log(String format, Object...args) {
		System.out.println(String.format(format, args));
	}
	
}
