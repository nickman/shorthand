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
import java.lang.management.ManagementFactory;
import java.util.LinkedHashSet;
import java.util.Set;

import com.heliosapm.shorthand.jmx.MetricJMXPublishOption;
import com.heliosapm.shorthand.util.ConfigurationHelper;

/**
 * <p>Title: ShorthandProperties</p>
 * <p>Description: Defines the shorthand configuration properties and defaults</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.shorthand.ShorthandProperties</code></p>
 */

public class ShorthandProperties {
	private ShorthandProperties() {}
	
	/** The PID of this JVM */
	public static final String PID = ManagementFactory.getRuntimeMXBean().getName().split("@")[0];
	/** The PID of this JVM as an int */
	public static final int IPID = Integer.parseInt(PID);
	

	/** System property to indicate that native memory allocations should be tracked */
	public static final String TRACK_MEM_PROP = "shorthand.unsafe.trackmem";

	/** The native memory tracking enablement default */
	public static final boolean TRACK_MEM_DEFAULT = false;
	
	
	/** System property name to specify the shorthand chronicle directory */
	public static final String CHRONICLE_DIR_PROP = "shorthand.store.chronicle.dir";
	/** The default shorthand chronicle directory */
	public static final String DEFAULT_CHRONICLE_DIR =  String.format("%s%sshorthand%s", System.getProperty("java.io.tmpdir"), File.separator, File.separator);

	/** System property name to specify if chronicle should use unsafe excerpts */
	public static final String CHRONICLE_UNSAFE_PROP = "shorthand.store.chronicle.unsafe";
    /** The default chronicle unsafe excerpt setting */
    public static final boolean DEFAULT_CHRONICLE_UNSAFE = false;

	
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
	
	/** The system property that defines how active metrics should be published. The value supplied should be an enum member name from {@link MetricJMXPublishOption} */
	public static final String PUBLISH_JMX_PROP = "shorthand.metrics.publish";
	/** The system property that defines the shorthand stale period in ms. which is the elapsed time in which a metric is considered stale with no activity */
	public static final String DEFAULT_PUBLISH_JMX = MetricJMXPublishOption.NONE.name();
	
	/** The system property that defines a comma separated package list where enum collector classes are located */
	public static final String ENUM_COLLECTOR_PACKAGES_PROP = "shorthand.metrics.publish";
	/** The base package list where enum collector classes are located */
	public static final String BASE_ENUM_COLLECTOR_PACKAGE = "com.heliosapm.shorthand.collectors";

	/**
	 * Returns the base and configured default package names for enum collector classes
	 * @return an array of package names
	 */
	public static String[] getEnumCollectorPackages() {
		Set<String> packages = new LinkedHashSet<String>();
		packages.add(BASE_ENUM_COLLECTOR_PACKAGE);
		String pConfig = ConfigurationHelper.getSystemThenEnvProperty(ENUM_COLLECTOR_PACKAGES_PROP, null);
		if(pConfig != null && !pConfig.trim().isEmpty()) {
			for(String s: pConfig.trim().split(",")) {
				if(s.trim().isEmpty()) continue;
				while(s.endsWith(".")) {
					s = s.substring(0, s.length()-1);
				}
				packages.add(s + ".");
			}
		}
		return packages.toArray(new String[packages.size()]);
	}
	
	
	/** The name of the class that will provide the instrumentation instance if we don't boot with this as the java-agent */
	public static final String AGENT_PROVIDER_CLASS_PROP = "shorthand.instrumentation.provider";
	/** The name of the field in the agent provider that will provide the instrumentation instance if we don't boot with this as the java-agent */
	public static final String AGENT_PROVIDER_FIELD = "shorthand.instrumentation.field";
	/** The name of the attribute in the agent provider that will provide the instrumentation instance if we don't boot with this as the java-agent */
	public static final String AGENT_PROVIDER_ATTRIBUTE = "shorthand.instrumentation.attribute";

    /** The system property that defines the number of milliseconds the shutdown service will wait for notifications to be sent before completing the shutdown */
    public static final String AGENT_SHUTDOWN_NOTIFICATION_TIMEOUT_PROP = "shorthand.shutdown.notification.time";    
    /** The default number of milliseconds the shutdown service will wait for notifications to be sent before completing the shutdown */
    public static final long DEFAULT_SHUTDOWN_NOTIFICATION_TIMEOUT = 500L;
    
    /** The system property that defines the multicast network to broadcast the startup */
    public static final String AGENT_BROADCAST_NETWORK_PROP = "shorthand.broadcast.network";
    /** The system property that defines the network interface to multicast on */
    public static final String AGENT_BROADCAST_NIC_PROP = "shorthand.broadcast.nic";
    /** The system property that defines the multicast port to broadcast the startup */
    public static final String AGENT_BROADCAST_PORT_PROP = "shorthand.broadcast.port";
    
    /** The default multicast network to broadcast JVM startup */
    public static final String DEFAULT_BROADCAST_NETWORK = "238.191.64.66";
    /** The default NIC name to broadcast JVM startup */
    public static final String DEFAULT_BROADCAST_NIC = "lo";
    /** The default port to broadcast JVM startup */
    public static final int DEFAULT_BROADCAST_PORT = 25493;
    
    /** The system property that disables broadcasts */
    public static final String DISABLE_BROADCAST_NETWORK_PROP = "shorthand.broadcast.disable";
    /** The default disable broadcasts */
    public static final boolean DEFAULT_DISABLE_BROADCAST_NETWORK = false;
    
//    -Dshorthand.broadcast.network=238.191.64.66,127.0.0.1
//    		-Dshorthand.broadcast.port=25493,25494    
    
}
