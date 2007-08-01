/*
 * Copyright 2002-2007 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.core.annotation;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;

import org.springframework.core.BridgeMethodResolver;
import org.springframework.util.Assert;

/**
 * <p>
 * General utility methods for working with annotations, handling bridge methods
 * (which the compiler generates for generic declarations) as well as super
 * methods (for optional &quot;annotation inheritance&quot;). Note that none of
 * this is provided by the JDK's introspection facilities themselves.
 * </p>
 * <p>
 * As a general rule for runtime-retained annotations (e.g. for transaction
 * control, authorization or service exposure), always use the lookup methods on
 * this class (e.g., {@link #findAnnotation(Method, Class)},
 * {@link #getAnnotation(Method, Class)}, and {@link #getAnnotations(Method)})
 * instead of the plain annotation lookup methods in the JDK. You can still
 * explicitly choose between lookup on the given class level only ({@link #getAnnotation(Method, Class)})
 * and lookup in the entire inheritance hierarchy of the given method ({@link #findAnnotation(Method, Class)}).
 * </p>
 *
 * @author Rod Johnson
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 2.0
 * @see java.lang.reflect.Method#getAnnotations()
 * @see java.lang.reflect.Method#getAnnotation(Class)
 */
public abstract class AnnotationUtils {

	/**
	 * <p>
	 * Get all {@link Annotation Annotations} from the supplied {@link Method}.
	 * </p>
	 * <p>
	 * Correctly handles bridge {@link Method Methods} generated by the
	 * compiler.
	 * </p>
	 *
	 * @param method the method to look for annotations on
	 * @return the annotations found
	 * @see org.springframework.core.BridgeMethodResolver#findBridgedMethod(Method)
	 */
	public static Annotation[] getAnnotations(final Method method) {

		return BridgeMethodResolver.findBridgedMethod(method).getAnnotations();
	}

	/**
	 * <p>
	 * Get a single {@link Annotation} of <code>annotationType</code> from the
	 * supplied {@link Method}.
	 * </p>
	 * <p>
	 * Correctly handles bridge {@link Method Methods} generated by the
	 * compiler.
	 * </p>
	 *
	 * @param method the method to look for annotations on
	 * @param annotationType the annotation class to look for
	 * @return the annotations found
	 * @see org.springframework.core.BridgeMethodResolver#findBridgedMethod(Method)
	 */
	public static <A extends Annotation> A getAnnotation(final Method method, final Class<A> annotationType) {

		return BridgeMethodResolver.findBridgedMethod(method).getAnnotation(annotationType);
	}

	/**
	 * <p>
	 * Get a single {@link Annotation} of <code>annotationType</code> from the
	 * supplied {@link Method}, traversing its super methods if no annotation
	 * can be found on the given method.
	 * </p>
	 * <p>
	 * Annotations on methods are not inherited by default, so we need to handle
	 * this explicitly.
	 * </p>
	 *
	 * @param method the method to look for annotations on
	 * @param annotationType the annotation class to look for
	 * @return the annotation of the given type found, or <code>null</code>
	 */
	public static <A extends Annotation> A findAnnotation(Method method, final Class<A> annotationType) {

		if (!annotationType.isAnnotation()) {
			throw new IllegalArgumentException(annotationType + " is not an annotation");
		}
		A annotation = getAnnotation(method, annotationType);
		Class<?> cl = method.getDeclaringClass();
		while (annotation == null) {
			cl = cl.getSuperclass();
			if (cl == null || cl.equals(Object.class)) {
				break;
			}
			try {
				method = cl.getDeclaredMethod(method.getName(), method.getParameterTypes());
				annotation = getAnnotation(method, annotationType);
			}
			catch (final NoSuchMethodException ex) {
				// We're done...
			}
		}
		return annotation;
	}

