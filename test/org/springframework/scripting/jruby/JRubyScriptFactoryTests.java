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

package org.springframework.scripting.jruby;

import junit.framework.TestCase;

import org.springframework.aop.support.AopUtils;
import org.springframework.aop.target.dynamic.Refreshable;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.JdkVersion;
import org.springframework.scripting.Calculator;
import org.springframework.scripting.Messenger;
import org.springframework.scripting.ScriptCompilationException;

/**
 * @author Rob Harrop
 * @author Rick Evans
 * @author Juergen Hoeller
 */
public class JRubyScriptFactoryTests extends TestCase {

	private static final String RUBY_SCRIPT_SOURCE_LOCATOR =
			"inline:require 'java'\n" +
					"class RubyBar\n" +
					"end\n" +
					"RubyBar.new";


	public void testStaticScript() throws Exception {
		if (JdkVersion.getMajorJavaVersion() < JdkVersion.JAVA_14) {
			return;
		}

		ApplicationContext ctx =
				new ClassPathXmlApplicationContext("org/springframework/scripting/jruby/jrubyContext.xml");
		Calculator calc = (Calculator) ctx.getBean("calculator");
		Messenger messenger = (Messenger) ctx.getBean("messenger");

		assertFalse("Scripted object should not be instance of Refreshable", calc instanceof Refreshable);
		assertFalse("Scripted object should not be instance of Refreshable", messenger instanceof Refreshable);

		assertEquals(calc, calc);
		assertEquals(messenger, messenger);
		assertTrue(!messenger.equals(calc));
		assertTrue(messenger.hashCode() != calc.hashCode());
		assertTrue(!messenger.toString().equals(calc.toString()));

		String desiredMessage = "Hello World!";
		assertEquals("Message is incorrect", desiredMessage, messenger.getMessage());
	}

	public void testNonStaticScript() throws Exception {
		if (JdkVersion.getMajorJavaVersion() < JdkVersion.JAVA_14) {
			return;
		}

		ApplicationContext ctx =
				new ClassPathXmlApplicationContext("org/springframework/scripting/jruby/jrubyRefreshableContext.xml");
		Messenger messenger = (Messenger) ctx.getBean("messenger");

		assertTrue("Should be a proxy for refreshable scripts", AopUtils.isAopProxy(messenger));
		assertTrue("Should be an instance of Refreshable", messenger instanceof Refreshable);

		String desiredMessage = "Hello World!";
		assertEquals("Message is incorrect.", desiredMessage, messenger.getMessage());

		Refreshable refreshable = (Refreshable) messenger;
		refreshable.refresh();

		assertEquals("Message is incorrect after refresh.", desiredMessage, messenger.getMessage());
		assertEquals("Incorrect refresh count", 2, refreshable.getRefreshCount());
	}

	public void testScriptCompilationException() throws Exception {
		if (JdkVersion.getMajorJavaVersion() < JdkVersion.JAVA_14) {
			return;
		}

		try {
			new ClassPathXmlApplicationContext("org/springframework/scripting/jruby/jrubyBrokenContext.xml");
			fail("Should throw exception for broken script file");
		}
		catch (BeanCreationException ex) {
			assertTrue(ex.contains(ScriptCompilationException.class));
		}
	}

	public void testCtorWithNullScriptSourceLocator() throws Exception {
		if (JdkVersion.getMajorJavaVersion() < JdkVersion.JAVA_14) {
			return;
		}

		try {
			new JRubyScriptFactory(null, new Class[]{Messenger.class});
			fail("Must have thrown exception by this point.");
		}
		catch (IllegalArgumentException expected) {
		}
	}

	public void testCtorWithEmptyScriptSourceLocator() throws Exception {
		if (JdkVersion.getMajorJavaVersion() < JdkVersion.JAVA_14) {
			return;
		}

		try {
			new JRubyScriptFactory("", new Class[]{Messenger.class});
			fail("Must have thrown exception by this point.");
		}
		catch (IllegalArgumentException expected) {
		}
	}

	public void testCtorWithWhitespacedScriptSourceLocator() throws Exception {
		if (JdkVersion.getMajorJavaVersion() < JdkVersion.JAVA_14) {
			return;
		}

		try {
			new JRubyScriptFactory("\n   ", new Class[]{Messenger.class});
			fail("Must have thrown exception by this point.");
		}
		catch (IllegalArgumentException expected) {
		}
	}

	public void testCtorWithNullScriptInterfacesArray() throws Exception {
		if (JdkVersion.getMajorJavaVersion() < JdkVersion.JAVA_14) {
			return;
		}

		try {
			new JRubyScriptFactory(RUBY_SCRIPT_SOURCE_LOCATOR, null);
			fail("Must have thrown exception by this point.");
		}
		catch (IllegalArgumentException expected) {
		}
	}

	public void testCtorWithEmptyScriptInterfacesArray() throws Exception {
		if (JdkVersion.getMajorJavaVersion() < JdkVersion.JAVA_14) {
			return;
		}

		try {
			new JRubyScriptFactory(RUBY_SCRIPT_SOURCE_LOCATOR, new Class[]{});
			fail("Must have thrown exception by this point.");
		}
		catch (IllegalArgumentException expected) {
		}
	}

