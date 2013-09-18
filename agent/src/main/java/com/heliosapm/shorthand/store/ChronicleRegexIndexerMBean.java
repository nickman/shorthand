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
package com.heliosapm.shorthand.store;

import java.util.Map;

import javax.management.ObjectName;

import com.heliosapm.shorthand.util.jmx.JMXHelper;

/**
 * <p>Title: ChronicleRegexIndexerMBean</p>
 * <p>Description: JMX MBean interface for {@link ChronicleRegexIndexer}</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.shorthand.store.ChronicleRegexIndexerMBean</code></p>
 */

public interface ChronicleRegexIndexerMBean {
	/** The indexer's JMX ObjectName */
	public static final ObjectName OBJECT_NAME = JMXHelper.objectName("com.heliosapm.shorthand.store:service=ChronicleIndexService");
	
	/** The new metric name notification type */
	public static final String NOTIF_NEW_METRIC = "shorthand.matching.metric.new";
	/** The stale metric name notification type */
	public static final String NOTIF_STALE_METRIC = "shorthand.matching.metric.stale";
	/**
	 * Returns the number of indexes
	 * @return the number of indexes
	 */
	public long getIndexCount();
	
	/**
	 * Returns an array of name indexes for metric names matching the passed pattern
	 * @param namePattern The pattern to match
	 * @return a [possibly zero length] array of matching metric name indexes
	 */
	public long[] search(String namePattern);
	
	/**
	 * Returns the number of metrics matching the passed pattern
	 * @param namePattern the pattern to count matches of
	 * @return the number of metrics matching the passed pattern
	 */
	public int count(String namePattern);
	
	/**
	 * Returns the number of names submitted for indexing
	 * @return the number of names submitted for indexing
	 */
	public long getSubmittedNameCount();
	/**
	 * Returns the number of names actually indexed 
	 * @return the number of names actually indexed
	 */
	public long getIndexedNames();
	
	/**
	 * Returns a map of index sizes keyed by the index regex pattern
	 * @return a map of index sizes keyed by the index regex pattern
	 */
	public Map<String, Long> getIndexSizes(); 
	
	/**
	 * Returns the rolling average search time in ns.
	 * @return the rolling average search time in ns.
	 */
	public long getAverageSearchTimeNanos();
	
	/**
	 * Returns the rolling average search time in ms.
	 * @return the rolling average search time in ms.
	 */
	public long getAverageSearchTimeMillis();
	
	
	/**
	 * Returns the last search time in ns.
	 * @return the last search time in ns.
	 */
	public long getLastSearchTimeNanos();
	
	/**
	 * Returns the last search time in ms.
	 * @return the last search time in ms.
	 */
	public long getLastSearchTimeMillis();
	
	
	
}
