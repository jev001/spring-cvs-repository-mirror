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

package org.springframework.aop.support;

import org.aopalliance.aop.Advice;

import org.springframework.aop.Pointcut;

/**
 * Convenient class for regexp method pointcuts that hold an Advice,
 * making them an Advisor.
 *
 * <p>Configure this class using the "pattern" and "patterns"
 * pass-through properties. These are analogous to the pattern
 * and patterns properties of AbstractRegexpMethodPointcut.
 *
 * <p>Can delegate to any AbstractRegexpMethodPointcut subclass.
 * To choose a specific one, either override <code>createPointcut</code>
 * or set the "perl5" flag accordingly.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see #setPattern
 * @see #setPatterns
 * @see #setPerl5
 * @see #createPointcut
 * @see AbstractRegexpMethodPointcut
 */
public class RegexpMethodPointcutAdvisor extends AbstractPointcutAdvisor {

	private String[] patterns;

	private boolean perl5 = true;

	private AbstractRegexpMethodPointcut pointcut;

	/**
	 * Create an empty RegexpMethodPointcutAdvisor.
	 * @see #setPattern
	 * @see #setPatterns
	 * @see #setPerl5
	 * @see #setAdvice
	 */
	public RegexpMethodPointcutAdvisor() {
	}

	/**
	 * Create a RegexpMethodPointcutAdvisor for the given advice.
	 * The pattern still needs to be specified afterwards.
	 * @param advice the advice to use
	 * @see #setPattern
	 * @see #setPatterns
	 * @see #setPerl5
	 */
	public RegexpMethodPointcutAdvisor(Advice advice) {
		setAdvice(advice);
	}

	/**
	 * Create a RegexpMethodPointcutAdvisor for the given advice.
	 * @param pattern the pattern to use
	 * @param advice the advice to use
	 * @see #setPerl5
	 */
	public RegexpMethodPointcutAdvisor(String pattern, Advice advice) {
		setPattern(pattern);
		setAdvice(advice);
	}

	/**
	 * Create a RegexpMethodPointcutAdvisor for the given advice.
	 * @param patterns the patterns to use
	 * @param advice the advice to use
	 * @see #setPerl5
	 */
	public RegexpMethodPointcutAdvisor(String[] patterns, Advice advice) {
		setPatterns(patterns);
		setAdvice(advice);
	}

	/**
	 * Convenience method when we have only a single pattern.
	 * Use either this method or setPatterns, not both.
	 * <p>To be passed through to the pointcut implementation.
	 * @see #setPatterns
	 */
	public void setPattern(String pattern) {
		setPatterns(new String[] { pattern });
	}

	/**
	 * Set the regular expressions defining methods to match.
	 * Matching will be the union of all these; if any match,
	 * the pointcut matches.
	 * <p>To be passed through to the pointcut implementation.
	 * @param patterns regular expressions describing methods to match
	 */
	public void setPatterns(String[] patterns) {
		this.patterns = patterns;
	}

	/**
	 * Set whether to use Perl5 regexp syntax. If on, Perl5RegexpMethodPointcut
	 * will be used (delegating to Jakarta ORO); if off, the JdkRegexpMethodPointcut
	 * (delegating to the JDK 1.4 regex package).
	 * <p>Alternatively, override the <code>createPointcut</code> method.
	 * @see #createPointcut
	 * @see Perl5RegexpMethodPointcut
	 * @see JdkRegexpMethodPointcut
	 */
	public void setPerl5(boolean perl5) {
		this.perl5 = perl5;
	}

	/**
	 * Initialize the singleton pointcut held within this advisor.
	 */
	public synchronized Pointcut getPointcut() {
		if (this.pointcut == null) {
			this.pointcut = createPointcut();
			this.pointcut.setPatterns(this.patterns);
		}
		return pointcut;
	}

	/**
	 * Create the default pointcut: a Perl5RegexpMethodPointcut.
	 * <p>Alternatively, set the "perl5" flag.
	 * @see #setPerl5
	 * @see AbstractRegexpMethodPointcut
	 * @see Perl5RegexpMethodPointcut
	 * @see JdkRegexpMethodPointcut
	 */
	protected AbstractRegexpMethodPointcut createPointcut() {
		if (this.perl5) {
			// needs Jakarta ORO on the classpath
			return new Perl5RegexpMethodPointcut();
		}
		else {
			// needs to run on JDK >= 1.4
			return new JdkRegexpMethodPointcut();
		}
	}

}
