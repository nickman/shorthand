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
package com.heliosapm.shorthand.instrumentor.shorthand;

/**
 * <p>Title: ResidentTransformerBuilder</p>
 * <p>Description: A dynamic compiler for building "resident" classfile transformers which are intended
 * to be registered with the JVM's instrumentation instance and stay there to transform as-yet unloaded classes.
 * These transformers are intended for use in the bootstrap agent during the JVM's lifecycle where target classes 
 * may not reside in any known classpath, or, in some cases, may not even exist yet. This is in contrast to "on-demand"
 * transformers which are registered to execute one retransform operation and then removed.</p>
 * <p>The generated classfile transformers will be built with an optimized class filter that zones in on the classes
 * in accordance with a supplied shorthand script.</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.shorthand.instrumentor.shorthand.ResidentTransformerBuilder</code></p>
 */

public class ResidentTransformerBuilder {
	//  byte[]  	transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer)
	
	/** The singleton instance */	
	private static volatile ResidentTransformerBuilder instance = null;
	/** The singleton instance ctor lock */
	private static final Object lock = new Object();
	
	/**
	 * Returns the builder singleton
	 * @return the builder singleton
	 */
	public static final ResidentTransformerBuilder getInstance() {
		if(instance==null) {
			synchronized(lock) {
				if(instance==null) {
					instance = new ResidentTransformerBuilder();
				}
			}
		}
		return instance;
	}	
	
	private ResidentTransformerBuilder() {
		
	}

}
