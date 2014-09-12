/**
 * Helios, OpenSource Monitoring
 * Brought to you by the Helios Development Group
 *
 * Copyright 2014, Helios Development Group and individual contributors
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
import java.lang.reflect.Method;
import java.security.ProtectionDomain;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import com.heliosapm.shorthand.attach.vm.agent.LocalAgentInstaller;

/**
 * <p>Title: ClassIntrumentor</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.ClassIntrumentor</code></p>
 */

public class ClassIntrumentor extends ClassVisitor {
	/** A map of target method name || descriptors keyed by the concatenation of both */
	final Map<String, String[]> targetMethods;
	/** The class name being instrumented */
	String cname = null;

	/**
	 * Creates a new ClassIntrumentor
	 * @param cw The class writer
	 * @param targetMethods  A map of target method name || descriptors keyed by the method
	 */
	public ClassIntrumentor(final ClassWriter cw, Map<String, String[]> targetMethods) {
		super(Opcodes.ASM4, cw);
		this.targetMethods = targetMethods;
	}
	
	
	/**
	 * {@inheritDoc}
	 * @see org.objectweb.asm.ClassVisitor#visitMethod(int, java.lang.String, java.lang.String, java.lang.String, java.lang.String[])
	 */
	@Override
	public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
		MethodVisitor mv= super.visitMethod(access, name, desc, signature, exceptions);
		final String key = name + "/" + desc;
		String[] nameDescPair = targetMethods.get(key);
		if(nameDescPair!=null && name.equals(nameDescPair[0]) && desc.equals(nameDescPair[1])) {
			log("Instrumenting [%s.%s(%s)], access: %s", cname, nameDescPair[0], nameDescPair[1], access);
			MethodInstrumentor mvw=new MethodInstrumentor(api, mv);
			return mvw;			
		}
		return mv;		
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.objectweb.asm.ClassVisitor#visit(int, int, java.lang.String, java.lang.String, java.lang.String, java.lang.String[])
	 */
	@Override
	public void visit(int version, int access, String name, String signature,
			String superName, String[] interfaces) {
		this.cname = name;
		super.visit(version, access, name, signature, superName, interfaces);
	}
	

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		log("Instrumenting InstrumentationPrototype.normalOp");
		final Instrumentation instr = LocalAgentInstaller.getInstrumentation();
		final byte[] byteCode = getByteCode(instr, InstrumentationPrototype.class);
		ClassReader classReader = new ClassReader(byteCode);
		ClassWriter classWriter = new ClassWriter(classReader, ClassWriter.COMPUTE_MAXS);
		final Map<String, String[]> targetMethods = new HashMap<String, String[]>();
		getMethod(targetMethods, InstrumentationPrototype.class, "instrumentedOp", String.class); 
		ClassIntrumentor ci = new ClassIntrumentor(classWriter, targetMethods);		
		classReader.accept(ci, 0);
		Map<String, byte[]> bc = Collections.singletonMap(InstrumentationPrototype.class.getName().replace('.', '/'), classWriter.toByteArray());
		retransform(instr, bc, InstrumentationPrototype.class);
	}

	
	/**
	 * Retrieves the byte code for the passed class
	 * @param instr The instrumentation instance
	 * @param clazz The class to get the byte code for
	 * @return the byte code
	 */
	public static byte[] getByteCode(final Instrumentation instr, final Class<?> clazz) {
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
	
	
	/**
	 * Retransforms the passed classes
	 * @param instr The instrumentation instance
	 * @param byteCode A map of class bytecode arrays keyed by the internal form class names they correspond to
	 * @param clazzes The classes to retransform
	 */
	public static void retransform(final Instrumentation instr, final Map<String, byte[]> byteCode, Class<?>...clazzes) {
		final ClassFileTransformer cft = new ClassFileTransformer() {
			@Override
			public byte[] transform(ClassLoader loader, String className,
					Class<?> classBeingRedefined,
					ProtectionDomain protectionDomain,
					byte[] classfileBuffer)
					throws IllegalClassFormatException {
				final byte[] bc = byteCode.get(className);
				if(bc!=null) {
					log("Retransforming [%s]", className);
					return bc;
				}				
				return classfileBuffer;
			}
		};
		instr.addTransformer(cft, true);
		try {
			instr.retransformClasses(clazzes);
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		} finally {
			instr.removeTransformer(cft);
		}				
	}
	
	
	
	/**
	 * Low maintenance logger
	 * @param msg The message format
	 * @param args The message tokens
	 */
	static public void log(final Object msg, final Object...args) {
		System.out.println(String.format(msg.toString(), args));
	}
	
	
	/**
	 * Populates the passed map with the matching method names and signature descriptors
	 * @param targets The map to populate
	 * @param clazz The class
	 * @param name The method name
	 * @param sig The method signature
	 */
	static public void getMethod(final Map<String, String[]> targets, final Class<?> clazz, String name, Class<?>...sig) {
		Method m = getMethod(clazz, name, sig);
		final String mname = m.getName();
		final String mdesc = Type.getMethodDescriptor(m);
		targets.put(mname + "/" + mdesc, new String[]{mname, mdesc});
	}
	
	/**
	 * Acquires the described method
	 * @param clazz The class
	 * @param name The method name
	 * @param sig The method signature
	 * @return the located method
	 */
	static public Method getMethod(final Class<?> clazz, String name, Class<?>...sig) {		
		try {
			try {
				return clazz.getDeclaredMethod(name, sig);				
			} catch (NoSuchMethodException nsme) {
				return clazz.getMethod(name, sig);
			}			
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}
	
}


/*

package asm.com.heliosapm;
import java.util.*;
import org.objectweb.asm.*;
import org.objectweb.asm.attrs.*;
public class InstrumentationPrototypeDump implements Opcodes {

public static byte[] dump () throws Exception {

ClassWriter cw = new ClassWriter(0);
FieldVisitor fv;
MethodVisitor mv;
AnnotationVisitor av0;

cw.visit(V1_6, ACC_PUBLIC + ACC_SUPER, "com/heliosapm/InstrumentationPrototype", null, "java/lang/Object", null);

{
mv = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
mv.visitCode();
mv.visitVarInsn(ALOAD, 0);
mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V");
mv.visitInsn(RETURN);
mv.visitMaxs(1, 1);
mv.visitEnd();
}
{
mv = cw.visitMethod(ACC_PUBLIC, "normalOp", "(Ljava/lang/String;)V", null, new String[] { "java/lang/NumberFormatException", "java/lang/InterruptedException" });
mv.visitCode();
mv.visitMethodInsn(INVOKESTATIC, "java/lang/System", "nanoTime", "()J");
mv.visitVarInsn(LSTORE, 2);
mv.visitVarInsn(ALOAD, 1);
mv.visitMethodInsn(INVOKESTATIC, "java/lang/Long", "parseLong", "(Ljava/lang/String;)J");
mv.visitMethodInsn(INVOKESTATIC, "java/lang/Thread", "sleep", "(J)V");
mv.visitInsn(RETURN);
mv.visitMaxs(2, 4);
mv.visitEnd();
}
{
mv = cw.visitMethod(ACC_PUBLIC, "instrumentedOp", "(Ljava/lang/String;)V", null, new String[] { "java/lang/NumberFormatException", "java/lang/InterruptedException" });
{
av0 = mv.visitAnnotation("Lcom/heliosapm/shorthand/instrumentor/annotations/Instrumented;", true);
av0.visit("lastInstrumented", new Long(1L));
av0.visit("version", new Integer(1));
{
AnnotationVisitor av1 = av0.visitArray("types");
av1.visit(null, "ElapsedNanos");
av1.visitEnd();
}
av0.visitEnd();
}
mv.visitCode();
Label l0 = new Label();
Label l1 = new Label();
Label l2 = new Label();
mv.visitTryCatchBlock(l0, l1, l2, "java/lang/Exception");
Label l3 = new Label();
mv.visitTryCatchBlock(l0, l3, l3, null);
mv.visitMethodInsn(INVOKESTATIC, "java/lang/System", "nanoTime", "()J");
mv.visitVarInsn(LSTORE, 2);
mv.visitTypeInsn(NEW, "java/util/concurrent/atomic/AtomicBoolean");
mv.visitInsn(DUP);
mv.visitInsn(ICONST_0);
mv.visitMethodInsn(INVOKESPECIAL, "java/util/concurrent/atomic/AtomicBoolean", "<init>", "(Z)V");
mv.visitVarInsn(ASTORE, 4);
mv.visitLabel(l0);
// =================================================================================================================================
//	Call to method with null.
// =================================================================================================================================
mv.visitVarInsn(ALOAD, 0);
mv.visitVarInsn(ALOAD, 1);
mv.visitMethodInsn(INVOKEVIRTUAL, "com/heliosapm/InstrumentationPrototype", "normalOp", "(Ljava/lang/String;)V");
// =================================================================================================================================
mv.visitVarInsn(ALOAD, 4);
mv.visitInsn(ICONST_0);
mv.visitMethodInsn(INVOKEVIRTUAL, "java/util/concurrent/atomic/AtomicBoolean", "set", "(Z)V");
mv.visitLabel(l1);
Label l4 = new Label();
mv.visitJumpInsn(GOTO, l4);
mv.visitLabel(l2);
mv.visitFrame(Opcodes.F_FULL, 4, new Object[] {"com/heliosapm/InstrumentationPrototype", "java/lang/String", Opcodes.LONG, "java/util/concurrent/atomic/AtomicBoolean"}, 1, new Object[] {"java/lang/Exception"});
mv.visitVarInsn(ASTORE, 5);
mv.visitVarInsn(ALOAD, 4);
mv.visitInsn(ICONST_1);
mv.visitMethodInsn(INVOKEVIRTUAL, "java/util/concurrent/atomic/AtomicBoolean", "set", "(Z)V");
mv.visitVarInsn(ALOAD, 5);
mv.visitMethodInsn(INVOKESTATIC, "com/heliosapm/shorthand/util/unsafe/UnsafeAdapter", "throwException", "(Ljava/lang/Throwable;)V");
mv.visitTypeInsn(NEW, "java/lang/RuntimeException");
mv.visitInsn(DUP);
mv.visitMethodInsn(INVOKESPECIAL, "java/lang/RuntimeException", "<init>", "()V");
mv.visitInsn(ATHROW);
mv.visitLabel(l3);
mv.visitFrame(Opcodes.F_SAME1, 0, null, 1, new Object[] {"java/lang/Throwable"});
mv.visitVarInsn(ASTORE, 6);
mv.visitMethodInsn(INVOKESTATIC, "java/lang/System", "nanoTime", "()J");
mv.visitVarInsn(LSTORE, 7);
mv.visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
mv.visitLdcInsn("Elapsed: %s, Failed: %s");
mv.visitInsn(ICONST_2);
mv.visitTypeInsn(ANEWARRAY, "java/lang/Object");
mv.visitInsn(DUP);
mv.visitInsn(ICONST_0);
mv.visitVarInsn(LLOAD, 7);
mv.visitMethodInsn(INVOKESTATIC, "java/lang/Long", "valueOf", "(J)Ljava/lang/Long;");
mv.visitInsn(AASTORE);
mv.visitInsn(DUP);
mv.visitInsn(ICONST_1);
mv.visitVarInsn(ALOAD, 4);
mv.visitInsn(AASTORE);
mv.visitMethodInsn(INVOKESTATIC, "java/lang/String", "format", "(Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/String;");
mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V");
mv.visitVarInsn(ALOAD, 6);
mv.visitInsn(ATHROW);
mv.visitLabel(l4);
mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
mv.visitMethodInsn(INVOKESTATIC, "java/lang/System", "nanoTime", "()J");
mv.visitVarInsn(LSTORE, 7);
mv.visitFieldInsn(GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
mv.visitLdcInsn("Elapsed: %s, Failed: %s");
mv.visitInsn(ICONST_2);
mv.visitTypeInsn(ANEWARRAY, "java/lang/Object");
mv.visitInsn(DUP);
mv.visitInsn(ICONST_0);
mv.visitVarInsn(LLOAD, 7);
mv.visitMethodInsn(INVOKESTATIC, "java/lang/Long", "valueOf", "(J)Ljava/lang/Long;");
mv.visitInsn(AASTORE);
mv.visitInsn(DUP);
mv.visitInsn(ICONST_1);
mv.visitVarInsn(ALOAD, 4);
mv.visitInsn(AASTORE);
mv.visitMethodInsn(INVOKESTATIC, "java/lang/String", "format", "(Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/String;");
mv.visitMethodInsn(INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V");
mv.visitInsn(RETURN);
mv.visitMaxs(7, 9);
mv.visitEnd();
}
cw.visitEnd();

return cw.toByteArray();
}
}


*/