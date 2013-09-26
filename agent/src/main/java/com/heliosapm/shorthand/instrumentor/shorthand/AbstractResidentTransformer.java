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
package com.heliosapm.shorthand.instrumentor.shorthand;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.security.ProtectionDomain;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javassist.ByteArrayClassPath;
import javassist.ClassPath;
import javassist.ClassPool;
import javassist.LoaderClassPath;

/**
 * <p>Title: AbstractResidentTransformer</p>
 * <p>Description: Base abstract resident transformer, used by {@link ResidentTransformerBuilder} to
 * create new transformer types that extend this type.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.shorthand.instrumentor.shorthand.AbstractResidentTransformer</code></p>
 * <p>Filters in order of efficiency:<ol>
 * 	<li>Exact classname</li>
 * 	<li>Classloader match</li>
 *  <li>isAnnotation</li>
 *  <li>Implements / Extends</li>
 *  <li>Already Instrumented ?</li>
 *  <li>Method Name Match</li>
 *  <li>Method Name Sig Match</li>
 *  <li></li>
 *  <li></li>
 * </ol></p>
 */

public class AbstractResidentTransformer implements ClassFileTransformer {
	/** The classpool used to inspect candidate classes without classloading them (which we can't) */
	protected final ClassPool cp = new ClassPool();
	
	/** A list of javassist classpaths added to the classpool, which should be removed on completion */
	protected final List<ClassPath> installedClassPaths = new ArrayList<ClassPath>();
	/** The binary name of the class being examined */
	protected String bClassName = null;
	/**
	 * {@inheritDoc}
	 * @see java.lang.instrument.ClassFileTransformer#transform(java.lang.ClassLoader, java.lang.String, java.lang.Class, java.security.ProtectionDomain, byte[])
	 */
	@Override
	public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined,
			ProtectionDomain protectionDomain, byte[] classfileBuffer)
			throws IllegalClassFormatException {
		bClassName = className.replace('/', '.');
		try {
			
		} finally {
			cleanAll();
		}
		
		return null;
	}
	
	
	/**
	 * Installs the candidate class into the javassist classpool so it can be analyzed
	 * @param loader The classloader the candidate class is being loaded in
	 * @param classfileBuffer The class byte code
	 */
	protected void initJSClass(ClassLoader loader, byte[] classfileBuffer) {
		ClassPath[] cps = new ClassPath[2];
		cps[0] = new LoaderClassPath(loader);
		cps[1] = new ByteArrayClassPath(bClassName, classfileBuffer);
		cp.appendClassPath(cps[0]);
		cp.appendClassPath(cps[1]);
		installedClassPaths.addAll(Arrays.asList(cps));
	}
	
	/**
	 * Cleans up all items in this instance on completion
	 */
	protected void cleanAll() {
		cleanClassPool();
		bClassName = null;
	}
	
	/**
	 * Clears all added items from the classpool
	 */
	protected void cleanClassPool() {
		if(!installedClassPaths.isEmpty()) {
			for(ClassPath classpath: installedClassPaths) {
				cp.removeClassPath(classpath);
			}
			installedClassPaths.clear();
		}
	}
	
	

}
