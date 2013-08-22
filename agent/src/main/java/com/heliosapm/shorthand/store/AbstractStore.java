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

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.cliffc.high_scale_lib.NonBlockingHashMap;

import com.heliosapm.shorthand.accumulator.MemSpaceAccessor;
import com.heliosapm.shorthand.collectors.ICollector;
import com.heliosapm.shorthand.util.unsafe.UnsafeAdapter;

/**
 * <p>Title: AbstractStore</p>
 * <p>Description: The common base class for {@link IStore} implementations</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.shorthand.store.AbstractStore</code></p>
 * @param <T> The collector type
 */

public abstract class AbstractStore<T extends Enum<T> & ICollector<T>> implements IStore<T> {
	/** The period switch address */
	private final long switchAddress;
	/** The index of metric name to slab id to find the snapshot */
	private final NonBlockingHashMap<String, Long> SNAPSHOT_INDEX_ON;
	/** The index of metric name to slab id to find the snapshot */
	private final NonBlockingHashMap<String, Long> SNAPSHOT_INDEX_OFF;

	private static final byte ON = 1;
	private static final byte OFF = 0;
	
	/**
	 * Creates a new AbstractStore
	 */
	public AbstractStore() {
		switchAddress = UnsafeAdapter.allocateMemory(UnsafeAdapter.BYTE_SIZE);
		UnsafeAdapter.putByte(switchAddress, OFF);
		SNAPSHOT_INDEX_ON = new NonBlockingHashMap<String, Long>(1024); // 1024, 0.75f, ManagementFactory.getOperatingSystemMXBean().getAvailableProcessors()
		SNAPSHOT_INDEX_OFF = new NonBlockingHashMap<String, Long>(1024);
	}
	
	/**
	 * Clears both cycle name indexes
	 */
	protected void clearAll() {
		SNAPSHOT_INDEX_ON.clear();
		SNAPSHOT_INDEX_OFF.clear();
	}
	
	protected boolean putAll(String metricName, long index) {
		return SNAPSHOT_INDEX_ON.put(metricName, index)==null &&
		SNAPSHOT_INDEX_OFF.put(metricName, index)==null;		
	}
	
	/**
	 * Returns the current snapshot index
	 * @return the current snapshot index
	 */
	protected NonBlockingHashMap<String, Long> getSnapshotIndex() {
		return switchOn() ? SNAPSHOT_INDEX_ON : SNAPSHOT_INDEX_OFF;
	}
	/**
	 * Returns the off-cycle snapshot index
	 * @return the off-cycle snapshot index
	 */
	protected NonBlockingHashMap<String, Long> getOffCycleSnapshotIndex() {
		return switchOn() ? SNAPSHOT_INDEX_OFF : SNAPSHOT_INDEX_ON;
	}
	
	protected NonBlockingHashMap<String, Long> getOffCycleSnapshotIndexCopy() {
		NonBlockingHashMap<String, Long> off = switchOn() ? SNAPSHOT_INDEX_OFF : SNAPSHOT_INDEX_ON;
		NonBlockingHashMap<String, Long> copy = new NonBlockingHashMap<String, Long>(off.size());
		copy.putAll(off);
		return copy;
	}
	
	
	private boolean switchOn() {
		return UnsafeAdapter.getByte(switchAddress)==ON;
	}
	
	/**
	 * Switch the snapshot indexes
	 * @return the current name index
	 */
	protected NonBlockingHashMap<String, Long> switchPeriod() {
		UnsafeAdapter.putByte(switchAddress, switchOn() ? OFF : ON);
		return getSnapshotIndex();
	}
	
	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.shorthand.store.IStore#indexKeyValues()
	 */
	@Override
	public Set<Map.Entry<String, Long>> indexKeyValues() {
		return getSnapshotIndex().entrySet();
	}

	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.shorthand.store.IStore#getMetricAddress(java.lang.String)
	 */
	@Override
	public Long getMetricAddress(String metricName) {
		return getSnapshotIndex().get(metricName);
	}
	
	protected void putMetricAddress(String metricName, long address) {
		SNAPSHOT_INDEX_OFF.put(metricName, address);
		long size = new MemSpaceAccessor(address).getMemSize();
		long altAddress = UnsafeAdapter.allocateMemory(size);
		UnsafeAdapter.copyMemory(address, altAddress, size);
		SNAPSHOT_INDEX_ON.put(metricName, altAddress);
	}
	
	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.shorthand.store.IStore#getMetricCacheSize()
	 */
	@Override
	public int getMetricCacheSize() {
		return getSnapshotIndex().size();
	}
	
	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.shorthand.store.IStore#getMetricCacheKeys()
	 */
	@Override
	public Set<String> getMetricCacheKeys() {
		return Collections.unmodifiableSet(getSnapshotIndex().keySet());
	}
}
