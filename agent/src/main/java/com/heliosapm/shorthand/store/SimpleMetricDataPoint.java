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

import gnu.trove.map.hash.TObjectLongHashMap;

import com.heliosapm.shorthand.collectors.ICollector;

/**
 * <p>Title: SimpleMetricDataPoint</p>
 * <p>Description: A container for the metric points for an individual icollector</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.shorthand.store.SimpleMetricDataPoint</code></p>
 * @param <T> The collector type
 */
public class SimpleMetricDataPoint<T extends Enum<T> & ICollector<T>> implements IMetricDataPoint<T> {
	/** The metric's source collector */
	protected final T collector;
	/** The submetric data points */
	protected final TObjectLongHashMap<String> dataPoints;
	
	/**
	 * Creates a new SimpleMetricDataPoint
	 * @param collector the collector for this metric data point
	 * @param dataPoints the data points from tier1
	 */
	SimpleMetricDataPoint(T collector, TObjectLongHashMap<String> dataPoints) {
		this.collector = collector;
		this.dataPoints = dataPoints;
	}

	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.shorthand.store.IMetricDataPoint#getCollector()
	 */
	@Override
	public T getCollector() {
		return collector;
	}

	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.shorthand.store.IMetricDataPoint#getShortName()
	 */
	@Override
	public String getShortName() {
		return collector.getShortName();
	}

	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.shorthand.store.IMetricDataPoint#getUnit()
	 */
	@Override
	public String getUnit() {
		return collector.getUnit();
	}

	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.shorthand.store.IMetricDataPoint#getDataPoints()
	 */
	@Override
	public TObjectLongHashMap<String> getDataPoints() {
		return dataPoints;
	}		
}
