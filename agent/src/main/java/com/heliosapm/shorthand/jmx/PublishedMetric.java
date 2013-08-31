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

import javax.management.MBeanRegistration;
import javax.management.MBeanServer;
import javax.management.ObjectName;

import com.heliosapm.shorthand.collectors.EnumCollectors;
import com.heliosapm.shorthand.store.ChronicleOffset;
import com.heliosapm.shorthand.store.ChronicleStore;
import com.higherfrequencytrading.chronicle.Excerpt;

/**
 * <p>Title: PublishedMetric</p>
 * <p>Description: The base mbean class for exposing live metrics through JMX</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.shorthand.jmx.PublishedMetric</code></p>
 */

public class PublishedMetric implements PublishedMetricMBean, MBeanRegistration {
	/** The name index */
	protected final long nameIndex;
	
	/** A name index excerpt */
	protected Excerpt nameEx = null;
	/** A data index excerpt */
	protected Excerpt dataEx = null;
	
	/**
	 * Creates a new PublishedMetric
	 * @param nameIndex The name index
	 */
	public PublishedMetric(long nameIndex) {
		super();
		this.nameIndex = nameIndex;
	}
	
	/**
	 * Returns the active metric data indexes
	 * @return the active metric data indexes
	 */
	protected long[] getDataIndexes() {
		if(nameEx==null) throw new RuntimeException("Cannot invoke getDataIndexes until MBean has bene published");
		long[] indexes = ChronicleOffset.getTier1Indexes(nameIndex, nameEx);
		long[] activeIndexes = new long[EnumCollectors.getInstance().enabledMembersForIndex(getEnumIndex(), getBitMask()).size()];
		int index = 0;
		for(long v: indexes) {
			if(v>0) {
				activeIndexes[index] = v;
				index++;
			}			
		}
		return activeIndexes;
	}
	
	/**
	 * {@inheritDoc}
	 * @see javax.management.MBeanRegistration#preRegister(javax.management.MBeanServer, javax.management.ObjectName)
	 */
	@Override
	public ObjectName preRegister(MBeanServer server, ObjectName name) throws Exception {
		return name;
	}

	/**
	 * {@inheritDoc}
	 * @see javax.management.MBeanRegistration#postRegister(java.lang.Boolean)
	 */
	@Override
	public void postRegister(Boolean registrationDone) {
		if(registrationDone!=null && registrationDone) {
			nameEx = ChronicleStore.getInstance().getNameIndexExcerpt();
			nameEx.index(nameIndex);
			dataEx = ChronicleStore.getInstance().getDataIndexExcerpt();
			dataIndexes = getDataIndexes();
		}
	}

	/**
	 * {@inheritDoc}
	 * @see javax.management.MBeanRegistration#preDeregister()
	 */
	@Override
	public void preDeregister() throws Exception {
		if(nameEx!=null) try { nameEx.close(); } catch (Exception e) {/* No Op */} finally { nameEx=null; }
		if(dataEx!=null) try { dataEx.close(); } catch (Exception e) {/* No Op */} finally { dataEx=null; }
	}

	/**
	 * {@inheritDoc}
	 * @see javax.management.MBeanRegistration#postDeregister()
	 */
	@Override
	public void postDeregister() {
		/* No Op */
	}
	

	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.shorthand.jmx.PublishedMetricMBean#getNameIndex()
	 */
	@Override
	public long getNameIndex() {
		return nameIndex;
	}

	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.shorthand.jmx.PublishedMetricMBean#getMetricName()
	 */
	@Override
	public String getMetricName() {
		return ChronicleOffset.getName(nameIndex, nameEx);
	}

	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.shorthand.jmx.PublishedMetricMBean#getEnumIndex()
	 */
	@Override
	public int getEnumIndex() {
		return (int)ChronicleOffset.EnumIndex.get(nameIndex, nameEx);
	}

	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.shorthand.jmx.PublishedMetricMBean#getCollectorName()
	 */
	@Override
	public String getCollectorName() {
		return EnumCollectors.getInstance().type(getEnumIndex()).getSimpleName();
	}

	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.shorthand.jmx.PublishedMetricMBean#getBitMask()
	 */
	@Override
	public int getBitMask() {
		return (int)ChronicleOffset.BitMask.get(nameIndex, nameEx);
	}

	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.shorthand.jmx.PublishedMetricMBean#getEnabledMetrics()
	 */
	@Override
	public String[] getEnabledMetrics() {		
		return EnumCollectors.getInstance().ref(getEnumIndex()).getEnabledNames(getBitMask());
	}

	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.shorthand.jmx.PublishedMetricMBean#getCreationTime()
	 */
	@Override
	public long getCreationTime() {
		return ChronicleOffset.CreateTime.get(nameIndex, nameEx);
	}

	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.shorthand.jmx.PublishedMetricMBean#getCreationDate()
	 */
	@Override
	public Date getCreationDate() {
		return new Date(ChronicleOffset.CreateTime.get(nameIndex, nameEx));
	}

	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.shorthand.jmx.PublishedMetricMBean#getPeriodStartTime()
	 */
	@Override
	public long getPeriodStartTime() {
		return ChronicleOffset.PeriodStart.get(nameIndex, nameEx);
	}

	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.shorthand.jmx.PublishedMetricMBean#getPeriodStartDate()
	 */
	@Override
	public Date getPeriodStartDate() {
		return new Date(ChronicleOffset.PeriodStart.get(nameIndex, nameEx));
	}

	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.shorthand.jmx.PublishedMetricMBean#getPeriodEndTime()
	 */
	@Override
	public long getPeriodEndTime() {
		return ChronicleOffset.PeriodEnd.get(nameIndex, nameEx);
	}

	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.shorthand.jmx.PublishedMetricMBean#getPeriodEndDate()
	 */
	@Override
	public Date getPeriodEndDate() {
		return new Date(ChronicleOffset.PeriodEnd.get(nameIndex, nameEx));
	}
	protected long[] dataIndexes = null;

//	private long[] dataIndexes;
//	
//	public long getWaitsMin()
//	  {
//	    this.dataEx.index(this.dataIndexes[2]);
//	    return ChronicleDataOffset.getDataPoint(this.dataIndexes[2], 0, this.dataEx);
//	  }
}
