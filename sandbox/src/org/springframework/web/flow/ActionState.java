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

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.util.Assert;
import org.springframework.util.Styler;

/**
 * A transitionable state that executes one or more actions when entered. If
 * more than one action is specified, they are executed in an ordered chain
 * until one returns a result event that matches a valid state transition for
 * this state. This is a form of the Chain of Responsibility (CoR) pattern.
 * <p>
 * Each action executed by this action state can optionally be qualified with a
 * <i>name</i> attribute. This name is used as a qualifier in determing what
 * transition should be executed for a given action result event. For example,
 * if an action named "myAction" returns a "success" result, a transition for
 * Event "myAction.success" will be searched, and if found, executed. If the
 * action is not named, a transition for the base "success" event will be
 * searched, and if found, executed.
 * 
 * @author Keith Donald
 * @author Erwin Vervaet
 */
public class ActionState extends TransitionableState {

	/**
	 * The set of actions to be executed when this action state is entered.
	 */
	private Set actionExecutors = new LinkedHashSet(1);

	/**
	 * Create a new action state.
	 * @param flow the owning flow
	 * @param id the state identifier (must be unique to the flow)
	 * @param targetAction the unnamed action to execute in this state
	 * @param transition the sole transition (path) out of this state
	 * @throws IllegalArgumentException when this state cannot be added to given
	 *         flow
	 */
	public ActionState(Flow flow, String id, Action targetAction, Transition transition)
			throws IllegalArgumentException {
		super(flow, id, transition);
		addAction(targetAction);
	}

	/**
	 * Create a new action state.
	 * @param flow the owning flow
	 * @param action The action and any configuration properties for use within
	 *        this state
	 * @param action the named action to execute in this state
	 * @param transition the sole transition (path) out of this state
	 * @throws IllegalArgumentException when this state cannot be added to given
	 *         flow
	 */
	public ActionState(Flow flow, String id, ActionStateAction action, Transition transition)
			throws IllegalArgumentException {
		super(flow, id, transition);
		addAction(action);
	}

	/**
	 * Create a new action state.
	 * @param flow the owning flow
	 * @param id the state identifier (must be unique to the flow)
	 * @param targetAction the unnamed action to execute in this state
	 * @param transitions the transitions out of this state
	 * @throws IllegalArgumentException when this state cannot be added to given
	 *         flow
	 */
	public ActionState(Flow flow, String id, Action targetAction, Transition[] transitions)
			throws IllegalArgumentException {
		super(flow, id, transitions);
		addAction(targetAction);
	}

	/**
	 * Create a new action state.
	 * @param flow the owning flow
	 * @param id the state identifier (must be unique to the flow)
	 * @param actionName the name of the named action
	 * @param action the named action to execute in this state
	 * @param transitions the transitions out of this state
	 * @throws IllegalArgumentException when this state cannot be added to given
	 *         flow
	 */
	public ActionState(Flow flow, String id, ActionStateAction action, Transition[] transitions)
			throws IllegalArgumentException {
		super(flow, id, transitions);
		addAction(action);
	}

	/**
	 * Create a new action state.
	 * @param flow the owning flow
	 * @param id the state identifier (must be unique to the flow)
	 * @param targetActions the unnamed actions to execute in this state
	 * @param transition the sole transition (path) out of this state
	 * @throws IllegalArgumentException when this state cannot be added to given
	 *         flow
	 */
	public ActionState(Flow flow, String id, Action[] targetActions, Transition transition)
			throws IllegalArgumentException {
		super(flow, id, transition);
		addActions(targetActions);
	}

	/**
	 * Create a new action state.
	 * @param flow the owning flow
	 * @param id the state identifier (must be unique to the flow)
	 * @param targetActions the unnamed actions to execute in this state
	 * @param transitions the transitions (paths) out of this state
	 * @throws IllegalArgumentException when this state cannot be added to given
	 *         flow
	 */
	public ActionState(Flow flow, String id, Action[] targetActions, Transition[] transitions)
			throws IllegalArgumentException {
		super(flow, id, transitions);
		addActions(targetActions);
	}

	/**
	 * Create a new action state.
	 * @param flow the owning flow
	 * @param id the state identifier (must be unique to the flow)
	 * @param actionNames the names of the named actions
	 * @param actions the named actions to execute in this state
	 * @param transition the transitions (paths) out of this state
	 * @throws IllegalArgumentException when this state cannot be added to given
	 *         flow
	 */
	public ActionState(Flow flow, String id, ActionStateAction[] actions, Transition transition)
			throws IllegalArgumentException {
		super(flow, id, transition);
		addActions(actions);
	}

