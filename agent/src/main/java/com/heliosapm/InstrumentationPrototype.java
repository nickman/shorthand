/**
 * Helios, OpenSource Monitoring
 * Brought to you by the Helios Development Group
 *
 * Copyright 2014, Helios Development Group and individual contributors
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
package com.heliosapm;

import java.util.concurrent.atomic.AtomicBoolean;

import com.heliosapm.shorthand.instrumentor.annotations.Instrumented;
import com.heliosapm.shorthand.util.unsafe.UnsafeAdapter;

/**
 * <p>Title: InstrumentationPrototype</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.InstrumentationPrototype</code></p>
 */

public class InstrumentationPrototype {

	/**
	 * Creates a new InstrumentationPrototype
	 */
	public InstrumentationPrototype() {
	
	}
	
	public void normalOp(String sleep) throws NumberFormatException, InterruptedException {
		final long start = System.nanoTime();
		Thread.sleep(Long.parseLong(sleep));
		//System.nanoTime() - start;
	}
	
	@Instrumented(lastInstrumented=1, version=1, types={"ElapsedNanos", "SomethingElse"})
	public void instrumentedOp(String sleep) throws NumberFormatException, InterruptedException {
		final long istart = System.nanoTime();
		final AtomicBoolean failed = new AtomicBoolean(false);
		try {			
			//normalOp(sleep);
			failed.set(false);
		} catch (Exception ex) {
			failed.set(true);
			UnsafeAdapter.throwException(ex);
			throw new RuntimeException(); // won't happen
		} finally {
			final long elapsed = System.nanoTime();
			System.out.println(String.format("Elapsed: %s, Failed: %s", elapsed, failed));
		}
		
	}
	
	
	

}
