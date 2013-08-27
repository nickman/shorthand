/**
* Helios Development Group LLC, 2013. 
 *
 */
package com.heliosapm.shorthand.datamapper;

import gnu.trove.map.hash.TIntLongHashMap;

import java.io.File;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import javassist.ClassClassPath;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtConstructor;
import javassist.CtField;
import javassist.CtMethod;
import javassist.CtNewMethod;
import javassist.Modifier;
import javassist.NotFoundException;

import com.heliosapm.shorthand.accumulator.CopiedAddressProcedure;
import com.heliosapm.shorthand.accumulator.MetricSnapshotAccumulator;
import com.heliosapm.shorthand.accumulator.MetricSnapshotAccumulator.HeaderOffsets;
import com.heliosapm.shorthand.collectors.DataStruct;
import com.heliosapm.shorthand.collectors.EnumCollectors;
import com.heliosapm.shorthand.collectors.ICollector;
import com.heliosapm.shorthand.collectors.MethodInterceptor;
import com.heliosapm.shorthand.util.unsafe.UnsafeAdapter;

/**
 * <p>Title: DataMapperBuilder</p>
 * <p>Description: Generates new {@link IDataMapper} classes</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.shorthand.collectors.DataMapperBuilder</code></p>
 */

public class DataMapperBuilder<T extends Enum<T> & ICollector<T>> {
	/** The singleton instance */	
	private static volatile DataMapperBuilder<?> instance = null;
	/** The singleton instance ctor lock */
	private static final Object lock = new Object();
	
    
    

	
	private ClassPool cp;
	private final CtClass dataMapperIface;
	private final CtClass dataMapperSuper;
	private final CtClass stringClazz;
	private final CtClass objectClazz;
	private final CtClass objectArrClazz;
	private final CtClass longArrArrClazz;
	private final CtClass mapClazz;	
	private final CtClass copiedAddressProcedureIface;
	private final CtMethod dataMapperPutMethod;
	private final CtMethod dataMappergetDpMethod;
	private final CtMethod dataMapperPrePutMethod;
	private final CtMethod dataMapperResetMethod;
	private final CtMethod dataMappertoStringMethod;
	
	/** A map of already created data mappers keyed by enum-name/bitmask */
	private final Map<String, IDataMapper> dataMappers = new ConcurrentHashMap<String, IDataMapper>();
	
	/**
	 * Returns the compiler singleton
	 * @param clazz The class to cast to
	 * @return the compiler
	 */
	public static final <T extends Enum<T> & ICollector<T>> DataMapperBuilder<T> getInstance() {
		if(instance==null) {
			synchronized(lock) {
				if(instance==null) {
					instance = new DataMapperBuilder<T>();
				}
			}
		}
		return (DataMapperBuilder<T>) instance;
	}
	
	/**
	 * Returns the data mapper for the passed key or null if it is not found
	 * @param key The data mapper key which is a string as the <b><code>[enum collector type name]/[bitMask]</code></b>
	 * @return the data mapper or null if not found
	 */
	public IDataMapper<T> dataMapper(String key) {
		return dataMappers.get(key);
	}
	
	/** Empty signature const */
	public static final CtClass[] EMPTY_SIG = {};
	
	
	private static final AtomicLong nameFactory = new AtomicLong();
	private DataMapperBuilder() {		
		try {
			cp = new ClassPool();
			cp.appendSystemPath();
			cp.appendClassPath(new ClassClassPath(getClass()));
			longArrArrClazz = cp.get(long[][].class.getName());
			stringClazz = cp.get(String.class.getName());
			objectClazz = cp.get(Object.class.getName());
			objectArrClazz = cp.get(Object[].class.getName());
			cp.importPackage(UnsafeAdapter.class.getPackage().getName());
			cp.importPackage(MetricSnapshotAccumulator.class.getPackage().getName());			
			cp.importPackage(EnumCollectors.class.getPackage().getName());
			cp.importPackage(TIntLongHashMap.class.getPackage().getName());
			cp.importPackage(HeaderOffsets.class.getPackage().getName());
			cp.importPackage(CopiedAddressProcedure.class.getPackage().getName());
			cp.importPackage("java.util");
			cp.importPackage(HeaderOffsets.class.getPackage().getName());
			
			mapClazz = cp.get(Map.class.getName());
			dataMapperIface = cp.get(IDataMapper.class.getName());
			dataMapperSuper = cp.get(AbstractDataMapper.class.getName());
			copiedAddressProcedureIface = cp.get(CopiedAddressProcedure.class.getName());
			CtMethod _dataMapperPutMethod = null, _dataMapperResetMethod = null, _dataMapperPrePutMethod = null, _dataMappergetDpMethod = null;
			for(CtMethod cm: dataMapperIface.getDeclaredMethods()) {
				if("put".equals(cm.getName())) {
					_dataMapperPutMethod = cm;					
				} else if("reset".equals(cm.getName())) {
					_dataMapperResetMethod = cm;
				} else if("prePut".equals(cm.getName())) {
					_dataMapperPrePutMethod = cm;
				} else if("getDataPoints".equals(cm.getName())) {
					_dataMappergetDpMethod = cm;
				}
			}
			dataMapperPutMethod = _dataMapperPutMethod;
			dataMapperResetMethod = _dataMapperResetMethod;
			dataMapperPrePutMethod = _dataMapperPrePutMethod;			
			dataMappertoStringMethod = new CtMethod(stringClazz, "toString", EMPTY_SIG, objectClazz);
			dataMappergetDpMethod = _dataMappergetDpMethod;
			if(dataMapperPutMethod==null) { throw new RuntimeException("Failed to find put method"); }
			if(dataMapperResetMethod==null) {throw new RuntimeException("Failed to find reset put method"); }
			if(dataMapperPrePutMethod==null) {throw new RuntimeException("Failed to find prePut method"); }
			if(dataMappergetDpMethod==null) {throw new RuntimeException("Failed to find getDataPoints method"); }
		} catch (NotFoundException e) {
			throw new RuntimeException("Failed to get CtClass for AbstractDataMapper", e);
		}
	}
	
