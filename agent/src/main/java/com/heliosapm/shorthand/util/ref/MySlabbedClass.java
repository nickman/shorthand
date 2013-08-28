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

import java.lang.ref.WeakReference;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import com.heliosapm.shorthand.util.unsafe.UnsafeAdapter;

/**
 * <p>Title: MySlabbedClass</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.shorthand.util.ref.MySlabbedClass</code></p>
 */

public class MySlabbedClass {
	private static final Set<WeakReference<MySlabbedClass>> phantomRefs = new CopyOnWriteArraySet<WeakReference<MySlabbedClass>>();
	
	/** The address to the memory allocated for this instance */
	private final long address;
	
	private MySlabbedClass() {
		address = UnsafeAdapter.allocateMemory(100);
	}

	public static MySlabbedClass newInstance() {
		final MySlabbedClass instance = new MySlabbedClass();
		phantomRefs.add(RunnableReferenceQueue.getInstance().buildWeakReference(instance, new Runnable(){
			public void run() {
				UnsafeAdapter.freeMemory(instance.address);
				phantomRefs.remove(this);
			}
		}));
		return instance;
	}

	
	public static void main(String[] args) {
		log("Slabbed Memory Deallocation Test");
		for(int i = 0; i < 10000; i++) {
			if(i%10==0) {
				log("Created [%s]  Phantoms: [%s]", i, phantomRefs.size());
				System.gc();
			}
			MySlabbedClass instance = MySlabbedClass.newInstance();
			try { Thread.sleep(500); } catch (Exception ex) {}
		}
	}
	
	public static void log(String format, Object...args) {
		System.out.println(String.format(format, args));
	}
	
}
