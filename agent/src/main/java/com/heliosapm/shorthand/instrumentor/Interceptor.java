/**
* Helios Development Group LLC, 2013. 
 *
 */
package com.heliosapm.shorthand.instrumentor;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.security.ProtectionDomain;
import java.util.Arrays;
import java.util.Date;

import javassist.ClassClassPath;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtField;
import javassist.CtMethod;
import javassist.bytecode.AnnotationsAttribute;
import javassist.bytecode.ConstPool;
import javassist.bytecode.annotation.Annotation;
import javassist.bytecode.annotation.ArrayMemberValue;
import javassist.bytecode.annotation.IntegerMemberValue;
import javassist.bytecode.annotation.LongMemberValue;
import javassist.bytecode.annotation.MemberValue;
import javassist.bytecode.annotation.StringMemberValue;

import javax.annotation.Nullable;

import org.junit.internal.runners.TestClass;
import org.reflections.Reflections;
import org.reflections.scanners.MethodParameterScanner;
import org.reflections.util.ConfigurationBuilder;

import com.google.common.base.Predicate;
import com.heliosapm.shorthand.attach.vm.agent.LocalAgentInstaller;
import com.heliosapm.shorthand.instrumentor.annotations.Instrumented;


/**
 * <p>Title: Interceptor</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.shorthand.test.Interceptor</code></p>
 */

public class Interceptor {

