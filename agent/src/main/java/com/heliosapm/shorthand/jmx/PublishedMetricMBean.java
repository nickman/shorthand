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
package com.heliosapm.shorthand.jmx;

import java.util.Date;

/**
 * <p>Title: PublishedMetricMBean</p>
 * <p>Description: JMX MBean interface for {@link PublishedMetric}</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.shorthand.jmx.PublishedMetricMBean</code></p>
 */

public interface PublishedMetricMBean  {
	/**
	 * Returns the chronicle name index for this metric 
	 * @return the chronicle name index for this metric
	 */
	public long getNameIndex();
	/**
	 * Returns the metric name
	 * @return the metric name
	 */
	public String getMetricName();
	/**
	 * Returns the enum collector index 
	 * @return the enum collector index
	 */
	public int getEnumIndex();
	/**
	 * Returns the enum collector name
	 * @return the enum collector name
	 */
	public String getCollectorName();
	/**
	 * Returns the enabled metric bitmask
	 * @return the enabled metric bitmask
	 */
	public int getBitMask();
	/**
	 * Returns the names of the enabled metrics
	 * @return the names of the enabled metrics
	 */
	public String[] getEnabledMetrics();
	/**
	 * Returns the metric creation time in long UTC
	 * @return the metric creation time
	 */
	public long getCreationTime();
	/**
	 * Returns the metric creation date
	 * @return the metric creation date
	 */
	public Date getCreationDate();
	/**
	 * Returns the metric period start time in long UTC
	 * @return the metric period start time
	 */
	public long getPeriodStartTime();
	/**
	 * Returns the metric period start date
	 * @return the metric period start date
	 */
	public Date getPeriodStartDate();
	/**
	 * Returns the metric period end time in long UTC
	 * @return the metric period end time
	 */
	public long getPeriodEndTime();
	/**
	 * Returns the metric period end date
	 * @return the metric period end date
	 */
	public Date getPeriodEndDate();
	
}
