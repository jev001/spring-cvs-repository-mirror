/*
 * Copyright 2007 the original author or authors.
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
package org.springframework.test.context;

import java.util.Properties;

import org.springframework.beans.factory.support.BeanDefinitionReader;
import org.springframework.beans.factory.support.PropertiesBeanDefinitionReader;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.GenericApplicationContext;

/**
 * <p>
 * Concrete implementation of {@link AbstractGenericContextLoader} which reads
 * bean definitions from Java {@link Properties} resources.
 * </p>
 *
 * @see #createBeanDefinitionReader(GenericApplicationContext)
 * @author Sam Brannen
 * @version $Revision$
 * @since 2.1
 */
public class GenericPropertiesContextLoader extends AbstractGenericContextLoader {

	// ------------------------------------------------------------------------|
	// --- CONSTRUCTORS -------------------------------------------------------|
	// ------------------------------------------------------------------------|

	/**
	 * Constructs a new {@link GenericPropertiesContextLoader} with the supplied
	 * {@link ContextConfigurationAttributes configuration attributes}.
	 *
	 * @param configAttributes Configuration attributes for the
	 *        {@link ApplicationContext} which this context loader loads.
	 */
	public GenericPropertiesContextLoader(final ContextConfigurationAttributes configAttributes) {

		super(configAttributes);
	}

	// ------------------------------------------------------------------------|
	// --- INSTANCE METHODS ---------------------------------------------------|
	// ------------------------------------------------------------------------|

	/**
	 * <p>
	 * Creates a new {@link PropertiesBeanDefinitionReader}.
	 * </p>
	 *
	 * @see org.springframework.test.context.AbstractGenericContextLoader#createBeanDefinitionReader(org.springframework.context.support.GenericApplicationContext)
	 * @see PropertiesBeanDefinitionReader
	 * @return a new PropertiesBeanDefinitionReader.
	 */
	@Override
	protected BeanDefinitionReader createBeanDefinitionReader(final GenericApplicationContext context) {

		return new PropertiesBeanDefinitionReader(context);
	}

	// ------------------------------------------------------------------------|

}
