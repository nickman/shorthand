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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.management.ManagementFactory;
import java.util.concurrent.atomic.AtomicReference;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;

import javax.management.MBeanServer;
import javax.management.MBeanServerConnection;
import javax.management.MBeanServerInvocationHandler;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import com.heliosapm.shorthand.attach.vm.VirtualMachine;
import com.heliosapm.shorthand.attach.vm.agent.LocalAgentInstaller;

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
	public static void mainX(String[] args)  {
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
	
	/** The created agent jar file name */
	protected static final AtomicReference<String> agentJar = new AtomicReference<String>(null); 
	
	/**
	 * Simple example of the install commands executed.
	 * @param args None
	 */
	public static void main(String[] args) {
		try {
			
			String pid = ManagementFactory.getRuntimeMXBean().getName().split("@")[0];
		    VirtualMachine vm = VirtualMachine.attach(pid);
		    // Check to see if transformer agent is installed
		    if(!vm.getSystemProperties().contains("demo.agent.installed")) {
		    	String fileName = createAgent();
		    	vm.loadAgent(fileName);  
		    	// that property will be set now, 
		    	// and the transformer MBean will be installed
		    }
			ObjectName on = new ObjectName("transformer:service=DemoTransformer");
			System.out.println("Instrumentation Deployed:" + ManagementFactory.getPlatformMBeanServer().isRegistered(on));
	    	MBeanServer server = ManagementFactory.getPlatformMBeanServer();
	    	Person person = new Person();
	    	person.sayHello(10);
	    	TransformerServiceMBean transformer = MBeanServerInvocationHandler.newProxyInstance(server, on, TransformerServiceMBean.class, false);
	    	transformer.transformClass(Person.class.getName(), "sayHello", null);
	    	System.out.println("Transformed [" + Person.class.getName() + "]");
	    	person.sayHello(10);
	    	//server.invoke(on, "transformClass", new Object[]{className}, new String[]{String.class.getName()});
		} catch (Exception ex) {
			System.err.println("Agent Installation Failed. Stack trace follows...");
			ex.printStackTrace(System.err);
		}
	}
	
	
	/**
	 * Creates the temporary agent jar file if it has not been created
	 * @return The created agent file name
	 */
	public static String createAgent() {
		if(agentJar.get()==null) {
			synchronized(agentJar) {
				if(agentJar.get()==null) {
					FileOutputStream fos = null;
					JarOutputStream jos = null;
					try {
						File tmpFile = File.createTempFile(LocalAgentInstaller.class.getName(), ".jar");
						System.out.println("Temp File:" + tmpFile.getAbsolutePath());
						tmpFile.deleteOnExit();		
						StringBuilder manifest = new StringBuilder();
						manifest.append("Manifest-Version: 1.0\nAgent-Class: " + AgentMain.class.getName() + "\n");
						manifest.append("Can-Redefine-Classes: true\n");
						manifest.append("Can-Retransform-Classes: true\n");
						manifest.append("Premain-Class: " + AgentMain.class.getName() + "\n");
						ByteArrayInputStream bais = new ByteArrayInputStream(manifest.toString().getBytes());
						Manifest mf = new Manifest(bais);
						fos = new FileOutputStream(tmpFile, false);
						jos = new JarOutputStream(fos, mf);
						addClassesToJar(jos, AgentMain.class, DemoTransformer.class, ModifyMethodTest.class, TransformerService.class, TransformerServiceMBean.class);
						jos.flush();
						jos.close();
						fos.flush();
						fos.close();
						agentJar.set(tmpFile.getAbsolutePath());
					} catch (Exception e) {
						throw new RuntimeException("Failed to write Agent installer Jar", e);
					} finally {
						if(fos!=null) try { fos.close(); } catch (Exception e) {}
					}

				}
			}
		}
		return agentJar.get();
	}
	
	/**
	 * Writes the passed classes to the passed JarOutputStream
	 * @param jos the JarOutputStream
	 * @param clazzes The classes to write
	 * @throws IOException on an IOException
	 */
	protected static void addClassesToJar(JarOutputStream jos, Class<?>...clazzes) throws IOException {
		for(Class<?> clazz: clazzes) {
			jos.putNextEntry(new ZipEntry(clazz.getName().replace('.', '/') + ".class"));
			jos.write(getClassBytes(clazz));
			jos.flush();
			jos.closeEntry();
		}
	}
	
	/**
	 * Returns the bytecode bytes for the passed class
	 * @param clazz The class to get the bytecode for
	 * @return a byte array of bytecode for the passed class
	 */
	public static byte[] getClassBytes(Class<?> clazz) {
		InputStream is = null;
		try {
			is = clazz.getClassLoader().getResourceAsStream(clazz.getName().replace('.', '/') + ".class");
			ByteArrayOutputStream baos = new ByteArrayOutputStream(is.available());
			byte[] buffer = new byte[8092];
			int bytesRead = -1;
			while((bytesRead = is.read(buffer))!=-1) {
				baos.write(buffer, 0, bytesRead);
			}
			baos.flush();
			return baos.toByteArray();
		} catch (Exception e) {
			throw new RuntimeException("Failed to read class bytes for [" + clazz.getName() + "]", e);
		} finally {
			if(is!=null) { try { is.close(); } catch (Exception e) {} }
		}
	}	

}
