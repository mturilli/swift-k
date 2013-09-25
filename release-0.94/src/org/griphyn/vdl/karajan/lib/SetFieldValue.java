/*
 * Copyright 2012 University of Chicago
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


/*
 * Created on Dec 26, 2006
 */
package org.griphyn.vdl.karajan.lib;

import java.util.Collections;
import java.util.Iterator;
import java.util.Map;

import org.apache.log4j.Logger;
import org.globus.cog.karajan.arguments.Arg;
import org.globus.cog.karajan.stack.VariableNotFoundException;
import org.globus.cog.karajan.stack.VariableStack;
import org.globus.cog.karajan.workflow.ExecutionException;
import org.globus.cog.karajan.workflow.futures.FutureFault;
import org.globus.cog.karajan.workflow.futures.FutureNotYetAvailable;
import org.griphyn.vdl.karajan.Pair;
import org.griphyn.vdl.karajan.PairIterator;
import org.griphyn.vdl.karajan.WaitingThreadsMonitor;
import org.griphyn.vdl.mapping.AbstractDataNode;
import org.griphyn.vdl.mapping.DSHandle;
import org.griphyn.vdl.mapping.InvalidPathException;
import org.griphyn.vdl.mapping.Mapper;
import org.griphyn.vdl.mapping.Path;
import org.griphyn.vdl.type.Type;

public class SetFieldValue extends VDLFunction {
	public static final Logger logger = Logger.getLogger(SetFieldValue.class);

	public static final Arg PA_VALUE = new Arg.Positional("value");

	static {
		setArguments(SetFieldValue.class, new Arg[] { OA_PATH, PA_VAR, PA_VALUE });
	}
	
	private String dst;
	private Tracer tracer;

	@Override
    protected void initializeStatic() {
        super.initializeStatic();
        tracer = Tracer.getTracer(this);
    }

    public Object function(VariableStack stack) throws ExecutionException {
		DSHandle var = (DSHandle) PA_VAR.getValue(stack);
		try {
		    Path path = parsePath(OA_PATH.getValue(stack), stack);
			DSHandle leaf = var.getField(path);
			AbstractDataNode value = (AbstractDataNode) PA_VALUE.getValue(stack);
			
			String mdst = dst;
			
			if (mdst == null) {
			    mdst = Tracer.getVarName(var);
			    if (var.getParent() == null) {
			        dst = mdst;
			    }
			}
			
			if (tracer.isEnabled()) {
			    log(leaf, value, stack, mdst);
			}
			    
            // TODO want to do a type check here, for runtime type checking
            // and pull out the appropriate internal value from value if it
            // is a DSHandle. There is no need (I think? maybe numerical casting?)
            // for type conversion here; but would be useful to have
            // type checking.
			
   			deepCopy(leaf, value, stack, 0);
			return null;
		}
		catch (FutureFault f) {
		    if (tracer.isEnabled()) {
		        tracer.trace(stack, var + " waiting for " + Tracer.getFutureName(f.getFuture()));
		    }
		    WaitingThreadsMonitor.addOutput(stack, Collections.singletonList(var));
			throw f;
		}
		catch (Exception e) { // TODO tighten this
			throw new ExecutionException(e);
		}
	}

    private void log(DSHandle leaf, DSHandle value, VariableStack stack, String dst) throws VariableNotFoundException {
        tracer.trace(stack, dst + " = " + Tracer.unwrapHandle(value));
    }

	String unpackHandles(DSHandle handle, Map<Comparable<?>, DSHandle> handles) { 
	    StringBuilder sb = new StringBuilder();
	    sb.append("{");
	    synchronized(handle) {
    	    Iterator<Map.Entry<Comparable<?>, DSHandle>> it = 
    	        handles.entrySet().iterator();
    	    while (it.hasNext()) { 
    	        Map.Entry<Comparable<?>, DSHandle> entry = it.next();
    	        sb.append(entry.getKey());
    	        sb.append('=');
    	        sb.append(entry.getValue().getValue());
    	        if (it.hasNext())
    	            sb.append(", ");
    	    }
	    }
	    sb.append("}");
	    return sb.toString();
	}
	