	/**
	 * Create a new action state.
	 * @param flow the owning flow
	 * @param id the state identifier (must be unique to the flow)
	 * @param actionNames the names of the named actions
	 * @param actions the named actions to execute in this state
	 * @param transitions the transitions (paths) out of this state
	 * @throws IllegalArgumentException when this state cannot be added to given
	 *         flow
	 */
	public ActionState(Flow flow, String id, ActionStateAction[] actions, Transition[] transitions)
			throws IllegalArgumentException {
		super(flow, id, transitions);
		addActions(actions);
	}

	/**
	 * Add an unnamed action to the state.
	 * @param action the action to add
	 */
	protected void addAction(Action action) {
		this.actionExecutors.add(createActionExecutor(new ActionStateAction(this, action)));
	}

	/**
	 * Add a named action to the state.
	 * @param actionName the name of the action
	 * @param action the action to add
	 */
	protected void addAction(ActionStateAction action) {
		action.setState(this);
		this.actionExecutors.add(createActionExecutor(action));
	}

	/**
	 * Add a collection of unnamed actions to this state.
	 * @param actions the actions to add
	 */
	protected void addActions(Action[] actions) {
		Assert.notEmpty(actions, "You must add at least one action");
		for (int i = 0; i < actions.length; i++) {
			addAction(actions[i]);
		}
	}

	/**
	 * Add a collection of named actions to this state.
	 * @param actionNames the names of the actions
	 * @param actions the actions to add
	 */
	protected void addActions(ActionStateAction[] actions) {
		Assert.notEmpty(actions, "You must add at least one action");
		for (int i = 0; i < actions.length; i++) {
			addAction(actions[i]);
		}
	}

	/**
	 * Create a wrapper object for a named action.
	 * @param actionName the name of the action to wrap
	 * @param action the action to wrap
	 * @return the wrapped named action
	 */
	protected ActionExecutor createActionExecutor(ActionStateAction action) {
		return new ActionExecutor(action);
	}

	/**
	 * Returns an iterator that lists the set of actions to execute for this
	 * state. Both named and unnamed actions will be returned, but all are
	 * wrapped as {@link ActionState.ActionExecutor} objects.
	 * @return the NamedAction iterator
	 */
	protected Iterator actionExecutors() {
		return this.actionExecutors.iterator();
	}

	/**
	 * Returns the number of actions executed by this action state when it is
	 * entered.
	 * @return the action count
	 */
	public int getActionCount() {
		return actionExecutors.size();
	}

	/**
	 * Returns the first action executed by this action state.
	 * @return the first action
	 */
	public ActionStateAction getAction() {
		return getActions()[0];
	}

	/**
	 * Returns the list of actions executed by this action state.
	 * @return the action list, as a typed array
	 */
	public ActionStateAction[] getActions() {
		ActionStateAction[] actions = new ActionStateAction[actionExecutors.size()];
		int i = 0;
		for (Iterator it = actionExecutors(); it.hasNext();) {
			actions[i++] = ((ActionExecutor)it.next()).getAction();
		}
		return actions;
	}

	/**
	 * Returns the name associated with an action instance executed by this
	 * action state.
	 * @param targetAction the action for which the name should be looked up
	 * @return the name of given action or <code>null</code> if the action
	 *         does not have a name
	 * @throws NoSuchElementException when given action is not an action
	 *         executed by this state
	 */
	public ActionStateAction getAction(Action targetAction) throws NoSuchElementException {
		Assert.notNull(targetAction, "The action should not be [null]");
		for (Iterator it = actionExecutors(); it.hasNext();) {
			ActionStateAction actionInfo = ((ActionExecutor)it.next()).getAction();
			if (actionInfo.getTargetAction() == targetAction) {
				return actionInfo;
			}
		}
		throw new NoSuchElementException("Target action '" + targetAction + "' is not an action executed by state '"
				+ this + "'");
	}

