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

import java.io.File;
import java.lang.reflect.Constructor;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javassist.ClassClassPath;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtConstructor;
import javassist.CtField;
import javassist.CtMethod;
import javassist.CtNewConstructor;
import javassist.Modifier;

import com.heliosapm.shorthand.collectors.EnumCollectors;
import com.heliosapm.shorthand.collectors.ICollector;
import com.heliosapm.shorthand.collectors.MethodInterceptor;
import com.heliosapm.shorthand.store.ChronicleStore;
import com.heliosapm.shorthand.util.StringHelper;
import com.heliosapm.shorthand.util.javassist.CodeBuilder;



/**
 * <p>Title: MetricMBeanBuilder</p>
 * <p>Description: Dynamically builds a metric MBean</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.shorthand.jmx.MetricMBeanBuilder</code></p>
 */

public class MetricMBeanBuilder {
	/** The singleton instance */	
	private static volatile MetricMBeanBuilder instance = null;
	/** The singleton instance ctor lock */
	private static final Object lock = new Object();
	
	/** A map of already created published metric classes keyed by enum-name/bitmask */
	private final Map<String, Constructor<? extends PublishedMetric>> mbeanClasses = new ConcurrentHashMap<String, Constructor<? extends PublishedMetric>>();

	
	/** The javassist classpool */
	private ClassPool cp;
	/** The published metric ct-class */
	private final CtClass pmIface;
	/** The published metric mbean ct-class */
	private final CtClass pmSuper;
	/** The string ct-class */
	private final CtClass stringClazz;
	/** The object ct-class */
	private final CtClass objectClazz;
	/** The long array ct-class */
	private final CtClass longArrClazz;


	
	/**
	 * Returns the MetricMBeanBuilder singleton
	 * @return the MetricMBeanBuilder singleton
	 */
	public static final MetricMBeanBuilder getInstance() {
		if(instance==null) {
			synchronized(lock) {
				if(instance==null) {
					instance = new MetricMBeanBuilder();
				}
			}
		}
		return instance;
	}
	
	private MetricMBeanBuilder() {
		try {
			cp = new ClassPool();
			cp.appendSystemPath();
			cp.appendClassPath(new ClassClassPath(PublishedMetric.class));
			stringClazz = cp.get(String.class.getName());
			objectClazz = cp.get(Object.class.getName());
			longArrClazz = cp.get(long[].class.getName());
			pmSuper = cp.get(PublishedMetric.class.getName());
			pmIface = cp.get(PublishedMetricMBean.class.getName());
			cp.importPackage(PublishedMetric.class.getPackage().getName());
			cp.importPackage(ChronicleStore.class.getPackage().getName());
		} catch (Exception ex) {
			throw new RuntimeException("Failed to initalize MetricMBeanBuilder", ex);
		}
	}
	
	public PublishedMetric getPublishedMetricInstance(int enumIndex, int bitMask, long nameIndex) {
		try {
			final String key = String.format("%s/%s", enumIndex, bitMask);
			Constructor<? extends PublishedMetric> ctor = mbeanClasses.get(key);
			if(ctor==null) {
				synchronized(mbeanClasses) {
					ctor = mbeanClasses.get(key);
					if(ctor==null) {
						Class<? extends PublishedMetric> clazz = generateClass(enumIndex, bitMask);
						ctor = clazz.getDeclaredConstructor(long.class);
						mbeanClasses.put(key, ctor);
					}
				}
			}			
			return ctor.newInstance(nameIndex);
		} catch (Exception ex) {
			throw new RuntimeException("Failed to create PublishedMBean class for [" + enumIndex + "/" + bitMask + "]", ex);
		}
	}
	
	public static void main(String[] args) {
		log("MetricMBeanBuilder------");
		ChronicleStore.getInstance();
		EnumCollectors.getInstance().typeForName(MethodInterceptor.class.getName());
		MetricMBeanBuilder.getInstance().getPublishedMetricInstance(1, 4095, 87);
	}
	
	private static final CtClass[] EMPTY_SIG = {};

