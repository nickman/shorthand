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

import java.util.regex.Pattern;

import javassist.ByteArrayClassPath;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.LoaderClassPath;

/**
 * <p>Title: ModifyMethodTest</p>
 * <p>Description: Adds system.out print to the instrumented method</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.shorthandexamples.ModifyMethodTest</code></p>
 */

public class ModifyMethodTest {
	
	/**
	 * Creates a new ModifyMethodTest
	 * @param className The internal form class name to modify
	 * @param methodName  The name of the method to transform
	 * @param methodSignature A regular expression to match the method signature. (if null, matches ".*")
	 * @param classLoader The intrumentation provided classloader
	 * @param byteCode The pre-transform byte code  
	 * @return  the modified byte code if successful, otherwise returns the original unmodified byte code
	 */
	public static byte[] instrument(String className, String methodName, String methodSignature, ClassLoader classLoader, byte[] byteCode) {
		String binName  = className.replace('/', '.');
		try {
			ClassPool cPool = new ClassPool(true);
			cPool.appendClassPath(new LoaderClassPath(classLoader));
			cPool.appendClassPath(new ByteArrayClassPath(binName, byteCode));
			CtClass ctClazz = cPool.get(binName);
			Pattern sigPattern = Pattern.compile((methodSignature==null|methodSignature.trim().isEmpty()) ? ".*" : methodSignature);
			int modifies = 0;
			for(CtMethod method: ctClazz.getDeclaredMethods()) {
				if(method.getName().equals(methodName)) {
					if(sigPattern.matcher(method.getSignature()).matches()) {
						method.insertBefore("System.out.println(\"\n\t-->Invoked method [" + binName + "." + method.toString() + "]\");");
						ctClazz.addMethod(method);
						modifies++;
					}
				}
			}
			System.out.println("[ModifyMethodTest] Intrumented [" + modifies + "] methods");
			return ctClazz.toBytecode();
		} catch (Exception ex) {
			System.err.println("Failed to compile retransform class [" + binName + "] Stack trace follows...");
			ex.printStackTrace(System.err);
			return byteCode; 
		}
	}

}
