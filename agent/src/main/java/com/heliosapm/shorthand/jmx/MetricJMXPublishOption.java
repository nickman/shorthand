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

import javax.management.ObjectName;

import com.heliosapm.shorthand.store.ChronicleOffset;
import com.heliosapm.shorthand.util.jmx.JMXHelper;

/**
 * <p>Title: MetricJMXPublishOption</p>
 * <p>Description: Enumerates the publication options for active metric JMX MBeans</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.shorthand.jmx.MetricJMXPublishOption</code></p>
 */

public enum MetricJMXPublishOption {
	/** No MBeans are published */
	NONE(new NullMetricJMXPublisher()),
	/** An MBean representing the metric's name index details will be published  */
	NAME(new NameMetricJMXPublisher()),
	/** An MBean representing the metric's name index and details and live data will be published  */
	DATA(new DataMetricJMXPublisher());
	
	/** The default option */
	public static final MetricJMXPublishOption DEFAULT = NONE;
	
	/**
	 * Attempts to decipher the intended option from the passed char sequence. Any failure returns the {@value #DEFAULT}.
	 * @param name The value to decipher an option from
	 * @return the deciphered option
	 */
	public static MetricJMXPublishOption forName(CharSequence name) {
		if(name==null) return DEFAULT;
		try {
			return MetricJMXPublishOption.valueOf(name.toString().trim().toUpperCase()); 
		} catch (Exception ex) {
			return DEFAULT;
		}
	}
	
	private MetricJMXPublishOption(MetricJMXPublisher publisher) {
		this.publisher = publisher;
	}
	
	private final MetricJMXPublisher publisher;
	
	
	/**
	 * Publishes the metric MBean according to the this option's directive
	 * @param metricName The metric name
	 * @param nameIndex The name index
	 */
	public void publish(String metricName, long nameIndex) {
		publisher.publish(metricName, nameIndex);
	}
	
	/**
	 * Unregisters the metric MBean according to the this option's directive
	 * @param metricName The metric name
	 * @param nameIndex The name index
	 */
	public void unPublish(String metricName, long nameIndex) {
		publisher.unPublish(metricName, nameIndex);
	}
	
	/**
	 * <p>Title: MetricJMXPublisher</p>
	 * <p>Description: Defines a class that handles publishing of JMX MBeans representing metrics</p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * <p><code>com.heliosapm.shorthand.jmx.MetricJMXPublishOption.MetricJMXPublisher</code></p>
	 */
	public static interface MetricJMXPublisher {
		/**
		 * Publishes an MBean
		 * @param metricName The metric name
		 * @param nameIndex The metric's name index
		 */
		public void publish(String metricName, long nameIndex);
		
		/**
		 * Unpublishes (unregisters) an MBean
		 * @param metricName The metric name
		 * @param nameIndex The metric's name index
		 */
		public void unPublish(String metricName, long nameIndex);
	}
	
	/**
	 * <p>Title: NullMetricJMXPublisher</p>
	 * <p>Description: A no op publisher</p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * <p><code>com.heliosapm.shorthand.jmx.MetricJMXPublishOption.NullMetricJMXPublisher</code></p>
	 */
	public static class NullMetricJMXPublisher implements MetricJMXPublisher {
		/**
		 * {@inheritDoc}
		 * @see com.heliosapm.shorthand.jmx.MetricJMXPublishOption.MetricJMXPublisher#publish(java.lang.String, long)
		 */
		@Override
		public void publish(String metricName, long nameIndex) {
			/* No Op */
		}
		
		/**
		 * {@inheritDoc}
		 * @see com.heliosapm.shorthand.jmx.MetricJMXPublishOption.MetricJMXPublisher#unPublish(java.lang.String, long)
		 */
		@Override
		public void unPublish(String metricName, long nameIndex) {
			/* No Op */
		}
	}
	
	/**
	 * <p>Title: NameMetricJMXPublisher</p>
	 * <p>Description: Publsihes a name index details MBean</p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * <p><code>com.heliosapm.shorthand.jmx.MetricJMXPublishOption.NameMetricJMXPublisher</code></p>
	 */
	public static class NameMetricJMXPublisher implements MetricJMXPublisher {
		/**
		 * {@inheritDoc}
		 * @see com.heliosapm.shorthand.jmx.MetricJMXPublishOption.MetricJMXPublisher#publish(java.lang.String, long)
		 */
		@Override
		public void publish(String metricName, long nameIndex) {
			ObjectName on = JMXHelper.isObjectName(metricName) ? JMXHelper.objectName(metricName) :
				JMXHelper.objectName("shorthand.metrics:name=%s", ObjectName.quote(metricName));
			JMXHelper.registerMBean(new PublishedMetric(nameIndex), on);
		}
		
		/**
		 * {@inheritDoc}
		 * @see com.heliosapm.shorthand.jmx.MetricJMXPublishOption.MetricJMXPublisher#unPublish(java.lang.String, long)
		 */
		@Override
		public void unPublish(String metricName, long nameIndex) {
			ObjectName on = JMXHelper.isObjectName(metricName) ? JMXHelper.objectName(metricName) :
				JMXHelper.objectName("shorthand.metrics:name=%s", ObjectName.quote(metricName));
			try { JMXHelper.unregisterMBean(on); } catch (Exception e) { /* No Op */ }
		}
	}
	
	/**
	 * <p>Title: NameMetricJMXPublisher</p>
	 * <p>Description: Publsihes a name index details MBean</p> 
	 * <p>Company: Helios Development Group LLC</p>
	 * @author Whitehead (nwhitehead AT heliosdev DOT org)
	 * <p><code>com.heliosapm.shorthand.jmx.MetricJMXPublishOption.NameMetricJMXPublisher</code></p>
	 */
	public static class DataMetricJMXPublisher implements MetricJMXPublisher {
		/**
		 * {@inheritDoc}
		 * @see com.heliosapm.shorthand.jmx.MetricJMXPublishOption.MetricJMXPublisher#publish(java.lang.String, long)
		 */
		@Override
		public synchronized void publish(String metricName, long nameIndex) {
			ObjectName on = JMXHelper.isObjectName(metricName) ? JMXHelper.objectName(metricName) :
				JMXHelper.objectName("shorthand.metrics:name=%s", ObjectName.quote(metricName));
			PublishedMetric pm = MetricMBeanBuilder.getInstance().getPublishedMetricInstance(
					(int)ChronicleOffset.EnumIndex.get(nameIndex), 
					(int)ChronicleOffset.BitMask.get(nameIndex), 
					nameIndex);
			JMXHelper.registerMBean(pm, on);
//			if(!JMXHelper.isRegistered(on)) {
//				JMXHelper.registerMBean(pm, on);
//			}
		}
		
		/**
		 * {@inheritDoc}
		 * @see com.heliosapm.shorthand.jmx.MetricJMXPublishOption.MetricJMXPublisher#unPublish(java.lang.String, long)
		 */
		@Override
		public void unPublish(String metricName, long nameIndex) {
			ObjectName on = JMXHelper.isObjectName(metricName) ? JMXHelper.objectName(metricName) :
				JMXHelper.objectName("shorthand.metrics:name=%s", ObjectName.quote(metricName));
			try { JMXHelper.unregisterMBean(on); } catch (Exception e) { /* No Op */ }
		}
	}
	
	
}
