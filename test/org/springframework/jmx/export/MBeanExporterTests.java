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

package org.springframework.jmx.export;

import java.util.HashMap;
import java.util.Map;

import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.modelmbean.ModelMBeanInfo;

import junit.framework.TestCase;

import org.springframework.beans.factory.xml.XmlBeanFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jmx.JmxTestBean;
import org.springframework.jmx.export.assembler.MBeanInfoAssembler;
import org.springframework.jmx.support.ObjectNameManager;

/**
 * @author Rob Harrop
 */
public class MBeanExporterTests extends TestCase {

	private static final String OBJECT_NAME = "spring:test=jmxMBeanAdaptor";

	public void testWithSuppliedMBeanServer() throws Exception {
		MBeanServer server = MBeanServerFactory.newMBeanServer();
		try {
			MBeanExporter adaptor = new MBeanExporter();
			adaptor.setBeans(getBeanMap());
			adaptor.setServer(server);
			adaptor.afterPropertiesSet();
			assertTrue("The bean was not registered with the MBeanServer",
					beanExists(server, ObjectNameManager.getInstance(OBJECT_NAME)));
		}
		finally {
			server.unregisterMBean(new ObjectName(OBJECT_NAME));
		}
	}

	public void testWithLocatedMBeanServer() throws Exception {
		MBeanServer server = MBeanServerFactory.createMBeanServer();
		try {
			MBeanExporter adaptor = new MBeanExporter();
			adaptor.setBeans(getBeanMap());
			adaptor.afterPropertiesSet();
			assertTrue("The bean was not registered with the MBeanServer",
					beanExists(server, ObjectNameManager.getInstance(OBJECT_NAME)));
			server.unregisterMBean(new ObjectName(OBJECT_NAME));
		}
		finally {
			MBeanServerFactory.releaseMBeanServer(server);
		}
	}

	public void testUserCreatedMBeanRegWithDynamicMBean() throws Exception {
		Map map = new HashMap();
		map.put("spring:name=dynBean", new TestDynamicMBean());

		MBeanServer server = MBeanServerFactory.createMBeanServer();
		try {
			InvokeDetectAssembler asm = new InvokeDetectAssembler();

			MBeanExporter adaptor = new MBeanExporter();
			adaptor.setServer(server);
			adaptor.setBeans(map);
			adaptor.setAssembler(asm);
			adaptor.afterPropertiesSet();

			Object name = server.getAttribute(ObjectNameManager.getInstance("spring:name=dynBean"), "name");
			assertEquals("The name attribute is incorrect", "Rob Harrop", name);
			assertFalse("Assembler should not have been invoked", asm.invoked);
		}
		finally {
			MBeanServerFactory.releaseMBeanServer(server);
		}
	}

	public void testAutodetectSkipsConnectorServerFactoryBean() throws Exception {
		XmlBeanFactory bf = new XmlBeanFactory(new ClassPathResource("autodetectMBeans.xml", getClass()));
		try {
			bf.getBean("exporter");
			assertTrue("ConnectorServerFactoryBean was skipped", true);
		}
		catch (Exception ex) {
			fail("ConnectorServerFactoryBean was not skipped: " + ex);
		}
		finally {
			bf.destroySingletons();
		}
	}

	private Map getBeanMap() {
		Map map = new HashMap();
		map.put(OBJECT_NAME, new JmxTestBean());
		return map;
	}

	private boolean beanExists(MBeanServer server, ObjectName objectName) throws Exception {
		ObjectInstance inst = server.getObjectInstance(objectName);
		return (inst != null);
	}


	private static class InvokeDetectAssembler implements MBeanInfoAssembler {

		private boolean invoked = false;

		public ModelMBeanInfo getMBeanInfo(String beanKey, Class beanClass) {
			invoked = true;
			return null;
		}
	}

}
