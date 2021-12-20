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
package edu.kit.metadatahub.doip.mapping;

import edu.kit.metadatahub.doip.server.*;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonReader;
import edu.kit.metadatahub.doip.ExtendedOperations;
import edu.kit.turntable.mapping.MappingSchema;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.PublicKey;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Stream;
import net.dona.doip.DoipConstants;
import net.dona.doip.InDoipMessage;
import net.dona.doip.InDoipSegment;
import net.dona.doip.client.DigitalObject;
import net.dona.doip.client.DoipException;
import net.dona.doip.server.DoipProcessor;
import net.dona.doip.server.DoipServerRequest;
import net.dona.doip.server.DoipServerResponse;
import net.dona.doip.util.GsonUtility;
import net.dona.doip.util.InDoipMessageUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Interface to convert the DOIP to another protocol supported by the underlying 
 * repository.
 *
 */
public interface IMappingInterface {

  /**
   * Logger for messages.
   */
  static final Logger LOGGER = LoggerFactory.getLogger(IMappingInterface.class);
 

  void initMapping(MappingSchema mapping);



  /**
   * List all service operations. By default, operations OP_HELLO,
   * OP_LIST_OPERATIONS, OP_CREATE and OP_SEARCH should be supported.
   * @param req DOIP request 
   * @param resp DOIP response
   * @throws DoipException Call is invalid
   * @throws IOException Error while reading/writing to buffers.
   */
  void listOperationsForService(DoipServerRequest req, DoipServerResponse resp) throws DoipException, IOException;

  /**
   * Create a new DigitalObject.
   * @param req DOIP request 
   * @param resp DOIP response
   * @throws DoipException Call is invalid
   * @throws IOException Error while reading/writing to buffers.
   */
  void create(DoipServerRequest req, DoipServerResponse resp) throws DoipException, IOException;
  /**
   * Search for resources using a provided search query and pagination
   * information. The search query should be a serialized data resource in JSON
   * format. If deserialization fails, the service will query for all data
   * resources and returns the selected page.
   * @param req DOIP request 
   * @param resp DOIP response
   * @throws DoipException Call is invalid
   * @throws IOException Error while reading/writing to buffers.
   */
  void search(DoipServerRequest req, DoipServerResponse resp) throws DoipException, IOException;

  /**
   * Validate provided document with referenced schema.
   * @param req DOIP request 
   * @param resp DOIP response
   * @throws DoipException Call is invalid
   * @throws IOException Error while reading/writing to buffers.
   */
  void validate(DoipServerRequest req, DoipServerResponse resp) throws DoipException, IOException;

  /**
   * Retrieve a single resource and/or element(s). The requests identifies the
   * resource by the 'targetId'. A single element is addressed by providing an
   * attribute 'element' having a value denoting the relative path of an
   * element. The path might map to a virtual folder matching multiple elements.
   * If a specific element is addressed, only the element data is returned.
   * Otherwise, the first segment contains the serialized data resource and
   * following segments may contain associated element's data if the attribute
   * 'includeElementData' is provided and has the value {@code true}.
   * @param req DOIP request 
   * @param resp DOIP response
   * @throws DoipException Call is invalid
   * @throws IOException Error while reading/writing to buffers.
   */
  void retrieve(DoipServerRequest req, DoipServerResponse resp) throws DoipException, IOException;
  /**
   * Update a single resource and/or element(s). The requests identifies the
   * resource by the 'targetId'. The input message contains the serialized
   * resource and payload, if desired. The update succeeds if the resource and
   * all provided payloads have been sent to the repository. As resource
   * metadata and payload are updated sequentially, it may happen, that the
   * resource is updated whereas one or more payloads are not due to an error.
   * In this case, NO rollback is performed but the resource remains in the
   * partly updated state.
   * @param req DOIP request 
   * @param resp DOIP response
   * @throws DoipException Call is invalid
   * @throws IOException Error while reading/writing to buffers.
   */
  void update(DoipServerRequest req, DoipServerResponse resp) throws DoipException, IOException;

  /**
   * Delete a single resource identified by the targetId in the request.
   * @param req DOIP request 
   * @param resp DOIP response
   * @throws DoipException Call is invalid
   * @throws IOException Error while reading/writing to buffers.
   */
  void delete(DoipServerRequest req, DoipServerResponse resp) throws DoipException, IOException;
  
  /**
   * List all operations supported for a particular object. Depending on the
   * addressed object, the list of supported operations may change. By default,
   * each object should support at least OP_LIST_OPERATIONS, OP_RETRIEVE,
   * OP_UPDATE and OP_DELETE.
   * @param req DOIP request 
   * @param resp DOIP response
   * @throws DoipException Call is invalid
   * @throws IOException Error while reading/writing to buffers.
   */
  void listOperationsForObject(String targetId, DoipServerRequest req, DoipServerResponse resp) throws DoipException, IOException;

 
  /**
   * Do a simple authentication test.
   *
   * @param doipReq Request
   * @param doipResp Response
   * @throws IOException Error writing to stream.
   */
  default void testAuthentication(DoipServerRequest doipReq, DoipServerResponse doipResp) throws IOException {
      LOGGER.warn("No authentication implemented yet!");
  }
}
