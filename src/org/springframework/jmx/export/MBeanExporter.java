/*
 * Copyright 2002-2004 the original author or authors.
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

package org.springframework.jmx.export;

import java.util.HashMap;
import java.util.Map;

import javax.management.JMException;
import javax.management.MBeanException;
import javax.management.MBeanServer;
import javax.management.ObjectName;
import javax.management.modelmbean.InvalidTargetObjectTypeException;
import javax.management.modelmbean.ModelMBean;
import javax.management.modelmbean.RequiredModelMBean;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.aop.framework.ProxyFactory;
import org.springframework.aop.support.AopUtils;
import org.springframework.aop.target.LazyInitTargetSource;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.jmx.export.assembler.AutodetectCapableMBeanInfoAssembler;
import org.springframework.jmx.export.assembler.SimpleReflectiveMBeanInfoAssembler;
import org.springframework.jmx.export.naming.KeyNamingStrategy;
import org.springframework.jmx.support.JmxUtils;

/**
 * A bean that allows for any Spring-managed to be exposed to an <code>MBeanServer</code>
 * without the need to define any JMX-specific information in the bean classes.
 *
 * <p>If the bean implements one of the JMX management interface then
 * MBeanExporter will simply register the MBean with the server automatically.
 *
 * <p>If the bean does not implement on the JMX management interface then
 * <code>MBeanExporter</code> will create the management information using the
 * supplied <code>ModelMBeanMetadataAssembler</code> implementation.
 *
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @author Marcus Brito
 * @since 1.2
 */
public class MBeanExporter implements BeanFactoryAware, InitializingBean, DisposableBean {

	/**
	 * <code>Log</code> instance for this class.
	 */
	protected final Log logger = LogFactory.getLog(getClass());

	/**
	 * The <code>MBeanServer</code> instance being used to register beans.
	 */
	private MBeanServer server;

	/**
	 * The beans to be exposed as JMX managed resources.
	 */
	private Map beans;

	/**
	 * Stores the <code>MBeanInfoAssembler</code> to use for this adapter.
	 */
	private org.springframework.jmx.export.assembler.MBeanInfoAssembler assembler = new SimpleReflectiveMBeanInfoAssembler();

	/**
	 * The strategy to use for creating <code>ObjectName</code>s for an object.
	 */
	private org.springframework.jmx.export.naming.ObjectNamingStrategy namingStrategy = new KeyNamingStrategy();

	/**
	 * Stores the <code>BeanFactory</code> for use in autodetection process.
	 */
	private ConfigurableListableBeanFactory beanFactory;

	/**
	 * The beans that have been registered by this adapter.
	 */
	private ObjectName[] registeredBeans;


	/**
	 * Specify an instance <code>MBeanServer</code> with which all beans should
	 * be registered. The <code>MBeanExporter</code> will attempt to locate an
	 * existing <code>MBeanServer</code> if none is supplied.
	 */
	public void setServer(MBeanServer server) {
		this.server = server;
	}

	/**
	 * Supply a <code>Map</code> of beans to be registered with the JMX
	 * <code>MBeanServer</code>.
	 */
	public void setBeans(Map beans) {
		this.beans = beans;
	}

	/**
	 * Set the implementation of the <code>MBeanInfoAssembler</code> interface
	 * to use for this instance.
	 */
	public void setAssembler(org.springframework.jmx.export.assembler.MBeanInfoAssembler assembler) {
		this.assembler = assembler;
	}

	/**
	 * Set the implementation of the <code>ObjectNamingStrategy</code> interface to
	 * use for this instance.
	 */
	public void setNamingStrategy(org.springframework.jmx.export.naming.ObjectNamingStrategy namingStrategy) {
		this.namingStrategy = namingStrategy;
	}

