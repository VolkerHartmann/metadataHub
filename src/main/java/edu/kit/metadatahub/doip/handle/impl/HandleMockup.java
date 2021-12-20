/*
 * Copyright 2021 Karlsruhe Institute of Technology.
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
package edu.kit.metadatahub.doip.handle.impl;

import edu.kit.metadatahub.doip.handle.IHandleManager;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *  Mockup for Handles.
 */
public class HandleMockup implements IHandleManager {
  /**
   * Logger.
   */
  private final static Logger LOGGER = LoggerFactory.getLogger(HandleMockup.class);
    private static final String NO_URL = "not resolved yet!";
    private Map<String, String> handleMap = new HashMap<>();
    private static final String PREFIX = "123456/";

    @Override
    public String createHandle() {
        return createHandle(NO_URL);
    }

    @Override
    public String createHandle(String resolveUrl) {
        String newHandle = PREFIX + UUID.randomUUID();
        handleMap.put(newHandle, resolveUrl);
      LOGGER.trace("Create handle -> '{}': '{}'", newHandle, resolveUrl);
        
        return newHandle;
    }

    @Override
    public String editHandle(String handle, String resolveUrl) {
        handleMap.put(handle, resolveUrl);
      LOGGER.trace("Edit handle -> '{}': '{}'", handle, resolveUrl);
        return handle;
    }

    @Override
    public String resolveHandle(String handle) {
      String resolveUrl = handleMap.get(handle);
      LOGGER.trace("Resolve handle: '{}' -> '{}'", handle, resolveUrl);
      
       return resolveUrl;
    }
    
}
