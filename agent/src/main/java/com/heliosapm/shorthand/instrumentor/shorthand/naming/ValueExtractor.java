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

import java.lang.reflect.Method;

/**
 * <p>Title: ValueExtractor</p>
 * <p>Description: Defines a static value extractor for a given MetricNamingToken for a passed class and method</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.shorthand.instrumentor.shorthand.MetricNamingToken.ValueExtractor</code></p>
 */
interface ValueExtractor {
	/**
	 * Returns a static metric name part for the passed class and method
	 * @param expression The token expression
	 * @param clazz The target class
	 * @param method The target method
	 * @param qualifiers Additional reference qualifiers such as indexes
	 * @return the static metric name part or the "%s" runtime replacement token and the Java expression that will replace it.
	 */
	public String[] getStringReplacement(CharSequence expression, Class<?> clazz, Method method, Object...qualifiers);
}