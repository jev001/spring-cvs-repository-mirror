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
package org.springframework.web.flow;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OptionalDataException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.util.ToStringCreator;
import org.springframework.web.flow.support.RandomGuid;

/**
 * Default implementation of FlowExecution that uses a stack-based data
 * structure to manage flow sessions.
 * <p>
 * This implementation of FlowExecution is Serializable so it can be safely
 * stored in an HTTP session.
 * @author Keith Donald
 * @author Erwin Vervaet
 * @see org.springframework.web.flow.FlowSession
 */
public class FlowExecutionStack implements FlowExecutionMBean, FlowExecution, Serializable {

	private static final long serialVersionUID = 3258688806151469104L;

	protected static final Log logger = LogFactory.getLog(FlowExecutionStack.class);

	/**
	 * The unique, random machine-generated flow execution identifier.
	 */
	private String id;

	private long creationTimestamp;

	/**
	 * The execution's root flow; the top level flow that acts as the starting
	 * point for this flow execution.
	 */
	private transient Flow rootFlow;

	/**
	 * Set only on deserialization so this object can be fully reconstructed.
	 */
	private String rootFlowId;

	/**
	 * The id of the last valid event that was signaled in this flow execution.
	 * Valid means the event indeed maps to a state transition (it is
	 * supported).
	 */
	private String eventId;

	/**
	 * The timestamp when the last valid event was signaled.
	 */
	private long eventTimestamp;

	/**
	 * The stack of active, currently executing flow sessions. As subflows are
	 * spawned, they are pushed onto the stack. As they end, they are popped off
	 * the stack.
	 */
	private Stack executingFlowSessions = new Stack();

	/**
	 * A thread-safe listener list, holding listeners monitoring the lifecycle
	 * of this flow execution.
	 */
	private transient FlowExecutionListenerList listenerList = new FlowExecutionListenerList();

	/**
	 * Create a new flow execution executing the provided flow.
	 * <p>
	 * The default list of flow execution listeners configured for given flow
	 * will also be notified of this flow execution.
	 * 
	 * @param rootFlow the root flow of this flow execution
	 */
	public FlowExecutionStack(Flow rootFlow) {
		Assert.notNull(rootFlow, "The root flow definition is required");
		this.id = new RandomGuid().toString();
		this.creationTimestamp = new Date().getTime();
		this.rootFlow = rootFlow;
		// add the list of default execution listeners configured for the flow
		listenerList.add(rootFlow.getFlowExecutionListenerList());
		if (logger.isDebugEnabled()) {
			logger.debug("Created new client execution for flow '" + rootFlow.getId() + "' with id '" + getId() + "'");
		}
	}

	// methods implementing FlowExecutionInfo

	public String getId() {
		return id;
	}

	public long getCreationTimestamp() {
		return this.creationTimestamp;
	}

	public long getUptime() {
		return new Date().getTime() - this.creationTimestamp;
	}

	public String getCaption() {
		return "[sessionId=" + getId() + ", " + getQualifiedActiveFlowId() + "]";
	}

	/**
	 * Returns whether or not this flow execution stack is empty.
	 */
	public boolean isEmpty() {
		return executingFlowSessions.isEmpty();
	}

	public boolean isActive() {
		return !isEmpty();
	}

	/**
	 * Check that this flow execution is active and throw an exception if it's
	 * not.
	 */
	protected void assertActive() throws IllegalStateException {
		if (!isActive()) {
			throw new IllegalStateException(
					"No active flow sessions executing - this flow execution has ended (or has never been started)");
		}
	}

	public String getActiveFlowId() {
		return getActiveFlowSession().getFlow().getId();
	}

	public String getQualifiedActiveFlowId() {
		assertActive();
		Iterator it = executingFlowSessions.iterator();
		StringBuffer qualifiedName = new StringBuffer(128);
		while (it.hasNext()) {
			FlowSession session = (FlowSession)it.next();
			qualifiedName.append(session.getFlow().getId());
			if (it.hasNext()) {
				qualifiedName.append('.');
			}
		}
		return qualifiedName.toString();
	}

	public String[] getFlowIdStack() {
		if (isEmpty()) {
			return new String[0];
		}
		else {
			Iterator it = executingFlowSessions.iterator();
			List stack = new ArrayList(executingFlowSessions.size());
			while (it.hasNext()) {
				FlowSession session = (FlowSession)it.next();
				stack.add(session.getFlow().getId());
			}
			return (String[])stack.toArray(new String[0]);
		}
	}

	public String getRootFlowId() {
		return rootFlow.getId();
	}

	public boolean isRootFlowActive() {
		return executingFlowSessions.size() == 1;
	}

	public String getCurrentStateId() {
		return getActiveFlowSession().getCurrentState().getId();
	}

	public String getEventId() {
		return this.eventId;
	}

