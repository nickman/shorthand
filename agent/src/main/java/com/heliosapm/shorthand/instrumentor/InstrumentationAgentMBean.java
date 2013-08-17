package com.heliosapm.shorthand.instrumentor;
/**
 * Helios Development Group LLC, 2010
 */


import java.io.File;
import java.lang.instrument.Instrumentation;
import java.net.URL;
import java.util.Set;

/**
 * <p>Title: InstrumentationAgentMBean</p>
 * <p>Description: JMX Management interface for InstrumentationAgent </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead
 * @version $LastChangedRevision$
 * <p><code>com.heliosapm.jmx.instrumentor.InstrumentationAgentMBean</code></p>
 */
public interface InstrumentationAgentMBean extends Instrumentation {
    /** The JMX Object name for the InstrumentationAgent  */
    public static final String OBJECT_NAME = "com.heliosapm.shorthand.agent:service=InstrumentationAgent";
    
	/**
	 * Returns the shallow size of the passed object
	 * @param obj The object to size
	 * @return the shallow size of the passed object
	 */
	public long sizeOf(Object obj);
	
	/**
	 * Returns the deep size of the passed object
	 * @param obj The object to size
	 * @return the deep size of the passed object
	 */
	public long deepSizeOf(Object obj);
	
	
	/**
	 * Returns the instrumentation impl name
	 * @return the instrumentation impl name
	 */
	public String getInstrumentationImplementation();
	
	/**
	 * Returns the instrumentation impl 
	 * @return the instrumentation impl 
	 */
	public Instrumentation getInstrumentationImpl();
	
	/**
	 * Indicates if the byteman retransformer is installed
	 * @return true if the byteman retransformer is installed, false otherwise
	 */
	public boolean isRetransformerInstalled();
	
	/**
	 * Installs the byteman retransformer if not already installed
	 */
	public void installRetransformer();
	
	/**
	 * Returns jars that this retransformer was asked to add to the boot classloader.
	 * @return a set of jar names
	 */
	public Set<String> getLoadedBootJars();

	/**
	 * Returns jars that this retransformer was asked to add to the system classloader.
	 * @return a set of jar names
	 */
	public Set<String> getLoadedSystemJars();


	/**
	 * Check whether compilation of rules is enabled or disabled 
	 * @return true if compilation of rules is enabled otherwise false
	 */
	public boolean isSkipOverrideRules();	
	
	/**
	 * Installs a rule script
	 * @param text The text of the script
	 * @param name The name of the script
	 * @return the textual reported results
	 */
	public String installScript(String text, String name);
	
	/**
	 * Installs a rule script from a URL
	 * @param scriptUrl The URL source of the script
	 * @return the textual reported results
	 */
	public String installScript(URL scriptUrl);
	
	/**
	 * Installs a rule script from a local file
	 * @param file The script file
	 * @return the textual reported results
	 */
	public String installScript(File file);
	
	/**
	 * Lists all the installed scripts
	 * @return a string listing all the installed scripts
	 */
	public String listScripts();
	
	/**
	 * Check whether verbose mode for rule processing is enabled or disabled 
	 * @return true if verbose mode is enabled etherwise false
	 */
	public boolean isVerbose();
	
	/**
	 * Sets verbose processing on or off
	 * @param verbose true for on, false for off
	 */
	public void setVerbose(boolean verbose);
	
	/**
	 * Check whether debug for rule processing is enabled or disabled 
	 * @return true if debug mode is enabled etherwise false
	 */
	public boolean isDebug();
	
	/**
	 * Sets debug mode on or off
	 * @param debug true for on, false for off
	 */
	public void setDebug(boolean debug);
	
	public String printCounters();
	
	
	/**
	 * Indicates whether compilation of rules is enabled or disabled
	 * @return true for compiled rules, false for interpreted
	 */
	public boolean isCompileToBytecode(); 
	
	/**
	 * Sets the compilation enablement for rules
	 * @param compiled true to compile, false to interpret
	 */
	public void setCompileToBytecode(boolean compiled);
	
	
	/**
	 * Returns an HTML table containing a dump of all installed rules
	 * @return a HTML table containing a dump of all installed rules
	 */
	public String printAllRules();
	
	/**
	 * Removes all installed rules
	 * @return the textual result of the op
	 */
	public String removeAllRules();
	
	/**
	 * Removes the named rule
	 * @param name The rule name to remove
	 */
	public void removeNamedRule(String name);
	
	/**
	 * Prints a detailed summary of the status of each installed rule script
	 * @return a detailed summary of the status of each installed rule script
	 */
	public String printRuleStatusSummary();
	
	/**
	 * Returns the number of installed rules
	 * @return the number of installed rules
	 */
	public int getInstalledRules();
	
    

}
