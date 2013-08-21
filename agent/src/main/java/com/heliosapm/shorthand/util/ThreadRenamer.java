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
package com.heliosapm.shorthand.util;

import java.util.Arrays;
import java.util.Stack;

/**
 * <p>Title: ThreadRenamer</p>
 * <p>Description: Stack based thread renaming push and pop</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.shorthand.util.ThreadRenamer</code></p>
 */

public class ThreadRenamer {
	private static final ThreadLocal<Stack<String>> namestack = new ThreadLocal<Stack<String>>() {		
		@Override
		protected Stack<String> initialValue() {
			return new Stack<String>();
		}
	};
	
	/**
	 * Appends the passed string value to the current thread name
	 * @param cs the value to append to the thread name
	 * @param items Token fillin args for the new thread name append
	 */
	public static void push(CharSequence cs, Object...items) {
		if(cs!=null) {
			Thread t = Thread.currentThread();
			namestack.get().push(t.getName());
			char[] indents = new char[namestack.get().size()];
			Arrays.fill(indents, '\t');			
			t.setName(new StringBuilder(t.getName()).append("\n").append(indents).append(String.format(cs.toString(), items)).toString());
		}
	}
	
	/**
	 * Restores the name of the current thread to its state before the most recent {@link #push(CharSequence, Object...)} call.
	 */
	public static void pop() {
		if(namestack.get().isEmpty()) return;
		Thread.currentThread().setName(namestack.get().pop());
	}
}
