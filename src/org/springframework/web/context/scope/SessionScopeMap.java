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

package org.springframework.web.context.scope;

import javax.servlet.http.HttpSession;

import org.springframework.aop.target.scope.ScopeMap;

/**
 * HttpSession-backed ScopeMap implementation. Relies
 * on a thread-bound request.
 *
 * @author Rod Johnson
 * @since 2.0
 * @see RequestContextHolder#currentSession()
 */
public class SessionScopeMap implements ScopeMap {

	private boolean synchronizeOnSession = false;


	/**
	 * Set if return should be synchronized on the session, to serialize
	 * parallel invocations from the same client.
	 */
	public final void setSynchronizeOnSession(boolean synchronizeOnSession) {
		this.synchronizeOnSession = synchronizeOnSession;
	}


	public boolean isPersistent() {
		return false;
	}

	public Object get(String name) {
		HttpSession session = RequestContextHolder.currentSession();
		if (this.synchronizeOnSession) {
			synchronized (session) {
				return session.getAttribute(name);
			}
		}
		return session.getAttribute(name);
	}

	public void put(String name, Object value) {
		HttpSession session = RequestContextHolder.currentSession();
		if (this.synchronizeOnSession) {
			synchronized (session) {
				session.setAttribute(name, value);
			}
		}
		else {
			session.setAttribute(name, value);
		}
	}

	public void remove(String name) {
		HttpSession session = RequestContextHolder.currentSession();
		session.removeAttribute(name);
	}

}
