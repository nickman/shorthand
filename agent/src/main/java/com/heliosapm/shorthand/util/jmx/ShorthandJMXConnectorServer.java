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
package com.heliosapm.shorthand.util.jmx;

import java.util.concurrent.atomic.AtomicBoolean;

import javax.management.ObjectName;
import javax.management.remote.JMXConnectorServer;
import javax.management.remote.JMXConnectorServerFactory;
import javax.management.remote.JMXServiceURL;

/**
 * <p>Title: ShorthandJMXConnectorServer</p>
 * <p>Description: JMX Connector Server bootstrap</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.shorthand.util.jmx.ShorthandJMXConnectorServer</code></p>
 */

public class ShorthandJMXConnectorServer {
	/** The singleton instance */
	private static volatile ShorthandJMXConnectorServer instance = null;
	/** The singleton instance ctor lock */
	private static final Object lock = new Object();

	/** Indicates if the connector server is running */
	private final AtomicBoolean started = new AtomicBoolean(false);
	/** The server instance */
	private JMXConnectorServer server = null;
	
	/**
	 * Acquires the ShorthandJMXConnectorServer singleton instance
	 * @return the ShorthandJMXConnectorServer singleton instance
	 */
	public static ShorthandJMXConnectorServer getInstance() {
		if(instance==null) {
			synchronized(lock) {
				if(instance==null) {
					instance = new ShorthandJMXConnectorServer();
				}
			}
		}
		return instance;
	}
	
	private ShorthandJMXConnectorServer() {
		try {
			JMXServiceURL serviceAddress = new JMXServiceURL("service:jmx:jmxmp://0.0.0.0:8006");
			server = JMXConnectorServerFactory.newJMXConnectorServer(serviceAddress, null, JMXHelper.getHeliosMBeanServer());
			String protocol = serviceAddress.getProtocol();
			int port = serviceAddress.getPort();
			ObjectName on = JMXHelper.objectName(
					new StringBuilder(server.getClass().getPackage().getName())
					.append(":protocol=").append(protocol)
					.append(",port=").append(port));
			JMXHelper.getHeliosMBeanServer().registerMBean(server, on);
			server.start();
			
			System.out.println("\n\t===============\n\tJMXServer Started\n\t===============\n");
		} catch (Exception ex) {			
			System.err.println("Failed to start JMXConnectorServer. Stack Trace Follows:");
			ex.printStackTrace(System.err);
		}
	}

}
