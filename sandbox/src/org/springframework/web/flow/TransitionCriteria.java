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
package org.springframework.web.flow;

/**
 * Interface for strategy objects encapsulating criteria that determine
 * whether or not a transition should execute given a flow execution
 * request context.
 * 
 * @see org.springframework.web.flow.Transition
 * @see org.springframework.web.flow.RequestContext
 * 
 * @author Keith Donald
 * @author Erwin Vervaet
 */
public interface TransitionCriteria {
	
	/**
	 * Check if the transition should execute based on the given flow
	 * execution request context.
	 * @param context the flow execution request context
	 * @return true if the transition should fire, false otherwise
	 */
	public boolean test(RequestContext context);
	
}
