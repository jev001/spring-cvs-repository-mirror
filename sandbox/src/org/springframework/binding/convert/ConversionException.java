/*
 * Copyright 2004-2005 the original author or authors.
 */
package org.springframework.binding.convert;

/**
 * Base class for exceptions thrown by the type conversion system.
 * @author Keith Donald
 */
public class ConversionException extends RuntimeException {
	private Object value;

	private Class convertToClass;

	private Class convertFromClass;

	public ConversionException(Object value, Class convertToClass) {
		this(value, convertToClass, null, null, null);
	}

	public ConversionException(Object value, Class convertToClass, Throwable cause) {
		this(value, convertToClass, null, cause, null);
	}

	public ConversionException(Object value, Class convertToClass, Throwable cause, String message) {
		this(value, convertToClass, null, cause, message);
	}

	public ConversionException(Object value, Class convertToClass, Class convertFromClass) {
		this(value, convertToClass, convertFromClass, null, null);
	}

	public ConversionException(Object value, Class convertToClass, Class convertFromClass, Throwable cause,
			String message) {
		super(message, cause);
		this.value = value;
		this.convertFromClass = convertFromClass;
		this.convertToClass = convertToClass;
	}

	public Object getValue() {
		return value;
	}

	/**
	 * @return Returns the convertFromClass.
	 */
	public Class getConvertFromClass() {
		if (convertFromClass == null && value != null) {
			return value.getClass();
		}
		return convertFromClass;
	}

	/**
	 * @return Returns the convertFromClass.
	 */
	public Class getConvertToClass() {
		return convertFromClass;
	}

}