	private Class<? extends PublishedMetric> generateClass(int enumIndex, int bitMask) {
		try {
			CodeBuilder implCode = new CodeBuilder();
			String collectorName = EnumCollectors.getInstance().type(enumIndex).getName();
			
			CtClass clazz = cp.makeClass("PublishedMetric_" + collectorName + "_" + bitMask, pmSuper);		
			CtClass clazzIface = cp.makeInterface("PublishedMetric_" + collectorName + "_" + bitMask + "MBean", pmIface);
//			clazz.addField(new CtField(longArrClazz, "dataIndexes", clazz));
			CtConstructor ctor = CtNewConstructor.copy(pmSuper.getDeclaredConstructor(new CtClass[]{CtClass.longType}), clazz, null);
			clazz.addConstructor(ctor);
			ctor.setBody("{ super($1); }");
			int dataIndex = 0;
			String methodName = null;
			for(ICollector<?> collector: EnumCollectors.getInstance().enabledMembersForIndex(enumIndex, bitMask)) {
				String[] subNames = collector.getSubMetricNames();				
				if(subNames.length==1) {
					methodName = String.format("get%s",  StringHelper.initCap(collector.getShortName()));									
					implCode.clear().appendFmt("{long id = dataIndexes[%s]; dataEx.index(id);  return ChronicleDataOffset.getDataPoint(dataIndexes[%s], 0, dataEx);}", dataIndex, dataIndex);
//					implCode.clear().appendFmt("{dataEx.index(dataIndexes[%s]);  return ChronicleDataOffset.getDataPoint(dataIndexes[%s], 0, dataEx);}", dataIndex, dataIndex);
					log("[%s]  %s", methodName, implCode);
					clazzIface.addMethod(new CtMethod(CtClass.longType, methodName, EMPTY_SIG, clazzIface));
					CtMethod implGetter = new CtMethod(CtClass.longType, methodName, EMPTY_SIG, clazz);
					clazz.addMethod(implGetter);
					implGetter.setBody(implCode.toString());					
				} else {
					int dataPointIndex = 0;
					for(String subMetricName: subNames) {
						methodName = String.format("get%s%s",  StringHelper.initCap(collector.getShortName()), subMetricName);
						implCode.clear().appendFmt("{long id = dataIndexes[%s]; dataEx.index(id); return ChronicleDataOffset.getDataPoint(dataIndexes[%s], %s, dataEx);}", dataIndex, dataIndex, dataPointIndex);
//						implCode.clear().appendFmt("{dataEx.index(dataIndexes[%s]); return ChronicleDataOffset.getDataPoint(dataIndexes[%s], %s, dataEx);}", dataIndex, dataIndex, dataPointIndex);
						log("[%s]  %s", methodName, implCode);
						clazzIface.addMethod(new CtMethod(CtClass.longType, methodName, EMPTY_SIG, clazzIface));
						CtMethod implGetter = new CtMethod(CtClass.longType, methodName, EMPTY_SIG, clazz);
						clazz.addMethod(implGetter);
						implGetter.setBody(implCode.toString());					
						dataPointIndex++;
					}
				}
				dataIndex++;				
				 
			}
			clazz.addInterface(clazzIface);
			//clazzIface.setModifiers(clazzIface.getModifiers() & ~Modifier.ABSTRACT);
			clazz.setModifiers(clazz.getModifiers() & ~Modifier.ABSTRACT);
			clazzIface.writeFile(System.getProperty("java.io.tmpdir") + File.separator + "js");
			clazz.writeFile(System.getProperty("java.io.tmpdir") + File.separator + "js");
			
			clazzIface.toClass();
			return clazz.toClass();
		} catch (Exception ex) {
			throw new RuntimeException("Failed to build published metric class for [" + enumIndex + "/" + bitMask + "]", ex);
		}
	}
	


			
			
	
	
	@SuppressWarnings("javadoc")
	public static void log(String fmt, Object...msgs) {
		System.out.println(String.format(fmt, msgs));
	}

}
