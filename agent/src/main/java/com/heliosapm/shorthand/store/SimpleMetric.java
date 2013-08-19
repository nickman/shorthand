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

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import com.heliosapm.shorthand.collectors.ICollector;

/**
 * <p>Title: SimpleMetric</p>
 * <p>Description: A simple pojo IMetric implementation</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.shorthand.store.SimpleMetric</code></p>
 * @param <T> The collector type
 */

public class SimpleMetric<T extends Enum<T> & ICollector<T>> implements IMetric<T> {
	/** The metric instance datapoints */
	protected final Map<T, IMetricDataPoint<T>> dataPoints;
	/** The metric name */
	protected final String name;
	/** The metric's collector type */
	protected final String collectorName;
	
	/** The metric period start timestamp */
	protected final long startTime;
	/** The metric period end timestamp */
	protected final long endTime;

	/**
	 * Creates a new SimpleMetric
	 * @param name The metric name
	 * @param collectorType The collector type
	 * @param startTime The period start time
	 * @param endTime The period end time
	 * @param dataPoints The metric data points
	 */
	SimpleMetric(String name, Class<T> collectorType, long startTime, long endTime, Map<T, IMetricDataPoint<T>> dataPoints) {
		this.dataPoints =  dataPoints;
		this.startTime = startTime; this.endTime = endTime;
		this.name = name;
		collectorName = collectorType.getSimpleName();
	}

	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.shorthand.store.IMetric#getName()
	 */
	@Override
	public String getName() {
		return name;
	}

	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.shorthand.store.IMetric#getPeriodStart()
	 */
	@Override
	public long getPeriodStart() {
		return startTime;
	}

	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.shorthand.store.IMetric#getPeriodEnd()
	 */
	@Override
	public long getPeriodEnd() {
		return endTime;
	}

	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.shorthand.store.IMetric#getPeriodStartDate()
	 */
	@Override
	public Date getPeriodStartDate() {
		return new Date(startTime);
	}

	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.shorthand.store.IMetric#getPeriodEndDate()
	 */
	@Override
	public Date getPeriodEndDate() {
		return new Date(endTime);
	}

	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.shorthand.store.IMetric#getCollectorTypeName()
	 */
	@Override
	public String getCollectorTypeName() {
		return collectorName;
	}
	
	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.shorthand.store.IMetric#getMetricDataPoints()
	 */
	@Override
	public Map<T, IMetricDataPoint<T>> getMetricDataPoints() {
		return dataPoints;
	}

	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		StringBuilder b = new StringBuilder("Metric [").append(name).append("]");
		b.append(" Collector:").append(getCollectorTypeName());
		b.append(" Period:[").append(getPeriodStartDate()).append(" -> ").append(getPeriodEndDate());
		b.append("\nDataPoints: [");
		for(Map.Entry<T, IMetricDataPoint<T>> entry: getMetricDataPoints().entrySet()) {
			b.append("\n\t").append(entry.getKey().name());
			T collector = entry.getValue().getCollector();
			TObjectLongHashMap<String> map = entry.getValue().getDataPoints();
			for(String subName: collector.getSubMetricNames()) {
				b.append("\n\t\t").append(subName).append(":").append(map.get(subName));
			}
		}
		b.append("\n]");
		return b.toString();
	}

	

	

}
