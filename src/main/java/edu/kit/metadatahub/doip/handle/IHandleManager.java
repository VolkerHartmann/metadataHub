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
package edu.kit.metadatahub.doip.handle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 */
public interface IHandleManager {

    /**
     * Logger.
     */
    public static Logger LOG = LoggerFactory.getLogger(IHandleManager.class);

    /**
     * Create a Handle PID.
     *
     * @return Handle PID.
     */
    String createHandle();

    /**
     * Create a Handle PID.
     *
     * @param resolveUrl URL to access digital object;
     * @return Handle PID.
     */
    String createHandle(String resolveUrl);

    /**
     * Create a Handle PID.
     *
     * @param handle Handle of the referenced digital object.
     * @param resolveUrl URL to access digital object;
     * @return Handle PID.
     */
    String editHandle(String handle, String resolveUrl);

    /**
     * Resolve Handle.
     *
     * Get URL to access referenced digital object.
     *
     * @param handle Handle of the referenced digital object.
     * @return URL to access digital object.
     */
    String resolveHandle(String handle);

}
