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
package org.springframework.test.junit;

import junit.framework.JUnit4TestAdapter;

import org.junit.runner.RunWith;
import org.springframework.test.annotation.ContextConfiguration;

/**
 * Extension of {@link SpringJUnit4ClassRunnerAppCtxTests}, which
 * verifies that we can specify an explicit, <em>relative path</em> location
 * for our application context.
 *
 * @see SpringJUnit4ClassRunnerAppCtxTests
 * @see AbsolutePathSpringJUnit4ClassRunnerAppCtxTests
 * @author Sam Brannen
 * @version $Revision$
 * @since 2.2
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "SpringJUnit4ClassRunnerAppCtxTests-context.xml" })
public class RelativePathSpringJUnit4ClassRunnerAppCtxTests extends
		SpringJUnit4ClassRunnerAppCtxTests {

	/* all tests are in the parent class. */

	// XXX Remove suite() once we've migrated to Ant 1.7 with JUnit 4 support.
	public static junit.framework.Test suite() {

		return new JUnit4TestAdapter(RelativePathSpringJUnit4ClassRunnerAppCtxTests.class);
	}

}
