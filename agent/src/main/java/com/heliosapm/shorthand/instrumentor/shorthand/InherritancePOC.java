package com.heliosapm.shorthand.instrumentor.shorthand;

import java.io.Closeable;
import java.io.IOException;
import java.io.Serializable;
import java.util.LinkedHashSet;

import javassist.ClassClassPath;
import javassist.ClassPool;
import javassist.CtClass;

public class InherritancePOC {

	public static void main(String[] args) {
		log("InherritancePOC");
		try {
			ClassPool cp = new ClassPool();
			cp.appendClassPath(new ClassClassPath(C.class));
			CtClass clazz = cp.get(A.class.getName());
			LinkedHashSet<String> supers = new LinkedHashSet<String>();
			LinkedHashSet<String> ifaces = new LinkedHashSet<String>();
			CtClass target = clazz;
			
			while(target!=null) {
				for(CtClass iface: target.getInterfaces()) {
					ifaces.add(iface.getName());
				}
				target = target.getSuperclass();
				if(target!=null) supers.add(target.getName());
			}
			log("Supers: %s", supers.toString());
			log("Interfaces: %s", ifaces.toString());
		} catch (Exception ex) {
			ex.printStackTrace(System.err);
		}
	}
	
	abstract class C implements Closeable {
		
	}
	
	class B extends C implements Serializable {
		@Override
		public void close() throws IOException {
		}
 	}
	
	class A extends B {
		
	}
	
	/**
	 * Simple out formatted logger
	 * @param fmt The format of the message
	 * @param args The message arguments
	 */
	public static void log(String fmt, Object...args) {
		System.out.println(String.format(fmt, args));
	}


}
