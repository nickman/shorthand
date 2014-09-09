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
package com.heliosapm.shorthand.util.net;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Arrays;
import java.util.Random;



/**
 * <p>Title: MemBuffTest</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.shorthand.util.net.MemBuffTest</code></p>
 */

public class MemBuffTest {

	/**
	 * Creates a new MemBuffTest
	 */
	public MemBuffTest() {
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		log("Mem URL Test");
		URL.setURLStreamHandlerFactory(new MemoryURLStreamHandlerFactory());
		Random r = new Random(System.currentTimeMillis());
		try {
			URL url = new URL("mem://localhost/foo");
			URLConnection conn = url.openConnection();
			OutputStream os = conn.getOutputStream();
			byte[] bytes = new byte[8000];
			byte[] obytes = new byte[8000];
			r.nextBytes(bytes);
			os.write(bytes);
			InputStream is = url.openStream();
			int readBytes = is.read(obytes);
			log("Read %s bytes", readBytes);
			log("Arrays Are Equal:%s", Arrays.equals(bytes, obytes));
			os.close();
			is.close();
			conn = null;
			url = null;
			Thread.currentThread().join();
		} catch (Exception ex) {
			ex.printStackTrace(System.err);
		}

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