	/**
	 * Specialization of State's <code>doEnterState</code> template method
	 * that executes behaviour specific to this state type in polymorphic
	 * fashion.
	 * <p>
	 * This implementation iterates over each configured <code>Action</code>
	 * instance and executes it. Execution continues until a <code>Action</code>
	 * returns a result event that matches a state transition, or the list of
	 * all actions is exhausted.
	 * @param context the state execution context
	 * @return ViewDescriptor a view descriptor signaling that control should be
	 *         returned to the client and a view rendered
	 * @throws CannotExecuteStateTransitionException when no action execution
	 *         resulted in a outcome event that could be mapped to a valid state
	 *         transition
	 */
	protected ViewDescriptor doEnterState(StateContext context) {
		Iterator it = actionExecutors();
		int executionCount = 0;
		String[] eventIds = new String[actionExecutors.size()];
		while (it.hasNext()) {
			ActionExecutor namedAction = (ActionExecutor)it.next();
			Event event = namedAction.execute(context);
			if (event != null) {
				eventIds[executionCount] = event.getId();
				context.setLastEvent(event);
				if (hasTransitionFor(context)) {
					return executeTransition(context);
				}
				else {
					if (logger.isDebugEnabled()) {
						logger
								.debug("Action execution #" + executionCount + " resulted in no transition on event '"
										+ eventIds[executionCount] + "' -- "
										+ "I will proceed to the next action in the chain");
					}
				}
			}
			else {
				eventIds[executionCount] = null;
			}
			executionCount++;
		}
		if (executionCount > 0) {
			throw new CannotExecuteStateTransitionException(this, "No transition was matched to the event(s) "
					+ "signaled by the " + executionCount + " action(s) that executed in this action state '" + getId()
					+ "' of flow '" + getFlow().getId()
					+ "'; transitions must be defined to handle action result outcomes -- "
					+ "possible flow configuration error? Note: the eventIds signaled were: '" + Styler.call(eventIds)
					+ "', while the supported set of transitional criteria for this action state is '"
					+ Styler.call(getTransitionalCriteria()) + "'");
		}
		else {
			throw new CannotExecuteStateTransitionException(this, new IllegalStateException(
					"No actions were executed, thus I cannot execute any state transition "
							+ "-- programmer configuration error; "
							+ "make sure you add at least one action to this state"));
		}
	}

	/**
	 * Wrapper class for actions that associates an action with its name (or
	 * <code>null</code> if its an unnamed action).
	 * <p>
	 * For internal use by the ActionState.
	 * 
	 * @author Keith Donald
	 * @author Erwin Vervaet
	 */
	protected static class ActionExecutor {

		protected final Log logger = LogFactory.getLog(ActionExecutor.class);

		private ActionStateAction action;

		/**
		 * Create a new action wrapper.
		 * @param state the state containing the action
		 * @param name the name of the action, or null if it's unnamed
		 * @param action the action to wrap
		 */
		public ActionExecutor(ActionStateAction action) {
			Assert.notNull(action, "The action state's action is required");
			this.action = action;
		}

		public ActionStateAction getAction() {
			return action;
		}

		/**
		 * Execute the wrapped action.
		 * @param context the flow execution context
		 * @return result of execution
		 */
		protected Event execute(RequestContext context) {
			try {
				if (logger.isDebugEnabled()) {
					logger.debug("Executing action '" + this + "'");
				}
				return getEvent(action.getTargetAction().execute(context));
			}
			catch (Exception e) {
				throw new ActionExecutionException(this, e);
			}
		}

		/**
		 * Get the event id to be used as grounds for a transition in the
		 * containing state, based on given result returned from action
		 * execution.
		 * <p>
		 * If the wrapped action is named, the name will be used as a qualifier
		 * for the event (e.g. "myAction.success").
		 */
		protected Event getEvent(Event resultEvent) {
			if (resultEvent == null) {
				return null;
			}
			if (action.isNamed()) {
				return new ActionNameQualifiedEvent(action.getName(), resultEvent);
			}
			else {
				return resultEvent;
			}
		}

		/**
		 * Wrapper to wrap an event as an event signaled by a named action.
		 */
		private static class ActionNameQualifiedEvent extends Event {

			private String actionName;

			private Event resultEvent;

			/**
			 * Create a new action name qualified event.
			 * @param actionName the name of the named action
			 * @param resultEvent the event to qualify
			 */
			public ActionNameQualifiedEvent(String actionName, Event resultEvent) {
				super(resultEvent.getSource());
				this.actionName = actionName;
				this.resultEvent = resultEvent;
			}

			public String getId() {
				return actionName + FlowConstants.SEPARATOR + resultEvent.getId();
			}

			public long getTimestamp() {
				return resultEvent.getTimestamp();
			}

			public String getStateId() {
				return resultEvent.getStateId();
			}

			public Object getParameter(String parameterName) {
				return resultEvent.getParameter(parameterName);
			}

			public Map getParameters() {
				return resultEvent.getParameters();
			}
		}

		public String toString() {
			return action.toString();
		}
	}
}