// ----------------------------------------------------------------------
//This code is developed as part of the Java CoG Kit project
//The terms of the license can be found at http://www.cogkit.org/license
//This message may not be removed or altered.
//----------------------------------------------------------------------

/*
 * Created on Mar 21, 2005
 */
package org.griphyn.vdl.karajan.lib;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;
import org.globus.cog.karajan.arguments.Arg;
import org.globus.cog.karajan.stack.VariableNotFoundException;
import org.globus.cog.karajan.stack.VariableStack;
import org.globus.cog.karajan.util.Identifier;
import org.globus.cog.karajan.util.KarajanIterator;
import org.globus.cog.karajan.util.ThreadingContext;
import org.globus.cog.karajan.util.TypeUtil;
import org.globus.cog.karajan.workflow.ExecutionException;
import org.globus.cog.karajan.workflow.events.Event;
import org.globus.cog.karajan.workflow.events.EventBus;
import org.globus.cog.karajan.workflow.events.EventListener;
import org.globus.cog.karajan.workflow.events.EventTargetPair;
import org.globus.cog.karajan.workflow.events.FutureNotificationEvent;
import org.globus.cog.karajan.workflow.futures.FutureEvaluationException;
import org.globus.cog.karajan.workflow.futures.FutureFault;
import org.globus.cog.karajan.workflow.futures.FutureIterator;
import org.globus.cog.karajan.workflow.futures.FutureIteratorIncomplete;
import org.globus.cog.karajan.workflow.nodes.AbstractParallelIterator;
import org.griphyn.vdl.util.VDL2Config;

public class ThrottledParallelFor extends AbstractParallelIterator {
	public static final Logger logger = Logger
			.getLogger(ThrottledParallelFor.class);
	
	public static final int DEFAULT_MAX_THREADS = 1024;

	public static final Arg A_NAME = new Arg.Positional("name");
	public static final Arg A_IN = new Arg.Positional("in");
	public static final Arg A_SELF_CLOSE = new Arg.Optional("selfclose", Boolean.FALSE);

	static {
		setArguments(ThrottledParallelFor.class, new Arg[] { A_NAME, A_IN, A_SELF_CLOSE });
	}

	public static final String THREAD_COUNT = "#threadcount";

	private int maxThreadCount = -1;

    protected void partialArgumentsEvaluated(VariableStack stack)
            throws ExecutionException {
        stack.setVar("selfclose", A_SELF_CLOSE.getValue(stack));
        super.partialArgumentsEvaluated(stack);
    }

    public void iterate(VariableStack stack, Identifier var, KarajanIterator i)
			throws ExecutionException {
		if (elementCount() > 0) {
			if (logger.isDebugEnabled()) {
				logger.debug("iterateParallel: " + stack.parentFrame());
			}
			stack.setVar(VAR, var);
			setChildFailed(stack, false);
			stack.setCaller(this);
			initializeChannelBuffers(stack);
			initThreadCount(stack, TypeUtil.toBoolean(stack.currentFrame().getVar("selfclose")), i);
			stack.currentFrame().deleteVar("selfclose");
			citerate(stack, var, i);
		}
		else {
			complete(stack);
		}
	}

	protected void citerate(VariableStack stack, Identifier var,
			KarajanIterator i) throws ExecutionException {
		ThreadCount tc = getThreadCount(stack);
		try {
			while (i.hasNext()) {
				Object value = tc.tryIncrement();
				VariableStack copy = stack.copy();
				copy.enter();
				ThreadingContext.set(copy, ThreadingContext.get(copy).split(
						i.current()));
				setIndex(copy, getArgCount());
				setArgsDone(copy);
				copy.setVar(var.getName(), value);
				addChannelBuffers(copy);
				startElement(getArgCount(), copy);
			}
			
			int left;
			synchronized(tc) {
			    // can only have closed and running = 0 in one place
			    tc.close();
			    left = tc.current();
			}
			if (left == 0) {
				complete(stack);
			}
		}
		catch (FutureIteratorIncomplete fii) {
			stack.setVar(ITERATOR, i);
			fii.getFutureIterator().addModificationAction(
					this,
					new FutureNotificationEvent(ITERATE, this, fii
							.getFutureIterator(), stack));
		}
	}

	protected void iterationCompleted(VariableStack stack)
			throws ExecutionException {
		closeBuffers(stack);
		stack.leave();
		ThreadCount tc = getThreadCount(stack);
		int running;
		boolean closed;
		boolean iteratorHasValues;
		synchronized(tc) {
		    closed = tc.isClosed();
		    running = tc.decrement();
		    iteratorHasValues = !tc.selfClose || tc.iteratorHasValues();
		}
		if (running == 0) {
		    if (closed || !iteratorHasValues) {
		        complete(stack);
		    }
		}
	}

	private void initThreadCount(VariableStack stack, boolean selfClose, KarajanIterator i) {
		if (maxThreadCount < 0) {
			try {
				maxThreadCount = TypeUtil.toInt(VDL2Config.getConfig()
						.getProperty("foreach.max.threads", String.valueOf(DEFAULT_MAX_THREADS)));
			}
			catch (IOException e) {
				maxThreadCount = DEFAULT_MAX_THREADS;
			}
		}
		stack.setVar(THREAD_COUNT, new ThreadCount(maxThreadCount, selfClose, i));
	}

	private ThreadCount getThreadCount(VariableStack stack)
			throws VariableNotFoundException {
		return (ThreadCount) stack.getVar(THREAD_COUNT);
	}

	private static class ThreadCount implements FutureIterator {
		private int maxThreadCount;
		private int crt;
		private boolean selfClose, closed;
		private List listeners;
		private KarajanIterator i;

		public ThreadCount(int maxThreadCount, boolean selfClose, KarajanIterator i) {
			this.maxThreadCount = maxThreadCount;
			this.selfClose = selfClose;
			this.i = i;
			crt = 0;
		}
		
		public boolean iteratorHasValues() {
            try {
                return i.hasNext();
            }
            catch (FutureFault e) {
                return false;
            }
        }

        public boolean isSelfClose() {
		    return selfClose;
		}

		public synchronized Object tryIncrement() {
		    // there is no way that both crt == 0 and i has no values outside this critical section
			if (crt < maxThreadCount) {
				Object o = i.next();
				crt++;
				return o;
			}
			else {
				throw new FutureIteratorIncomplete(this, this);
			}
		}

		public synchronized int decrement() {
			crt--;
			notifyListeners();
			return crt;
		}

		private void notifyListeners() {
			if (listeners != null) {
				Iterator i = listeners.iterator();
				while (i.hasNext()) {
					EventTargetPair etp = (EventTargetPair) i.next();
					i.remove();
					EventBus.post(etp.getTarget(), etp.getEvent());
				}
			}
		}

		public boolean hasAvailable() {
			return false;
		}

		public int count() {
			return 0;
		}

		public synchronized int current() {
			return crt;
		}

		public Object peek() {
			return null;
		}

		public boolean hasNext() {
			return false;
		}

		public Object next() {
			return null;
		}

		public void remove() {
		}

		public synchronized void addModificationAction(EventListener target,
				Event event) {
			if (listeners == null) {
				listeners = new ArrayList();
			}
			listeners.add(new EventTargetPair(event, target));
			if (crt < maxThreadCount) {
				notifyListeners();
			}
		}

		public synchronized void close() {
		    this.closed = true;
		}

		public void fail(FutureEvaluationException e) {
		}

		public Object getValue() throws ExecutionException {
			return null;
		}

		public synchronized boolean isClosed() {
		    return closed;
		}
	}
}