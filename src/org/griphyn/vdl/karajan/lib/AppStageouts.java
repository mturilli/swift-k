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
 * Created on Jan 5, 2007
 */
package org.griphyn.vdl.karajan.lib;

import java.util.LinkedList;
import java.util.List;

import k.rt.ExecutionException;
import k.rt.Stack;
import k.thr.LWThread;

import org.globus.cog.karajan.analyzer.ArgRef;
import org.globus.cog.karajan.analyzer.ChannelRef;
import org.globus.cog.karajan.analyzer.Signature;
import org.globus.cog.karajan.compiled.nodes.InternalFunction;
import org.griphyn.vdl.mapping.AbsFile;
import org.griphyn.vdl.mapping.DSHandle;
import org.griphyn.vdl.mapping.Path;

public class AppStageouts extends InternalFunction {
    private ArgRef<String> jobid;
    private ArgRef<List<List<Object>>> files;
    private ArgRef<String> dir;
    private ArgRef<String> stagingMethod;
    
    private ChannelRef<List<String>> cr_stageout;
    
    @Override
    protected Signature getSignature() {
        return new Signature(params("jobid", "files", "dir", "stagingMethod"), returns(channel("stageout")));
    }

    protected void runBody(LWThread thr) {
        try {
            Stack stack = thr.getStack();
            List<List<Object>> files = this.files.getValue(stack);
            String stagingMethod = this.stagingMethod.getValue(stack);
            String dir = this.dir.getValue(stack);
            for (List<Object> pv : files) { 
                Path p = (Path) pv.get(0);
                DSHandle handle = (DSHandle) pv.get(1);
                AbsFile file = new AbsFile(SwiftFunction.filename(handle.getField(p))[0]);
                String protocol = file.getProtocol();
                if (protocol.equals("file")) {
                    protocol = stagingMethod;
                }
                String path = file.getDir().equals("") ? file.getName() : file.getDir()
                        + "/" + file.getName();
                String relpath = path.startsWith("/") ? path.substring(1) : path;
                cr_stageout.append(stack, 
                    makeList(dir + "/" + relpath,
                        protocol + "://" + file.getHost() + "/" + path));
            }
        }
        catch (Exception e) {
            throw new ExecutionException(this, e);
        }
    }
    
    private List<String> makeList(String s1, String s2) {
        List<String> l = new LinkedList<String>();
        l.add(s1);
        l.add(s2);
        return l;
    }
}
