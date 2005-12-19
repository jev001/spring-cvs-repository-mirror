/*
 * Copyright 2002-2005 the original author or authors.
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

package org.springframework.aop.aspectj.annotation;

import junit.framework.TestCase;
import org.aspectj.lang.reflect.PerClauseKind;

import org.springframework.aop.Pointcut;
import org.springframework.aop.aspectj.annotation.AbstractAspectJAdvisorFactoryTests.ExceptionAspect;
import org.springframework.aop.aspectj.annotation.AbstractAspectJAdvisorFactoryTests.PerTargetAspect;
import org.springframework.aop.aspectj.annotation.AbstractAspectJAdvisorFactoryTests.PerThisAspect;

/**
 * 
 * @since 2.0
 * @author Rod Johnson
 *
 */
public class AspectMetadataTests extends TestCase {

	public void testNotAnAspect() {
		try {
			new AspectMetadata(String.class);
			fail();
		}
		catch (IllegalArgumentException ex) {
			// Ok
		}
	}
	
	public void testSingletonAspect() {
		AspectMetadata am = new AspectMetadata(ExceptionAspect.class);
		assertFalse(am.isPerThisOrPerTarget());
		assertSame(Pointcut.TRUE, am.getPerClausePointcut());
		assertEquals(PerClauseKind.SINGLETON, am.getAjType().getPerClause().getKind());
	}
	
	public void testPerTargetAspect() {
		AspectMetadata am = new AspectMetadata(PerTargetAspect.class);
		assertTrue(am.isPerThisOrPerTarget());
		assertNotSame(Pointcut.TRUE, am.getPerClausePointcut());
		assertEquals(PerClauseKind.PERTARGET, am.getAjType().getPerClause().getKind());
	}
	
	public void testPerThisAspect() {
		AspectMetadata am = new AspectMetadata(PerThisAspect.class);
		assertTrue(am.isPerThisOrPerTarget());
		assertNotSame(Pointcut.TRUE, am.getPerClausePointcut());
		assertEquals(PerClauseKind.PERTHIS, am.getAjType().getPerClause().getKind());
	}
}
