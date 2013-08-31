package com.heliosapm.shorthand.instrumentor;

import java.io.File;
import java.io.PrintWriter;
import java.lang.instrument.ClassDefinition;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarFile;

import javassist.convert.Transformer;

import javax.xml.crypto.dsig.Transform;

import org.cliffc.high_scale_lib.Counter;

import com.heliosapm.shorthand.util.StringPrintWriter;
import com.heliosapm.shorthand.util.URLHelper;
/**
 * Helios Development Group LLC, 2010
 */



/**
 * <p>Title: InstrumentationAgent</p>
 * <p>Description: JMX enabled byteman agent extension</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead 
 * <p><code>com.heliosapm.shorthand.instrumentor.InstrumentationAgent</code></p>
 */
public class InstrumentationAgent implements  InstrumentationAgentMBean {
	/** The instrumentation instance */
	protected final Instrumentation instrumentation;
	/** The byteman retransformer */
	protected ShorthandRetransformer retransformer = null;
	
	/** The byteman script repository */
	protected ScriptRepository scriptRepository = null;
	
	/** The helper manager */
	protected final ShorthandHelperManager helperManager;
	/** The deep object sizer */
	protected final ObjectSizer deepObjectSizer;
	
	
	/** The name of the class that will provide the instrumentation instance if we don't boot with this as the java-agent */
	public static final String AGENT_PROVIDER_CLASS = "org.jboss.aop.standalone.PluggableInstrumentor";
	/** The name of the field in the agent provider that will provide the instrumentation instance if we don't boot with this as the java-agent */
	public static final String AGENT_PROVIDER_FIELD = "instrumentor";
	
	/** Constant empty list */
	private static final List<String> EMPTY_STR_LIST = Collections.unmodifiableList(new ArrayList<String>(0));
	
	private static final Map<Object, Counter> synchCounterMap;
	
