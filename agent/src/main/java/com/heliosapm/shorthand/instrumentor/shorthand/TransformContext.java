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

import javassist.ClassPool;
import javassist.LoaderClassPath;

/**
 * <p>Title: TransformContext</p>
 * <p>Description: A A transform context bound to a thread processing a retransform. This allows the thread to reuse resources
 * across multiple calls to the instrumentation instance across multiple classfile transformers</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.shorthand.instrumentor.shorthand.TransformContext</code></p>
 */

public class TransformContext {
	/** The TransformContext for the current thread */
	private static final ThreadLocal<TransformContext> CONTEXT = new ThreadLocal<TransformContext>();
	
	/** The classpool instance */
	private final ResetableClassPool classPool = new ResetableClassPool();
	/** The shorthand script instance we're working on */
	private final ShorthandScript shorthandScript;
	
	/**
	 * Acquires a TransformContext bound to the current thread for the passed shorthandScript
	 * @param shorthandScript The shorthand script to set in focus. If one is already in focus, it will be discarded
	 * @return a TransformContext bound to the current thread for the passed shorthandScript
	 */
	public static TransformContext getTransformContext(ShorthandScript shorthandScript) {
		TransformContext tc = CONTEXT.get();
		if(tc == null) {
			tc = new TransformContext(shorthandScript);
			CONTEXT.set(tc);
		} else {
			if(tc.shorthandScript!=shorthandScript) {
				CONTEXT.remove();
				tc = new TransformContext(shorthandScript);
				CONTEXT.set(tc);
			}
		}
		return tc;
	}
	
	/**
	 * Creates a new TransformContext and appends the intrumentation and target class classloaders to the classpool as classpaths 
	 * @param script The shorthand script instance we're working on
	 */
	private TransformContext(ShorthandScript script) {
		classPool.appendSystemPath();
		shorthandScript = script;
		classPool.appendClassPath(new LoaderClassPath(shorthandScript.getEnumCollectorClassLoader()));
		classPool.appendClassPath(new LoaderClassPath(shorthandScript.getTargetClassLoader()));
	}
	
	/**
	 * Returns the classpool
	 * @return the classpool
	 */
	public ClassPool getClassPool() {
		return classPool;
	}
	
//	/**
//	 * Determines if the named class byte-code targetted to be loaded by the passed classloader inherrits from the passed ct-class.
//	 * Inherritance will be positive for interfaces or super classes
//	 * @param className The binary class name of the class to test
//	 * @param loader The classloader of the class to test
//	 * @param byteCode The bytecode of the class to test
//	 * @param ctClassName The name of the ctclass to test against
//	 * @return true if the byte-code defined class inherrits from (or is equal to) the passed ctclass, false otherwise
//	 */
//	public boolean isInherritedFrom(String className, ClassLoader loader, byte[] byteCode, String ctClassName, Class<?> ctClass) {
//		try {
//			
//		} catch (Exception e) {
//			loge("Failed to determine inherritance of [%s] to [%s]", e, className, ctClass.getName());
//			return false;
//		}
//	}
	
	
	
	/**
	 * Simple out formatted logger
	 * @param fmt The format of the message
	 * @param args The message arguments
	 */
	public static void log(String fmt, Object...args) {
		fmt = "[TransformContext] - [%s]" + fmt;
		int lngth = (args==null ? 0 : args.length)+1;
		Object[] _args = new Object[lngth];
		_args[lngth-1] = Thread.currentThread().getName();
		if(args!=null) System.arraycopy(args, 0, _args, 0, args.length);
		System.out.println(String.format(fmt, _args));
	}
	
	/**
	 * Simple err formatted logger
	 * @param fmt The format of the message
	 * @param args The message arguments
	 */
	public static void loge(String fmt, Object...args) {
		fmt = "[TransformContext] - [%s]" + fmt;
		int lngth = (args==null ? 0 : args.length)+1;
		Object[] _args = new Object[lngth];
		_args[lngth-1] = Thread.currentThread().getName();
		if(args!=null) System.arraycopy(args, 0, _args, 0, args.length);		
		System.err.println(String.format(fmt, _args));
	}
	
	/**
	 * Simple err formatted logger
	 * @param fmt The format of the message
	 * @param t The throwable to print stack trace for
	 * @param args The message arguments
	 */
	public static void loge(String fmt, Throwable t, Object...args) {
		fmt = "[TransformContext] - [%s]" + fmt;
		int lngth = (args==null ? 0 : args.length)+1;
		Object[] _args = new Object[lngth];
		_args[lngth-1] = Thread.currentThread().getName();
		if(args!=null) System.arraycopy(args, 0, _args, 0, args.length);		
		System.err.println(String.format(fmt, _args));
		t.printStackTrace(System.err);
	}
	

}
