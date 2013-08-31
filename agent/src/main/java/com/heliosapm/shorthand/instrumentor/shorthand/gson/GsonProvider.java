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
package com.heliosapm.shorthand.instrumentor.shorthand.gson;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.heliosapm.shorthand.instrumentor.shorthand.ShorthandScript;



/**
 * <p>Title: GsonProvider</p>
 * <p>Description: Provides pre-configured GSON instances</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.shorthand.instrumentor.shorthand.gson.GsonProvider</code></p>
 */

public class GsonProvider {
	/** The singleton instance */
	private static volatile GsonProvider instance = null;
	/** The singleton instance ctor lock */
	private static final Object lock = new Object();
	
	private final Gson prettyPrinter;
	private final Gson stdGson;
	private final Gson noSerGson;
	
	

	private GsonProvider() {
		prettyPrinter = new GsonBuilder()
			.setPrettyPrinting()
			.excludeFieldsWithoutExposeAnnotation()
			.registerTypeAdapter(ShorthandScript.class, new ShorthandScript())
			.create();
		stdGson = new GsonBuilder()
		.excludeFieldsWithoutExposeAnnotation()
		.registerTypeAdapter(ShorthandScript.class, new ShorthandScript())
		.create();
		noSerGson = new GsonBuilder()
		.excludeFieldsWithoutExposeAnnotation()		
		.create(); 
	}
	
	/**
	 * Acquires the singleton GsonProvider instance
	 * @return the singleton GsonProvider instance
	 */
	public static GsonProvider getInstance() {		
		if(instance==null) {
			synchronized(lock) {
				if(instance==null) {
					instance = new GsonProvider();
				}
			}
		}
		return instance;
	}
	
	/**
	 * Returns the pretty printing Gson instance
	 * @return the pretty printing Gson instance
	 */
	public Gson getPrettyPrinter() {
		return prettyPrinter;
	}

	/**
	 * Returns the standard configured Gson instance
	 * @return the standard configured Gson instance
	 */
	public Gson getGson() {
		return stdGson;
	}

	/**
	 * Returns the no ser/deser gson instance
	 * @return the noSerGson
	 */
	public Gson getNoSerGson() {
		return noSerGson;
	}

}
