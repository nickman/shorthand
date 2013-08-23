/**
* Helios Development Group LLC, 2013. 
 *
 */
package com.heliosapm.shorthand.datamapper;

import gnu.trove.map.hash.TIntLongHashMap;
import gnu.trove.map.hash.TObjectLongHashMap;
import gnu.trove.procedure.TObjectLongProcedure;

import java.io.File;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import javassist.ClassClassPath;
import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.NotFoundException;

import com.heliosapm.shorthand.accumulator.CopiedAddressProcedure;
import com.heliosapm.shorthand.accumulator.MetricSnapshotAccumulator;
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
	
    /** The system prop name to indicate if data mappers should be compiled, or if they should use the DefaultDataMapper */
    public static final String USE_COMPILED_DATAMAPPERS = "shorthand.datamapper.compile";
    
    

	
	private ClassPool cp;
	private final CtClass dataMapperIface;
	private final CtClass dataMapperSuper;
	private final CtClass stringClazz;
	private final CtClass objectClazz;
	private final CtClass objectArrClazz;
	private final CtClass copiedAddressProcedureIface;
	private final CtMethod dataMapperPutMethod;
	private final CtMethod copiedAddressMethod;
	private final CtMethod dataMapperPrePutMethod;
	private final CtMethod dataMapperResetMethod;
	private final CtMethod dataMappertoStringMethod;
	private final CtMethod dataMapperGetByAddressMethod;
	
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
			stringClazz = cp.get(String.class.getName());
			objectClazz = cp.get(Object.class.getName());
			objectArrClazz = cp.get(Object[].class.getName());
			cp.importPackage(UnsafeAdapter.class.getPackage().getName());
			cp.importPackage(MetricSnapshotAccumulator.class.getPackage().getName());			
			cp.importPackage(EnumCollectors.class.getPackage().getName());
			cp.importPackage(TIntLongHashMap.class.getPackage().getName());
			cp.importPackage(CopiedAddressProcedure.class.getPackage().getName());
			
			
			cp.importPackage("java.util");
			dataMapperIface = cp.get(IDataMapper.class.getName());
			dataMapperSuper = cp.get(DefaultDataMapper.class.getName());
			copiedAddressProcedureIface = cp.get(CopiedAddressProcedure.class.getName());
			CtMethod _dataMapperPutMethod = null, _dataMapperResetMethod = null, _dataMapperPrePutMethod = null, _dataMapperGetByAddressMethod = null;
			for(CtMethod cm: dataMapperIface.getDeclaredMethods()) {
				if("put".equals(cm.getName())) {
					_dataMapperPutMethod = cm;					
				} else if("reset".equals(cm.getName())) {
					_dataMapperResetMethod = cm;
				} else if("prePut".equals(cm.getName())) {
					_dataMapperPrePutMethod = cm;
				} else if("get".equals(cm.getName())) {
					if(cm.getParameterTypes()[0].equals(CtClass.longType)) {
						_dataMapperGetByAddressMethod = cm;
					}
				}
			}
			dataMapperPutMethod = _dataMapperPutMethod;
			dataMapperResetMethod = _dataMapperResetMethod;
			dataMapperPrePutMethod = _dataMapperPrePutMethod;			
			dataMapperGetByAddressMethod = _dataMapperGetByAddressMethod;
			dataMappertoStringMethod = new CtMethod(stringClazz, "toString", EMPTY_SIG, objectClazz);
			copiedAddressMethod = new CtMethod(objectClazz, "addressSpace", new CtClass[]{stringClazz, CtClass.longType, objectArrClazz}, copiedAddressProcedureIface);
			
			if(dataMapperPutMethod==null) { throw new RuntimeException("Failed to find put method"); }
			if(dataMapperResetMethod==null) {throw new RuntimeException("Failed to find reset put method"); }
			if(dataMapperPrePutMethod==null) {throw new RuntimeException("Failed to find prePut method"); }
			if(dataMapperGetByAddressMethod==null) {throw new RuntimeException("Failed to find get method"); }
		} catch (NotFoundException e) {
			throw new RuntimeException("Failed to get CtClass for DefaultDataMapper", e);
		}
	}
	
	//public void reset(long address, TObjectLongHashMap<ICcomollector<?>> offsets);
	
	/**
	 * @param enumCollectorTypeName The enum collector [class] to generate a data mapper for
	 * @param bitMask The bit mask
	 * @return a data mapper
	 */
	public IDataMapper getIDataMapper(String enumCollectorTypeName, int bitMask) {
		Class<T> clazz = (Class<T>) EnumCollectors.getInstance().typeForName(enumCollectorTypeName);
		return getIDataMapper(clazz, bitMask);
	}
	
	
	/**
	 * @param enumCollectorType The enum collector [class] to generate a data mapper for
	 * @param bitMask The bit mask
	 * @return a data mapper
	 */
	public IDataMapper getIDataMapper(Class<T> enumCollectorType, int bitMask) {
		if(!System.getProperty(USE_COMPILED_DATAMAPPERS, "true").toLowerCase().trim().equals("true")) {
			return DefaultDataMapper.INSTANCE;
		}
		final String key = enumCollectorType.getName() + "/" + bitMask;
		IDataMapper dataMapper = dataMappers.get(key);
		if(dataMapper==null) {
			synchronized(dataMappers) {
				dataMapper = dataMappers.get(key);
				if(dataMapper==null) {
					try {
						CtClass clazz = cp.makeClass("DataMapper_" + enumCollectorType.getName() + "_" + bitMask, dataMapperSuper);
						CtMethod putMethod = new CtMethod(dataMapperPutMethod.getReturnType(), dataMapperPutMethod.getName(), dataMapperPutMethod.getParameterTypes(), clazz);
						CtMethod toStringMethod  = new CtMethod(dataMappertoStringMethod.getReturnType(), dataMappertoStringMethod.getName(), dataMappertoStringMethod.getParameterTypes(), clazz);						
						CtMethod getByAddressMethod  = new CtMethod(dataMapperGetByAddressMethod.getReturnType(), dataMapperGetByAddressMethod.getName(), dataMapperGetByAddressMethod.getParameterTypes(), clazz);
						CtMethod addrsSpaceMethod = new CtMethod(copiedAddressMethod.getReturnType(), copiedAddressMethod.getName(), copiedAddressMethod.getParameterTypes(), clazz);
						
						clazz.addMethod(putMethod);
						clazz.addMethod(toStringMethod);
						clazz.addMethod(getByAddressMethod);
						
						clazz.addInterface(copiedAddressProcedureIface);
						clazz.addMethod(addrsSpaceMethod);
						
						

						final TObjectLongHashMap<T> offsets = (TObjectLongHashMap<T>) EnumCollectors.getInstance().offsets(enumCollectorType.getName(), bitMask);


						final StringBuilder getByAddressSrc = new StringBuilder("{\n\t");
						final StringBuilder putSrc = new StringBuilder("{");
						final StringBuilder addressSpaceSrc = new StringBuilder("{");
						final StringBuilder toStringSrc = new StringBuilder(String.format("{ return \"CompiledDataMapper Collector [%s] BitMask[%s] [", enumCollectorType.getSimpleName(), bitMask));
						if(!offsets.isEmpty()) {
							// ===============================
							// puts only. the compiled version
							// can automatically pre-apply
							// ===============================
							
//							T[] preApplies = offsets.keySet().iterator().next().getPreApplies(bitMask);
//							
//							if(preApplies.length>0) {
//								for(T _collector: preApplies) {
//									long offset = offsets.get(_collector);
//									//address = $1, offsets = $2, data = $3
//									String cname = _collector.getDeclaringClass().getName() + "." + _collector.name(); 
//									toStringSrc.append(_collector.name()).append("(").append(offset).append("),");
//									putSrc.append("\n\t").append(cname)
//										.append(".apply(").append(offset).append("+$1, $3);");
//									offsets.remove(_collector);
//								}
//							}
							
							// ======================
							// address space pre loop
							// ======================
							
							
							addressSpaceSrc.append("\n\tlong copyAddress = -1;");
							addressSpaceSrc.append("\n\tTIntLongHashMap data;"); 
							addressSpaceSrc.append("\n\tlong[] values;");
							addressSpaceSrc.append("\n\tint i;");
							
							
							getByAddressSrc.append("\n\tTIntLongHashMap data;"); 
							getByAddressSrc.append("\n\tlong[] values;");
							getByAddressSrc.append("\n\tint i;");

							
							
							
							addressSpaceSrc.append("\n\t\tif(copyAddress==-1L) return Collections.emptyMap();");
							addressSpaceSrc.append("\n\t\tint enumIndex = UnsafeAdapter.getInt(copyAddress + MetricSnapshotAccumulator.HeaderOffsets.EnumIndex.offset);"); 
							addressSpaceSrc.append("\n\t\tint bitmask = UnsafeAdapter.getInt(copyAddress + MetricSnapshotAccumulator.HeaderOffsets.BitMask.offset);");
							addressSpaceSrc.append("\n\t\tif(bitmask !=").append(bitMask).append(")  throw new RuntimeException(\"bitMask mismatch\");");
							addressSpaceSrc.append("\n\t\tClass enumClass = EnumCollectors.getInstance().type(enumIndex);");
							addressSpaceSrc.append("\n\t\tif(!enumClass.equals(").append(enumCollectorType.getName()).append(".class))  throw new RuntimeException(\"enum class mismatch\");");
							addressSpaceSrc.append("\n\t\tfinal Map map = new EnumMap(enumClass);");
							
							getByAddressSrc.append("\n\t\tint enumIndex = UnsafeAdapter.getInt($1 + MetricSnapshotAccumulator.HeaderOffsets.EnumIndex.offset);"); 
							getByAddressSrc.append("\n\t\tint bitmask = UnsafeAdapter.getInt($1 + MetricSnapshotAccumulator.HeaderOffsets.BitMask.offset);");
							getByAddressSrc.append("\n\t\tif(bitmask !=").append(bitMask).append(")  throw new RuntimeException(\"bitMask mismatch\");");
							getByAddressSrc.append("\n\t\tClass enumClass = EnumCollectors.getInstance().type(enumIndex);");
							getByAddressSrc.append("\n\t\tif(!enumClass.equals(").append(enumCollectorType.getName()).append(".class))  throw new RuntimeException(\"enum class mismatch\");");
							getByAddressSrc.append("\n\t\tfinal Map map = new EnumMap(enumClass);");
							
//							TObjectLongHashMap<T> offsets = (TObjectLongHashMap<T>) EnumCollectors.getInstance().offsets(enumIndex, bitmask);
//							final Map<T, TIntLongHashMap> map = new EnumMap<T, TIntLongHashMap>(enumClass);
							
							final AtomicLong dataOffset = new AtomicLong(MetricSnapshotAccumulator.HEADER_SIZE);
							offsets.forEachEntry(new TObjectLongProcedure<T>() {
								@Override
								public boolean execute(T collector, long offset) {
									String cname = collector.getDeclaringClass().getName() + "." + collector.name();
									// ======================
									// toString source
									// ======================
									toStringSrc.append(collector.name()).append("(").append(offset).append("),");
									// ======================
									// put source
									// ======================									
									putSrc.append("\n\t").append(cname)
									.append(".apply(").append(offset).append("L+$1, $3);");
									// ======================
									// address space source
									// ======================
									DataStruct ds = collector.getDataStruct();
									String[] subNames = collector.getSubMetricNames();
									addressSpaceSrc.append("\n\t\tdata = new TIntLongHashMap(").append(subNames.length).append(");");
									getByAddressSrc.append("\n\t\tdata = new TIntLongHashMap(").append(subNames.length).append(");");
									for(int i = 0; i < ds.size; i++) {
										addressSpaceSrc.append(String.format("data.put(%s, UnsafeAdapter.getLong(copyAddress + %s + (%s*UnsafeAdapter.LONG_SIZE)));", i, offset, i));								
										getByAddressSrc.append(String.format("data.put(%s, UnsafeAdapter.getLong($1 + %s + (%s*UnsafeAdapter.LONG_SIZE)));", i, offset, i));
									}
									addressSpaceSrc.append(String.format("\n\t\tmap.put(%s.%s, data);", collector.getDeclaringClass().getName(), collector.name()));
									getByAddressSrc.append(String.format("\n\t\tmap.put(%s.%s, data);", collector.getDeclaringClass().getName(), collector.name()));
									// ======================
									return true;
								}
							});
							// ======================
							// address space post loop
							// ======================
							addressSpaceSrc.append("\n\t\treturn map;\n}");									
							getByAddressSrc.append("\n\t\treturn map;\n}");
//							addressSpaceSrc.append("\n\t} finally {");
//							addressSpaceSrc.append("\n\t\tif(copyAddress!=-1) UnsafeAdapter.freeMemory(copyAddress);");
//							addressSpaceSrc.append("\n\t}\n}");
							// ======================
							
						} 
						putSrc.append("\n}");
						toStringSrc.deleteCharAt(toStringSrc.length()-1).append("]\";}");
						//log("Get Source:\n" + getByAddressSrc.toString());
						//log("Put Source:\n" + putSrc.toString());
						putMethod.setBody(putSrc.toString());
						toStringMethod.setBody(toStringSrc.toString());
						addrsSpaceMethod.setBody(addressSpaceSrc.toString());
						getByAddressMethod.setBody(getByAddressSrc.toString());

						clazz.setModifiers(clazz.getModifiers() & ~Modifier.ABSTRACT);
						clazz.writeFile(System.getProperty("java.io.tmpdir") + File.separator + "js");
						Class<? extends IDataMapper> jClazz = clazz.toClass();
						dataMapper = jClazz.newInstance();						
						dataMappers.put(key, dataMapper);
						log("\n\t====================================\n\tCompiled DataMapper\n\t" + dataMapper.toString() + "\n\t====================================\n");
					} catch (Exception ex) {
						System.err.println("Failed to generate DataMapper. Default instance will be returned. Stack trace follows...");
						ex.printStackTrace(System.err);
						return DefaultDataMapper.INSTANCE;
					}					
				}
			}
		}
		return dataMapper;
	}
	
	public static void main(String[] args) {
		log("DataMapperBuilder Test");
		IDataMapper dataMapper = DataMapperBuilder.getInstance().getIDataMapper(MethodInterceptor.class.getName(), MethodInterceptor.allMetricsMask);
		log("Created data mapper:" + dataMapper.toString());
	}
	
	public static void log(Object msg) {
		System.out.println(msg);
	}
	
}
