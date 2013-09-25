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
 * Created on Jun 30, 2006
 */
package org.griphyn.vdl.mapping;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.griphyn.vdl.mapping.file.AirsnMapper;
import org.griphyn.vdl.mapping.file.ArrayFileMapper;
import org.griphyn.vdl.mapping.file.CSVMapper;
import org.griphyn.vdl.mapping.file.ConcurrentMapper;
import org.griphyn.vdl.mapping.file.ExternalMapper;
import org.griphyn.vdl.mapping.file.FileSystemArrayMapper;
import org.griphyn.vdl.mapping.file.FixedArrayFileMapper;
import org.griphyn.vdl.mapping.file.ROIMapper;
import org.griphyn.vdl.mapping.file.RegularExpressionMapper;
import org.griphyn.vdl.mapping.file.SimpleFileMapper;
import org.griphyn.vdl.mapping.file.SingleFileMapper;
import org.griphyn.vdl.mapping.file.StructuredRegularExpressionMapper;
import org.griphyn.vdl.mapping.file.TestMapper;

public class MapperFactory {
	private static Map<String, Class<? extends Mapper>> mappers;
	
	private static Map<String, Set<String>> validParams;

	static {
	    mappers = new HashMap<String, Class<? extends Mapper>>();
	    validParams = new HashMap<String, Set<String>>();
	    
		// the following are general purpose file mappers
		registerMapper("simple_mapper", SimpleFileMapper.class);
		registerMapper("single_file_mapper", SingleFileMapper.class);
		registerMapper("fixed_array_mapper", FixedArrayFileMapper.class);
		registerMapper("concurrent_mapper", ConcurrentMapper.class);
		registerMapper("filesys_mapper", FileSystemArrayMapper.class);
		registerMapper("regexp_mapper", RegularExpressionMapper.class);
		registerMapper("structured_regexp_mapper",
			StructuredRegularExpressionMapper.class);
		registerMapper("csv_mapper", CSVMapper.class);
		registerMapper("array_mapper", ArrayFileMapper.class);

		// the following are application-specific mappers
		registerMapper("airsn_mapper", AirsnMapper.class);
		registerMapper("roi_mapper", ROIMapper.class);
		registerMapper("ext", ExternalMapper.class);
		registerMapper("test_mapper", TestMapper.class);
	}

	public synchronized static Mapper getMapper(String type, MappingParamSet params) throws InvalidMapperException {
		Class<? extends Mapper> cls = mappers.get(type);
		if (cls == null) {
			throw new InvalidMapperException("No such mapper: "+type);
		}
		try {
			Mapper mapper = cls.newInstance();
			mapper.setParams(params);
			return mapper;
		}
		catch (Exception e) {
			throw new InvalidMapperException(type + ": " + e.getMessage(), e);
		}
	}
	
	public synchronized static boolean isValidMapperType(String type) {
	    return mappers.containsKey(type);
	}
	
    @SuppressWarnings("unchecked")
    public static void registerMapper(String type, String cls) throws ClassNotFoundException {
	    registerMapper(type, (Class<? extends Mapper>) MapperFactory.class.getClassLoader().loadClass(cls));
	}

    public synchronized static void registerMapper(String type, Class<? extends Mapper> cls) {
		mappers.put(type, cls);
		try {
		    Mapper m = cls.newInstance();
		    validParams.put(type, m.getSupportedParamNames());
		}
		catch (Exception e) {
		    throw new RuntimeException("Cannot instantiate a '" + type + "'", e);
		}
	}

    
    public static Set<String> getValidParams(String type) {
        return validParams.get(type);
    }
}