	public long getEventTimestamp() {
		return this.eventTimestamp;
	}

	/**
	 * Set the last event id processed by this flow execution. This will also
	 * update the last event timestamp, which management clients can use to
	 * monitor the activity of this execution to detect idle status.
	 * @param eventId The last event id to set
	 */
	public void setEventId(String eventId) {
		Assert.notNull(eventId, "The eventId is required");
		this.eventId = eventId;
		this.eventTimestamp = System.currentTimeMillis();
		if (logger.isDebugEnabled()) {
			logger.debug("Event '" + eventId + "' within state '" + getCurrentStateId() + "' for flow '"
					+ getActiveFlowId() + "' signaled");
		}
		if (logger.isDebugEnabled()) {
			logger.debug("Set last event id to '" + eventId + "' and updated timestamp to " + this.eventTimestamp);
		}
	}

	public boolean sessionExists(String flowId) {
		Iterator it = executingFlowSessions.iterator();
		while (it.hasNext()) {
			FlowSession session = (FlowSession)it.next();
			if (session.getFlow().getId().equals(flowId)) {
				return true;
			}
		}
		return false;
	}

	public short getStatus(String flowId) throws IllegalArgumentException {
		Iterator it = executingFlowSessions.iterator();
		while (it.hasNext()) {
			FlowSession session = (FlowSession)it.next();
			if (session.getFlow().getId().equals(flowId)) {
				return session.getStatus().getShortCode();
			}
		}
		throw new IllegalArgumentException("No such session for flow '" + flowId + "'");
	}

	// methods implementing FlowExecution

	public FlowExecutionListenerList getListenerList() {
		return listenerList;
	}

	public Flow getActiveFlow() {
		return getActiveFlowSession().getFlow();
	}

	public Flow getRootFlow() {
		return rootFlow;
	}

	public State getCurrentState() {
		return getActiveFlowSession().getCurrentState();
	}

	/**
	 * Set the state that is currently active in this flow execution.
	 * @param newState The new current state
	 */
	protected void setCurrentState(State newState) {
		getActiveFlowSession().setCurrentState(newState);
	}

	public ViewDescriptor start(Event event) {
		Assert.state(!isActive(), "This flow execution is already started");
		this.eventTimestamp = System.currentTimeMillis();
		activateFlowSession(this.rootFlow, event.getParameters());
		LocalFlowExecutionContext context = new LocalFlowExecutionContext(event, this);
		context.fireStarted();
		return this.rootFlow.getStartState().enter(context);
	}

	/*
	 * Note: this entry point implementation is synchronized, locked on a per
	 * client (session) basis for this flow execution. Synchronization prevents
	 * a client from being able to signal other events before previously
	 * signaled ones have processed in-full, preventing possible race
	 * conditions.
	 */
	public synchronized ViewDescriptor signalEvent(Event event) {
		assertActive();
		String eventId = event.getId();
		String stateId = event.getStateId();
		if (!StringUtils.hasText(stateId)) {
			if (logger.isDebugEnabled()) {
				logger
						.debug("Current state id was not provided in request to signal event '"
								+ eventId
								+ "' in flow "
								+ getCaption()
								+ "' - pulling current state id from session - "
								+ "note: if the user has been using the browser back/forward buttons, the currentState could be incorrect.");
			}
			stateId = getCurrentStateId();
		}
		TransitionableState state = getActiveFlow().getRequiredTransitionableState(stateId);
		if (!state.equals(getCurrentState())) {
			if (logger.isDebugEnabled()) {
				logger.debug("Event '" + eventId + "' in state '" + state.getId()
						+ "' was signaled by client; however the current flow execution state is '"
						+ getCurrentStateId() + "'; updating current state to '" + state.getId() + "'");
			}
			setCurrentState(state);
		}
		LocalFlowExecutionContext context = new LocalFlowExecutionContext(event, this);
		context.fireRequestSubmitted(event);
		ViewDescriptor view = state.executeTransitionOnEvent(event, context);
		context.fireRequestProcessed(event);
		return view;
	}

	// flow session management helpers

	/**
	 * Activate given flow session in this flow execution stack. This will push
	 * the flow session onto the stack and mark it as the active flow session.
	 * @param flowSession the flow session to activate
	 */
	protected void activateFlowSession(Flow subFlow, Map input) {
		FlowSession flowSession = createFlowSession(subFlow, input);
		if (!executingFlowSessions.isEmpty()) {
			getActiveFlowSession().setStatus(FlowSessionStatus.SUSPENDED);
		}
		executingFlowSessions.push(flowSession);
		flowSession.setStatus(FlowSessionStatus.ACTIVE);
	}

