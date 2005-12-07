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

package org.springframework.web.filter;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.web.context.scope.RequestContextHolder;

/**
 * Servlet filter that exposes the request to the current thread,
 * through both LocaleContextHolder and RequestContextHolder.
 *
 * <p>Useful for application objects that need access to the current request.
 * Does not require any configuration parameters.
 *
 * <p>Alternatively, Spring's RequestContextListener and Spring's DispatcherServlet
 * also expose the same request context to the current thread.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @since 2.0
 * @see org.springframework.context.i18n.LocaleContextHolder
 * @see org.springframework.web.context.scope.RequestContextHolder
 * @see org.springframework.web.context.scope.RequestContextListener
 * @see org.springframework.web.servlet.DispatcherServlet
 */
public class RequestContextFilter extends OncePerRequestFilter {

	protected void doFilterInternal(
			HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {

		try {
			LocaleContextHolder.setLocale(request.getLocale());
			RequestContextHolder.setRequest(request);
			if (logger.isDebugEnabled()) {
				logger.debug("Bound request to thread: " + request);
			}
			filterChain.doFilter(request, response);
		}
		finally {
			RequestContextHolder.setRequest(null);
			LocaleContextHolder.setLocale(null);
			if (logger.isDebugEnabled()) {
				logger.debug("Cleared thread-bound request: " + request);
			}
		}
	}

}
