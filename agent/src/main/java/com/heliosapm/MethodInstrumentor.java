/**
 * Helios, OpenSource Monitoring
 * Brought to you by the Helios Development Group
 *
 * Copyright 2014, Helios Development Group and individual contributors
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
package com.heliosapm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;

/**
 * <p>Title: MethodInstrumentor</p>
 * <p>Description: </p> 
 * <p>Company: Helios Development Group LLC</p>
 * @author Whitehead (nwhitehead AT heliosdev DOT org)
 * <p><code>com.heliosapm.MethodInstrumentor</code></p>
 */

public class MethodInstrumentor extends MethodVisitor {
	/** The delegate method visitor */
	final MethodVisitor mv;
	/** The annotation visitor to extract existing @Instrumented annotations on the method */
	final InstrumentedAnnotationVisitor annViz;
	/** The api version */
	final int api;
	final Set<AnnotationDef> annotations = new HashSet<AnnotationDef>();
	
	
	
	/**
	 * Creates a new MethodInstrumentor
	 * @param api the ASM API version implemented by this visitor
	 * @param mv the method visitor to which this visitor must delegate method calls. May be null.
	 */
	public MethodInstrumentor(int api, MethodVisitor mv) {
		super(api, mv);
		this.api = api;
		this.mv = mv;
		annViz = new InstrumentedAnnotationVisitor(api);
	}
	
	
	/**
	 * {@inheritDoc}
	 * @see org.objectweb.asm.MethodVisitor#visitCode()
	 */
	@Override
	public void visitCode() {		
		super.visitCode();
		
		// if method matches, do yer stuff here.
	}
	
	/**
	 * {@inheritDoc}
	 * @see org.objectweb.asm.MethodVisitor#visitAnnotation(java.lang.String, boolean)
	 */
	@Override
	public AnnotationVisitor visitAnnotation(String desc, boolean visible) {	
		log("ANNOTATION: [%s], visible: [%s]", desc, visible);
		annViz.def = new AnnotationDef(desc);		
		//mv.visitAnnotation(desc, visible);
//		new InstrumentedAnnotationClassVisitor(api).visitAnnotation(desc, visible);
		if(!annotations.isEmpty()) {
			log("Applying [%s] Annotations", annotations.size());
		}
		return annViz;
				//super.visitAnnotation(desc, visible);
	}
	
	protected void onAnnotation(AnnotationDef def) {
		log("\nAnnotation Def:" + def);
		annotations.add(def);
	}
	
	
	
	
	

	
	public class AnnotationDef {
		final String annotationName;
		final Map<String, Object> values = new HashMap<String, Object>();
		final Map<String, Object[]> arrays = new HashMap<String, Object[]>();
		public AnnotationDef(final String annotationName) {
			this.annotationName = annotationName;
//			log("Created annotation: [%s]", this.annotationName);
		}
		public void addValue(final String name, final Object value) {
			values.put(name, value);
//			log("Added value: name:[%s], value:[%s], type:[%s]", name, value, value.getClass().getName());
		}
		public void addArray(final String name, final List<Object> values) {
			arrays.put(name, values.toArray());
//			log("Added Array: name:[%s], values:%s, type:[%s]", name, values, values.get(0).getClass().getName());
		}
		public String toString() {
			StringBuilder b = new StringBuilder("AnnotationDef: [");
			b.append("\n\tType: ").append(annotationName);
			b.append("\n\tValues: ");
			for(Map.Entry<String, Object> ve: values.entrySet()) {
				b.append("\n\t\t").append(ve.getKey()).append(":").append(ve.getValue()).append(" [").append(ve.getValue().getClass().getName()).append("]");
			}
			b.append("\n\tArrays: ");
			for(Map.Entry<String, Object[]> ve: arrays.entrySet()) {
				b.append("\n\t\t").append(ve.getKey()).append("[");
				for(Object o: ve.getValue()) {
					b.append("\n\t\t\t").append(o).append(" [").append(o.getClass().getName()).append("]");
				}
				b.append("\n\t\t]");
			}						
			return b.append("\n]").toString();
		}
		
	}
	
