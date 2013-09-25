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


package org.griphyn.vdl.mapping;

import java.util.Collection;
import java.util.Map;

import org.griphyn.vdl.type.Type;

/** A DSHandle refers to a (possibly complex) piece of SwiftScript
 *  data.
 *
 * A DSHandle has a type.
 *
 * A DSHandle may have a value that is accessible
 * as a Java object through the getValue() method, however that is
 * not always the case. For example in the case of data mapped from
 * files on disk, the value of those files is inaccessible through
 * the getValue methods.
 *
 * A DSHandle may have a filename (on disk) indicating where the
 * data for this DSHandle is stored. That data is not accessible
 * through the getValue interfaces.
 *
 * A DSHandle may have descendant nodes, in the case of arrays or
 * other complex structures. Each of those descendant nodes is a
 * DSHandle.
 */
public interface DSHandle {

    /** get the type of the dataset.
     */
    public Type getType();

    public void init(MappingParamSet params);

    public DSHandle getRoot();

    public DSHandle getParent();

    /** returns the field below this node that is referred to by the
     *  supplied path. The path must have no wildcards.
     */
    public DSHandle getField(Path path) throws InvalidPathException;

    /** returns a collection of fields below this node that are referred to
     *  by the supplied path. The path may contain wildcards. If it does not,
     *  then the returned collection should contain a single member, which is
     *  the same as would be returned by getField().
     *
     *  @return a Collection of DSHandle objects
     */
    public Collection<DSHandle> getFields(Path path) throws InvalidPathException, HandleOpenException;

    public Object getValue();

    public void setValue(Object value);

    /** create a new logical component */
    public DSHandle createField(Comparable<?> key) throws NoSuchFieldException;
 
    // special file oriented methods, not sure if these apply to 
    // all datasets

    /** close */
    public void closeShallow();

    public void closeDeep();

    public Collection<Path> getFringePaths() throws HandleOpenException;

    public Map<Comparable<?>, DSHandle> getArrayValue();

    public Path getPathFromRoot();

    public void set(DSHandle svar);

    public String getParam(MappingParam name);

    public boolean isClosed();

    public void addListener(DSHandleListener listener);

    Mapper getMapper();

    public String getIdentifier();

    public String getIdentifyingString();

    public boolean isRestartable();
    
    public void setWriteRefCount(int count);
    
    public int updateWriteRefCount(int delta);
}