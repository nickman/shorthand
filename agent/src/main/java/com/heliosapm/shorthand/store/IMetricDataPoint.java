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

/**
 * <p>Title: IMetricDataPoint</p>
 * <p>Description: Defines the datapoints of an {@link IMetric}.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.shorthand.store.IMetricDataPoint</code></p>
 * @param <T> The collector type
 */

public interface IMetricDataPoint<T> {
	/**
	 * Returns the collector instance
	 * @return the collector instance
	 */
	public T getCollector();
	/**
	 * Returns the metric short name
	 * @return the metric short name
	 */
	public String getShortName();
	/**
	 * Returns the metric unit
	 * @return the metric unit
	 */
	public String getUnit();
	/**
	 * Returns a map of the period data points for this metric and collector keyed by the sub-metric name
	 * @return a map of the period data points
	 */
	public TObjectLongHashMap<String> getDataPoints();

}
