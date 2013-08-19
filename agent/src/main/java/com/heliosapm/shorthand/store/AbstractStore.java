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

import com.heliosapm.shorthand.collectors.ICollector;

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
	protected final ConcurrentHashMap<String, Long> SNAPSHOT_INDEX;

	/**
	 * Creates a new AbstractStore
	 */
	public AbstractStore() {
		SNAPSHOT_INDEX = new ConcurrentHashMap<String, Long>(1024, 0.75f, ManagementFactory.getOperatingSystemMXBean().getAvailableProcessors());
	}
	
	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.shorthand.store.IStore#indexKeyValues()
	 */
	@Override
	public Set<Map.Entry<String, Long>> indexKeyValues() {
		return SNAPSHOT_INDEX.entrySet();
	}

	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.shorthand.store.IStore#getMetricAddress(java.lang.String)
	 */
	@Override
	public Long getMetricAddress(String metricName) {
		return SNAPSHOT_INDEX.get(metricName);
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
}
