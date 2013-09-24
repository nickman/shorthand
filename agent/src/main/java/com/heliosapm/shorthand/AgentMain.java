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
 */

public class AgentMain {

	public static void main(String[] args) {
		log(VersionHelper.getVersionBanner(AgentMain.class));
	}
	
	public static void premain(String agentArgs, Instrumentation inst) {
		log("[%s] Premain: Args:[%s]  Instrumentation:[%s]", VersionHelper.getVersionBanner(AgentMain.class), agentArgs, inst);
	}

	public static void premain(String agentArgs) {
		log("[%s] Premain: Args:[%s]", VersionHelper.getVersionBanner(AgentMain.class), agentArgs);
	}

	public static void agentmain(String agentArgs, Instrumentation inst) {
		log("[%s] Agentmain: Args:[%s]  Instrumentation:[%s]", VersionHelper.getVersionBanner(AgentMain.class), agentArgs, inst);
	}

	public static void agentmain(String agentArgs) {
		log("[%s] Agentmain: Args:[%s]", VersionHelper.getVersionBanner(AgentMain.class), agentArgs);
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
