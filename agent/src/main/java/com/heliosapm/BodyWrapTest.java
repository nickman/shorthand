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
package com.heliosapm;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;
import java.util.HashSet;
import java.util.Set;

import javassist.ByteArrayClassPath;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.CtNewMethod;
import javassist.LoaderClassPath;

import com.heliosapm.shorthand.attach.vm.agent.LocalAgentInstaller;
import com.heliosapm.shorthand.util.unsafe.UnsafeAdapter;

/**
 * <p>Title: BodyWrapTest</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.BodyWrapTest</code></p>
 */

public class BodyWrapTest {
	protected final Instrumentation instr;
	ClassPool cp = new ClassPool();
	
	public static final String WRAPPED = 
		"final long startTime = System.currentTimeMillis();" + 
		"try {" + 
		"$_ = $proceed($$);" +
		"} catch(Throwable t) {" +
		"System.err.println(\"Err\"); " +
		"throw new RuntimeException(\"Ouch!\");" + 
		"} finally {" + 
		"System.out.println(\"Elapsed:\" + System.currentTimeMillis()-startTime); " +
		"}";
		
	public static final String PROCEED = 
			"$_ = $proceed($$);";
			
		
	
	/**
	 * Creates a new BodyWrapTest
	 */
	public BodyWrapTest() {
		log("BodyWrapTest");
		instr = LocalAgentInstaller.getInstrumentation();
		log("Instrumentation [%s]", instr);
	}
	
	public static void main(String...args) {
		BodyWrapTest bwt = new BodyWrapTest();
		bwt.run();
	}
	
	public void run() {
		instrument(TestPojo.class.getName(), "foo");
		TestPojo pc = new TestPojo();
		for(int i = 0; i < 10; i++) {
			try {
				pc.foo();
			} catch (Exception ex) {
				//ex.printStackTrace(System.err);
			}
		}
		log("Done");
	}
	
	public void instrument(final String className, final String methodName, final Class<?>...signature) {
		try {
			cp.appendClassPath(new LoaderClassPath(PojoCaller.class.getClassLoader()));
			CtClass ctclass = ct(TestPojo.class);
			CtMethod ctm = ctclass.getDeclaredMethod("bar");
//			CtClass anon = cp.makeClass(TestPojo.class.getName() + "$DelegateFoo");
//			anon.addMethod(CtNewMethod.copy(ctm, "delegateFoo", ctclass, null));
			byte[] anonbytecode = ctclass.toBytecode();
			Class<?> aclazz = UnsafeAdapter.defineAnonymousClass(TestPojo.class, anonbytecode, null);
			log("Anon Class:" + aclazz.getName());
			byte[] bcode = getByteCode(TestPojo.class);
			ClassPool cpx = new ClassPool();
			cpx.appendClassPath(new ByteArrayClassPath(TestPojo.class.getName(), bcode));
			ctclass = cpx.get(TestPojo.class.getName());
			ctclass.writeFile("/tmp/shorthand");
			bcode = getByteCode(aclazz);
			cpx.appendClassPath(new ByteArrayClassPath(aclazz.getName(), bcode));
			ctclass = cpx.get(aclazz.getName());
			ctclass.writeFile("/tmp/shorthand");
			
			
//			
//			
//			
//			
//			
//			
//			final Set<String> edited = new HashSet<String>();
//			edited.add(TestPojo.class.getName().replace('.', '/'));
//			final byte[] bytecode = ctclass.toBytecode();
//			final ClassLoader[] cl = new ClassLoader[1];
//			final ClassFileTransformer cft = new ClassFileTransformer() {
//				@Override
//				public byte[] transform(ClassLoader loader, String className,
//						Class<?> classBeingRedefined,
//						ProtectionDomain protectionDomain,
//						byte[] classfileBuffer)
//						throws IllegalClassFormatException {
//					if(edited.contains(className)) {
//						cl[0] = loader;
//						log("Transforming [%s]", className);
//						return bytecode;
//					}
//					return classfileBuffer;					
//				}
//			};
//			instr.addTransformer(cft, true);
//			instr.retransformClasses(TestPojo.class);
//			instr.removeTransformer(cft);
//			ClassPool cpx = new ClassPool();
//			cpx.appendClassPath(new ByteArrayClassPath(ctclass.getName(), bytecode));
//			ctclass = cpx.get(TestPojo.class.getName());
//			ctclass.writeFile("/tmp/shorthand");
//			ctclass = cp.get(PojoCaller.class.getName());
//			ctclass.writeFile("/tmp/shorthand");
		} catch (Exception ex) {
			log("Instrumentation failed. Stack trace follows:");
			ex.printStackTrace(System.err);
		}
	}
	
	
	public byte[] getByteCode(final Class<?> clazz) {
		final String hname = clazz.getName().replace('.', '/');
		final byte[][] bytecode = new byte[1][1];
		final ClassLoader[] cl = new ClassLoader[1];
		final ClassFileTransformer cft = new ClassFileTransformer() {
			@Override
			public byte[] transform(ClassLoader loader, String className,
					Class<?> classBeingRedefined,
					ProtectionDomain protectionDomain,
					byte[] classfileBuffer)
					throws IllegalClassFormatException {
				if(hname.equals(className)) {
					cl[0] = loader;
					bytecode[0] = classfileBuffer;
				}
				return classfileBuffer;					
			}
		};
		instr.addTransformer(cft, true);
		try {
			instr.retransformClasses(clazz);
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		} finally {
			instr.removeTransformer(cft);
		}		
		return bytecode[0];
	}
	
	public CtClass[] ct(Class<?>...sig) {
		CtClass[] cts = new CtClass[sig.length];
		for(int i = 0; i < sig.length; i++) {
			cts[i] = ct(sig[i]);
		}
		return cts;
	}
	
	public CtClass ct(Class<?> clazz) {
		return ct(clazz.getName());
	}
	
	public CtClass ct(String name) {
		try {
			return cp.get(name);
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}
	
	
	static public void log(final Object msg, final Object...args) {
		System.out.println(String.format(msg.toString(), args));
	}

}
