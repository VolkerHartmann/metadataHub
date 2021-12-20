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

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import edu.kit.metadatahub.doip.handle.IHandleManager;
import edu.kit.metadatahub.doip.handle.impl.HandleMockup;
import edu.kit.metadatahub.doip.mapping.metadata.impl.SchemaRecordMapper;
import edu.kit.metadatahub.doip.server.util.DoipUtil;
import edu.kit.rest.util.SimpleServiceClient;
import edu.kit.turntable.mapping.Datacite43Schema;
import edu.kit.turntable.mapping.MappingSchema;
import edu.kit.turntable.mapping.Pid;
import edu.kit.turntable.mapping.SchemaRecordSchema;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import net.dona.doip.DoipConstants;
import net.dona.doip.client.DigitalObject;
import net.dona.doip.client.DoipException;
import net.dona.doip.server.DoipServerRequest;
import net.dona.doip.server.DoipServerResponse;
import net.dona.doip.util.GsonUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

/**
 * Generic Mapping service from DOIP to HTTP. For adaptions to an existing
 * service the mapping file has to be used.
 *
 */
public class Mapping2HttpService implements IMappingInterface {

  /**
   * Logger.
   */
  private final static Logger LOGGER = LoggerFactory.getLogger(Mapping2HttpService.class);
  MappingSchema mappingSchema;

  IHandleManager handleManager;

  SchemaRecordMapper metadataMapper = new SchemaRecordMapper();

  @Override
  public void initMapping(MappingSchema mapping) {
    mappingSchema = mapping;
    handleManager = new HandleMockup();

  }

  @Override
  public void listOperationsForService(DoipServerRequest req, DoipServerResponse resp) throws DoipException, IOException {
    throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
  }

  @Override
  public void create(DoipServerRequest req, DoipServerResponse resp) throws DoipException, IOException {
    LOGGER.debug("Repo: Create...");
    DoipUtil doipUtil = new DoipUtil();
    DigitalObject digitalObject = doipUtil.getDigitalObject(req);
    Map<String, byte[]> streamMap = DoipUtil.getStreams(req);
    Datacite43Schema datacite = doipUtil.getDatacite(req);

    // There should be an implementation class inside the mapping...
    SchemaRecordSchema metadata = metadataMapper.mapFromDatacite(datacite);
    // for Metastore handle is created outside (yet)
    Pid pid = new Pid();
    pid.setIdentifier(handleManager.createHandle());
    pid.setIdentifierType("HANDLE");
    metadata.setPid(pid);

    // Build Request using mapping. (Todo) 
    // Example code for mapping to schema registry of metastore!
    SimpleServiceClient simpleClient = SimpleServiceClient.create("http://localhost:8040/api/v1/schemas");
    simpleClient.accept(MediaType.parseMediaType("application/json"));
    // For this example we use a POST
    // add entries to form
    for (String key : streamMap.keySet()) {
      LOGGER.trace("Found stream: '{}' -> '{}'", key, streamMap.get(key).length);
    }
    InputStream documentStream = new ByteArrayInputStream(streamMap.get("schema"));
    simpleClient.withFormParam("schema", documentStream);
    simpleClient.withFormParam("record", metadata);
    // post form and get HTTP status
    HttpStatus resource = simpleClient.postForm(); //Resource(srs, SchemaRecordSchema.class);
    // get response as object
    metadata = simpleClient.getResponseBody(SchemaRecordSchema.class);
    // After registration reference for PID should be set...
    handleManager.editHandle(pid.getIdentifier(), metadata.getSchemaDocumentUri());
    datacite = metadataMapper.mapToDatacite(metadata);

    LOGGER.trace("Build response...");
    DigitalObject dobj = new DigitalObject();
    dobj.id = datacite.getIdentifiers().iterator().next().getIdentifier();
    if (dobj.attributes == null) {
      dobj.attributes = new JsonObject();
    }
    dobj.attributes.add(DoipUtil.ATTR_DATACITE, GsonUtility.getGson().toJsonTree(datacite));
    dobj.type = DoipUtil.TYPE_DO;
    dobj.elements = digitalObject.elements;
    JsonElement dobjJson = GsonUtility.getGson().toJsonTree(dobj);
    LOGGER.trace("Writing DigitalObject to output message.");
    resp.writeCompactOutput(dobjJson);
    resp.setStatus(DoipConstants.STATUS_OK);
    resp.setAttribute(DoipConstants.MESSAGE_ATT, "Successfully created!");
    LOGGER.trace("Returning from create().");
  }

  @Override
  public void search(DoipServerRequest req, DoipServerResponse resp) throws DoipException, IOException {
    throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
  }

  @Override
  public void validate(DoipServerRequest req, DoipServerResponse resp) throws DoipException, IOException {
    throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
  }

  @Override
  public void retrieve(DoipServerRequest req, DoipServerResponse resp) throws DoipException, IOException {
    throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
  }

  @Override
  public void update(DoipServerRequest req, DoipServerResponse resp) throws DoipException, IOException {
    throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
  }

  @Override
  public void delete(DoipServerRequest req, DoipServerResponse resp) throws DoipException, IOException {
    throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
  }

  @Override
  public void listOperationsForObject(String targetId, DoipServerRequest req, DoipServerResponse resp) throws DoipException, IOException {
    throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
  }

}