	/**
	 * Create a new flow session object. Subclasses can override this to return
	 * a special implementation if required.
	 * @param flow The flow that should be associated with the flow session
	 * @param input The input parameters used to populate the flow session
	 * @return The newly created flow session
	 */
	protected FlowSession createFlowSession(Flow flow, Map input) {
		return new FlowSession(flow, input);
	}

	/**
	 * End the active flow session of this flow execution. This will pop the top
	 * element from the stack and activate the now top flow session.
	 * @return the flow session that ended
	 */
	protected FlowSession endActiveFlowSession() {
		FlowSession endingSession = (FlowSession)executingFlowSessions.pop();
		endingSession.setStatus(FlowSessionStatus.ENDED);
		if (!executingFlowSessions.isEmpty()) {
			getActiveFlowSession().setStatus(FlowSessionStatus.ACTIVE);
		}
		return endingSession;
	}

	/**
	 * Returns the flow session associated with the root flow.
	 */
	public FlowSession getRootFlowSession() {
		assertActive();
		return (FlowSession)executingFlowSessions.get(0);
	}

	/**
	 * Returns the currently active flow session.
	 */
	public FlowSession getActiveFlowSession() {
		assertActive();
		return (FlowSession)executingFlowSessions.peek();
	}

	// subclassing hooks

	/**
	 * Returns the name of the flow execution attribute, a special index to
	 * lookup this flow execution as an attribute.
	 * <p>
	 * The flow execution will also be exposed in the model returned from the
	 * <code>getModel()</code> method under this name.
	 * @return This flow execution's name
	 */
	protected Object getFlowExecutionAttributeName() {
		return "flowExecution";
	}

	/**
	 * The flow execution id will be exposed in the model returned from the
	 * <code>getModel()</code> method under this name.
	 * @return This flow execution's id's name
	 */
	protected Object getFlowExecutionIdAttributeName() {
		return FlowConstants.FLOW_EXECUTION_ID_ATTRIBUTE;
	}

	/**
	 * The current state of the flow execution will be exposed in the model
	 * returned from the <code>getModel()</code> method under this name.
	 * @return This flow execution's current state name
	 */
	protected Object getCurrentStateIdAttributeName() {
		return FlowConstants.CURRENT_STATE_ID_ATTRIBUTE;
	}

	// methods implementing FlowModel

	public Map getModel() {
		Map model = new HashMap(getActiveFlowSession().getModel());
		// the flow execution itself is available in the model
		model.put(getFlowExecutionAttributeName(), this);
		// these are added for convenience for views that aren't easily
		// javabean aware
		model.put(getFlowExecutionIdAttributeName(), getId());
		model.put(getCurrentStateIdAttributeName(), getCurrentStateId());
		return model;
	}

	// custom serialization

	private void writeObject(ObjectOutputStream out) throws IOException {
		out.writeObject(this.id);
		out.writeObject(this.getRootFlow().getId());
		out.writeObject(this.eventId);
		out.writeLong(this.eventTimestamp);
		out.writeObject(this.executingFlowSessions);
	}

	private void readObject(ObjectInputStream in) throws OptionalDataException, ClassNotFoundException, IOException {
		this.id = (String)in.readObject();
		this.rootFlowId = (String)in.readObject();
		this.eventId = (String)in.readObject();
		this.eventTimestamp = in.readLong();
		this.executingFlowSessions = (Stack)in.readObject();
	}

	public synchronized void rehydrate(FlowLocator flowLocator, FlowExecutionListener[] listeners) {
		// implementation note: we cannot integrate this code into the
		// readObject() method since we need the flow locator and listener list!
		if (this.rootFlow != null) {
			// nothing to do, we're already hydrated
			return;
		}
		Assert
				.notNull(rootFlowId,
						"The root flow id was not set during deserialization: cannot restore--was this flow execution deserialized properly?");
		this.rootFlow = flowLocator.getFlow(rootFlowId);
		this.rootFlowId = null;
		Iterator it = this.executingFlowSessions.iterator();
		while (it.hasNext()) {
			FlowSession session = (FlowSession)it.next();
			session.rehydrate(flowLocator);
		}
		if (isActive()) {
			// sanity check
			Assert.isTrue(getRootFlow() == getRootFlowSession().getFlow(),
					"the root flow of the execution should be the same of the flow in the root flow session");
		}
		this.listenerList = new FlowExecutionListenerList();
		this.listenerList.add(this.rootFlow.getFlowExecutionListenerList());
		this.listenerList.add(listeners);
	}

	public String toString() {
		return executingFlowSessions.isEmpty() ? "[Empty FlowExecutionStack " + getId() + "; no flows are active]"
				: new ToStringCreator(this).append("id", getId()).append("activeFlowId", getActiveFlowId()).append(
						"currentStateId", getCurrentStateId()).append("rootFlow", isRootFlowActive()).append(
						"executingFlowSessions", executingFlowSessions).toString();
	}
}