	//public void reset(long address, TObjectLongHashMap<ICcomollector<?>> offsets);
	
	/**
	 * @param enumCollectorTypeName The enum collector [class] to generate a data mapper for
	 * @param bitMask The bit mask
	 * @return a data mapper
	 */
	public IDataMapper<?> getIDataMapper(String enumCollectorTypeName, int bitMask) {
		Class<T> clazz = (Class<T>) EnumCollectors.getInstance().typeForName(enumCollectorTypeName);
		return getIDataMapper(clazz, bitMask);
	}
	
	
	/**
	 * @param enumCollectorType The enum collector [class] to generate a data mapper for
	 * @param bitMask The bit mask
	 * @return a data mapper
	 */
	public IDataMapper<?> getIDataMapper(Class<T> enumCollectorType, int bitMask) {
		final String key = enumCollectorType.getName() + "/" + bitMask;
		IDataMapper<?> dataMapper = dataMappers.get(key);
		if(dataMapper==null) {
			synchronized(dataMappers) {
				dataMapper = dataMappers.get(key);
				if(dataMapper==null) {
					try {
						final int enumIndex = EnumCollectors.getInstance().index(enumCollectorType.getName());
						CtClass clazz = cp.makeClass("DataMapper_" + enumCollectorType.getName() + "_" + bitMask, dataMapperSuper);
						CtMethod putMethod = new CtMethod(dataMapperPutMethod.getReturnType(), dataMapperPutMethod.getName(), dataMapperPutMethod.getParameterTypes(), clazz);
						CtMethod toStringMethod  = new CtMethod(dataMappertoStringMethod.getReturnType(), dataMappertoStringMethod.getName(), dataMappertoStringMethod.getParameterTypes(), clazz);
						
						CtMethod getDpsMethod = new CtMethod(dataMappergetDpMethod.getReturnType(), dataMappergetDpMethod.getName(), dataMappergetDpMethod.getParameterTypes(), clazz);
						CtMethod resetMethod = new CtMethod(dataMapperResetMethod.getReturnType(), dataMapperResetMethod.getName(), dataMapperResetMethod.getParameterTypes(), clazz);
						CtField offsetsField = new CtField(mapClazz, "offsets", clazz);
						CtField bitMaskField = new CtField(CtClass.intType, "bitMask", clazz);
						CtField enumIndexField = new CtField(CtClass.intType, "enumIndex", clazz);
						CtField defaultValuesField = new CtField(longArrArrClazz, "resetValues", clazz);
						
						offsetsField.setModifiers(offsetsField.getModifiers() | Modifier.FINAL);
						bitMaskField.setModifiers(offsetsField.getModifiers() | Modifier.FINAL);
						enumIndexField.setModifiers(offsetsField.getModifiers() | Modifier.FINAL);
						enumIndexField.setModifiers(defaultValuesField.getModifiers() | Modifier.FINAL);
						
						
						CtConstructor ctCtor = new CtConstructor(new CtClass[]{CtClass.intType, CtClass.intType}, clazz);
						
						clazz.addField(offsetsField);
						clazz.addField(enumIndexField);
						clazz.addField(bitMaskField);
						clazz.addField(defaultValuesField);
						
						clazz.addConstructor(ctCtor);
						
						clazz.addMethod(putMethod);
						clazz.addMethod(toStringMethod);
						clazz.addMethod(getDpsMethod);						
						clazz.addMethod(resetMethod);
						
						
						
						ctCtor.setBody(String.format("{\n\tenumIndex = $1;\n\tbitMask = $2;" + 
								"\n\toffsets = %s.%s.getOffsets($2);" + 
								"\n\tresetValues = ((ICollector)EnumCollectors.getInstance().ref($1)).getDefaultValues($2);" +  
								"\n}",								
								enumCollectorType.getName(), enumCollectorType.getEnumConstants()[0].name()));
						
						//EnumCollectors.getInstance().offsets($1, $2);\n}");  						
						

						final Map<T, Long> offsets = (Map<T, Long>) EnumCollectors.getInstance().offsets(enumCollectorType.getName(), bitMask);
						
						//============================================================
						//  Simple Accessors
						//============================================================
					 	clazz.addMethod(CtNewMethod.getter("getEnumIndex", enumIndexField));
					 	clazz.addMethod(CtNewMethod.getter("getBitMask", bitMaskField));
					 	clazz.addMethod(CtNewMethod.getter("getOffsets", offsetsField));
						//============================================================
						

						final StringBuilder putSrc = new StringBuilder("{\n\tUnsafeAdapter.putByte($1+").append(HeaderOffsets.Touch.offset).append("L, TOUCHED);");
						final StringBuilder getDpSrc = new StringBuilder("{\n\tlong[][] datapoints = new long[" + offsets.size() + "][0];\n\t");
						final StringBuilder resetSrc = new StringBuilder("{\n\tMetricSnapshotAccumulator.HeaderOffsets.Touch.set($1, 0L);");

						final StringBuilder toStringSrc = new StringBuilder(String.format("{ return \"CompiledDataMapper Collector [%s] BitMask[%s] [", enumCollectorType.getSimpleName(), bitMask));
						if(!offsets.isEmpty()) {
							// ===============================
							// puts only. the compiled version
							// can automatically pre-apply
							// ===============================
							int index = 0;
							for(Map.Entry<T, Long> offsetEntry: offsets.entrySet()) {
								ICollector<T> collector = offsetEntry.getKey();
								long offset = offsetEntry.getValue();
								String cname = collector.getClass().getName() + "." + collector.name();
								// ======================
								// toString source
								// ======================
								toStringSrc.append(collector.name()).append("(").append(offset).append("),");
								// ======================
								// put source
								// ======================									
								putSrc.append("\n\t").append(cname).append(".apply(").append(offset).append("L+$1, $2);");
								// ======================
								// get datapoints source
								// ======================									
								int arrSize = collector.getDataStruct().size;
								getDpSrc.append(String.format("\n\tdatapoints[%s] = UnsafeAdapter.getLongArray(($1 + %sL), %s);", index, offset, arrSize));
								// ======================
								// reset source
								// ======================	
								resetSrc.append(String.format("\n\tUnsafeAdapter.putLongArray(($1 + %sL), resetValues[%s]);", offset, index));

								
								index++;
							}
						} 
						getDpSrc.append("\n\treturn datapoints;}");
						resetSrc.append("\n}");
						putSrc.append("\n}");
						toStringSrc.deleteCharAt(toStringSrc.length()-1).append("]\";}");
						//log("Get Source:\n" + getByAddressSrc.toString());
						//log("Put Source:\n" + putSrc.toString());
						putMethod.setBody(putSrc.toString());
						toStringMethod.setBody(toStringSrc.toString());
						resetMethod.setBody(resetSrc.toString());
						getDpsMethod.setBody(getDpSrc.toString());
						clazz.setModifiers(clazz.getModifiers() & ~Modifier.ABSTRACT);
						clazz.writeFile(System.getProperty("java.io.tmpdir") + File.separator + "js");
						@SuppressWarnings("unchecked")
						Class<? extends IDataMapper<T>> jClazz = clazz.toClass();
						
						dataMapper = jClazz.getDeclaredConstructor(int.class, int.class).newInstance(enumIndex, bitMask);						
						dataMappers.put(key, dataMapper);
						log("\n\t====================================\n\tCompiled DataMapper\n\t" + dataMapper.toString() + "\n\t====================================\n");
					} catch (Exception ex) {
						System.err.println("Failed to generate DataMapper. Default instance will be returned. Stack trace follows...");
						ex.printStackTrace(System.err);
						throw new RuntimeException("Failed to generate DataMapper", ex);
					}					
				}
			}
		}
		return dataMapper;
	}
	
	public static void main(String[] args) {
		log("DataMapperBuilder Test");
		IDataMapper<?> dataMapper = DataMapperBuilder.getInstance().getIDataMapper(MethodInterceptor.class.getName(), MethodInterceptor.allMetricsMask);
		log("Created data mapper:" + dataMapper.toString());
	}
	
	public static void log(Object msg) {
		System.out.println(msg);
	}
	
}

