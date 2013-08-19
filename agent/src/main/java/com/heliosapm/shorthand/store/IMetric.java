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

import java.util.Date;
import java.util.Map;

import com.heliosapm.shorthand.collectors.ICollector;

/**
 * <p>Title: IMetric</p>
 * <p>Description: Defines a metric read from the live store</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.shorthand.store.IMetric</code></p>
 * @param <T> The collector type
 */

public interface IMetric<T extends Enum<T> & ICollector<T>> {
	/**
	 * Returns the metric name
	 * @return the metric name
	 */
	public String getName();
	/**
	 * Returns the period start as a UTC long
	 * @return the period start
	 */
	public long getPeriodStart();
	/**
	 * Returns the period end as a UTC long
	 * @return the period end
	 */
	public long getPeriodEnd();
	/**
	 * Returns the period start as a java Date
	 * @return the period start
	 */
	public Date getPeriodStartDate();
	/**
	 * Returns the period end as a java Date
	 * @return the period end
	 */
	public Date getPeriodEndDate();
	/**
	 * Returns the class name of the collector type
	 * @return the class name of the collector type
	 */
	public String getCollectorTypeName();

	/**
	 * Returns a map of thiss metric's metric data points
	 * @return a map of thiss metric's metric data points
	 */
	public Map<T, IMetricDataPoint<T>> getMetricDataPoints();
}
