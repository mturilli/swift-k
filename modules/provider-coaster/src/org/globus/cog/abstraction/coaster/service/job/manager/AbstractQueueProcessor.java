//----------------------------------------------------------------------
//This code is developed as part of the Java CoG Kit project
//The terms of the license can be found at http://www.cogkit.org/license
//This message may not be removed or altered.
//----------------------------------------------------------------------

/*
 * Created on Feb 13, 2008
 */
package org.globus.cog.abstraction.coaster.service.job.manager;

import java.util.Iterator;

import org.globus.cog.abstraction.interfaces.Task;
import org.globus.cog.karajan.util.Queue;

public abstract class AbstractQueueProcessor extends Thread implements QueueProcessor {
    private final Queue q;
    private Queue.Cursor cursor;
    private boolean shutdownFlag;
    private Iterator i;
    private boolean wrap;

    public AbstractQueueProcessor(String name) {
        super(name);
        q = new Queue();
    }

    public void enqueue(Task t) {
        synchronized (q) {
            if (shutdownFlag) {
                throw new IllegalStateException("Queue is shut down");
            }
            q.enqueue(new AssociatedTask(t));
            q.notifyAll();
        }
    }

    public void shutdown() {
        synchronized(q) {
            shutdownFlag = true;
        }
    }

    protected boolean getShutdownFlag() {
        return shutdownFlag;
    }

    protected final Queue getQueue() {
        return q;
    }

    protected final AssociatedTask take() throws InterruptedException {
        return (AssociatedTask) q.take();
    }

    protected final boolean hasWrapped() {
        synchronized (q) {
            return wrap;
        }
    }

    protected final void remove() {
        synchronized (q) {
            if (cursor == null) {
                throw new IllegalThreadStateException(
                        "next() was never called");
            }
            else {
                cursor.remove();
            }
        }
    }

    protected int queuedTaskCount() {
        synchronized (q) {
            return q.size();
        }
    }
}
