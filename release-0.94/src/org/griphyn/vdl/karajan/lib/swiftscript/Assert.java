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


package org.griphyn.vdl.karajan.lib.swiftscript;

// import org.apache.log4j.Logger;
import org.globus.cog.karajan.arguments.Arg;
import org.globus.cog.karajan.stack.VariableStack;
import org.globus.cog.karajan.workflow.ExecutionException;
import org.griphyn.vdl.karajan.AssertFailedException;
import org.griphyn.vdl.karajan.lib.VDLFunction;
import org.griphyn.vdl.mapping.AbstractDataNode;
import org.griphyn.vdl.mapping.DSHandle;
import org.griphyn.vdl.type.Types;

/**
    Throw AssertionException if input is false or 0. 
    Optional second argument is string message printed on failure. 
 */
public class Assert extends VDLFunction {

    //    private static final Logger logger = 
    //    Logger.getLogger(Assert.class);
    
    static {
        setArguments(Assert.class, new Arg[] { Arg.VARGS });
    }
    
    @Override
    protected Object function(VariableStack stack) 
    throws ExecutionException {
        AbstractDataNode[] args = waitForAllVargs(stack);
        String message = "";
        
        if (args.length == 2)
            if (args[1].getType() == Types.STRING)
                message = (String) args[1].getValue();
            else
                throw new ExecutionException
                ("Second argument to assert() must be a String!");
         
        checkAssert(args[0], message);
        
        return null;
    }

    private void checkAssert(DSHandle value, String message) 
    throws ExecutionException
    {
        boolean success = true; 
        if (value.getType() == Types.BOOLEAN) { 
            if (! (Boolean) value.getValue())
                success = false;
        }
        else if (value.getType() == Types.INT) {
            double d = ((Integer) value.getValue()).intValue();
            if (d == 0)
                success = false;
        } 
        else 
            throw new ExecutionException
            ("First argument to assert() must be boolean or int!");
        if (! success)
            throw new AssertFailedException(message);
    }
}