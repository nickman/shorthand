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

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

import javassist.CannotCompileException;
import javassist.ClassClassPath;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.NotFoundException;

import com.heliosapm.shorthand.util.net.BufferManager;

/**
 * <p>Title: DynamicClassCompiler</p>
 * <p>Description: A test utility for generating dynamic classes</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.shorthand.testclasses.dynamic.DynamicClassCompiler</code></p>
 */

public class DynamicClassCompiler {
	
	/** The debug class output directory */
	public static final String DEBUG_CLASS_DIR;
	
	static {
		String tmp = System.getProperty("java.io.tmpdir");
		if(!tmp.endsWith(File.separator)) tmp = tmp + File.separator;
		tmp = tmp + "js";
		DEBUG_CLASS_DIR = tmp;
		System.out.println("DEBUG Class Dir:" + DEBUG_CLASS_DIR);
		File f = new File(DEBUG_CLASS_DIR);
		if(!f.exists()) {
			f.mkdirs();
		}
	}
	
	
	/**
	 * Generates a class that simply extends the passed parent
	 * @param name The name of the new class
	 * @param parent The parent class to extend or implement
	 * @return the URL where the class can be classloaded from
	 */
	public static URL generateClass(String name, Class<?> parent) {
		final String urlKey = name + ".jar";
		URL _url = BufferManager.getInstance().getMemBufferURL(urlKey); 
		ClassPool cp = new ClassPool();
		cp.appendSystemPath();
		cp.appendClassPath(new ClassClassPath(parent));
		try {
			CtClass parentClazz = cp.get(parent.getName());
			CtClass clazz = cp.makeClass(name, parentClazz);
			JarOutputStream jos = new JarOutputStream(_url.openConnection().getOutputStream(), new Manifest());
			String entryName = name.replace('.', '/') + ".class";
			jos.putNextEntry(new JarEntry(entryName));
			byte[] byteCode = clazz.toBytecode();
			System.out.println("Byte code size for [" + name + "]:" + byteCode.length);
			jos.write(clazz.toBytecode());
			jos.closeEntry();
			jos.flush();
			jos.finish();
			jos.close();
			clazz.writeFile(DEBUG_CLASS_DIR);
			clazz.detach();
			parentClazz.detach();						
//			BufferManager.getInstance().registerMemBuffer(_url, memBuffer);
			return _url;
		} catch (IOException ex) {
			throw new RuntimeException("Failed to write dynamic class [" + name + "] bytecode to membuffer", ex);
		} catch (CannotCompileException cex) {
			throw new RuntimeException("Failed to compile dynamic class [" + name + "]", cex);
		} catch (NotFoundException nfe) {
			throw new RuntimeException("Failed to load CtClass from ClassPool for [" + parent.getName() + "]", nfe);
		}
	}
}
