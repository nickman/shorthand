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
package com.heliosapm.shorthand;

import java.io.File;

/**
 * <p>Title: ShorthandProperties</p>
 * <p>Description: Defines the shorthand configuration properties and defaults</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.shorthand.ShorthandProperties</code></p>
 */

public class ShorthandProperties {
	private ShorthandProperties() {}
	
	/** System property name to specify the shorthand chronicle directory */
	public static final String CHRONICLE_DIR_PROP = "shorthand.store.chronicle.dir";
	/** The default shorthand chronicle directory */
	public static final String DEFAULT_CHRONICLE_DIR =  String.format("%s%sshorthand", System.getProperty("java.io.tmpdir"), File.separator);
	
    /** The system prop name to indicate if mem-spaces should be padded to the next largest pow(2) */
    public static final String USE_POW2_ALLOC_PROP = "shorthand.memspace.padcache";
    /** The default mem-space pad enablement */
    public static final String DEFAULT_USE_POW2_ALLOC = "false";


	/** The system property that defines the shorthand period in ms. */
	public static final String PERIOD_PROP = "shorthand.period";
	/** The system property that defines the shorthand stale period in ms. which is the elapsed time in which a metric is considered stale with no activity */
	public static final String STALE_PERIOD_PROP = "shorthand.period.stale";
	
	/** The system property that defines if the period clock should be disabled, usually for testing purposes */
	public static final String DISABLE_PERIOD_CLOCK_PROP = "shorthand.period.disabled";
	
	/** The default shorthand period in ms, which is 15000 */
	public static final long DEFAULT_PERIOD = 15000;
	/** The default shorthand stale period in ms, which is 5 minutes */
	public static final long DEFAULT_STALE_PERIOD = DEFAULT_PERIOD *2;  // DEFAULT_PERIOD * 4 * 5;  // 5 minutes

    
}
