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
package org.springframework.web.flow.struts;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.springframework.util.Assert;
import org.springframework.validation.Errors;
import org.springframework.web.flow.FlowExecution;
import org.springframework.web.flow.action.AbstractAction;
import org.springframework.web.flow.config.BeanFactoryFlowServiceLocator;
import org.springframework.web.flow.config.FlowServiceLocator;
import org.springframework.web.flow.support.HttpFlowExecutionManager;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.struts.BindingActionForm;
import org.springframework.web.struts.TemplateAction;
import org.springframework.web.util.WebUtils;

/**
 * Struts Action that acts a front controller entry point into the web flow
 * system. Typically, a FlowAction exists per top-level (root) flow definition
 * in the application. Alternatively, a single FlowController may manage all
 * flow executions by parameterization with the appropriate <code>flowId</code>
 * in views that start new flow executions.
 * 
 * @author Keith Donald
 * @author Erwin Vervaet
 */
public class FlowAction extends TemplateAction {

	public static String ACTION_FORM_ATTRIBUTE = "_bindingActionForm";

	public static final String ACTION_PATH_ATTRIBUTE = "actionPath";

	protected String getActionPathAttributeName() {
		return ACTION_PATH_ATTRIBUTE;
	}

	protected String getActionFormAttributeName() {
		return ACTION_FORM_ATTRIBUTE;
	}

	private String getFlowId(ActionMapping mapping) {
		Assert.isInstanceOf(FlowActionMapping.class, mapping);
		return ((FlowActionMapping)mapping).getFlowId();
	}

	/**
	 * The main entry point for this action. Looks for a flow execution ID in
	 * the request. If none exists, it creates one. If one exists, it looks in
	 * the user's session to find the current FlowExecution. The request should
	 * also contain the current state ID and event ID. These String values can
	 * be passed to the FlowExecution to execute the action. Execution will
	 * typically result in a state transition.
	 * @see org.springframework.web.struts.TemplateAction#doExecuteAction(org.apache.struts.action.ActionMapping,
	 *      org.apache.struts.action.ActionForm,
	 *      javax.servlet.http.HttpServletRequest,
	 *      javax.servlet.http.HttpServletResponse)
	 */
	protected ActionForward doExecuteAction(ActionMapping mapping, ActionForm form, HttpServletRequest request,
			HttpServletResponse response) throws Exception {
		FlowServiceLocator locator = new BeanFactoryFlowServiceLocator(getWebApplicationContext());
		HttpFlowExecutionManager executionManager = new HttpFlowExecutionManager(getFlowId(mapping), locator);
		ModelAndView modelAndView = executionManager.handleRequest(request, response);
		// this is not very clean (pulling attribute from hard coded name)
		FlowExecution flowExecution = (FlowExecution)modelAndView.getModel().get(FlowExecution.ATTRIBUTE_NAME);
		if (flowExecution != null && flowExecution.isActive()) {
			if (form instanceof BindingActionForm) {
				BindingActionForm bindingForm = (BindingActionForm)form;
				bindingForm.setErrors((Errors)flowExecution.getAttribute(AbstractAction.LOCAL_FORM_OBJECT_ERRORS_NAME,
						Errors.class));
				bindingForm.setRequest(request);
			}
		}
		return createForwardFromModelAndView(modelAndView, mapping, request);
	}

	/**
	 * Return a Struts ActionForward given a ModelAndView. We need to add all
	 * attributes from the ModelAndView as request attributes.
	 */
	private ActionForward createForwardFromModelAndView(ModelAndView modelAndView, ActionMapping mapping,
			HttpServletRequest request) {
		if (modelAndView != null) {
			WebUtils.exposeRequestAttributes(request, modelAndView.getModel());
			ActionForward forward = mapping.findForward(modelAndView.getViewName());
			if (forward == null) {
				forward = new ActionForward(modelAndView.getViewName());
			}
			return forward;
		}
		else {
			if (logger.isInfoEnabled()) {
				logger.info("No model and view; returning a [null] forward");
			}
			return null;
		}
	}
}