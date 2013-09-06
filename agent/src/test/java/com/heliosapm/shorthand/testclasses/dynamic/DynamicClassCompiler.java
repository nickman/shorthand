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
package com.heliosapm.shorthand.testclasses.dynamic;

import java.io.IOException;
import java.net.URL;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;

import javassist.CannotCompileException;
import javassist.ClassClassPath;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.NotFoundException;

import com.heliosapm.shorthand.util.net.BufferManager;
import com.heliosapm.shorthand.util.net.MemBuffer;

/**
 * <p>Title: DynamicClassCompiler</p>
 * <p>Description: A test utility for generating dynamic classes</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.shorthand.testclasses.dynamic.DynamicClassCompiler</code></p>
 */

public class DynamicClassCompiler {
	
	
	/**
	 * Generates a class that simply extends the passed parent
	 * @param name The name of the new class
	 * @param parent The parent class to extend or implement
	 * @return the URL where the class can be classloaded from
	 */
	public static URL generateClass(String name, Class<?> parent) {
		ClassPool cp = new ClassPool();
		cp.appendSystemPath();
		cp.appendClassPath(new ClassClassPath(parent));
		try {
			CtClass parentClazz = cp.get(parent.getName());
			CtClass clazz = cp.makeClass(name, parentClazz);		
			MemBuffer memBuffer = BufferManager.getInstance().getMemBuffer(name + ".jar");	
			JarOutputStream jos = new JarOutputStream(memBuffer.getOutputStream());
			String[] parts = name.split("\\.");
			parts[parts.length-1] = parts[parts.length-1] + ".class";
			for(int i = 0; i < parts.length-1; i++) {
				jos.putNextEntry(new ZipEntry(parts[i] + "/"));
				jos.closeEntry();
			}
			jos.putNextEntry(new ZipEntry(parts[parts.length-1]));
			jos.write(clazz.toBytecode());
			jos.closeEntry();
			jos.flush();
			jos.finish();
			jos.close();
			//memBuffer.getOutputStream().write(clazz.toBytecode());
			clazz.detach();
			parentClazz.detach();			
			return BufferManager.getInstance().getMemBufferURL(name + ".jar");			
		} catch (IOException ex) {
			throw new RuntimeException("Failed to write dynamic class [" + name + "] bytecode to membuffer", ex);
		} catch (CannotCompileException cex) {
			throw new RuntimeException("Failed to compile dynamic class [" + name + "]", cex);
		} catch (NotFoundException nfe) {
			throw new RuntimeException("Failed to load CtClass from ClassPool for [" + parent.getName() + "]", nfe);
		}
	}
}
