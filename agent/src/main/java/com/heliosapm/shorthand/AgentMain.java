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
package com.heliosapm.shorthand;

import java.lang.instrument.Instrumentation;

import com.heliosapm.shorthand.util.version.VersionHelper;

/**
 * <p>Title: AgentMain</p>
 * <p>Description: The shorthand agent bootstrap class.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.shorthand.AgentMain</code></p>
 * TODO:
 * <ol>
 * 	<li>Starter for java-agent</li>
 *  <li>Agent Install Boot</li>
 *  <li>Agent Installer</li>
 *  <li>Includes for XML and properties</li>
 *  <li>Set system props</li>
 *  <li>Embedded Shorthand / ref to text file / ref to URL</li>
 *  <li>Implement predefs for shorthand compiler</li>
 *  <li>Load agent jar into boot classpath</li>
 *  <li>Create agent jar classpath ObjectName</li>
 *  <li>Logging - logback ? (preferably something not classpath searching)</li>
 *  <li></li>
 * </ol>
 */

public class AgentMain {
	/** The agent install provided instrumentation instance */
	private static volatile Instrumentation INST = null;
	
	/** The agent locator class name */
	public static final String AGENT_LOCATOR = "com.heliosapm.shorthand.attach.vm.agent.LocalAgentInstaller";
	
	/**
	 * Command line invoker for the shorthand agent.
	 * @param args Options are as follows:<ul>
	 * 	<li><b>install</b>: Installs the agent on a target vm. Syntax:<b><code>install &lt;pid&gt; [&lt;XML Config&gt;]</code></b>
	 * where <b>XML Config</b> is a file name or URL pointing to the shorthand agent configuration file.</li>
	 *  <li><b>list</b>: Lists the shorthand agent enabled JVMs locatable on the same host</li>
	 *  <li><b>tbd</b>:</li>
	 * </ul>
	 * In the absence of a command, a version and help banner is printed to stdout.
	 */
	public static void main(String[] args) {
		log(VersionHelper.getVersionBanner(AgentMain.class));
	}
	
	/**
	 * The <b><code>-javaagent</code></b> agent install hook.
	 * @param agentArgs The shorthand XML configuration file or URL
	 * @param inst The JVM's instrumentation instance
	 */
	public static void premain(String agentArgs, Instrumentation inst) {
		log("[%s] Premain: Args:[%s]  Instrumentation:[%s]", VersionHelper.getVersionBanner(AgentMain.class), agentArgs, inst);
		INST = inst;
	}

	/**
	 * The <b><code>-javaagent</code></b> agent install hook.
	 * @param agentArgs The shorthand XML configuration file or URL
	 */
	public static void premain(String agentArgs) {
		log("[%s] Premain: Args:[%s]", VersionHelper.getVersionBanner(AgentMain.class), agentArgs);
		premain(agentArgs, getBackupInstrumentation());
	}

	/**
	 * The runtime agent install hook.
	 * @param agentArgs The shorthand XML configuration file or URL
	 * @param inst The JVM's instrumentation instance
	 */
	public static void agentmain(String agentArgs, Instrumentation inst) {
		log("[%s] Agentmain: Args:[%s]  Instrumentation:[%s]", VersionHelper.getVersionBanner(AgentMain.class), agentArgs, inst);
		premain(agentArgs, inst);
	}

	/**
	 * The runtime agent install hook.
	 * @param agentArgs The shorthand XML configuration file or URL
	 */
	public static void agentmain(String agentArgs) {
		log("[%s] Agentmain: Args:[%s]", VersionHelper.getVersionBanner(AgentMain.class), agentArgs);
		premain(agentArgs, getBackupInstrumentation());
	}
	
	
	/**
	 * Loads the instrumentation instance using the agent locator class
	 * @return the instrumentation instance or null if it could not be loaded
	 */
	private static Instrumentation getBackupInstrumentation() {
		try {
			Class<?> locatorClass = Class.forName(AGENT_LOCATOR);
			return (Instrumentation) locatorClass.getDeclaredMethod("getInstrumentation").invoke(null);
		} catch (Exception ex) {
			loge("Failed to acquire Instrumentation instance", ex);
			return null;
		}
	}
	
	/**
	 * Returns the acquired instrumentation instance
	 * @return the acquired instrumentation instance
	 */
	public static Instrumentation getInstrumentation() {
		return INST;
	}
	
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
