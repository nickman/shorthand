package com.heliosapm.shorthand.instrumentor;

import java.io.PrintWriter;
import java.lang.instrument.Instrumentation;
import java.util.List;

import javassist.convert.Transformer;

import com.heliosapm.shorthand.instrumentor.helper.ShorthandHelper;
import com.heliosapm.shorthand.util.StringPrintWriter;

/**
 * Helios Development Group LLC, 2013
 */



/**
 * <p>Title: ShorthandRetransformer</p>
 * <p>Description: Extended {@link Retransformer} to more simply expose internal functionality</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead 
 * <p><code>com.heliosapm.jmx.instrumentor.ShorthandRetransformer</code></p>
 */

public class ShorthandRetransformer { //extends Retransformer {
	
	/**
	 * Creates a new ShorthandRetransformer
	 * @param inst
	 * @param scriptPaths
	 * @param scriptTexts
	 * @param isRedefine
	 * @throws Exception
	 */
	public ShorthandRetransformer(Instrumentation inst, List<String> scriptPaths,
			List<String> scriptTexts, boolean isRedefine) throws Exception {
		super(inst, scriptPaths, scriptTexts, isRedefine);
		iceHelperManager = new ShorthandHelperManager(inst);
	}

    /**
     * The routine which actually does the real bytecode transformation. this is public because it needs to be
     * callable form the type checker script. In normal running the javaagent is the only class which has a handle
     * on the registered transformer so it is the only one which can reach this point.
     * @param ruleScript
     * @param loader
     * @param className
     * @param targetClassBytes
     * @return the byte code of the transformed class
     */
	@Override
    public byte[] transform(RuleScript ruleScript, ClassLoader loader, String className, byte[] targetClassBytes)   {
        TransformContext transformContext = new TransformContext(this, ruleScript, className, loader, iceHelperManager);
        return transformContext.transform(targetClassBytes);
    }
	
    public static boolean enableTriggers(boolean enable) {
    	ShorthandHelper.afterRuleExecute();
        return Transformer.enableTriggers(enable);
    }

	
	/**
	 * Removes all the scripts
	 */
	public void removeAllScripts() {
		try {
			this.removeScripts(null, new PrintWriter(System.out));
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	
	/**
	 * {@inheritDoc}
	 * @see org.jboss.byteman.agent.Retransformer#listScripts(java.io.PrintWriter)
	 */
	@Override
	public void listScripts(PrintWriter out) throws Exception {
		super.listScripts(out);
	}

	/**
	 * {@inheritDoc}
	 * @see org.jboss.byteman.agent.Transformer#isTransformable(java.lang.String)
	 */
	@Override
	public boolean isTransformable(String className) {
		return super.isTransformable(className);
	}

	/**
	 * {@inheritDoc}
	 * @see org.jboss.byteman.agent.Transformer#isSkipClass(java.lang.Class)
	 */
	@Override
	public boolean isSkipClass(Class<?> clazz) {
		return super.isSkipClass(clazz);
	}

	/**
	 * {@inheritDoc}
	 * @see org.jboss.byteman.agent.Transformer#dumpScript(org.jboss.byteman.agent.RuleScript)
	 */
	@Override
	public void dumpScript(RuleScript ruleScript) {
		super.dumpScript(ruleScript);
	}

	/**
	 * {@inheritDoc}
	 * @see org.jboss.byteman.agent.Transformer#isBytemanClass(java.lang.String)
	 */
	@Override
	public boolean isBytemanClass(String className) {
		return super.isBytemanClass(className);
	}
	
	/**
	 * Returns the script repository for this retransformer
	 * @return the script repository
	 */	
	public ScriptRepository getScriptRepository() {
		return this.scriptRepository;
	}
	
	/**
	 * Lists all the installed scripts
	 * @return a string listing all the installed scripts
	 */
	public String listScripts() {
		StringPrintWriter pw = new StringPrintWriter();
		try {
			this.listScripts(pw);
		} catch (Exception e) {
			e.printStackTrace(pw);
		}
		return pw.toString();
	}
	
	

}
