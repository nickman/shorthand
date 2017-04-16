/**
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
 */
package com.heliosapm.shorthand.compiler;

/**
 * <p>Title: ShorthandException</p>
 * <p>Description: The base shorthand parser exception class</p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.shorthand.compiler.ShorthandException</code></p>
 */

public class ShorthandException extends RuntimeException {

	
	/**  */
	private static final long serialVersionUID = 9081853398192952099L;
	/** The failed expression */
	protected final String expression;


	/**
	 * Creates a new ShorthandException
	 * @param message The failure message
	 * @param expression The expression that failed
	 * @param cause The underlying cause
	 */
	public ShorthandException(String message, String expression, Throwable cause) {
		super(message, cause);
		this.expression = expression;
	}
	
	/**
	 * Creates a new ShorthandException
	 * @param message The failure message
	 * @param expression The expression that failed
	 */
	public ShorthandException(String message, String expression) {
		super(message);
		this.expression = expression;
	}
	
	/**
	 * Returns the failed expression
	 * @return the failed expression
	 */
	public String getExpression() {
		return expression;
	}
	
	/**
	 * {@inheritDoc}
	 * @see java.lang.Throwable#toString()
	 */
	@Override
	public String toString() {		
		return new StringBuilder(super.toString()).append("Expression [").append(expression).append("]").toString();
	}

}
