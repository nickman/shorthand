/**
* Helios Development Group LLC, 2013. 
 *
 */
package com.heliosapm.shorthand.instrumentor.shorthand;

import java.io.File;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.security.ProtectionDomain;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import javassist.CannotCompileException;
import javassist.ClassClassPath;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.bytecode.Descriptor;
import javassist.expr.ExprEditor;
import javassist.expr.Handler;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;
import com.heliosapm.shorthand.attach.vm.agent.LocalAgentInstaller;
import com.heliosapm.shorthand.datamapper.IDataMapper;
import com.heliosapm.shorthand.instrumentor.shorthand.naming.MetricNameProvider;
import com.heliosapm.shorthand.util.javassist.CodeBuilder;
import com.heliosapm.shorthand.util.unsafe.UnsafeAdapter;


/**
 * <p>Title: ShorthandCompiler</p>
 * <p>Description: Complies the static instrumentor class. Calls to this class are injected into target methods.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.shorthand.instrumentor.shorthand.ShorthandCompiler</code></p>
 */

public class ShorthandCompiler implements RemovalListener<String, ShorthandStaticInterceptor> {
	/** The singleton instance */	
	private static volatile ShorthandCompiler instance = null;
	/** The singleton instance ctor lock */
	private static final Object lock = new Object();
	
	/** The Javassist Debug Directory */
	public static final String JS_DEBUG = System.getProperty("java.io.tmpdir") + File.separator + "js";

	
	private CodeBuilder cb;

	/** A cache of interceptors keyed by class name */
	protected final Cache<String, ShorthandStaticInterceptor> interceptorCache = CacheBuilder.newBuilder().weakValues().removalListener(this).build();
	/** A cache of private invokers keyed by class name and method name/sig */
	protected final Cache<String, PrivateMethodInvoker> privateInvokerCache = CacheBuilder.newBuilder().weakValues().build();
	
	/**
	 * Returns the compiler singleton
	 * @return the compiler singleton
	 */
	public static final ShorthandCompiler getInstance() {
		if(instance==null) {
			synchronized(lock) {
				if(instance==null) {
					instance = new ShorthandCompiler();
				}
			}
		}
		return instance;
	}	
	
	
	
	/**
	 * {@inheritDoc}
	 * @see com.google.common.cache.RemovalListener#onRemoval(com.google.common.cache.RemovalNotification)
	 */
	@Override
	public void onRemoval(RemovalNotification<String, ShorthandStaticInterceptor> notification) {
		log("Removed ShorthandStaticInterceptor [%s]. Cause: [%s]", notification.getKey(), notification.getCause().name());
	}
	
	/**
	 * Compiles the passed script
	 * @param script The script to compile
	 */
	public void compile(ShorthandScript script) {
		for(Map.Entry<Class<?>, Set<Member>> entry: script.getTargetMembers().entrySet()) {
			/* No Op */
		}
	}
	
	protected Handler getLastThrowableHandler(CtMethod ctMethod) {
		final AtomicReference<Handler> handler = new AtomicReference<Handler>(null);
		try {
			ctMethod.instrument(new ExprEditor(){
				/**
				 * {@inheritDoc}
				 * @see javassist.expr.ExprEditor#edit(javassist.expr.Handler)
				 */
				@Override
				public void edit(Handler h) throws CannotCompileException {
					try {
						if(!h.isFinally() && h.getType().equals(throwableCtClass)) {
							handler.set(h);
						}
					} catch (Exception ex) {}
					super.edit(h);
				}
			});
		} catch (Exception ex) {
			ex.printStackTrace(System.err);			
		}
		return handler.get();
	}
	
