/*
 * Copyright 2002-2004 the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.springframework.util;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

/**
 * Miscellaneous class utility methods. Mainly for internal use within the
 * framework; consider Jakarta's Commons Lang for a more comprehensive suite
 * of utilities.
 * @author Keith Donald
 * @author Rob Harrop
 * @author Juergen Hoeller
 */
public abstract class ClassUtils {

	/** All primitive classes */
	private static Class[] PRIMITIVE_CLASSES = {boolean.class, byte.class, char.class, short.class,
	                                            int.class, long.class, float.class, double.class};

	/** The package separator character '.' */
	private static final char PACKAGE_SEPARATOR_CHAR = '.';

	/** The inner class separator character '$' */
	private static final char INNER_CLASS_SEPARATOR_CHAR = '$';


	/**
	 * Replacement for Class.forName() that also returns Class instances for primitives.
	 * @param name the name of the Class
	 * @return Class instance for the supplied name
	 */
	public static Class forName(String name) throws ClassNotFoundException{
		// Most class names will be quite long, considering that they
		// SHOULD sit in a package, so a length check is worthwhile.
		if (name.length() <= 8) {
			// could be a primitive - likely
			for (int i = 0; i < PRIMITIVE_CLASSES.length; i++) {
				Class clazz = PRIMITIVE_CLASSES[i];
				if (clazz.getName().equals(name)) {
					return clazz;
				}
			}
		}
		return Class.forName(name);
	}

	/**
	 * Get the class name without the qualified package name.
	 * @param clazz the class to get the short name for
	 * @return the class name of the class without the package name
	 * @throws IllegalArgumentException if the class is null
	 */
	public static String getShortName(Class clazz) {
		return getShortName(clazz.getName());
	}

	/**
	 * Return the uncaptilized short string name of a Java class.
	 * @param clazz the class
	 * @return the short name rendered in a standard JavaBeans property format
	 */
	public static String getShortNameAsProperty(Class clazz) {
		return StringUtils.uncapitalize(getShortName(clazz));
	}

	/**
	 * Get the class name without the qualified package name.
	 * @param className the className to get the short name for
	 * @return the class name of the class without the package name
	 * @throws IllegalArgumentException if the className is empty
	 */
	public static String getShortName(String className) {
		char[] charArray = className.toCharArray();
		int lastDot = 0;
		for (int i = 0; i < charArray.length; i++) {
			if (charArray[i] == PACKAGE_SEPARATOR_CHAR) {
				lastDot = i + 1;
			}
			else if (charArray[i] == INNER_CLASS_SEPARATOR_CHAR) {
				charArray[i] = PACKAGE_SEPARATOR_CHAR;
			}
		}
		return new String(charArray, lastDot, charArray.length - lastDot);
	}

	/**
	 * Return a static method of a class.
	 * @param methodName the static method name
	 * @param clazz the class which defines the method
	 * @param args the parameter types to the method
	 * @return the static method, or null if no static method was found
	 * @throws IllegalArgumentException if the method name is blank or the clazz is null
	 */
	public static Method getStaticMethod(Class clazz, String methodName, Class[] args) {
		try {
			Method method = clazz.getDeclaredMethod(methodName, args);
			if ((method.getModifiers() & Modifier.STATIC) != 0) {
				return method;
			}
		} catch (NoSuchMethodException ex) {
		}
		return null;
	}

	/**
	 * Return a path suitable for use with ClassLoader.getResource (also
	 * suitable for use with Class.getResource by prepending a slash ('/') to
	 * the return value. Built by taking the package of the specified class
	 * file, converting all dots ('.') to slashes ('/'), adding a trailing slash
	 * if necesssary, and concatenating the specified resource name to this.
	 * <br/>As such, this function may be used to build a path suitable for
	 * loading a resource file that is in the same package as a class file,
	 * although {link org.springframework.core.io.ClassPathResource} is usually
	 * even more convenient.
	 * @param clazz the Class whose package will be used as the base
	 * @param resourceName the resource name to append. A leading slash is optional.
	 * @return the built-up resource path
	 * @see java.lang.ClassLoader#getResource(String)
	 * @see java.lang.Class#getResource(String)
	 */
	public static String addResourcePathToPackagePath(Class clazz, String resourceName) {
		if (!resourceName.startsWith("/")) {
			return classPackageAsResourcePath(clazz) + "/" + resourceName;
		}
		else {
			return classPackageAsResourcePath(clazz) + resourceName;
		}
	}

	/**
	 * Given an input class object, return a string which consists of the
	 * class's package name as a pathname, i.e., all dots ('.') are replaced by
	 * slashes ('/'). Neither a leading nor trailing slash is added. The result
	 * could be concatenated with a slash and the name of a resource, and fed
	 * directly to ClassLoader.getResource(). For it to be fed to Class.getResource,
	 * a leading slash would also have to be prepended to the return value.
	 * @param clazz the input class. A null value or the default (empty) package
	 * will result in an empty string ("") being returned.
	 * @return a path which represents the package name
	 * @see java.lang.ClassLoader#getResource(String)
	 * @see java.lang.Class#getResource(String)
	 */
	public static String classPackageAsResourcePath(Class clazz) {
		if (clazz == null || clazz.getPackage() == null) {
			return "";
		}
		return clazz.getPackage().getName().replace('.', '/');
	}

}