	/**
	 * Implemented to grab the <code>BeanFactory</code> to allow for auto detection of
	 * managed bean resources.
	 */
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		if (beanFactory instanceof ConfigurableListableBeanFactory) {
			this.beanFactory = (ConfigurableListableBeanFactory) beanFactory;
		}
		else {
			logger.info("Not running in a ConfigurableListableBeanFactory: auto-detection of managed beans is disabled");
		}
	}

	/**
	 * Start bean registration automatically when deployed in an
	 * <code>ApplicationContext</code>.
	 * @see #registerBeans()
	 */
	public void afterPropertiesSet() throws JMException {
		// register the beans now
		registerBeans();
	}

	/**
	 * Registers the defined beans with the <code>MBeanServer</code>. Each bean is exposed
	 * to the <code>MBeanServer</code> via a <code>ModelMBean</code>. The actual implemetation
	 * of the <code>ModelMBean</code> interface used depends on the implementation of the
	 * <code>ModelMBeanProvider</code> interface that is configuerd. By default the <code>
	 * RequiredModelMBean</code> class that is supplied with all JMX implementations is used. The management
	 * interface produced for each bean is dependent on te <code>MBeanInfoAssembler</code>
	 * implementation being used. The <code>ObjectName</code> given to each bean is dependent on
	 * the implementation of the <code>ObjectNamingStrategy</code> interface being used.
	 */
	protected void registerBeans() throws JMException {

		// If no server was provided then try to load one.
		// This is useful in environment such as
		// JBoss where there is already an MBeanServer loaded
		if (this.server == null) {
			this.server = JmxUtils.locateMBeanServer();
		}

		// The beans property may be null, for example
		// if we are relying solely on auto-detection.
		if (this.beans == null) {
			this.beans = new HashMap();
		}

		if (this.beanFactory != null) {
			// Autodetect any beans that are already MBeans.
			logger.info("Autodetecting user-defined JMX MBeans");
			autodetectMBeans();

			// Allow the metadata assembler a chance to
			// vote for bean inclusion.
			if (this.assembler instanceof org.springframework.jmx.export.assembler.AutodetectCapableMBeanInfoAssembler) {
				autodetectBeans((AutodetectCapableMBeanInfoAssembler) this.assembler);
			}
		}

		// Check we now have at least one bean.
		if (this.beans.isEmpty()) {
			throw new IllegalArgumentException("Must specify at least one bean for registration");
		}

		Object[] keys = this.beans.keySet().toArray();
		this.registeredBeans = new ObjectName[keys.length];

		try {
			for (int x = 0; x < keys.length; x++) {
				String key = (String) keys[x];
				Object val = this.beans.get(key);
				ObjectName objectName = registerBean(key, val);
				this.registeredBeans[x] = objectName;
				if (logger.isInfoEnabled()) {
					logger.info("Registered MBean: " + objectName.toString());
				}
			}
		}
		catch (InvalidTargetObjectTypeException ex) {
			// We should never get this!
			logger.error("An invalid object type was used when specifying a managed resource", ex);
			throw new JMException("An invalid object type was used when specifying a managed resource. " +
					"This is a serious error and points to an error in the Spring JMX code. Root cause: " +
					ex.getMessage());
		}
	}

	/**
	 * Registers an individual bean with the <code>MBeanServer</code>. This method
	 * is responsible for deciding <strong>how</strong> a bean should be exposed
	 * to the <code>MBeanServer</code>. Specifically, if the <code>mapValue</code>
	 * is the name of a bean that is configured for lazy initialization, then
	 * a prxoy to the resource is registered with the <code>MBeanServer</code>
	 * so that the the lazy load behavior is honored. If the bean is already an
	 * MBean then it will be registered directly with the <code>MBeanServer</code>
	 * without any intervention. For all other beans or bean names, the resource
	 * itself is registered with the <code>MBeanServer</code> directly.
	 * @param beanKey the key associated with this bean in the beans map
	 * @param mapValue the value configured for this bean in the beans map
	 * May be either the <code>String</code> name of a bean, or the bean itself.
	 * @return the <code>ObjectName</code> under which the resource was registered
	 * @throws JMException in case of an error in the underlying JMX infrastructure
	 * @throws InvalidTargetObjectTypeException an error in the definition of the MBean resource
	 * @see #setBeans
	 * @see #registerLazyInit
	 * @see #registerMBean
	 * @see #registerSimpleBean
	 */
	private ObjectName registerBean(String beanKey, Object mapValue)
			throws JMException, InvalidTargetObjectTypeException {

		if (mapValue instanceof String) {
			String beanName = (String) mapValue;
			BeanDefinition beanDefinition = this.beanFactory.getBeanDefinition(beanName);
			if (beanDefinition.isLazyInit()) {
				if (logger.isDebugEnabled()) {
					logger.debug("Found bean name for lazy init bean with key [" + beanKey +
							"]. Registering bean with lazy init support.");
				}
				return registerLazyInit(beanKey, beanName);
			}
			else {
				if (logger.isDebugEnabled()) {
					logger.debug("String value under key [" + beanKey + "] either did not point to " +
							"a bean or the bean it pointed to was not registered for lazy initialization. " +
							"Registering bean normally with JMX server.");
				}
				Object bean = this.beanFactory.getBean(beanName);
				return registerBean(beanKey, bean);
			}
		}
		else {
			if (JmxUtils.isMBean(mapValue.getClass())) {
				if (logger.isDebugEnabled()) {
					logger.debug("Located MBean under key [" + beanKey + "] registering with JMX server " +
							"without Spring intervention.");
				}
				return registerMBean(beanKey, mapValue);
			}
			else {
				if (logger.isDebugEnabled()) {
					logger.debug("Located bean under key [" + beanKey + "] registering with JMX server.");
				}
				return registerSimpleBean(beanKey, mapValue);
			}
		}
	}

	/**
	 * Registers a plain bean directly with the <code>MBeanServer</code>. The
	 * management interface for the bean is created by the configured
	 * <code>MBeanInfoAssembler</code>.
	 * @param beanKey the key associated with this bean in the beans map
	 * @param bean the bean to register
	 * @return the <code>ObjectName</code> under which the bean was registered
	 * with the <code>MBeanServer</code>
	 * @throws JMException in case of an error in the underlying JMX infrastructure
	 * @throws InvalidTargetObjectTypeException an error in the definition of the MBean resource
	 */
	private ObjectName registerSimpleBean(String beanKey, Object bean)
			throws JMException, InvalidTargetObjectTypeException {

		ObjectName objectName = this.namingStrategy.getObjectName(bean, beanKey);
		if (logger.isDebugEnabled()) {
			logger.debug("Registering and assembling MBean: " + objectName);
		}

		ModelMBean mbean = createModelMBean();
		Class beanClass = (AopUtils.isAopProxy(bean) ? bean.getClass().getSuperclass() : bean.getClass());
		mbean.setModelMBeanInfo(this.assembler.getMBeanInfo(beanKey, beanClass));
		mbean.setManagedResource(bean, "ObjectReference");

		this.server.registerMBean(mbean, objectName);
		return objectName;
	}

	/**
	 * Registers beans that are configured for lazy initialization with the
	 * <code>MBeanServer<code> indirectly through a proxy.
	 * @param beanKey the key associated with this bean in the beans map
	 * @return the <code>ObjectName</code> under which the bean was registered
	 * with the <code>MBeanServer</code>
	 * @throws JMException an error in the underlying JMX infrastructure
	 * @throws InvalidTargetObjectTypeException an error in the definition of the MBean resource.
	 */
	private ObjectName registerLazyInit(String beanKey, String beanName)
			throws JMException, InvalidTargetObjectTypeException {

		LazyInitTargetSource targetSource = new LazyInitTargetSource();
		targetSource.setTargetBeanName(beanName);
		targetSource.setBeanFactory(this.beanFactory);

		ProxyFactory proxyFactory = new ProxyFactory();
		proxyFactory.setTargetSource(targetSource);
		proxyFactory.setProxyTargetClass(true);
		proxyFactory.setFrozen(true);

		Object proxy = proxyFactory.getProxy();
		ObjectName objectName = this.namingStrategy.getObjectName(proxy, beanKey);
		if (logger.isDebugEnabled()) {
			logger.debug("Registering lazy-init MBean: " + objectName);
		}

		ModelMBean mbean = createModelMBean();
		mbean.setModelMBeanInfo(this.assembler.getMBeanInfo(beanKey, targetSource.getTargetClass()));
		mbean.setManagedResource(proxy, "ObjectReference");

		this.server.registerMBean(mbean, objectName);
		return objectName;
	}

	/**
	 * Registers an existing MBean with the <code>MBeanServer</code>.
	 * @param beanKey the key associated with this bean in the beans map
	 * @param mbean the bean to register
	 * @return the <code>ObjectName</code> under which the bean was registered
	 * with the <code>MBeanServer</code>.
	 * @throws JMException an error in the underlying JMX infrastructure.
	 * an error in the definition of the MBean resource.
	 */
	private ObjectName registerMBean(String beanKey, Object mbean) throws JMException {
		ObjectName objectName = this.namingStrategy.getObjectName(mbean, beanKey);
		this.server.registerMBean(mbean, objectName);
		return objectName;
	}

	/**
	 * Attempts to detect any beans defined in the <code>ApplicationContext</code> that are
	 * valid MBeans and registers them automatically with the <code>MBeanServer</code>.
	 */
	private void autodetectMBeans() {
		autodetect(new AutodetectCallback() {
			public boolean include(String beanName, Class beanClass) {
				return JmxUtils.isMBean(beanClass);
			}
		});
	}

	/**
	 * Invoked when using an <code>AutodetectCapableMBeanInfoAssembler</code>.
	 * Gives the assembler the opportunity to add additional beans from the
	 * <code>BeanFactory</code> to the list of beans to be exposed via JMX.
	 * <p>This implementation prevents a bean from being added to the list
	 * automatically if it has already been added manually, and it prevents
	 * certain internal classes from being registered automatically.
	 */
	private void autodetectBeans(final AutodetectCapableMBeanInfoAssembler assembler) {
		autodetect(new AutodetectCallback() {
			public boolean include(String beanName, Class beanClass) {
				return (beanClass != null && assembler.includeBean(beanName, beanClass));
			}
		});
	}

	/**
	 * Performs the actual autodetection process, delegating to an instance
	 * <code>AutodetectCallback</code> to vote on the inclusion of a given bean.
	 * @param callback the <code>AutodetectCallback</code> to use when deciding
	 * whether to include a bean or not
	 */
	private void autodetect(AutodetectCallback callback) {
		String[] beanNames = this.beanFactory.getBeanDefinitionNames();
		for (int x = 0; x < beanNames.length; x++) {
			String beanName = beanNames[x];
			Class type = this.beanFactory.getType(beanName);
			if (callback.include(beanName, type)) {
				Object bean = this.beanFactory.getBean(beanName);
				if (!this.beans.containsValue(bean)) {
					// not already registered for JMXification
					this.beans.put(beanName, bean);
					if (logger.isInfoEnabled()) {
						logger.info("Bean with name '" + beanName + "' has been autodetected for JMX exposure");
					}
				}
				else {
					if (logger.isInfoEnabled()) {
						logger.info("Bean with name '" + beanName + "' is already registered for JMX exposure");
					}
				}
			}
		}
	}

	/**
	 * Create an instance of a class that implements <code>ModelMBean</code>.
	 * <p>This method is called to obtain a <code>ModelMBean</code> instance to
	 * use when registering a bean. This method is called once per bean during the
	 * registration phase and must return a new instance of <code>ModelMBean</code>
	 * @return a new instance of a class that implements <code>ModelMBean</code>
	 * @throws MBeanException if creation of the ModelMBean failed
	 */
	protected ModelMBean createModelMBean() throws MBeanException {
		return new RequiredModelMBean();
	}


	/**
	 * Unregisters all the beans when the enclosing <code>BeanFactory</code> is destroyed.
	 */
	public void destroy() throws Exception {
		logger.info("Unregistering all JMX-exposed beans on shutdown");
		for (int x = 0; x < this.registeredBeans.length; x++) {
			this.server.unregisterMBean(this.registeredBeans[x]);
		}
	}


	/**
	 * Internal callback interface for the autodetection process.
	 */
	private static interface AutodetectCallback {

		/**
		 * Called during the autodetection process to decide whether
		 * or not a bean should be include.
		 * @param beanName the name of the bean
		 * @param beanClass the class of the bean
		 */
		boolean include(String beanName, Class beanClass);
	}

}
