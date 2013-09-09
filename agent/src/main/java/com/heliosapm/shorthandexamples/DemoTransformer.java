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

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;

/**
 * <p>Title: DemoTransformer</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.shorthandexamples.DemoTransformer</code></p>
 */

public class DemoTransformer implements ClassFileTransformer {
	/** The normal form class name of the class to transform */
	protected String className;
	/** The class loader of the class */
	protected ClassLoader classLoader;
	/** The method name */
	protected String methodName;
	/** The method signature */
	protected String methodSignature;
	
	/**
	 * Creates a new DemoTransformer
	 * @param classLoader The classloader to match
	 * @param className The binary class name of the class to transform
	 * @param methodName The method name
	 * @param methodSignature A regular expression matching the method signature
	 */
	public DemoTransformer(ClassLoader classLoader, String className, String methodName, String methodSignature) {
		this.className = className;
		this.classLoader = classLoader;
		this.methodName = methodName;
		this.methodSignature = methodSignature;
	}

	/**
	 * {@inheritDoc}
	 * @see java.lang.instrument.ClassFileTransformer#transform(java.lang.ClassLoader, java.lang.String, java.lang.Class, java.security.ProtectionDomain, byte[])
	 */
	@Override
	public byte[] transform(ClassLoader loader, String className,
			Class<?> classBeingRedefined, ProtectionDomain protectionDomain,
			byte[] classfileBuffer) throws IllegalClassFormatException {
		if(className.equals(this.className) && loader.equals(classLoader)) {
			return ModifyMethodTest.instrument(className, methodName, methodSignature, loader, classfileBuffer);
		}
		return classfileBuffer;
	}

}
