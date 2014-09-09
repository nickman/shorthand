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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Enumeration;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import com.heliosapm.shorthand.util.URLHelper;
import com.heliosapm.shorthand.util.jmx.JMXHelper;

/**
 * <p>Title: LazyClassLoader</p>
 * <p>Description: A lazy classloader which can be defined by a token but not resolved until called.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.shorthand.instrumentor.shorthand.LazyClassLoader</code></p>
 */

public class LazyClassLoader extends ClassLoader {
	/** The classloader delegate, null until resolved */
	protected volatile ClassLoader resolvedDelegate = null;
	/** The lazy classloader stub, an opaque object that represents the classloader until it is resolved */
	protected final Object classLoaderStub;
	
	
	/**
	 * Creates a new LazyClassLoader
	 * @param classLoaderStub The lazy classloader stub, an opaque object that represents the classloader until it is resolved
	 */
	public LazyClassLoader(Object classLoaderStub) {
		this.classLoaderStub = classLoaderStub;
	}
	
	/**
	 * Resolves the class loader stub if it is still null. 
	 */
	protected void resolve() {
		if(resolvedDelegate==null) {
			resolvedDelegate = classLoaderFrom(classLoaderStub);
		}
	}

	/**
	 * 
	 * @see java.lang.ClassLoader#clearAssertionStatus()
	 */
	public void clearAssertionStatus() {
		resolve();
		resolvedDelegate.clearAssertionStatus();
	}

	/**
	 * @param obj
	 * @return
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	public boolean equals(Object obj) {
		resolve();
		return resolvedDelegate.equals(obj);
	}

	/**
	 * @param arg0
	 * @return
	 * @see java.lang.ClassLoader#getResource(java.lang.String)
	 */
	public URL getResource(String arg0) {
		resolve();
		return resolvedDelegate.getResource(arg0);
	}

	/**
	 * @param arg0
	 * @return
	 * @see java.lang.ClassLoader#getResourceAsStream(java.lang.String)
	 */
	public InputStream getResourceAsStream(String arg0) {
		resolve();
		return resolvedDelegate.getResourceAsStream(arg0);
	}

	/**
	 * @param name
	 * @return
	 * @throws IOException
	 * @see java.lang.ClassLoader#getResources(java.lang.String)
	 */
	public Enumeration<URL> getResources(String name) throws IOException {
		resolve();
		return resolvedDelegate.getResources(name);
	}

	/**
	 * @return
	 * @see java.lang.Object#hashCode()
	 */
	public int hashCode() {
		resolve();
		return resolvedDelegate.hashCode();
	}

	/**
	 * @param name
	 * @return
	 * @throws ClassNotFoundException
	 * @see java.lang.ClassLoader#loadClass(java.lang.String)
	 */
	public Class<?> loadClass(String name) throws ClassNotFoundException {
		resolve();
		return resolvedDelegate.loadClass(name);
	}

	/**
	 * @param className
	 * @param enabled
	 * @see java.lang.ClassLoader#setClassAssertionStatus(java.lang.String, boolean)
	 */
	public void setClassAssertionStatus(String className, boolean enabled) {
		resolve();
		resolvedDelegate.setClassAssertionStatus(className, enabled);
	}

	/**
	 * @param enabled
	 * @see java.lang.ClassLoader#setDefaultAssertionStatus(boolean)
	 */
	public void setDefaultAssertionStatus(boolean enabled) {
		resolve();
		resolvedDelegate.setDefaultAssertionStatus(enabled);
	}

	/**
	 * @param packageName
	 * @param enabled
	 * @see java.lang.ClassLoader#setPackageAssertionStatus(java.lang.String, boolean)
	 */
	public void setPackageAssertionStatus(String packageName, boolean enabled) {
		resolve();
		resolvedDelegate.setPackageAssertionStatus(packageName, enabled);
	}

	/**
	 * @return
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		if(resolvedDelegate==null) {
			return String.format("LazyClassLoader Unresolved from stub [%s]", classLoaderStub);
		} else {
			return String.format("LazyClassLoader [%s] from stub [%s]", resolvedDelegate.toString(), classLoaderStub);
		}
	}

	/**
	 * Attempts to derive a classloader from the passed object.
	 * @param obj The object to derive a classloader from
	 * @return a classloader
	 */
	public static ClassLoader classLoaderFrom(Object obj) {
		if(obj==null) {
			return ClassLoader.getSystemClassLoader();
		} else if(obj instanceof ClassLoader) {
			return (ClassLoader)obj;
		} else if(obj instanceof Class) {
			return ((Class<?>)obj).getClassLoader();
		} else if(obj instanceof URL) {
			return new URLClassLoader(new URL[]{(URL)obj}); 
		} else if(URLHelper.isValidURL(obj.toString())) {
			URL url = URLHelper.toURL(obj.toString());
			return new URLClassLoader(new URL[]{url});
		} else if(obj instanceof ObjectName) {
			return getClassLoader((ObjectName)obj);
		} else if(JMXHelper.isObjectName(obj.toString())) {
			return getClassLoader(JMXHelper.objectName(obj.toString()));
		} else if(obj instanceof File) {
			File f = (File)obj;
			return new URLClassLoader(new URL[]{URLHelper.toURL(f)});
		} else if(new File(obj.toString()).canRead()) {
			File f = new File(obj.toString());
			return new URLClassLoader(new URL[]{URLHelper.toURL(f)});			
		} else {
			throw new IllegalStateException("Failed to resolve class loader from stub [" + obj + "]");
		}
		
	}
	
	/**
	 * Returns the classloader represented by the passed ObjectName
	 * @param on The ObjectName to resolve the classloader from
	 * @return a classloader
	 */
	public static ClassLoader getClassLoader(ObjectName on) {
		try {
			MBeanServer server = JMXHelper.getHeliosMBeanServer();
			if(server.isInstanceOf(on, ClassLoader.class.getName())) {
				return server.getClassLoader(on);
			}
			return server.getClassLoaderFor(on);
		} catch (Exception ex) {
			throw new RuntimeException("Failed to get classloader for object name [" + on + "]", ex);
		}
	}

}
