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
package com.heliosapm.shorthand.util.classload;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * <p>Title: MultiClassLoader</p>
 * <p>Description: A classloader that delegates to the configured classloaders in the order defined.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.shorthand.util.classload.MultiClassLoader</code></p>
 */

public class MultiClassLoader extends ClassLoader {
	/** The delegate classloaders which will be searched for classes in the array order */
	protected Set<ClassLoader> delegates;

	/**
	 * Creates a new MultiClassLoader
	 * @param delegates The delegate classloaders which will be searched for classes in the array order
	 */
	public MultiClassLoader(Set<ClassLoader> delegates) {
		if(delegates==null || delegates.isEmpty()) throw new IllegalArgumentException("Cannot create MultiClassLoader with zero delegates");
		this.delegates = new LinkedHashSet<ClassLoader>(delegates);
	}
	
	/**
	 * {@inheritDoc}
	 * @see java.lang.ClassLoader#findClass(java.lang.String)
	 */
	@Override
	public Class<?> findClass(String name) throws ClassNotFoundException {
		for(ClassLoader cl: delegates) {
			try {
				return cl.loadClass(name);
			} catch (ClassNotFoundException cnex) {}
		}
		throw new ClassNotFoundException("Failed to find class [" + name + "]");
	}

}
