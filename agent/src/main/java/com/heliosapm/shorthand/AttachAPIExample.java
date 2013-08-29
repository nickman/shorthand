/**
 * Helios, OpenSource Monitoring
 * Brought to you by the Helios Development Group
 *
 * Copyright 2013, Helios Development Group and individual contributors
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

import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryUsage;
import java.util.List;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.openmbean.CompositeData;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import com.sun.tools.attach.VirtualMachine;
import com.sun.tools.attach.VirtualMachineDescriptor;

/**
 * <p>Title: AttachAPIExample</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.shorthand.AttachAPIExample</code></p>
 */

public class AttachAPIExample {

	/**
	 * Uses the attach API to locate all JVMs accessible on this machine.
	 * @param args None
	 */
	public static void main(String[] args) {
		// Get my PID
		final String MYPID = ManagementFactory.getRuntimeMXBean().getName().split("@")[0];
		log("Scanning for JVMs...");
		// List all the Virtual Machine Descriptors
		List<VirtualMachineDescriptor> descriptors = VirtualMachine.list(); 
		for(VirtualMachineDescriptor vmd: descriptors) {
			VirtualMachine vm = null;
			// Do this in a catch block in case we run into a JVM that is not the same "bit" as we are
			try {
				vm = vmd.provider().attachVirtualMachine(vmd.id());
				String display = vmd.displayName().trim().isEmpty() ? "Unknown" : vmd.displayName();
				log("JVM%sPID: %s Display: %s", vmd.id().equals(MYPID) ? " (Me) " : " ", vmd.id(), display);
				String connectorAddress = vm.getAgentProperties().getProperty("com.sun.management.jmxremote.localConnectorAddress", null);
				if(connectorAddress!=null) {
					log("\tConnector Found Installed at [%s]", connectorAddress);
				} else {
					String javaHome = vm.getSystemProperties().getProperty("java.home");
					File agentJarFile = new File(javaHome + File.separator + "lib" + File.separator + "management-agent.jar");
					if(agentJarFile.exists()) {
						log("I think we can find this JVM's management agent here: [%s]", agentJarFile.toString());
						vm.loadAgent(agentJarFile.getAbsolutePath());
						connectorAddress = vm.getAgentProperties().getProperty("com.sun.management.jmxremote.localConnectorAddress", null);
						log("\tConnector Installed at [%s]", connectorAddress);
					} else {
						log("Cannot find the agent jar for JVM [%s] at [%s]", vmd.id(), javaHome);
					}
				}
				// Now lets try and connect and read some MBean values
				if(connectorAddress!=null) {
					log("Attaching to JVM [%s]...", vmd.id());
					JMXServiceURL jmxUrl = new JMXServiceURL(connectorAddress);
					JMXConnector connector = null;
					try {
						connector = JMXConnectorFactory.connect(jmxUrl);
						MBeanServerConnection conn = connector.getMBeanServerConnection();
						MemoryUsage heap = MemoryUsage.from((CompositeData)conn.getAttribute(new ObjectName(ManagementFactory.MEMORY_MXBEAN_NAME), "HeapMemoryUsage"));
						log("Heap Usage: %s", heap);
					} finally {
						if(connector!=null) {
							try { connector.close(); } catch (Exception ex) {/* No Op */}
						}
					}
				}
			} catch (Exception ex) {
				/* No Op */
			} finally {
				if(vm!=null) try { vm.detach(); } catch (Exception ex) {/* No Op */}
				log("======================================");
			}
		}

	}

	public static void log(String fmt, Object...args) {
		System.out.println(String.format(fmt, args));
	}
	
}
