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
package com.heliosapm.shorthandexamples;

import java.io.File;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import com.heliosapm.shorthand.attach.vm.VirtualMachine;

/**
 * <p>Title: AgentInstaller</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.shorthandexamples.AgentInstaller</code></p>
 */

public class AgentInstaller {
	/**
	 * Installs the loader agent on the target JVM identified in <code>args[0]</code>
	 * and then transforms all the classes identified in <code>args[1..n]</code>.
	 * @param args The target JVM pid in [0] followed by the classnames to transform
	 */
	public static void main(String[] args)  {
		String agentPath = "D:\\work\\workspace\\myjar\\loaded.jar";
	    String vid = args[0]; 
	    VirtualMachine vm = VirtualMachine.attach(vid);
	    // Check to see if transformer agent is installed
	    if(!vm.getSystemProperties().contains("demo.agent.installed")) {
	    	vm.loadAgent(agentPath);  
	    	// that property will be set now, 
	    	// and the transformer MBean will be installed
	    }
	    // Check to see if connector is installed
	    String connectorAddress = vm.getAgentProperties().getProperty("com.sun.management.jmxremote.localConnectorAddress", null);
	    if(connectorAddress==null) {
	    	// It's not, so install the management agent
	    	String javaHome = vm.getSystemProperties().getProperty("java.home");
	    	File managementAgentJarFile = new File(javaHome + File.separator + "lib" + File.separator + "management-agent.jar");
			vm.loadAgent(managementAgentJarFile.getAbsolutePath());
			connectorAddress = vm.getAgentProperties().getProperty("com.sun.management.jmxremote.localConnectorAddress", null);
			// Now it's installed
	    }
	    // Now connect and transform the classnames provided in the remaining args.
	    JMXConnector connector = null;
	    try {
		    // This is the ObjectName of the MBean registered when loaded.jar was installed.
		    ObjectName on = new ObjectName("transformer:service=DemoTransformer");
		    // Here we're connecting to the target JVM through the management agent
	    	connector = JMXConnectorFactory.connect(new JMXServiceURL(connectorAddress));
	    	MBeanServerConnection server = connector.getMBeanServerConnection();
	    	for(int i = 1; i < args.length; i++) {
	    		String className = args[i];
	    		// Call transformClass on the transformer MBean
	    		server.invoke(on, "transformClass", new Object[]{className}, new String[]{String.class.getName()});
	    	}
	    } catch (Exception ex) {
	    	ex.printStackTrace(System.err);
	    } finally {
	    	if(connector!=null) try { connector.close(); } catch (Exception e) {}
	    }
		// Done. (Hopefully)
	}
}
