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
package com.heliosapm.shorthand.instrumentor.shorthand;

import java.util.concurrent.atomic.AtomicBoolean;

import org.cliffc.high_scale_lib.Counter;
import org.cliffc.high_scale_lib.NonBlockingHashMap;

import com.heliosapm.shorthand.accumulator.MetricSnapshotAccumulator;
import com.heliosapm.shorthand.datamapper.IDataMapper;

/**
 * <p>Title: ShorthandStaticInterceptor</p>
 * <p>Description: Instrumented methods are injected with customized child class instances of this class.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.shorthand.instrumentor.shorthand.ShorthandStaticInterceptor</code></p>
 */

public class ShorthandStaticInterceptor {
	/** A reference to the snapshot accumulator */
	protected static final MetricSnapshotAccumulator<?> accumulator = MetricSnapshotAccumulator.getInstance();
	
	/** Shared and object keyed counters */
	protected static final NonBlockingHashMap<Object, Counter> counters = new NonBlockingHashMap<Object, Counter>();
	/** Shared and object keyed flags */
	protected static final NonBlockingHashMap<Object, AtomicBoolean> flags = new NonBlockingHashMap<Object, AtomicBoolean>(); 

	
	/**
	 * Delegates this snapshot collection to the {@link MetricSnapshotAccumulator} 
	 * @param metricName The metric name
	 * @param dataMapper The data mapper supplied by child instance
	 * @param collectedValues The values collected being passed as a snapshot
	 */
	protected static void snap(String metricName, IDataMapper<?> dataMapper, long...collectedValues) {
		accumulator.snap(metricName, dataMapper, collectedValues);
	}
	
	
	/*
	 * Extended classes will invoke these IDataMapper methods:
	 *
	 * public long[] methodEnter(int bitMask);
     * public long[] methodExit(long[] values);			--> snap
     * public long[] methodException(long[] values);	--> snap
     * 
     * Extended classes will supply these methods:
     * 
     * public static long[] methodEnter()
     * public static void methodExit(long[])
     * public static void methodError(long[])
     *  
	 */
}