	/**
	 * Creates a new Interceptor
	 */
	public Interceptor() {
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		log("Instrument Test");
		TestClass tc = new TestClass();
		int i = -1;
		try {
			i = tc.getFileCount("/home/nwhitehead", false);
			log("File Count:" + i);
		} catch (Exception ex) {
			ex.printStackTrace(System.err);
		}
		try {
			Instrumentation instr = LocalAgentInstaller.getInstrumentation(1000);
			log("Acquired Instrumentation:" + instr);
			ClassPool cp = new ClassPool();
			cp.appendClassPath(new ClassClassPath(tc.getClass()));
			CtClass target = cp.get(tc.getClass().getName());
			log("Target CtClass:" + target);
			CtMethod targetMethod = target.getDeclaredMethod("getFileCount", new CtClass[]{cp.get(String.class.getName()), CtClass.booleanType});			
			log("Target Method:" + targetMethod);
			
			
			
			
			
			
			CtClass ihandler = cp.makeClass("x.Invoker");
			CtMethod ihanderMethod = new CtMethod(targetMethod, ihandler, null);
			ihanderMethod.setModifiers(ihanderMethod.getModifiers() | Modifier.STATIC);
			ihanderMethod.insertParameter(target);
			CtClass threadLocalClazz = cp.get(ThreadLocal.class.getName());
			CtField cf = new CtField(threadLocalClazz, "xThreadLocal", ihandler);
			cf.setModifiers(cf.getModifiers() | Modifier.STATIC);
			cf.setModifiers(cf.getModifiers() | Modifier.PUBLIC);
			cf.setModifiers(cf.getModifiers() | Modifier.FINAL);
			ihandler.addField(cf, CtField.Initializer.byNew(threadLocalClazz));
			StringBuilder b = new StringBuilder();
			b.append("{try {");
			b.append("System.out.println(\"Intercepted!\");");
			b.append("return $1.getFileCount($2, $3);");
			b.append("} catch (Throwable t) {");
			b.append("System.err.println(\"Interception failed !\"); com.heliosapm.shorthand.test.StaticUnsafe.UNSAFE.throwException(t); return -1;");
//			b.append("if(t instanceof java.lang.RuntimeException) throw (java.lang.RuntimeException)t;");
//			b.append("else throw (java.io.FileNotFoundException)t;");
			b.append("} }");
			//ihanderMethod.setBody("{System.out.println(\"Intercepted!\"); return $1.getFileCount($2, $3);}");
			ihanderMethod.setBody(b.toString());
			
			ihandler.addMethod(ihanderMethod);
			ihandler.writeFile("/tmp/js");
			Class<?> clazz = ihandler.toClass();
			
			//targetMethod.useCflow("x");
			targetMethod.insertBefore("{if(x.Invoker.xThreadLocal.get()==null) { x.Invoker.xThreadLocal.set(new Object()); return x.Invoker.getFileCount($0, $1, $2);} else { x.Invoker.xThreadLocal.remove(); }}");
			ConstPool constpool = targetMethod.getMethodInfo().getConstPool();
			target.removeMethod(targetMethod);
			AnnotationsAttribute attr = new AnnotationsAttribute(constpool, AnnotationsAttribute.visibleTag);
			Annotation annot = new Annotation("com.heliosapm.shorthand.test.Instrumented", constpool);
//			Annotation annot = new Annotation("MetricCollection-7", constpool);
			ArrayMemberValue amv = new ArrayMemberValue(constpool);
			amv.setValue(new MemberValue[]{new StringMemberValue("MetricCollection-7", constpool)});
			annot.addMemberValue("types", amv);
			annot.addMemberValue("version", new IntegerMemberValue(constpool, 34));
			annot.addMemberValue("lastInstrumented", new LongMemberValue(System.currentTimeMillis(), constpool));
			attr.addAnnotation(annot);		
			targetMethod.getMethodInfo().addAttribute(attr);
			target.addMethod(targetMethod);

			
			target.writeFile("/tmp/js");
			final byte[] byteCode = target.toBytecode();
			final String bname = tc.getClass().getName().replace('.', '/');
			instr.addTransformer(new ClassFileTransformer(){
				/**
				 * {@inheritDoc}
				 * @see java.lang.instrument.ClassFileTransformer#transform(java.lang.ClassLoader, java.lang.String, java.lang.Class, java.security.ProtectionDomain, byte[])
				 */
				@Override
				public byte[] transform(ClassLoader loader, String className,
						Class<?> classBeingRedefined,
						ProtectionDomain protectionDomain,
						byte[] classfileBuffer)
						throws IllegalClassFormatException {
						if(className.equals(bname)) {
							log("=========Instrumented Class=============");
							return byteCode;
						}
					return classfileBuffer;
				}
			}, true);
			instr.retransformClasses(tc.getClass());
		} catch (Exception e) {
			e.printStackTrace(System.err);
		}
		try {
			i = tc.getFileCount("/home/nwhitehead", false);
			log("File Count:" + i);
			i = tc.getFileCount("/home/x", false);
			log("File Count:" + i);
			
		} catch (Exception ex) {
			ex.printStackTrace(System.err);
		}
		log("Listing Annotations:");
		try {
			Method method = tc.getClass().getDeclaredMethod("getFileCount", String.class, boolean.class);
			Instrumented instrumented = method.getAnnotation(Instrumented.class);
			if(instrumented!=null) {
				log("Instrumented Method:" + method.toGenericString());
				log("\tTypes:" + Arrays.toString(instrumented.types()));
				log("\tVersion:" + instrumented.version());
				log("\tLast Instrumented:" + new Date(instrumented.lastInstrumented()));
			}
			
		} catch (Exception ex) {
			ex.printStackTrace(System.err);
		}
		
		Predicate<String> pred = new Predicate<String>() {
			/**
			 * {@inheritDoc}
			 * @see com.google.common.base.Predicate#apply(java.lang.Object)
			 */
			@Override
			public boolean apply(@Nullable String input) {
				log("Predicate Input [" + input + "]");
				return false;
			}
		};
		
		Reflections reflections = new Reflections(new ConfigurationBuilder()
			.filterInputsBy(pred)
//			.addClassLoader(tc.getClass().getClassLoader())
			.addScanners(new MethodParameterScanner())
		);
//		for(Class<?> clazz: reflections.getTypesAnnotatedWith(Instrumented.class, true)) {
//			log("Class with @Instrumented method: " + clazz.getName());
//		}
		
		for(Method clazz: reflections.getMethodsMatchParams(int.class)) {
			log("Class with @Instrumented method: " + clazz.getName());
		}
		
	}
	
	public static void log(Object msg) {
		System.out.println(msg);
	}

}