	public void testResourceScriptFromTag() throws Exception {
		if (JdkVersion.getMajorJavaVersion() < JdkVersion.JAVA_14) {
			return;
		}

		ApplicationContext ctx = new ClassPathXmlApplicationContext("jruby-with-xsd.xml", getClass());
		Messenger messenger = (Messenger) ctx.getBean("messenger");
		assertEquals("Hello World!", messenger.getMessage());
		assertFalse(messenger instanceof Refreshable);
	}

	public void testInlineScriptFromTag() throws Exception {
		if (JdkVersion.getMajorJavaVersion() < JdkVersion.JAVA_14) {
			return;
		}

		ApplicationContext ctx = new ClassPathXmlApplicationContext("jruby-with-xsd.xml", getClass());
		Calculator calculator = (Calculator) ctx.getBean("calculator");
		assertNotNull(calculator);
		assertFalse(calculator instanceof Refreshable);
	}

	public void testRefreshableFromTag() throws Exception {
		if (JdkVersion.getMajorJavaVersion() < JdkVersion.JAVA_14) {
			return;
		}

		ApplicationContext ctx = new ClassPathXmlApplicationContext("jruby-with-xsd.xml", getClass());
		Messenger messenger = (Messenger) ctx.getBean("refreshableMessenger");
		assertEquals("Hello World!", messenger.getMessage());
		assertTrue("Messenger should be Refreshable", messenger instanceof Refreshable);
	}

	public void testWithComplexArg() throws Exception {
		if (JdkVersion.getMajorJavaVersion() < JdkVersion.JAVA_14) {
			return;
		}

		ApplicationContext ctx = new ClassPathXmlApplicationContext("jrubyContext.xml", getClass());
		Printer printer = (Printer) ctx.getBean("printer");
		CountingPrintable printable = new CountingPrintable();
		printer.print(printable);
		assertEquals(1, printable.count);
	}

	public void testWithPrimitiveArgsInReturnTypeAndParameters() throws Exception {
		if (JdkVersion.getMajorJavaVersion() < JdkVersion.JAVA_14) {
			return;
		}

		ApplicationContext ctx = new ClassPathXmlApplicationContext("jrubyContextForPrimitives.xml", getClass());
		PrimitiveAdder adder = (PrimitiveAdder) ctx.getBean("adder");
		assertEquals(2, adder.addInts(1, 1));
		assertEquals(4, adder.addShorts((short) 1, (short) 3));
		assertEquals(5, adder.addLongs(2L, 3L));
		assertEquals(5, new Float(adder.addFloats(2.0F, 3.1F)).intValue());
		assertEquals(5, new Double(adder.addDoubles(2.0, 3.1)).intValue());
		assertFalse(adder.resultIsPositive(-200, 1));
		assertEquals("ri", adder.concatenate('r', 'i'));
		assertEquals('c', adder.echo('c'));
	}

	public void testWithWrapperArgsInReturnTypeAndParameters() throws Exception {
		if (JdkVersion.getMajorJavaVersion() < JdkVersion.JAVA_14) {
			return;
		}

		ApplicationContext ctx = new ClassPathXmlApplicationContext("jrubyContextForWrappers.xml", getClass());
		WrapperAdder adder = (WrapperAdder) ctx.getBean("adder");
		assertEquals(new Integer(2), adder.addInts(new Integer(1), new Integer(1)));
		assertEquals(Integer.class, adder.addInts(new Integer(1), new Integer(1)).getClass());
		assertEquals(new Short((short) 4), adder.addShorts(new Short((short) 1), new Short((short) 3)));
		assertEquals(Short.class, adder.addShorts(new Short((short) 1), new Short((short) 3)).getClass());
		assertEquals(new Long(5L), adder.addLongs(new Long(2L), new Long(3L)));
		assertEquals(Long.class, adder.addLongs(new Long(2L), new Long(3L)).getClass());
		assertEquals(5, adder.addFloats(new Float(2.0F), new Float(3.1F)).intValue());
		assertEquals(Float.class, adder.addFloats(new Float(2.0F), new Float(3.1F)).getClass());
		assertEquals(5, new Double(adder.addDoubles(new Double(2.0), new Double(3.1)).intValue()).intValue());
		assertEquals(Double.class, adder.addDoubles(new Double(2.0), new Double(3.1)).getClass());
		assertFalse(adder.resultIsPositive(new Integer(-200), new Integer(1)).booleanValue());
		assertEquals(Boolean.class, adder.resultIsPositive(new Integer(-200), new Integer(1)).getClass());
		assertEquals("ri", adder.concatenate(new Character('r'), new Character('i')));
		assertEquals(String.class, adder.concatenate(new Character('r'), new Character('i')).getClass());
		assertEquals(new Character('c'), adder.echo(new Character('c')));
		assertEquals(Character.class, adder.echo(new Character('c')).getClass());
		Integer[] numbers = new Integer[]{new Integer(1), new Integer(2), new Integer(3), new Integer(4), new Integer(5)};
		assertEquals("12345", adder.concatArrayOfIntegerWrappers(numbers));
		assertEquals(String.class, adder.concatArrayOfIntegerWrappers(numbers).getClass());

		Short[] shorts = adder.populate(new Short((short) 1), new Short((short) 2));
		assertEquals(2, shorts.length);
		assertNotNull(shorts[0]);
		assertEquals(new Short((short) 1), shorts[0]);
		assertNotNull(shorts[1]);
		assertEquals(new Short((short) 2), shorts[1]);
	}


	private static final class CountingPrintable implements Printable {

		public int count;

		public String getContent() {
			this.count++;
			return "Hello World!";
		}
	}

}
