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
package com.heliosapm.shorthand.store;

import java.lang.management.ManagementFactory;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.cliffc.high_scale_lib.NonBlockingHashMap;

import com.heliosapm.shorthand.accumulator.MemSpaceAccessor;
import com.heliosapm.shorthand.collectors.CollectorSet;
import com.heliosapm.shorthand.collectors.EnumCollectors;
import com.heliosapm.shorthand.collectors.ICollector;
import com.heliosapm.shorthand.util.unsafe.UnsafeAdapter;

/**
 * <p>Title: AbstractStore</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.shorthand.store.AbstractStore</code></p>
 * @param <T> The collector type
 */

public abstract class AbstractStore<T extends Enum<T> & ICollector<T>> implements IStore<T> {
	
	/** The index of metric name to slab id to find the snapshot */
	protected final NonBlockingHashMap<String, Long> SNAPSHOT_INDEX;
	/** The index of metric name to chronicle index for unloaded metrics */
	protected final NonBlockingHashMap<String, Long> UNLOADED_INDEX;
	
	/** Indicates if the mem-spaces should be padded */
	protected boolean padCache = true;
    /** The system prop name to indicate if mem-spaces should be padded to the next largest pow(2) */
    public static final String USE_POW2_ALLOC_PROP = "shorthand.memspace.padcache";
	

	/**
	 * Creates a new AbstractStore
	 */
	protected AbstractStore() {
		SNAPSHOT_INDEX = new NonBlockingHashMap<String, Long>(1024);
		UNLOADED_INDEX = new NonBlockingHashMap<String, Long>(1024);
		padCache = System.getProperty(USE_POW2_ALLOC_PROP, "true").toLowerCase().trim().equals("true");
	}
	
	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.shorthand.store.IStore#indexKeyValues()
	 */
	@Override
	public Set<Map.Entry<String, Long>> indexKeyValues() {
		return SNAPSHOT_INDEX.entrySet();
	}
	
	/** The byte value that all newly created mem-spaces are initialized to */
	public static final byte MEM_INIT = 0;

	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.shorthand.store.IStore#getMetricAddress(java.lang.String, com.heliosapm.shorthand.collectors.CollectorSet)
	 */
	@Override
	public long getMetricAddress(String metricName, CollectorSet<T> collectorSet) {
		Long address = SNAPSHOT_INDEX.get(metricName);
		if(address==null) address = UNLOADED_INDEX.get(metricName);
		if(address==null || address <0) {
			synchronized(SNAPSHOT_INDEX) {
				address = SNAPSHOT_INDEX.get(metricName);
				if(address==null || address <0) {
					int requestedMem = (int)(collectorSet.getTotalAllocation());
					int memSize = padCache ? findNextPositivePowerOfTwo(requestedMem) : requestedMem;
					long nameIndex;
					boolean unloaded = false;
					if(address==null) {
						nameIndex = newMetricName(metricName, (T) collectorSet.getReferenceCollector(), collectorSet.getBitMask());
					} else {
						nameIndex = address * -1;
						unloaded = true;
					}
					address = UnsafeAdapter.allocateMemory(memSize);
					MemSpaceAccessor.get(address).initializeHeader(memSize, nameIndex, collectorSet.getBitMask(), EnumCollectors.getInstance().index(collectorSet.getReferenceCollector().getDeclaringClass().getName()));
					MemSpaceAccessor.get(address).reset();
					long memSpaceRef = UnsafeAdapter.allocateMemory(UnsafeAdapter.LONG_SIZE * 2);
					UnsafeAdapter.putLong(memSpaceRef, 0);
					UnsafeAdapter.putLong(memSpaceRef + UnsafeAdapter.LONG_SIZE, address);
					SNAPSHOT_INDEX.put(metricName, memSpaceRef);
					if(unloaded) UNLOADED_INDEX.remove(metricName);
					address = memSpaceRef;
				}
			}
		}
		return address;
		
		
	}
	
	
	
	/**
     * Finds the next positive power of 2 for the passed value
     * @param value the value to find the next power of 2 for
     * @return the next power of 2
     */
    public static int findNextPositivePowerOfTwo(final int value) {
		return 1 << (32 - Integer.numberOfLeadingZeros(value - 1));
	}	
	
	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.shorthand.store.IStore#cacheMetricAddress(java.lang.String, long)
	 */
	@Override
	public void cacheMetricAddress(String metricName, long address) {
		SNAPSHOT_INDEX.put(metricName, address);
	}
	
	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.shorthand.store.IStore#getMetricCacheSize()
	 */
	@Override
	public int getMetricCacheSize() {
		return SNAPSHOT_INDEX.size();
	}
	
	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.shorthand.store.IStore#getMetricCacheKeys()
	 */
	@Override
	public Set<String> getMetricCacheKeys() {
		return Collections.unmodifiableSet(SNAPSHOT_INDEX.keySet());
	}
	
	@SuppressWarnings("javadoc")
	public static void log(String fmt, Object...msgs) {
		System.out.println(String.format("[ChronicleStore]" + fmt, msgs));
	}
	@SuppressWarnings("javadoc")
	public static void loge(String fmt, Throwable t, Object...msgs) {
		System.err.println(String.format("[ChronicleStore]" + fmt, msgs));
		if(t!=null) t.printStackTrace(System.err);
	}
	@SuppressWarnings("javadoc")
	public static void loge(String fmt, Object...msgs) {
		loge(fmt, null, msgs);
	}	
}
