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
package com.heliosapm.shorthandexamples;

import java.lang.instrument.Instrumentation;

import javax.management.ObjectName;

/**
 * <p>Title: TransformerService</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.shorthandexamples.TransformerService</code></p>
 */

public class TransformerService implements TransformerServiceMBean {
	/** The JVM's instrumentation instance */
	protected final Instrumentation instrumentation;
	
	/**
	 * Creates a new TransformerService
	 * @param instrumentation  The JVM's instrumentation instance 
	 */
	public TransformerService(Instrumentation instrumentation) {
		this.instrumentation = instrumentation;
	}

	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.shorthandexamples.TransformerServiceMBean#transformClass(java.lang.String)
	 */
	@Override
	public void transformClass(String className) {
		Class<?> targetClazz = null;
		ClassLoader targetClassLoader = null;
		// first see if we can locate the class through normal means
		try {
			targetClazz = Class.forName(className);
			targetClassLoader = targetClazz.getClassLoader();
			transform(targetClazz, targetClassLoader);
			return;
		} catch (Exception ex) { /* Nope */ }
		// now try the hard/slow way
		for(Class<?> clazz: instrumentation.getAllLoadedClasses()) {
			if(clazz.getName().equals(className)) {
				targetClazz = clazz;
				targetClassLoader = targetClazz.getClassLoader();
				transform(targetClazz, targetClassLoader);
				return;				
			}
		}
		throw new RuntimeException("Failed to locate class [" + className + "]");
	}
	
	/**
	 * Registers a transformer and executes the transform
	 * @param clazz The class to transform
	 * @param classLoader The classloader the class was loaded from
	 */
	protected void transform(Class<?> clazz, ClassLoader classLoader) {
		DemoTransformer dt = new DemoTransformer(clazz.getName(), classLoader);
		instrumentation.addTransformer(dt, true);
		try {
			instrumentation.retransformClasses(clazz);
		} catch (Exception ex) {
			throw new RuntimeException("Failed to transform [" + clazz.getName() + "]", ex);
		} finally {
			instrumentation.removeTransformer(dt);
		}		
	}
}