	/**
	 * <p>
	 * Finds the first {@link Class} in the inheritance hierarchy of the
	 * specified <code>clazz</code> (including the specified
	 * <code>clazz</code> itself) which declares an annotation for the
	 * specified <code>annotationType</code>, or <code>null</code> if not
	 * found.
	 * </p>
	 * <p>
	 * The standard {@link Class} API does not provide a mechanism for
	 * determining which class in an inheritance hierarchy actually declares an
	 * {@link Annotation}, so we need to handle this explicitly.
	 * </p>
	 *
	 * @see Class#isAnnotationPresent(Class)
	 * @see Class#getDeclaredAnnotations()
	 * @param annotationType the Class object corresponding to the annotation
	 *        type
	 * @param clazz the Class object corresponding to the class on which to
	 *        check for the annotation
	 * @return the first {@link Class} in the inheritance hierarchy of the
	 *         specified <code>clazz</code> which declares an annotation for
	 *         the specified <code>annotationType</code>, or
	 *         <code>null</code> if not found.
	 * @throws IllegalArgumentException if a supplied argument is
	 *         <code>null</code>.
	 */
	public static Class<?> findAnnotationDeclaringClass(final Class<? extends Annotation> annotationType,
			final Class<?> clazz) throws IllegalArgumentException {

		Assert.notNull(annotationType, "annotationType can not be null.");
		Assert.notNull(clazz, "clazz can not be null.");

		if (Object.class.equals(clazz)) {
			return null;
		}

		return (isAnnotationDeclaredLocally(annotationType, clazz)) ? clazz : findAnnotationDeclaringClass(
				annotationType, clazz.getSuperclass());
	}

	/**
	 * <p>
	 * Returns <code>true</code> if an annotation for the specified
	 * <code>annotationType</code> is declared locally on the supplied
	 * <code>clazz</code>, else <code>false</code>.
	 * </p>
	 * <p>
	 * Note: this method does <strong>not</strong> determine if the annotation
	 * is {@link java.lang.annotation.Inherited inherited}. For greater clarity
	 * regarding inherited annotations, consider using
	 * {@link #isAnnotationInherited(Class, Class)} instead.
	 * </p>
	 *
	 * @param annotationType the Class object corresponding to the annotation
	 *        type
	 * @param clazz the Class object corresponding to the class on which to
	 *        check for the annotation
	 * @see Class#getDeclaredAnnotations()
	 * @see #isAnnotationInherited(Class, Class)
	 * @return <code>true</code> if an annotation for the specified
	 *         <code>annotationType</code> is declared locally on the supplied
	 *         <code>clazz</code>.
	 * @throws IllegalArgumentException if a supplied argument is
	 *         <code>null</code>.
	 */
	public static boolean isAnnotationDeclaredLocally(final Class<? extends Annotation> annotationType,
			final Class<?> clazz) throws IllegalArgumentException {

		Assert.notNull(annotationType, "annotationType can not be null.");
		Assert.notNull(clazz, "clazz can not be null.");

		boolean declaredLocally = false;
		for (final Annotation annotation : Arrays.asList(clazz.getDeclaredAnnotations())) {
			if (annotation.annotationType().equals(annotationType)) {
				declaredLocally = true;
				break;
			}
		}
		return declaredLocally;
	}

	/**
	 * <p>
	 * Returns <code>true</code> if an annotation for the specified
	 * <code>annotationType</code> is present on the supplied
	 * <code>clazz</code> and is
	 * {@link java.lang.annotation.Inherited inherited} (i.e., not declared
	 * locally for the class), else <code>false</code>.
	 * </p>
	 *
	 * @param annotationType the Class object corresponding to the annotation
	 *        type
	 * @param clazz the Class object corresponding to the class on which to
	 *        check for the annotation
	 * @see Class#isAnnotationPresent(Class)
	 * @see #isAnnotationDeclaredLocally(Class, Class)
	 * @return <code>true</code> if an annotation for the specified
	 *         <code>annotationType</code> is present on the supplied
	 *         <code>clazz</code> and is
	 *         {@link java.lang.annotation.Inherited inherited}.
	 * @throws IllegalArgumentException if a supplied argument is
	 *         <code>null</code>.
	 */
	public static boolean isAnnotationInherited(final Class<? extends Annotation> annotationType, final Class<?> clazz)
			throws IllegalArgumentException {

		Assert.notNull(annotationType, "annotationType can not be null.");
		Assert.notNull(clazz, "clazz can not be null.");

		return (clazz.isAnnotationPresent(annotationType) && !isAnnotationDeclaredLocally(annotationType, clazz));
	}

}
