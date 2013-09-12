/**
* Helios Development Group LLC, 2013. 
 *
 */
package com.heliosapm.shorthand.instrumentor.shorthand;

import java.io.File;
import java.lang.instrument.ClassDefinition;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.security.ProtectionDomain;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import javassist.CannotCompileException;
import javassist.ClassClassPath;
import javassist.ClassPool;
import javassist.CtBehavior;
import javassist.CtClass;
import javassist.CtConstructor;
import javassist.CtField;
import javassist.CtMethod;
import javassist.Modifier;
import javassist.bytecode.Descriptor;
import javassist.expr.ExprEditor;
import javassist.expr.Handler;

import org.cliffc.high_scale_lib.NonBlockingHashMap;
import org.cliffc.high_scale_lib.NonBlockingHashMapLong;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;
import com.heliosapm.shorthand.accumulator.MetricSnapshotAccumulator;
import com.heliosapm.shorthand.attach.vm.agent.LocalAgentInstaller;
import com.heliosapm.shorthand.datamapper.DataMapperBuilder;
import com.heliosapm.shorthand.datamapper.IDataMapper;
import com.heliosapm.shorthand.instrumentor.shorthand.naming.MetricNameCompiler;
import com.heliosapm.shorthand.instrumentor.shorthand.naming.MetricNameProvider;
import com.heliosapm.shorthand.util.StringHelper;
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
	
	/** A serial number for assigning to instrumentor classes */
	protected static final AtomicLong INSTRUMENTOR_SERIAL = new AtomicLong(0);
	/** A serial number for assigning to instrumentor class methods */
	protected static final AtomicLong INSTRUMENTOR_METHOD_SERIAL = new AtomicLong(0);
	
	/**
	 * Compiles the passed script
	 * @param script The script to compile
	 */
	public void compile(ShorthandScript script) {
		try {
			final int enumIndex = script.getEnumIndex();
			final int bitMask = script.getBitMask();
			final IDataMapper dataMapper = DataMapperBuilder.getInstance().getIDataMapper(enumIndex, bitMask);
			for(Map.Entry<Class<?>, Set<Member>> entry: script.getTargetMembers().entrySet()) {
				Class<?> targetClass = entry.getKey();
				final long classSerial = INSTRUMENTOR_SERIAL.incrementAndGet();
				final ClassLoader classLoader = targetClass.getClassLoader();
				final String instumentorKey = targetClass.getName() + "/" + (classLoader==null ? "system" : classLoader.toString());
				final String instumentorClassName = String.format("%s__ShorthandInstrumentor_%s_%s_%s", targetClass.getName(), enumIndex, bitMask, classSerial);
				final CtClass ctInstrumentClass = classPool.makeClass(instumentorClassName, staticInterceptorCtClass);
				final CtClass ctTargetClass = classPool.get(targetClass.getName());
				final String targetClassInternalForm = targetClass.getName().replace('.', '/');
				// ===============================================================================================
				//		Generate static instrumentor fields
				// ===============================================================================================
				// The data mapper
				CtField ctField = new CtField(iDataMapperCtClass,  "dataMapper", ctInstrumentClass);
				ctField.setModifiers(ctField.getModifiers() | Modifier.FINAL | Modifier.PROTECTED | Modifier.STATIC);
				ctInstrumentClass.addField(ctField, CtField.Initializer.byExpr(String.format("DataMapperBuilder.getInstance().getIDataMapper(%s, %s)", enumIndex, bitMask))); 
				
//				// ===============================================================================================
//				//		Generate static instrumentor methods
//				// ===============================================================================================
//				
//				final CtMethod methodEnter = new CtMethod(longArrClass, "methodEnter", new CtClass[]{nonBlockingHashMapLongCtClass}, ctInstrumentClass);
//				final CtMethod methodExit = new CtMethod(CtClass.voidType, "methodExit", new CtClass[]{stringCtClass, nonBlockingHashMapLongCtClass}, ctInstrumentClass);
//				final CtMethod methodError = new CtMethod(CtClass.voidType, "methodError", new CtClass[]{stringCtClass, nonBlockingHashMapLongCtClass}, ctInstrumentClass);
//				
//
//				final CodeBuilder methodEnterSource = new CodeBuilder("{");
//				final CodeBuilder methodExitSource = new CodeBuilder("{");
//				final CodeBuilder methodErrorSource = new CodeBuilder("{");
//				
//				methodEnterSource.appendFmt("long[] v = dataMapper.methodEnter(); $1.put(Thread.currentThread().getId(), v); return v;}");
//				methodExitSource.appendFmt("snap($1, dataMapper, (long[])$2.remove(Thread.currentThread().getId()));}");
//				methodErrorSource.appendFmt("snap($1, dataMapper, (long[])$2.remove(Thread.currentThread().getId()));}");
//				
//				
//				methodEnter.setBody(methodEnterSource.toString());
//				methodExit.setBody(methodExitSource.toString());
//				methodError.setBody(methodErrorSource.toString());
//				
//				methodEnter.setModifiers((methodEnter.getModifiers() | Modifier.FINAL | Modifier.PROTECTED | Modifier.STATIC) & ~Modifier.ABSTRACT & ~Modifier.PUBLIC);
//				methodExit.setModifiers((methodExit.getModifiers() | Modifier.FINAL | Modifier.PROTECTED | Modifier.STATIC) & ~Modifier.ABSTRACT & ~Modifier.PUBLIC);
//				methodError.setModifiers((methodError.getModifiers() | Modifier.FINAL | Modifier.PROTECTED | Modifier.STATIC) & ~Modifier.ABSTRACT & ~Modifier.PUBLIC);
//				
//				ctInstrumentClass.addMethod(methodEnter);
//				ctInstrumentClass.addMethod(methodExit);
//				ctInstrumentClass.addMethod(methodError);
//				
//				// ===============================================================================================
				
				for(Member member: entry.getValue()) {
					final String signatureString = StringHelper.getMemberDescriptor(member);
					final long methodSerial = INSTRUMENTOR_METHOD_SERIAL.incrementAndGet();
					final CtBehavior targetBehavior;
					if(member instanceof Field) {
						loge("Shorthand compiler does not support field access interception yet");
						continue;
					} else if(member instanceof Constructor) {
						targetBehavior = ctTargetClass.getConstructor(signatureString);
						ctTargetClass.removeConstructor((CtConstructor)targetBehavior);
					} else {
						targetBehavior = ctTargetClass.getMethod(member.getName(), signatureString);
						ctTargetClass.removeMethod((CtMethod)targetBehavior);
					}
					log("Instumenting [%s.%s(%s)]", targetClass.getName(), targetBehavior.getName(), signatureString);
					// ===============================================================================================
					//		Metric Naming
					// ===============================================================================================					
					String[] naming = MetricNameCompiler.getMetricNameCodePoints(targetClass, member, script.getMetricNameTemplate());

					
					// ===============================================================================================
					//		Localized static instrumentor fields
					// ===============================================================================================
					// The collected values stack
					final String valueStackFieldName = "valueStack_" + methodSerial;
					ctField = new CtField(nonBlockingHashMapLongCtClass,  valueStackFieldName , ctInstrumentClass);
					ctField.setModifiers(ctField.getModifiers() | Modifier.FINAL | Modifier.PROTECTED | Modifier.STATIC);
					ctInstrumentClass.addField(ctField, CtField.Initializer.byNew(nonBlockingHashMapLongCtClass));


					// ===============================================================================================
					//		Instrument target method
					// ===============================================================================================
										
					targetBehavior.insertBefore(String.format("ShorthandStaticInterceptor.methodEnter(%s.%s, %s.dataMapper);", instumentorClassName, valueStackFieldName, instumentorClassName));
					if(naming.length==1) {
						targetBehavior.insertAfter(String.format("ShorthandStaticInterceptor.methodExit(\"%s\", %s.%s, %s.dataMapper);", naming[0], instumentorClassName, valueStackFieldName, instumentorClassName));
						targetBehavior.addCatch(String.format("ShorthandStaticInterceptor.methodError(\"%s\", %s.%s, %s.dataMapper); UnsafeAdapter.throwException($e); throw new RuntimeException();", naming[0], instumentorClassName, valueStackFieldName, instumentorClassName),  throwableCtClass, "$e");
					} else {
						targetBehavior.insertAfter(String.format("ShorthandStaticInterceptor.methodExit(String.format(\"%s\", new Object[]{%s}), %s.%s, %s.dataMapper);", naming[0], naming[1], instumentorClassName, valueStackFieldName, instumentorClassName));
						targetBehavior.addCatch(String.format("$e.printStackTrace(System.err); ShorthandStaticInterceptor.methodError(String.format(\"%s\", new Object[]{%s}), %s.%s, %s.dataMapper); UnsafeAdapter.throwException($e); throw new RuntimeException();", naming[0], naming[1], instumentorClassName, valueStackFieldName, instumentorClassName),  throwableCtClass, "$e");
//						targetBehavior.insertAfter(String.format("ShorthandStaticInterceptor.snap(String.format(\"%s\", new Object[]{%s}), %s.dataMapper, values);", naming[0], naming[1], instumentorClassName, valueStackFieldName));
//						targetBehavior.addCatch(String.format("ShorthandStaticInterceptor.snap(String.format(\"%s\", new Object[]{%s}), %s.dataMapper, (long[])%s.%s.get()); UnsafeAdapter.throwException($e); throw new RuntimeException();", naming[0], naming[1], instumentorClassName, instumentorClassName, valueStackFieldName), throwableCtClass, "$e");
						
					}
					
					if(member instanceof Constructor) {						
						ctTargetClass.addConstructor((CtConstructor)targetBehavior);
					} else {						
						ctTargetClass.addMethod((CtMethod)targetBehavior);
						
					}
										
				}
				classPool.get(instrumentation.getClass().getName()).writeFile(JS_DEBUG);
				MetricSnapshotAccumulator.getInstance();
				ctInstrumentClass.writeFile(JS_DEBUG);
				ctTargetClass.writeFile(JS_DEBUG);
				final byte[] ctInstrumentBytes = ctInstrumentClass.toBytecode();
				final byte[] ctTargetBytes = ctTargetClass.toBytecode();
				ClassFileTransformer cft = new ClassFileTransformer() {
					/**
					 * {@inheritDoc}
					 * @see java.lang.instrument.ClassFileTransformer#transform(java.lang.ClassLoader, java.lang.String, java.lang.Class, java.security.ProtectionDomain, byte[])
					 */
					@Override
					public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) throws IllegalClassFormatException {
						if(!targetClassInternalForm.equals(className)) return classfileBuffer;
						try {
							defineClassMethod.invoke(classLoader, instumentorClassName, ctInstrumentBytes, 0, ctInstrumentBytes.length, protectionDomain);
							return ctTargetBytes;
						} catch (Exception ex) {
							ex.printStackTrace(System.err);
							return classfileBuffer;
						}						
					}
				};
				log("Instrumentation reTrans:%s  redef:%s", instrumentation.isRetransformClassesSupported(), instrumentation.isRedefineClassesSupported());
				instrumentation.addTransformer(cft, true);
				try {
					instrumentation.retransformClasses(targetClass);
					//instrumentation.redefineClasses(new ClassDefinition(targetClass, ctTargetBytes));
				} finally {
					instrumentation.removeTransformer(cft);
				}				
			}
			
		} catch (Exception ex) {
			ex.printStackTrace(System.err);
		}
	}
	
	/**
	 * Retrieves the last catch or finally block in the passed method
	 * @param ctMethod The method to scan
	 * @param finallyBlock true for a finally block, false for a catch block
	 * @return the last hanlder or none if one was not found
	 */
	protected Handler getLastHandler(CtMethod ctMethod, final boolean finallyBlock) {
		final AtomicReference<Handler> handler = new AtomicReference<Handler>(null);
		try {
			ctMethod.instrument(new ExprEditor(){
				@Override
				public void edit(Handler h) throws CannotCompileException {
					try {
						if(h.isFinally()==finallyBlock && h.getType().equals(throwableCtClass)) {
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
		Member member = method;
		log("Signature [%s]", StringHelper.getMemberDescriptor(member));
		try {
			Class<?> clazz = method.getDeclaringClass();
			CtClass ctClazz = classPool.get(clazz.getName());
			CtClass[] ctParams = new CtClass[method.getParameterTypes().length];
			for(int i = 0; i < ctParams.length; i++) {
				ctParams[i] = classPool.get(method.getParameterTypes()[i].getName());
			}
			log("Instrumenting [%s]", Descriptor.ofParameters(ctParams));
			CtMethod ctMethod = ctClazz.getDeclaredMethod(method.getName(), ctParams);
			Handler lastThrowable = getLastHandler(ctMethod, false);
			log("Last Throwable Handler [%s]", lastThrowable);
			
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
			instr.addTransformer(cft, false);
			instr.retransformClasses(clazz);
			instr.removeTransformer(cft);
			log("Transformed class [%s]", clazz.getName());
		} catch (Exception ex) {
			ex.printStackTrace(System.err);
		}
	}
	
	public static void main(String[] args) {
		log("ShorthandCompiler");
		ManagementFactory.getThreadMXBean().setThreadContentionMonitoringEnabled(true);
		ManagementFactory.getThreadMXBean().setThreadCpuTimeEnabled(true);
		ShorthandCompiler compiler = ShorthandCompiler.getInstance();
		try {
//			compiler.wrap(TestClass.class.getDeclaredMethod("awaitTermination", long.class, TimeUnit.class));
//			TestClass tep = new TestClass();
//			tep.awaitTermination(10, TimeUnit.MILLISECONDS);
//			tep.awaitTermination(10, null);
			ShorthandScript script = ShorthandScript.parse(TestClass.class.getName() + " awaitTermination MethodInterceptor[4095] '${class}/${method}/${arg[1]}'");
			compiler.compile(script);
			while(true) {
			//for(int i = 0; i < 100000; i++) {
				TestClass tep = new TestClass();
				tep.awaitTermination(10, TimeUnit.MILLISECONDS);
				Thread.sleep(3000);
			}
		} catch (Exception ex) {
			ex.printStackTrace(System.err);
		}
			
	}

	/** The instrumentation instance */
	protected final Instrumentation instrumentation;
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
	/** The long[] ct-class */
	protected final CtClass longArrClass;
	/** The thread local ct-class */
	protected final CtClass threadLocalCtClass;
	/** The non-blocking hashmap long ct-class */
	protected final CtClass nonBlockingHashMapLongCtClass;
	/** The non-blocking hashmap ct-class */
	protected final CtClass nonBlockingHashMapCtClass;
	
	/** The {@link ClassLoader#} define class method */
	protected final Method defineClassMethod;
	
	private ShorthandCompiler() {
		try {
			defineClassMethod = ClassLoader.class.getDeclaredMethod("defineClass", String.class, byte[].class, int.class, int.class, ProtectionDomain.class);
			defineClassMethod.setAccessible(true);
			instrumentation = LocalAgentInstaller.getInstrumentation();
			iDataMapperCtClass = classPool.get(IDataMapper.class.getName());
			atomicBooleanCtClass = classPool.get(AtomicBoolean.class.getName());
			stringCtClass = classPool.get(String.class.getName());
			metricNameProviderCtClass = classPool.get(MetricNameProvider.class.getName());
			staticInterceptorCtClass = classPool.get(ShorthandStaticInterceptor.class.getName());
			throwableCtClass = classPool.get(Throwable.class.getName());
			longArrClass = classPool.get(long[].class.getName());
			threadLocalCtClass = classPool.get(ThreadLocal.class.getName());
			nonBlockingHashMapLongCtClass = classPool.get(NonBlockingHashMapLong.class.getName());
			nonBlockingHashMapCtClass = classPool.get(NonBlockingHashMap.class.getName());
			
			
			classPool.appendClassPath(new ClassClassPath(UnsafeAdapter.class));
			classPool.importPackage("com.heliosapm.shorthand.util.unsafe");
			classPool.importPackage("java.util");
			classPool.importPackage(NonBlockingHashMap.class.getPackage().getName());
			classPool.importPackage(DataMapperBuilder.class.getPackage().getName());
			classPool.importPackage(ShorthandStaticInterceptor.class.getPackage().getName());
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}
	
	
	public static class TestClass {
		private static final Random R = new Random(System.currentTimeMillis());
		private static long nextWait() {
			return Math.abs(R.nextInt(100));
		}
		public void foo() {
			awaitTermination(100, TimeUnit.NANOSECONDS);
		}
		public boolean awaitTermination(long t, TimeUnit unit) {
			try {
				Thread.sleep(nextWait());
				return true;
			} catch (Throwable e) {
				//return false;
				throw new RuntimeException("INTERRUPTED WHILE WAITING");
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