	public void wrap(Method method) {
		log("Wrapping [%s]", method.toGenericString());
		try {
			Class<?> clazz = method.getDeclaringClass();
			CtClass ctClazz = classPool.get(clazz.getName());
			CtClass[] ctParams = new CtClass[method.getParameterTypes().length];
			for(int i = 0; i < ctParams.length; i++) {
				ctParams[i] = classPool.get(method.getParameterTypes()[i].getName());
			}
			log("Instrumenting [%s]", Descriptor.ofParameters(ctParams));
			CtMethod ctMethod = ctClazz.getDeclaredMethod(method.getName(), ctParams);
			Handler handler = getLastThrowableHandler(ctMethod);
			log("Last Throwable Handler [%s]", handler);
			
			ctMethod.insertBefore("System.out.println(\"MethodEnter[\" + Arrays.toString($args) + \"]\");");
			ctMethod.insertAfter("System.out.println(\"MethodExit[\" + Arrays.toString($args) + \"]\");");
			
			// com.heliosapm.shorthand.util.unsafe.UnsafeAdapter
			ctMethod.addCatch("System.out.println(\"MethodError[\" + Arrays.toString($args) + \"]\"); UnsafeAdapter.throwException($e); throw new RuntimeException();", throwableCtClass, "$e");
			final String targetName = clazz.getName().replace('.', '/');
			final byte[] byteCode = ctClazz.toBytecode(); 
			ctClazz.writeFile(JS_DEBUG);
			ClassFileTransformer cft = new ClassFileTransformer() {
				/**
				 * {@inheritDoc}
				 * @see java.lang.instrument.ClassFileTransformer#transform(java.lang.ClassLoader, java.lang.String, java.lang.Class, java.security.ProtectionDomain, byte[])
				 */
				@Override
				public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
					if(className.equals(targetName)) return byteCode;					
					return classfileBuffer;
				}
			};
			Instrumentation instr = LocalAgentInstaller.getInstrumentation();
			instr.addTransformer(cft, true);
			instr.retransformClasses(clazz);
			instr.removeTransformer(cft);
			log("Transformed class [%s]", clazz.getName());
		} catch (Exception ex) {
			ex.printStackTrace(System.err);
		}
	}
	
	public static void main(String[] args) {
		log("ShorthandCompiler");
		ShorthandCompiler compiler = ShorthandCompiler.getInstance();
		try {
			compiler.wrap(TestClass.class.getDeclaredMethod("awaitTermination", long.class, TimeUnit.class));
			TestClass tep = new TestClass();
			tep.awaitTermination(10, TimeUnit.MILLISECONDS);
			tep.awaitTermination(10, null);
		} catch (Exception ex) {
			ex.printStackTrace(System.err);
		}
			
	}

	/** The compiler's classpool */
	protected final ClassPool classPool = new ClassPool(true);
	/** The data mapper ct-class */
	protected final CtClass iDataMapperCtClass;
	/** The atomic boolean ct-class */
	protected final CtClass atomicBooleanCtClass;
	/** The string ct-class */
	protected final CtClass stringCtClass;
	/** The metric bane provider ct-class */
	protected final CtClass metricNameProviderCtClass;
	/** The static interceptor ct-class */
	protected final CtClass staticInterceptorCtClass;
	/** The throwable ct-class */
	protected final CtClass throwableCtClass;
	
	private ShorthandCompiler() {
		try {
			iDataMapperCtClass = classPool.get(IDataMapper.class.getName());
			atomicBooleanCtClass = classPool.get(AtomicBoolean.class.getName());
			stringCtClass = classPool.get(String.class.getName());
			metricNameProviderCtClass = classPool.get(MetricNameProvider.class.getName());
			staticInterceptorCtClass = classPool.get(ShorthandStaticInterceptor.class.getName());
			throwableCtClass = classPool.get(Throwable.class.getName());
			
			classPool.appendClassPath(new ClassClassPath(UnsafeAdapter.class));
			classPool.importPackage("com.heliosapm.shorthand.util.unsafe");
			classPool.importPackage("java.util");
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}
	
	
	public static class TestClass {
		public boolean awaitTermination(long t, TimeUnit unit) {
			try {
				Thread.currentThread().join(TimeUnit.MILLISECONDS.convert(t, unit));
				return true;
			} catch (InterruptedException e) {
				return false;
				//throw new RuntimeException("INTERRUPTED WHILE WAITING");
			}
		}
	}
	
	
	/*
	 * Fields (per pointcut):
	 * ======================
	 * String XXXmetricFormat
	 * IDataMapper XXXdataMapper
	 * 
	 * Methods:
	 * ========
	 * public static String metricNameXXX(Object...args)
     * public static long[] methodEnter()
     * public static void methodExit(String metricName, long[])
     * public static void methodError(String metricName, long[])
     * [public static <?> invokePrivateXXX(Object...args)]
     * 
     * 
	 */

	
	/**
	 * Simple out formatted logger
	 * @param fmt The format of the message
	 * @param args The message arguments
	 */
	public static void log(String fmt, Object...args) {
		System.out.println(String.format(fmt, args));
	}
	
	/**
	 * Simple err formatted logger
	 * @param fmt The format of the message
	 * @param args The message arguments
	 */
	public static void loge(String fmt, Object...args) {
		System.err.println(String.format(fmt, args));
	}
	
	/**
	 * Simple err formatted logger
	 * @param fmt The format of the message
	 * @param t The throwable to print stack trace for
	 * @param args The message arguments
	 */
	public static void loge(String fmt, Throwable t, Object...args) {
		System.err.println(String.format(fmt, args));
		t.printStackTrace(System.err);
	}

}