    /** make dest look like source - if its a simple value, copy that
	    and if its an array then recursively copy */
	public static void deepCopy(DSHandle dest, DSHandle source, VariableStack stack, int level) throws ExecutionException {
	    ((AbstractDataNode) source).waitFor();
		if (source.getType().isPrimitive()) {
			dest.setValue(source.getValue());
		}
		else if (source.getType().isArray()) {
		    copyArray(dest, source, stack, level);
		}
		else if (source.getType().isComposite()) {
		    copyStructure(dest, source, stack, level);
		}
		else {
		    copyNonComposite(dest, source, stack, level);
		}
	}

    @Override
    public String getTextualName() {
        return "assignment";
    }

    @SuppressWarnings("unchecked")
    private static void copyStructure(DSHandle dest, DSHandle source,
            VariableStack stack, int level) throws ExecutionException {
        Type type = dest.getType();
        Iterator<String> fni = (Iterator<String>) stack.currentFrame().getVar("it" + level);
        if (fni == null) {
            fni = type.getFieldNames().iterator();
            stack.currentFrame().setVar("it" + level, fni);
        }
        String fname = (String) stack.currentFrame().getVar("f" + level);
        while (fni.hasNext() || fname != null) {
            if (fname == null) {
                fname = fni.next();
                stack.currentFrame().setVar("f" + level, fname);
            }
            Path fpath = Path.EMPTY_PATH.addFirst(fname);
            try {
                DSHandle dstf = dest.getField(fpath);
                try {
                    DSHandle srcf = source.getField(fpath);
                    deepCopy(dstf, srcf, stack, level + 1);
                }
                catch (InvalidPathException e) {
                    // do nothing. It's an unused field in the source.
                }
            }
            catch (InvalidPathException e) {
                throw new ExecutionException("Internal type inconsistency detected. " + 
                    dest + " claims not to have a " + fname + " field");
            }
            stack.currentFrame().deleteVar("f" + level);
            fname = null;
        }
        stack.currentFrame().deleteVar("it" + level);
        dest.closeShallow();
    }

    private static void copyNonComposite(DSHandle dest, DSHandle source,
            VariableStack stack, int level) throws ExecutionException {
        Path dpath = dest.getPathFromRoot();
        Mapper dmapper = dest.getRoot().getMapper();
        if (dmapper.canBeRemapped(dpath)) {
            if (logger.isDebugEnabled()) {
                logger.debug("Remapping " + dest + " to " + source);
            }
            dmapper.remap(dpath, source.getMapper(), source.getPathFromRoot());
            dest.closeShallow();
        }
        else {
            if (stack.currentFrame().isDefined("fc")) {
                FileCopier fc = (FileCopier) stack.currentFrame().getVarAndDelete("fc");
                if (!fc.isClosed()) {
                    throw new FutureNotYetAvailable(fc);
                }
                else {
                    if (fc.getException() != null) {
                        throw new ExecutionException("Failed to copy " + source + " to " + dest, fc.getException());
                    }
                }
                dest.closeShallow();
            }
            else {
                FileCopier fc = new FileCopier(source.getMapper().map(source.getPathFromRoot()), 
                    dest.getMapper().map(dpath));
                stack.setVar("fc", fc);
                try {
                    fc.start();
                }
                catch (Exception e) {
                    throw new ExecutionException("Failed to start file copy", e);
                }
                throw new FutureNotYetAvailable(fc);
            }
        }
    }

    private static void copyArray(DSHandle dest, DSHandle source,
            VariableStack stack, int level) throws ExecutionException {
        PairIterator it;
        if (stack.isDefined("it" + level)) {
            it = (PairIterator) stack.getVar("it" + level);
        }
        else {
            it = new PairIterator(source.getArrayValue());
            stack.setVar("it" + level, it);
        }
        Pair pair = (Pair) stack.currentFrame().getVar("p" + level);
        while (it.hasNext() || pair != null) {
            if (pair == null) {
                pair = (Pair) it.next();
                stack.currentFrame().setVar("p" + level, pair);
            }
            Comparable<?> lhs = (Comparable<?>) pair.get(0);
            DSHandle rhs = (DSHandle) pair.get(1);
            Path memberPath = Path.EMPTY_PATH.addLast(lhs, true);
            
            DSHandle field;
            try {
                field = dest.getField(memberPath);
            }
            catch (InvalidPathException ipe) {
                throw new ExecutionException("Could not get destination field",ipe);
            }
            deepCopy(field, rhs, stack, level + 1);
            stack.currentFrame().deleteVar("p" + level);
            pair = null;
        }
        stack.currentFrame().deleteVar("it" + level);
        dest.closeShallow();
    }
}
