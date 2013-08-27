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
package com.heliosapm.shorthand.jmx;

/**
 * <p>Title: MetricMBeanBuilder</p>
 * <p>Description: Dynamically builds a metric MBean</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.shorthand.jmx.MetricMBeanBuilder</code></p>
 */

public class MetricMBeanBuilder {

	
	
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
