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

package org.springframework.beans.factory.support;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

import net.sf.cglib.proxy.Callback;
import net.sf.cglib.proxy.CallbackFilter;
import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;
import net.sf.cglib.proxy.NoOp;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.BeanFactory;

/**
 * Default object instantiation strategy for use in BeanFactories.
 * Uses CGLIB to generate subclasses dynamically if methods need to be
 * overridden by the container, to implement Method Injection.
 *
 * <p>Using Method Injection features requires CGLIB on the classpath.
 * However, the core IoC container will still run without CGLIB being available.
 *
 * @author Rod Johnson
 * @version $Id$
 */
public class CglibSubclassingInstantiationStrategy extends SimpleInstantiationStrategy {

	/**
	 * Index in the CGLIB callback array for passthrough behaviour,
	 * in which case the subclass won't override the original class.
	 */
	private static final int PASSTHROUGH = 0;

	/**
	 * Index in the CGLIB callback array for a method that should
	 * be overridden to provide method lookup.
	 */
	private static final int LOOKUP_OVERRIDE = 1;
	
	/**
	 * Index in the CGLIB callback array for a method that should
	 * be overridden using generic Methodreplacer functionality.
	 */
	private static final int METHOD_REPLACER = 2;


	protected Object instantiateWithMethodInjection(RootBeanDefinition beanDefinition, BeanFactory owner) {
		// must generate CGLIB subclass
		return new CglibSubclassCreator(beanDefinition, owner).instantiate(null, null);
	}

	protected Object instantiateWithMethodInjection(RootBeanDefinition beanDefinition, BeanFactory owner,
													Constructor ctor, Object[] args) {
		return new CglibSubclassCreator(beanDefinition, owner).instantiate(ctor, args);
	}


	/**
	 * An inner class so we don't have a CGLIB dependency in core.
	 */
	private static class CglibSubclassCreator {

		private static final Log logger = LogFactory.getLog(CglibSubclassCreator.class);

		private RootBeanDefinition beanDefinition;

		private BeanFactory owner;

		public CglibSubclassCreator(RootBeanDefinition beanDefinition, BeanFactory owner) {
			this.beanDefinition = beanDefinition;
			this.owner = owner;
		}

		/**
		 * Create a new instance of a dynamically generated subclasses implementing the required
		 * lookups
		 * @param ctor constructor to use. If this is null, use the no-arg constructor (no
		 * paramterization, or Setter Injection)
		 * @param args arguments to use for the constructor. Ignored if the ctor parameter is
		 * null
		 * @return new instance of the dynamically generated class
		 */
		public Object instantiate(Constructor ctor, Object[] args) {
			Enhancer enhancer = new Enhancer();
			enhancer.setSuperclass(this.beanDefinition.getBeanClass());
			enhancer.setCallbackFilter(new CallbackFilterImpl());
			enhancer.setCallbacks(new Callback[] {
					NoOp.INSTANCE,
					new LookupOverrideMethodInterceptor(),
					new ReplaceOverrideMethodInterceptor()
			});

			return (ctor == null) ? 
					enhancer.create() : 
					enhancer.create(ctor.getParameterTypes(), args);
		}
		
		/**
		 * Class providing hashCode and equals methods required by CGLIB to
		 * ensure that CGLIB doesn't generate a distinct class per bean.
		 * Identity is based on class and bean definition. 
		 */
		private class CglibIdentitySupport {
			/**
			 * Exposed for equals method to allow access to enclosing class field
			 */
			protected RootBeanDefinition getBeanDefinition() {
				return beanDefinition;
			}
			
			public int hashCode() {
				return beanDefinition.hashCode();
			}
			
			public boolean equals(Object other) {
				return (other.getClass() == getClass()) &&
						((CglibIdentitySupport) other).getBeanDefinition() == beanDefinition;
			}
		}


		/**
		 * CGLIB MethodInterceptor to override methods, replacing them with an
		 * implementation that returns a bean looked up in the container.
		 */
		private class LookupOverrideMethodInterceptor extends CglibIdentitySupport implements MethodInterceptor {

			public Object intercept(Object o, Method m, Object[] args, MethodProxy mp) throws Throwable {
				// cast is safe as CallbackFilter filters are used selectively
				LookupOverride lo = (LookupOverride) beanDefinition.getMethodOverrides().getOverride(m);
				return owner.getBean(lo.getBeanName());
			}			
		}
		
		/**
		 * CGLIB MethodInterceptor to override methods, replacing them with a call
		 * to a generic MethodReplacer
		 */
		private class ReplaceOverrideMethodInterceptor extends CglibIdentitySupport implements MethodInterceptor {

			public Object intercept(Object o, Method m, Object[] args, MethodProxy mp) throws Throwable {
				ReplaceOverride lo = (ReplaceOverride) beanDefinition.getMethodOverrides().getOverride(m);
				// TODO could cache if a singleton for minor performance optimization
				MethodReplacer mr = (MethodReplacer) owner.getBean(lo.getMethodReplacerBeanName());
				return mr.reimplement(o, m, args);
			}
		}


		/**
		 * CGLIB object to filter method interception behavior.
		 */
		private class CallbackFilterImpl extends CglibIdentitySupport implements CallbackFilter {
			
			private Set methodNames = new HashSet();

			public int accept(Method method) {
				
				if (!methodNames.contains(method.getName())) {
					methodNames.add(method.getName());
				}
				else {
					beanDefinition.getMethodOverrides().addOverloadedMethodName(method.getName());
				}
				
				MethodOverride methodOverride = beanDefinition.getMethodOverrides().getOverride(method);
				if (logger.isInfoEnabled()) {
					logger.info("Override for '" + method.getName() + "' is [" + methodOverride + "]");
				}
				if (methodOverride == null) {
					return PASSTHROUGH;
				}
				else if (methodOverride instanceof LookupOverride) {
					return LOOKUP_OVERRIDE;
				}
				else if (methodOverride instanceof ReplaceOverride) {
					return METHOD_REPLACER;
				}
				throw new UnsupportedOperationException("Unexpected MethodOverride subclass: " +
							methodOverride.getClass().getName());
			}
		}
	}

}
