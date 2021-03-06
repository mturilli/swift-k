/*
 * Swift Parallel Scripting Language (http://swift-lang.org)
 * Code from Java CoG Kit Project (see notice below) with modifications.
 *
 * Copyright 2005-2014 University of Chicago
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

//----------------------------------------------------------------------
//This code is developed as part of the Java CoG Kit project
//The terms of the license can be found at http://www.cogkit.org/license
//This message may not be removed or altered.
//----------------------------------------------------------------------

/*
 * Created on Feb 17, 2005
 */
package org.globus.cog.karajan.util;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * A class implementing deep comparison operation for lists and maps.
 * 
 * @author Mihael Hategan
 *
 */
public abstract class AbstractEqualityComparator implements EqualityComparator {

	public boolean equals(Object o1, Object o2) {
		if (o1 instanceof List) {
			if (o2 instanceof List) {
				return compareLists((List) o1, (List) o2);
			}
			return false;
		}
		if (o1 instanceof Map) {
			if (o2 instanceof Map) {
				return compareMaps((Map) o1, (Map) o2);
			}
		}
		return compareOne(o1, o2);
	}
	
	protected abstract boolean compareOne(Object o1, Object o2);
	
	private boolean compareLists(List l1, List l2) {
		if (l1.size() != l2.size()) {
			return false;
		}
		Iterator i1 = l1.iterator();
		Iterator i2 = l2.iterator();
		while (i1.hasNext()) {
			if (!equals(i1.next(), i2.next())) {
				return false;
			}
		}
		return true;
	}
	
	private boolean compareMaps(Map m1, Map m2) {
		//this will only compare values. They keys in the maps
		//must still be equal using the object's equals() method
		if (m1.size() != m2.size()) {
			return false;
		}
		
		Iterator i1 = m1.keySet().iterator();
		while (i1.hasNext()) {
			Object key = i1.next();
			if (!m2.containsKey(key)) {
				return false;
			}
			if (!compareOne(m1.get(key), m2.get(key))) {
				return false;
			}
		}
		return true;
	}
}