	protected class InstrumentedAnnotationClassVisitor extends ClassVisitor {

		/**
		 * Creates a new InstrumentedAnnotationClassVisitor
		 * @param api
		 */
		public InstrumentedAnnotationClassVisitor(int api) {
			super(api);
		}
		
		/**
		 * {@inheritDoc}
		 * @see org.objectweb.asm.ClassVisitor#visitAnnotation(java.lang.String, boolean)
		 */
		@Override
		public AnnotationVisitor visitAnnotation(String desc, boolean visible) { 
			
			return super.visitAnnotation(desc, visible);
			
		}
	}

	
	protected class InstrumentedAnnotationVisitor extends AnnotationVisitor {
		protected AnnotationDef def = null;
		protected final AtomicInteger nest = new AtomicInteger(0);
		protected String currentArr = null;
		protected List<Object> arrValues = null;
		/**
		 * Creates a new InstrumentedAnnotationVisitor
		 * @param api
		 */
		public InstrumentedAnnotationVisitor(int api) {
			super(api);
		}

		/**
		 * {@inheritDoc}
		 * @see org.objectweb.asm.AnnotationVisitor#visit(java.lang.String, java.lang.Object)
		 */
		@Override
		public void visit(String name, Object value) {
			log("ANNOT-VISIT: name: [%s], value: [%s]", name, value);
			if(name==null && arrValues != null) {
				arrValues.add(value);
			} else {
				def.addValue(name, value);
			}
			super.visit(name, value);
		}

		/**
		 * {@inheritDoc}
		 * @see org.objectweb.asm.AnnotationVisitor#visitEnum(java.lang.String, java.lang.String, java.lang.String)
		 */
		@Override
		public void visitEnum(String name, String desc, String value) {
			log("ANNOT-VISIT-ENUM: name: [%s], desc: [%s], value: [%s]", name, desc, value);
			super.visitEnum(name, desc, value);
		}

		/**
		 * {@inheritDoc}
		 * @see org.objectweb.asm.AnnotationVisitor#visitAnnotation(java.lang.String, java.lang.String)
		 */
		@Override
		public AnnotationVisitor visitAnnotation(String name, String desc) {
			log("ANNOT-VISIT-ANNOT: name: [%s], desc: [%s]", name, desc);
			return super.visitAnnotation(name, desc);
		}

		/**
		 * {@inheritDoc}
		 * @see org.objectweb.asm.AnnotationVisitor#visitArray(java.lang.String)
		 */
		@Override
		public AnnotationVisitor visitArray(String name) {
			int n = nest.incrementAndGet();
			log("ANNOT-VISIT-ARR: name: [%s], nested: [%s]", name, n);
			if(n==0) {
				return super.visitArray(name);
			} else if(n==1) {
				currentArr = name;
				arrValues = new ArrayList<Object>();				
			} else {
				log("NESTED:" + nest);
			}
			return this;
		}

		/**
		 * {@inheritDoc}
		 * @see org.objectweb.asm.AnnotationVisitor#visitEnd()
		 */
		@Override
		public void visitEnd() {
			
			log("ANNOT-VISIT-END: nested: [%s]", nest.get());
			if(nest.get()==0) {
				onAnnotation(def);
			} else if(nest.get()==1) {
				def.addArray(currentArr, arrValues);
				currentArr = null;
				arrValues = null;				
			}
			nest.decrementAndGet();
			super.visitEnd();
			
		}
	}
	
	
	/**
	 * Low maintenance logger
	 * @param msg The message format
	 * @param args The message tokens
	 */
	static public void log(final Object msg, final Object...args) {
		System.out.println(String.format(msg.toString(), args));
	}
	
	
}