	static {
		System.setProperty("org.jboss.byteman.sysprops.strict", "false");
		System.setProperty("org.jboss.byteman.allow.config.update", "true");
		System.setProperty("org.jboss.byteman.transform.all", "true");
		try {
			 //private static HashMap<Object, Counter> counterMap = new HashMap<Object, Counter>();
			Field f = Helper.class.getDeclaredField("counterMap");
			f.setAccessible(true);
			synchCounterMap = (HashMap<Object, Counter>)f.get(null);			
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}
	
	/**
	 * Creates a new InstrumentationAgent
	 */
	public InstrumentationAgent() {
		instrumentation = getInstrumentation();
		deepObjectSizer = new ObjectSizer(instrumentation);
		helperManager = new ShorthandHelperManager(instrumentation);
	}
	
	/**
	 * Creates a new InstrumentationAgent
	 * @param inst The provided instrumentation
	 */
	public InstrumentationAgent(Instrumentation inst) {
		instrumentation = inst;
		deepObjectSizer = new ObjectSizer(instrumentation);
		helperManager = new ShorthandHelperManager(instrumentation);
	}
	
	
	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.shorthand.instrumentor.InstrumentationAgentMBean#isRetransformerInstalled()
	 */
	public synchronized boolean isRetransformerInstalled() {
		return retransformer != null;
	}
	
	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.shorthand.instrumentor.InstrumentationAgentMBean#installRetransformer()
	 */
	public synchronized void installRetransformer() {
		if(!isRetransformerInstalled()) {
			try {
				retransformer = new ShorthandRetransformer(instrumentation, EMPTY_STR_LIST, EMPTY_STR_LIST, true);
				scriptRepository = retransformer.getScriptRepository();
				instrumentation.addTransformer(retransformer, true);
				log.info("\n\t=====================================================\n\tInstalled Byteman Retransformer\n\t=====================================================");
			} catch (Exception e) {
				retransformer = null;
				log.error("Failed to install Byteman retransformer", e);
				throw new RuntimeException("Failed to install Byteman retransformer", e);
			}
		}
	}
	
	public String printCounters() {
		StringBuilder b = new StringBuilder("<table border='1'><tr><th>Counter Name</th><th>Count</th></tr>");
		Set<Object> keys = new HashSet<Object>(synchCounterMap.keySet());
		for(Object key: keys) {
			b.append("<tr><td>").append(key).append("</td><td>").append(synchCounterMap.get(key).count()).append("</td></tr>");
		}
		b.append("</table>");		
		return b.toString();
	}
	
	
	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.shorthand.instrumentor.InstrumentationAgentMBean#installScript(java.lang.String, java.lang.String)
	 */
	public String installScript(String text, String name) {
		StringPrintWriter sw = new StringPrintWriter();		
		try {
			retransformer.installScript(Collections.singletonList(text), Collections.singletonList(name), sw);
		} catch (Exception e) {
			log.error("Failed to install script [" + name + "]", e);
			e.printStackTrace(sw);
		}
		return sw.toString();		
	}
	
	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.shorthand.instrumentor.InstrumentationAgentMBean#getInstalledRules()
	 */
	public int getInstalledRules() {
		return scriptRepository.currentRules().size();
	}
	
	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.shorthand.instrumentor.InstrumentationAgentMBean#installScript(java.net.URL)
	 */
	public String installScript(URL scriptUrl) {
		String text = URLHelper.getTextFromURL(scriptUrl);
		String fileName = scriptUrl.getFile();
		return installScript(text, fileName);
	}
	
	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.shorthand.instrumentor.InstrumentationAgentMBean#installScript(java.io.File)
	 */
	public String installScript(File file) {
		return installScript(URLHelper.toURL(file));
	}
	
	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.shorthand.instrumentor.InstrumentationAgentMBean#isVerbose()
	 */
	public boolean isVerbose() {
		return Transformer.isVerbose();
	}
	
	
	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.shorthand.instrumentor.InstrumentationAgentMBean#setVerbose(boolean)
	 */
	public void setVerbose(boolean verbose) {
		if(verbose) System.setProperty(Transformer.VERBOSE, "true");
		else System.clearProperty(Transformer.VERBOSE);
		retransformer.updateConfiguration(Transformer.VERBOSE);
	}
	
	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.shorthand.instrumentor.InstrumentationAgentMBean#isDebug()
	 */
	public boolean isDebug() {
		return Transformer.isDebug();
	}
	
	/**
	 * Sets debug mode on or off
	 * @param debug true for on, false for off
	 */
	public void setDebug(boolean debug) {
		if(debug) System.setProperty(Transformer.DEBUG, "true");
		else System.clearProperty(Transformer.DEBUG);
		retransformer.updateConfiguration(Transformer.DEBUG);

	}
	
	
	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.shorthand.instrumentor.InstrumentationAgentMBean#isCompileToBytecode()
	 */
	public boolean isCompileToBytecode() {
		return Transformer.isCompileToBytecode();
	}
	
	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.shorthand.instrumentor.InstrumentationAgentMBean#setCompileToBytecode(boolean)
	 */
	public void setCompileToBytecode(boolean compiled) {
		if(compiled) System.setProperty(Transformer.COMPILE_TO_BYTECODE, "true");
		else System.clearProperty(Transformer.COMPILE_TO_BYTECODE);		
		retransformer.updateConfiguration(Transformer.COMPILE_TO_BYTECODE);
	}
	
	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.shorthand.instrumentor.InstrumentationAgentMBean#dumpAllRules()
	 */
	public String printAllRules() {
		StringBuilder b = new StringBuilder("<table border='1'><tr><th>File</th><th>Line</th><th>Name</th><th>Type</th><th>Method</th><th>Helper</th><th>Location</th><th>Text</th></tr>");
		
		for(RuleScript ruleScript: scriptRepository.currentRules()) {
			b.append("<tr>");
			b.append("<td>").append(ruleScript.getFile()).append("</td>");
			b.append("<td>").append(ruleScript.getLine()).append("</td>");
			b.append("<td>").append(ruleScript.getName()).append("</td>");
			b.append("<td>").append(ruleScript.isInterface() ? "Interface:" : "Class:").append(ruleScript.getTargetClass()).append("</td>");
			b.append("<td>").append(ruleScript.getTargetMethod()).append("</td>");
			b.append("<td>").append(ruleScript.getTargetHelper()).append("</td>");
			b.append("<td>").append(ruleScript.getTargetLocation()).append("</td>");
			b.append("<td>").append(ruleScript.getRuleText()).append("</td>");
			b.append("</tr>");
		}
		b.append("</table>");
		return b.toString();
	}
	
	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.shorthand.instrumentor.InstrumentationAgentMBean#removeNamedRule(java.lang.String)
	 */
	public void removeNamedRule(String name) {
		scriptRepository.removeScript(name);
	}
	
	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.shorthand.instrumentor.InstrumentationAgentMBean#removeAllRules()
	 */
	public String removeAllRules() {		
		StringPrintWriter spw = new StringPrintWriter();
		try {
			retransformer.removeScripts(null, spw);
		} catch (Exception e) {
			e.printStackTrace(spw);
		}
		return spw.toString();
	}
	
	public String getRuleKey(RuleScript rs) {
		StringBuilder b = new StringBuilder("RuleScript [");
		if(rs.getFile()!=null) b.append("f:").append(rs.getFile()).append(", ");
		b.append("n:").append(rs.getName()).append(", ");
		b.append("t:").append(rs.isInterface() ? "I-" : "C-").append(rs.getTargetClass()).append(", ");
		b.append("l:").append(rs.getTargetLocation().getLocationType().name()).append(", ");
		b.append("m:").append(rs.getTargetMethod());
		return b.toString();
	}
	
	
	
	/**
	 * Returns the full text of the passed rule script
	 * @param ruleScript The rule script to get the text of
	 * @return the rule script text
	 */
	protected String getRuleScriptText(RuleScript ruleScript) {
		StringPrintWriter spw = new StringPrintWriter();
        String file = ruleScript.getFile();
        int line = ruleScript.getLine();
        if (file != null) {
            spw.println("# " + file + " line " + line);
        }
        spw.println("RULE " + ruleScript.getName());
        if (ruleScript.isInterface()) {
            spw.println("INTERFACE " + ruleScript.getTargetClass());
        } else {
            spw.println("CLASS " + ruleScript.getTargetClass());
        }
        spw.println("METHOD " + ruleScript.getTargetMethod());
        if (ruleScript.getTargetHelper() != null) {
            spw.println("HELPER " + ruleScript.getTargetHelper());
        }
        spw.println(ruleScript.getTargetLocation());
        spw.println(ruleScript.getRuleText());
        spw.println("ENDRULE");
        return spw.toString();
	}
	
	
	public String printRuleStatusSummary() {
		StringBuilder b = new StringBuilder("<table border='1'><tr><th>ID</th><th>Transform Count</th>Transform Detail</tr>");
		
		for(RuleScript ruleScript: scriptRepository.currentRules()) {
			b.append("<tr>");
			b.append("<td><ul>");
				b.append("<li>").append(ruleScript.getFile()).append("</li>");
				b.append("<li>").append(ruleScript.getName()).append("</li>");
				b.append("<li>").append(ruleScript.isInterface() ? "Interface:" : "Class:").append(ruleScript.getTargetClass()).append("</li>");
				b.append("<li>").append(ruleScript.getTargetMethod()).append("</li>");
			b.append("</ul></td>");
			b.append("<td>").append(ruleScript.getTransformedCount()).append("</td>");
			b.append("<td><ul>");
			for(Transform t: ruleScript.getTransformed()) {
				b.append("<li>").append(t.getDetail()).append("</li>");
			}
			b.append("</ul></td>");
			
			b.append("</tr>");
		}
		b.append("</table>");
		return b.toString();
	}
	
	
	/**
	 * Returns the shallow size of the passed object
	 * @param obj The object to size
	 * @return the shallow size of the passed object
	 */
	public long sizeOf(Object obj) {
		return helperManager.getObjectSize(obj);
	}
	
	/**
	 * Returns the deep size of the passed object
	 * @param obj The object to size
	 * @return the deep size of the passed object
	 */
	public long deepSizeOf(Object obj) {
		return deepObjectSizer.deepSizeOf(obj);
	}
	

	
	/**
	 * Returns the instrumentation impl name
	 * @return the instrumentation impl name
	 */
	public String getInstrumentationImplementation() {
		return instrumentation.getClass().getName();
	}
	
	/**
	 * Returns the instrumentation impl 
	 * @return the instrumentation impl 
	 */
	public Instrumentation getInstrumentationImpl() {
		return instrumentation;
	}
	
	/**
	 * {@inheritDoc}
	 * @see com.heliosapm.shorthand.instrumentor.InstrumentationAgentMBean#listScripts()
	 */
	public String listScripts() {
		return retransformer.listScripts();
	}
	
	
	private Instrumentation getInstrumentation() {
		try {
			Class<?> clazz = Class.forName(AGENT_PROVIDER_CLASS);
			Field field = clazz.getDeclaredField(AGENT_PROVIDER_FIELD);
			field.setAccessible(true);
			return (Instrumentation) field.get(null);
		} catch (Exception e) {
			return null;
		}
	}
	
	public void startService() throws Exception {
		if(instrumentation!=null) {
			log.info("\n\t=============================\n\tStarted InstrumentationAgent\n\t=============================\n");
			installRetransformer();
		} else {
			log.warn("\n\t=============================\n\tInstrumentationAgent Did Not Acquire instrumentation. Stopping...\n\t=============================\n");
			stopService();
			try {
				server.unregisterMBean(serviceName);
			} catch (Exception e) {}
		}
	}

	/**
	 * @param transformer
	 * @param canRetransform
	 * @see java.lang.instrument.Instrumentation#addTransformer(java.lang.instrument.ClassFileTransformer, boolean)
	 */
	public void addTransformer(ClassFileTransformer transformer,
			boolean canRetransform) {
		instrumentation.addTransformer(transformer, canRetransform);
	}

	/**
	 * @param transformer
	 * @see java.lang.instrument.Instrumentation#addTransformer(java.lang.instrument.ClassFileTransformer)
	 */
	public void addTransformer(ClassFileTransformer transformer) {
		instrumentation.addTransformer(transformer);
	}

	/**
	 * @param jarfile
	 * @see java.lang.instrument.Instrumentation#appendToBootstrapClassLoaderSearch(java.util.jar.JarFile)
	 */
	public void appendToBootstrapClassLoaderSearch(JarFile jarfile) {
		instrumentation.appendToBootstrapClassLoaderSearch(jarfile);
	}

	/**
	 * @param jarfile
	 * @see java.lang.instrument.Instrumentation#appendToSystemClassLoaderSearch(java.util.jar.JarFile)
	 */
	public void appendToSystemClassLoaderSearch(JarFile jarfile) {
		instrumentation.appendToSystemClassLoaderSearch(jarfile);
	}

	/**
	 * @return
	 * @see java.lang.instrument.Instrumentation#getAllLoadedClasses()
	 */
	public Class[] getAllLoadedClasses() {
		return new Class[]{};
	}

	/**
	 * @param loader
	 * @return
	 * @see java.lang.instrument.Instrumentation#getInitiatedClasses(java.lang.ClassLoader)
	 */
	public Class[] getInitiatedClasses(ClassLoader loader) {
		return instrumentation.getInitiatedClasses(loader);
	}

	/**
	 * @param objectToSize
	 * @return
	 * @see java.lang.instrument.Instrumentation#getObjectSize(java.lang.Object)
	 */
	public long getObjectSize(Object objectToSize) {
		return instrumentation.getObjectSize(objectToSize);
	}

	/**
	 * @param theClass
	 * @return
	 * @see java.lang.instrument.Instrumentation#isModifiableClass(java.lang.Class)
	 */
	public boolean isModifiableClass(Class<?> theClass) {
		return instrumentation.isModifiableClass(theClass);
	}

	/**
	 * @return
	 * @see java.lang.instrument.Instrumentation#isNativeMethodPrefixSupported()
	 */
	public boolean isNativeMethodPrefixSupported() {
		return instrumentation.isNativeMethodPrefixSupported();
	}

	/**
	 * @return
	 * @see java.lang.instrument.Instrumentation#isRedefineClassesSupported()
	 */
	public boolean isRedefineClassesSupported() {
		return instrumentation.isRedefineClassesSupported();
	}

	/**
	 * @return
	 * @see java.lang.instrument.Instrumentation#isRetransformClassesSupported()
	 */
	public boolean isRetransformClassesSupported() {
		return instrumentation.isRetransformClassesSupported();
	}

	/**
	 * @param definitions
	 * @throws ClassNotFoundException
	 * @throws UnmodifiableClassException
	 * @see java.lang.instrument.Instrumentation#redefineClasses(java.lang.instrument.ClassDefinition[])
	 */
	public void redefineClasses(ClassDefinition... definitions)
			throws ClassNotFoundException, UnmodifiableClassException {
		instrumentation.redefineClasses(definitions);
	}

	/**
	 * @param transformer
	 * @return
	 * @see java.lang.instrument.Instrumentation#removeTransformer(java.lang.instrument.ClassFileTransformer)
	 */
	public boolean removeTransformer(ClassFileTransformer transformer) {
		return instrumentation.removeTransformer(transformer);
	}

	/**
	 * @param classes
	 * @throws UnmodifiableClassException
	 * @see java.lang.instrument.Instrumentation#retransformClasses(java.lang.Class<?>[])
	 */
	public void retransformClasses(Class<?>... classes)
			throws UnmodifiableClassException {
		instrumentation.retransformClasses(classes);
	}

	/**
	 * @param transformer
	 * @param prefix
	 * @see java.lang.instrument.Instrumentation#setNativeMethodPrefix(java.lang.instrument.ClassFileTransformer, java.lang.String)
	 */
	public void setNativeMethodPrefix(ClassFileTransformer transformer,
			String prefix) {
		instrumentation.setNativeMethodPrefix(transformer, prefix);
	}

	/**
	 * @param scriptTexts
	 * @param scriptNames
	 * @param out
	 * @throws Exception
	 * @see org.jboss.byteman.agent.Retransformer#installScript(java.util.List, java.util.List, java.io.PrintWriter)
	 */
	public void installScript(List<String> scriptTexts, List<String> scriptNames, PrintWriter out) throws Exception {
		retransformer.installScript(scriptTexts, scriptNames, out);
	}

	/**
	 * @param scriptTexts
	 * @param out
	 * @throws Exception
	 * @see org.jboss.byteman.agent.Retransformer#removeScripts(java.util.List, java.io.PrintWriter)
	 */
	public void removeScripts(List<String> scriptTexts, PrintWriter out)
			throws Exception {
		retransformer.removeScripts(scriptTexts, out);
	}

	/**
	 * @return
	 * @see org.jboss.byteman.agent.Retransformer#getLoadedBootJars()
	 */
	public Set<String> getLoadedBootJars() {
		return retransformer.getLoadedBootJars();
	}

	/**
	 * @return
	 * @see org.jboss.byteman.agent.Retransformer#getLoadedSystemJars()
	 */
	public Set<String> getLoadedSystemJars() {
		return retransformer.getLoadedSystemJars();
	}


	public boolean isSkipOverrideRules() {
		return retransformer.skipOverrideRules();
	}
	
//	
//	
//	/*
//<form method="post" action="HtmlAdaptor">
//   <input type="hidden" name="action" value="invokeOp">
//   <input type="hidden" name="name" value='com.heliosapm.shorthand.agent:service=InstrumentationAgent'>
//   <input type="hidden" name="methodIndex" value="4">
//   <hr align='left' width='80'>
//   <h4>java.lang.String installScript()</h4>
//   <p>Operation exposed for management</p>
//
//	<table cellspacing="2" cellpadding="2" border="1">
//		<tr class="OperationHeader">
//			<th>Param</th>
//			<th>ParamType</th>
//			<th>ParamValue</th>
//			<th>ParamDescription</th>
//		</tr>
//
//		<tr>
//			<td>p1</td>
//		   <td>java.lang.String</td>
//         <td> 
//
//            <input type="text" name="arg0">
//
//         </td>
//         <td>(no description)</td>
//		</tr>
//
//		<tr>
//			<td>p2</td>
//		   <td>java.lang.String</td>
//         <td> 
//
//            <input type="text" name="arg1">
//
//         </td>
//         <td>(no description)</td>
//		</tr>
//
//	</table>
//
//	<input type="submit" value="Invoke">
//	 */
}
