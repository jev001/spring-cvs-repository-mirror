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
package org.springframework.rules.reporting;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.MessageSource;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.rules.UnaryPredicate;
import org.springframework.rules.predicates.CompoundBeanPropertyExpression;
import org.springframework.rules.predicates.ParameterizedBinaryPredicate;
import org.springframework.rules.predicates.Range;
import org.springframework.rules.predicates.StringLengthConstraint;
import org.springframework.rules.predicates.UnaryAnd;
import org.springframework.rules.predicates.UnaryFunctionResultConstraint;
import org.springframework.rules.predicates.UnaryNot;
import org.springframework.rules.predicates.UnaryOr;
import org.springframework.rules.predicates.beans.BeanPropertiesExpression;
import org.springframework.rules.predicates.beans.BeanPropertyValueConstraint;
import org.springframework.rules.predicates.beans.ParameterizedBeanPropertyExpression;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.DefaultObjectStyler;
import org.springframework.util.visitor.ReflectiveVisitorSupport;
import org.springframework.util.visitor.Visitor;

/**
 * @author Keith Donald
 */
public class DefaultMessageTranslator implements Visitor {
    protected static final Log logger = LogFactory
            .getLog(DefaultMessageTranslator.class);

    private ReflectiveVisitorSupport visitorSupport = new ReflectiveVisitorSupport();

    private boolean appendValue = false;
    
    PropertyResults results;

    private List args = new ArrayList();

    private MessageSource messages;

    public DefaultMessageTranslator(MessageSource messages) {
        setMessageSource(messages);
    }

    public void setMessageSource(MessageSource messages) {
        Assert.notNull(messages);
        this.messages = messages;
    }

    public String getMessage(UnaryPredicate constraint) {
        return buildMessage(null, null, constraint, Locale.getDefault());
    }

    public String getMessage(String objectName, UnaryPredicate constraint) {
        return buildMessage(objectName, null, constraint, Locale.getDefault());
    }

    public String getMessage(String objectName, Object rejectedValue,
            UnaryPredicate constraint) {
        return buildMessage(objectName, rejectedValue, constraint, Locale
                .getDefault());
    }

    public String getMessage(String objectName, ValidationResults results) {
        return buildMessage(objectName, results.getRejectedValue(), results
                .getViolatedConstraint(), Locale.getDefault());
    }

    public String getMessage(PropertyResults results) {
        Assert.notNull(results);
        return buildMessage(results.getPropertyName(), results
                .getRejectedValue(), results.getViolatedConstraint(), Locale
                .getDefault());
    }

    private String buildMessage(String objectName, Object rejectedValue,
            UnaryPredicate constraint, Locale locale) {
        StringBuffer buf = new StringBuffer(255);
        MessageSourceResolvable[] args = resolveArguments(constraint);
        if (logger.isDebugEnabled()) {
            logger.debug(DefaultObjectStyler.evaluate(args));
        }
        if (objectName != null) {
            buf.append(messages.getMessage(resolvableObjectName(objectName),
                    locale));
            buf.append(' ');
            if (appendValue) {
                if (rejectedValue != null) {
                    buf.append("'" + rejectedValue + "'");
                    buf.append(' ');
                }
            }
        }
        for (int i = 0; i < args.length - 1; i++) {
            MessageSourceResolvable arg = args[i];
            buf.append(messages.getMessage(arg, locale));
            buf.append(' ');
        }
        buf.append(messages.getMessage(args[args.length - 1], locale));
        buf.append(".");
        return buf.toString();
    }

    private MessageSourceResolvable[] resolveArguments(UnaryPredicate constraint) {
        visitorSupport.invokeVisit(this, constraint);
        return (MessageSourceResolvable[])args
                .toArray(new MessageSourceResolvable[0]);
    }

    void visit(CompoundBeanPropertyExpression rule) {
        visitorSupport.invokeVisit(this, rule.getPredicate());
    }

    void visit(BeanPropertiesExpression e) {
        add(
                getMessageCode(e.getPredicate()),
                new Object[] { resolvableObjectName(e.getOtherPropertyName()) },
                e.toString());
    }

    void visit(ParameterizedBeanPropertyExpression e) {
        add(getMessageCode(e.getPredicate()),
                new Object[] { e.getParameter() }, e.toString());
    }

    private void add(String code, Object[] args, String defaultMessage) {
        MessageSourceResolvable resolvable = new DefaultMessageSourceResolvable(
                new String[] { code }, args, defaultMessage);
        if (logger.isDebugEnabled()) {
            logger.debug("Adding resolvable: " + resolvable);
        }
        this.args.add(resolvable);
    }

    private MessageSourceResolvable resolvableObjectName(String objectName) {
        return new DefaultMessageSourceResolvable(new String[] { objectName },
                null, objectName);
    }

    void visit(BeanPropertyValueConstraint valueConstraint) {
        visitorSupport.invokeVisit(this, valueConstraint.getPredicate());
    }

    void visit(UnaryAnd and) {
        Iterator it = and.iterator();
        while (it.hasNext()) {
            UnaryPredicate p = (UnaryPredicate)it.next();
            visitorSupport.invokeVisit(this, p);
            if (it.hasNext()) {
                add("and", null, "add");
            }
        }
    }

    void visit(UnaryOr or) {
        Iterator it = or.iterator();
        while (it.hasNext()) {
            UnaryPredicate p = (UnaryPredicate)it.next();
            visitorSupport.invokeVisit(this, p);
            if (it.hasNext()) {
                add("or", null, "or");
            }
        }
    }

    void visit(UnaryNot not) {
        add("not", null, "not");
        visitorSupport.invokeVisit(this, not.getPredicate());
    }

    //@TODO - consider standard visitor here...
    void visit(StringLengthConstraint constraint) {
        UnaryFunctionResultConstraint c = (UnaryFunctionResultConstraint)constraint
                .getPredicate();
        Object p = c.getPredicate();
        MessageSourceResolvable resolvable;
        if (p instanceof ParameterizedBinaryPredicate) {
            resolvable = handleParameterizedBinaryPredicate((ParameterizedBinaryPredicate)p);
        }
        else {
            resolvable = handleRange((Range)p);
        }
        Object[] args = new Object[] { resolvable };
        add(getMessageCode(constraint), args, constraint.toString());
    }

    void visit(UnaryFunctionResultConstraint c) {
        visitorSupport.invokeVisit(this, c.getPredicate());
    }

    private MessageSourceResolvable handleParameterizedBinaryPredicate(
            ParameterizedBinaryPredicate p) {
        MessageSourceResolvable resolvable = new DefaultMessageSourceResolvable(
                new String[] { getMessageCode(p.getPredicate()) },
                new Object[] { p.getParameter() }, p.toString());
        return resolvable;
    }

    private MessageSourceResolvable handleRange(Range r) {
        MessageSourceResolvable resolvable = new DefaultMessageSourceResolvable(
                new String[] { getMessageCode(r) }, new Object[] { r.getMin(),
                        r.getMax() }, r.toString());
        return resolvable;
    }

    void visit(UnaryPredicate constraint) {
        add(getMessageCode(constraint), null, constraint.toString());
    }

    private String getMessageCode(Object o) {
        if (o instanceof TypeResolvable) {
            String type = ((TypeResolvable)o).getType();
            if (type != null) { return type; }
        }
        return ClassUtils.getShortNameAsProperty(o.getClass());
    }

}