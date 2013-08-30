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
package com.heliosapm.shorthand.util.javassist;

import java.io.IOException;

/**
 * <p>Title: CodeBuilder</p>
 * <p>Description: A javassist source code builder/helper</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.shorthand.util.javassist.CodeBuilder</code></p>
 */

public class CodeBuilder implements CharSequence, Appendable {
	/** The underlying string builder  */
	protected final StringBuilder source;
	
	/**
	 * Creates a new CodeBuilder
	 */
	public CodeBuilder() {
		source = new StringBuilder();
	}
	
	/**
	 * {@inheritDoc}
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return source.toString();
	}
	
	/**
	 * Creates a new CodeBuilder
	 * @param initial The initial string builder to start with
	 * 
	 */
	public CodeBuilder(CharSequence initial) {
		source = new StringBuilder(initial);
	}
	
	/**
	 * Clears the content of this code builder
	 * @return this code builder
	 */
	public CodeBuilder clear() {
		source.setLength(0);
		return this;
	}
	
	/**
	 * Appends a resolved string format template
	 * @param format The string format template
	 * @param args The arguments to populate the format template
	 * @return this builder
	 */
	public CodeBuilder appendFmt(CharSequence format, Object...args) {
		source.append(String.format(format.toString(), args));
		return this;
	}
	

	/**
	 * {@inheritDoc}
	 * @see java.lang.Appendable#append(java.lang.CharSequence)
	 */
	@Override
	public CodeBuilder append(CharSequence csq) throws IOException {
		source.append(csq);
		return this;
	}

	/**
	 * {@inheritDoc}
	 * @see java.lang.Appendable#append(java.lang.CharSequence, int, int)
	 */
	@Override
	public CodeBuilder append(CharSequence csq, int start, int end) throws IOException {
		source.append(csq, start, end);
		return this;
	}

	/**
	 * {@inheritDoc}
	 * @see java.lang.Appendable#append(char)
	 */
	@Override
	public CodeBuilder append(char c) throws IOException {
		source.append(c);
		return this;
	}

	/**
	 * {@inheritDoc}
	 * @see java.lang.CharSequence#length()
	 */
	@Override
	public int length() {
		return source.length();
	}

	/**
	 * {@inheritDoc}
	 * @see java.lang.CharSequence#charAt(int)
	 */
	@Override
	public char charAt(int index) {
		return source.charAt(index);
	}

	/**
	 * {@inheritDoc}
	 * @see java.lang.CharSequence#subSequence(int, int)
	 */
	@Override
	public CodeBuilder subSequence(int start, int end) {
		return new CodeBuilder(source.subSequence(start, end));
	}

}
