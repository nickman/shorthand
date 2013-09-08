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
package com.heliosapm.shorthand.instrumentor.shorthand.naming;

/**
 * <p>Title: MetricNameProvider</p>
 * <p>Description: A compiled {@link com.heliosapm.shorthand.instrumentor.shorthand.ShorthandScript} metric template translator to produce runtime metric names </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.shorthand.instrumentor.shorthand.naming.MetricNameProvider</code></p>
 */

public interface MetricNameProvider {
	/**
	 * Determines the method interception metric name as specified by the related {@link com.heliosapm.shorthand.instrumentor.shorthand.ShorthandScript}.
	 * The two parameters are the only values that might be used to qualify a metric name at runtime.
	 * All other metric name fragments should be determined at compile time
	 * @param returnValue The return value of the method invocation
	 * @param methodArgs The arguments to the method invocation
	 * @return the metric name
	 */
	public String getMetricName(Object returnValue, Object...methodArgs);
	
	/**
	 * Returns the pre-compiled metric name for which the the related {@link com.heliosapm.shorthand.instrumentor.shorthand.ShorthandScript}
	 * had no runtime dependencies 
	 * @return the metric name
	 */
	public String getMetricName();
}
