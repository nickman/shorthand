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

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import javassist.ClassPath;
import javassist.ClassPool;
import javassist.NotFoundException;

/**
 * <p>Title: ResetableClassPool</p>
 * <p>Description:  An extension of javassist's {@link ClassPool} that provides a quickie reset to eject all the added CtClasses.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.shorthand.instrumentor.shorthand.ResetableClassPool</code></p>
 */

public class ResetableClassPool extends ClassPool {
	protected final Set<ClassPath> addedClasspaths = new CopyOnWriteArraySet<ClassPath>();

	/**
	 * Creates a new ResetableClassPool
	 */
	public ResetableClassPool() {
		super();
	}

	/**
	 * Creates a new ResetableClassPool
	 * @param useDefaultPath true if the system search path is appended
	 */
	public ResetableClassPool(boolean useDefaultPath) {
		super(useDefaultPath);
	}

	/**
	 * Creates a new ResetableClassPool
	 * @param parentPool the parent classpool
	 */
	public ResetableClassPool(ClassPool parentPool) {
		super(parentPool);
	}
	
	/**
	 * {@inheritDoc}
	 * @see javassist.ClassPool#appendClassPath(javassist.ClassPath)
	 */
	@Override
	public ClassPath appendClassPath(ClassPath cp) {
		if(cp!=null) addedClasspaths.add(cp);
		return super.appendClassPath(cp);
	}
	
	@Override
	public ClassPath appendClassPath(String pathname) throws NotFoundException {
		ClassPath path = super.appendClassPath(pathname);
		addedClasspaths.add(path);
		return path;
	}

}
