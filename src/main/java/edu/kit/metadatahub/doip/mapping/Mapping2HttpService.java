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
import edu.kit.metadatahub.doip.mapping.metadata.IMetadataMapper;
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
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import net.dona.doip.DoipConstants;
import net.dona.doip.InDoipSegment;
import net.dona.doip.client.DigitalObject;
import net.dona.doip.client.DoipException;
import net.dona.doip.client.Element;
import net.dona.doip.server.DoipServerRequest;
import net.dona.doip.server.DoipServerResponse;
import net.dona.doip.util.GsonUtility;
import net.dona.doip.util.InDoipMessageUtil;
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
  private static final Logger LOGGER = LoggerFactory.getLogger(Mapping2HttpService.class);

  private static final String ATTRIBUTE_ALL_ELEMENTS = "includeElementData";
  private static final String ATTRIBUTE_ELEMENT = "element";

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
    // Configuration needed:
    // - RequestUrl: http://localhost:8040/api/v1/schemas
    // - Accept mimetype: "application/json"
    // Mapper for datacite to proprietary metadata and vice versa: "edu.kit.metadatahub.doip.mapping.SchemaRecordMapper"
    // - POST
    //   - param for schema: "schema'
    //   - param for metadata: "record"
    // - Response class: SchemaRecordSchema.class
    // Define placeholders:
    // base URL
    String baseUrl = "http://localhost:8040/api/v1/schemas";
    String acceptType = "application/json";
    SimpleServiceClient simpleClient = SimpleServiceClient.create(baseUrl);
    simpleClient.accept(MediaType.parseMediaType(acceptType));
    String httpVerb = "POST";
    String bodyParam4Schema = "schema";
    String bodyParam4Metadata = "record";
    String[] header = {"ETag"};
    String metadataClassName = "edu.kit.turntable.mapping.SchemaRecordSchema";
    String mapperClassName = "edu.kit.metadatahub.doip.mapping.metadata.impl.SchemaRecordMapper";
    Object metadataMapper = null;
    Class<?> metadataClass = null;
    try {
      metadataClass = Class.forName(metadataClassName);
      metadataMapper = Class.forName(mapperClassName).getDeclaredConstructor().newInstance();
    } catch (ClassNotFoundException ex) {
      LOGGER.error(null, ex);
    } catch (NoSuchMethodException ex) {
      LOGGER.error(null, ex);
    } catch (Exception ex) {
      LOGGER.error(null, ex);
    }

    // For this example we use a POST
    // add entries to form
    for (String key : streamMap.keySet()) {
      LOGGER.trace("Found stream: '{}' -> '{}'", key, streamMap.get(key).length);
    }
      Map<String, String> container = new HashMap<>();
    if (header != null) {
      for (String attr : header) {
        LOGGER.trace("Add header: '{}'", attr);
        String value = null;
        if ((digitalObject.attributes != null) &&(digitalObject.attributes.getAsJsonObject("header") != null) &&(!digitalObject.attributes.getAsJsonObject("header").get(attr).isJsonNull())) {
          value = digitalObject.attributes.getAsJsonObject("header").get(attr).getAsString();
          if (value != null) {
           LOGGER.trace("Add header: '{}'= '{}'", attr, value);
            simpleClient.withHeader(attr, value);
          }
        }
        LOGGER.trace("Add key for collecting header: '{}'", attr);
        container.put(attr, value);
      }
      simpleClient.collectResponseHeader(container);
    }
    InputStream documentStream = new ByteArrayInputStream(streamMap.get("schema"));
    simpleClient.withFormParam(bodyParam4Schema, documentStream);
    simpleClient.withFormParam(bodyParam4Metadata, metadata);
    // post form and get HTTP status
    HttpStatus resource = simpleClient.postForm(); //Resource(srs, SchemaRecordSchema.class);

    if (resource.is2xxSuccessful()) {

      // get response as object
      Object responseBody;
      responseBody = simpleClient.getResponseBody(metadataClass);
      // After registration reference for PID should be set...
      handleManager.editHandle(pid.getIdentifier(), metadata.getSchemaDocumentUri());
      datacite = ((IMetadataMapper) metadataMapper).mapToDatacite(responseBody);

      LOGGER.trace("Build response...");
      DigitalObject dobj = new DigitalObject();
      dobj.id = datacite.getIdentifiers().iterator().next().getIdentifier();
      if (dobj.attributes == null) {
        dobj.attributes = new JsonObject();
      }
      dobj.attributes.add(DoipUtil.ATTR_DATACITE, GsonUtility.getGson().toJsonTree(datacite));
      dobj.type = DoipUtil.TYPE_DO;
      dobj.elements = digitalObject.elements;
      JsonObject restHeader = new JsonObject();
      for (String items : container.keySet()) {
        restHeader.addProperty(items, container.get(items));
      }
      dobj.attributes.add("header", restHeader);
      JsonElement dobjJson = GsonUtility.getGson().toJsonTree(dobj);
      LOGGER.trace("Writing DigitalObject to output message.");
      resp.writeCompactOutput(dobjJson);
      resp.setStatus(DoipConstants.STATUS_OK);
      resp.setAttribute(DoipConstants.MESSAGE_ATT, "Successfully created!");
    } else {
      String status = null;
      switch(resource) {
        case BAD_REQUEST:
              status = DoipConstants.STATUS_BAD_REQUEST;
              break;
        case UNAUTHORIZED:
              status = DoipConstants.STATUS_UNAUTHENTICATED;
              break;
        case CONFLICT:
              status = DoipConstants.STATUS_CONFLICT;
              break;
        case FORBIDDEN:
              status = DoipConstants.STATUS_FORBIDDEN;
              break;
        case NOT_FOUND:
              status = DoipConstants.STATUS_NOT_FOUND;
              break;
        default:
              status = DoipConstants.STATUS_ERROR;
      }
      resp.setStatus(status);
      resp.setAttribute(DoipConstants.MESSAGE_ATT, resource.getReasonPhrase());
    }
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
    LOGGER.debug("Repo: Retrieve...");
    // Check for correct syntax...
    InDoipSegment firstSegment = InDoipMessageUtil.getFirstSegment(req.getInput());
    if (firstSegment != null) {
      LOGGER.error("First segment is not null!?");
      resp.setStatus(DoipConstants.STATUS_BAD_REQUEST);
      resp.setAttribute(DoipConstants.MESSAGE_ATT, "Input is not allowed for retrieving a digital object!");
    }
    DigitalObject digitalObject = new DigitalObject();
    digitalObject.elements = new ArrayList<>();
    digitalObject.id = req.getTargetId();
    // Get request using mapping. (Todo) 
    // As DOIP provide all elements of DO there is one configuration for each element.
    // Example code for mapping to schema registry of metastore!
    // Configuration needed:
    // - element "schema"
    //   - RequestUrl: http://localhost:8040/api/v1/schemas/{schemaId}
    //     - Accept mimetype: "application/json"
    // - element "metadata"
    //   - RequestUrl: http://localhost:8040/api/v1/schemas/{schemaId}
    //     - Accept mimetype: "application/vnd.datamanager.schema-record+json"
    //     - Response class: SchemaRecordSchema.class
    //     - Mapper for datacite to proprietary metadata and vice versa: "edu.kit.metadatahub.doip.mapping.SchemaRecordMapper"
    // - GET
    //    element: schema
    //   - param for schema: "schema'
    //   - param for metadata: "record"
    // Define placeholders:
    // base URL
    // Input is not allowed for retrieve
    String[] allElements = {"schema", "metadata"};
    String[] baseUrl = {"http://localhost:8040/api/v1/schemas/{targetId}", "http://localhost:8040/api/v1/schemas/{targetId}"};
    String[] acceptMimetype = {"text/plain", "application/vnd.datamanager.schema-record+json"};
    String[] responseClass = {null, ""};
    String[] metadataClassName = {null, "edu.kit.turntable.mapping.SchemaRecordSchema"};
    String[] mapperClassName = {null, "edu.kit.metadatahub.doip.mapping.metadata.impl.SchemaRecordMapper"};
    String[][] header = {{"ETag"},{"ETag"}};
    String[] selectedElements = new String[0];
    if (req.getAttribute(ATTRIBUTE_ALL_ELEMENTS) != null && req.getAttribute(ATTRIBUTE_ALL_ELEMENTS).getAsBoolean()) {
      selectedElements = allElements;
    }
    if (req.getAttribute(ATTRIBUTE_ELEMENT) != null) {
      selectedElements = new String[1];
      selectedElements[0] = req.getAttributeAsString(ATTRIBUTE_ELEMENT);
    }
    Object metadataMapper = null;
    Class<?> metadataClass = null;
   for (String element : selectedElements) {
      int index;
      boolean elementFound = false;
      for (index = 0; index < allElements.length; index++) {
        if (allElements[index].equals(element)) {
          elementFound = true;
          break;
        }
      }
      if (!elementFound) { 
        continue;
      }
      // First of all get targetId.
      String targetId = req.getTargetId();
      // Replace targetId in URL
      baseUrl[index] = baseUrl[index].replace("{targetId}", URLEncoder.encode(targetId, Charset.forName("UTF-8")));
      LOGGER.trace("Retrieve element '{}' from '{}'.", element, baseUrl[index]);

      try {
        metadataClass = null;
        if (metadataClassName[index] != null) {
          metadataClass = Class.forName(metadataClassName[index]);
          metadataMapper = Class.forName(mapperClassName[index]).getDeclaredConstructor().newInstance();
        }
      } catch (ClassNotFoundException ex) {
        LOGGER.error(null, ex);
      } catch (NoSuchMethodException ex) {
        LOGGER.error(null, ex);
      } catch (Exception ex) {
        LOGGER.error(null, ex);
      }
      SimpleServiceClient simpleClient = SimpleServiceClient.create(baseUrl[index]);
      if (acceptMimetype[index] != null) {
        simpleClient.accept(MediaType.parseMediaType(acceptMimetype[index]));
      }
      Map<String, String> container = new HashMap<>();
      if (header[index] != null) {
        for (String attr : header[index]) {
          LOGGER.trace("Add header: '{}'", attr);
          String value = null;
          if ((digitalObject.attributes != null) &&(digitalObject.attributes.getAsJsonObject("header") != null) &&(!digitalObject.attributes.getAsJsonObject("header").get(attr).isJsonNull())) {
            value = digitalObject.attributes.getAsJsonObject("header").get(attr).getAsString();
            if (value != null) {
             LOGGER.trace("Add header: '{}'= '{}'", attr, value);
              simpleClient.withHeader(attr, value);
            }
          }
          LOGGER.trace("Add key for collecting header: '{}'", attr);
          container.put(attr, value);
        }
        simpleClient.collectResponseHeader(container);
      }
      Object response;
      if (metadataClass != null) {
        Object responseBody = simpleClient.getResource(metadataClass);
        Datacite43Schema datacite = ((IMetadataMapper) metadataMapper).mapToDatacite(responseBody);
        response = datacite;
      } else {
        response = simpleClient.getResource(String.class);
      }
      // GET response and HTTP status
      HttpStatus resource = simpleClient.getResponseStatus();

      if (!resource.is2xxSuccessful()) {
        String status = null;
        switch(resource) {
          case BAD_REQUEST:
                status = DoipConstants.STATUS_BAD_REQUEST;
                break;
          case UNAUTHORIZED:
                status = DoipConstants.STATUS_UNAUTHENTICATED;
                break;
          case CONFLICT:
                status = DoipConstants.STATUS_CONFLICT;
                break;
          case FORBIDDEN:
                status = DoipConstants.STATUS_FORBIDDEN;
                break;
          case NOT_FOUND:
                status = DoipConstants.STATUS_NOT_FOUND;
                break;
          default:
                status = DoipConstants.STATUS_ERROR;
        }
        resp.setStatus(status);
        resp.setAttribute(DoipConstants.MESSAGE_ATT, resource.getReasonPhrase());
        break;
      }
      // Add response to response object
      Element doipElement = new Element();
      doipElement.id = element;
      if (response instanceof String) {
        doipElement.in = new ByteArrayInputStream(((String) response).getBytes());
      } else {
        JsonElement jsonElement = GsonUtility.getGson().toJsonTree(response);
        LOGGER.trace("Writing DigitalObject to output message.");
        doipElement.in = new ByteArrayInputStream(jsonElement.toString().getBytes());
      }
      digitalObject.elements.add(doipElement);
//      writeElementToOutput(resp, doipElement);
      if (digitalObject.attributes == null) {
        digitalObject.attributes = new JsonObject();
      }
      JsonObject restHeader = new JsonObject();
      if (digitalObject.attributes.get("header") != null) {
        restHeader = digitalObject.attributes.getAsJsonObject("header");
      }
      for (String items : container.keySet()) {
        restHeader.addProperty(items, container.get(items));
      }
      digitalObject.attributes.add("header", restHeader);
    }
//    Element doipElement = new Element();
//    doipElement.id = "dummy";
//    doipElement.in = new ByteArrayInputStream("nothing".getBytes());
//    writeElementToOutput(resp, doipElement);
    JsonElement digitalObjectAsJson = GsonUtility.getGson().toJsonTree(digitalObject);
    LOGGER.debug("JSON element: '{}'", digitalObjectAsJson.toString());
    resp.getOutput().writeJson(digitalObjectAsJson);
    // attach elements
    for  (Element singleElement : digitalObject.elements) {
      writeElementToOutput(resp, singleElement);
    }
  
    resp.setStatus(DoipConstants.STATUS_OK);
    resp.getOutput().close();
    resp.commit();
    LOGGER.debug("Finished retrieve!");
  }

  @Override
  public void update(DoipServerRequest req, DoipServerResponse resp) throws DoipException, IOException {
    LOGGER.debug("Repo: Update...");
    DoipUtil doipUtil = new DoipUtil();
    DigitalObject digitalObject = doipUtil.getDigitalObject(req);
    Map<String, byte[]> streamMap = DoipUtil.getStreams(req);
    Datacite43Schema datacite = doipUtil.getDatacite(req);

    // There should be an implementation class inside the mapping...
    SchemaRecordSchema metadata = metadataMapper.mapFromDatacite(datacite);

    // Build Request using mapping. (Todo) 
    // Example code for mapping to schema registry of metastore!
    // Configuration needed:
    // - RequestUrl: http://localhost:8040/api/v1/schemas
    // - Accept mimetype: "application/json"
    // Mapper for datacite to proprietary metadata and vice versa: "edu.kit.metadatahub.doip.mapping.SchemaRecordMapper"
    // - POST
    //   - param for schema: "schema'
    //   - param for metadata: "record"
    // - Response class: SchemaRecordSchema.class
    // Define placeholders:
    // base URL
    String baseUrl = "http://localhost:8040/api/v1/schemas/{targetId}";
    String acceptType = "application/json";
    String httpVerb = "PUT";
    String[] header = {"If-Match"};
    String bodyParam4Schema = "schema";
    String bodyParam4Metadata = "record";
    String metadataClassName = "edu.kit.turntable.mapping.SchemaRecordSchema";
    String mapperClassName = "edu.kit.metadatahub.doip.mapping.metadata.impl.SchemaRecordMapper";
    Object metadataMapper = null;
    Class<?> metadataClass = null;
    try {
      metadataClass = Class.forName(metadataClassName);
      metadataMapper = Class.forName(mapperClassName).getDeclaredConstructor().newInstance();
    } catch (ClassNotFoundException ex) {
      LOGGER.error(null, ex);
    } catch (NoSuchMethodException ex) {
      LOGGER.error(null, ex);
    } catch (Exception ex) {
      LOGGER.error(null, ex);
    }
       // First of all get targetId.
      String targetId = req.getTargetId();
      // Replace targetId in URL
     baseUrl = baseUrl.replace("{targetId}", URLEncoder.encode(targetId, Charset.forName("UTF-8")));
    SimpleServiceClient simpleClient = SimpleServiceClient.create(baseUrl);
    simpleClient.accept(MediaType.parseMediaType(acceptType));

    // For this example we use a POST
    // add entries to form
    for (String key : streamMap.keySet()) {
      LOGGER.trace("Found stream: '{}' -> '{}'", key, streamMap.get(key).length);
    }
    InputStream documentStream = new ByteArrayInputStream(streamMap.get("schema"));
      Map<String, String> container = new HashMap<>();
    if (header != null) {
      for (String attr : header) {
        LOGGER.trace("Add header: '{}'", attr);
        String value = null;
        if ((digitalObject.attributes != null) &&(digitalObject.attributes.getAsJsonObject("header") != null) &&(!digitalObject.attributes.getAsJsonObject("header").get(attr).isJsonNull())) {
          value = digitalObject.attributes.getAsJsonObject("header").get(attr).getAsString();
          if (value != null) {
            simpleClient.withHeader(attr, value);
          }
        }
        LOGGER.trace("Add key for collecting header: '{}'= '{}'", attr, value);
        container.put(attr, value);
      }
      simpleClient.collectResponseHeader(container);
    }
    simpleClient.withFormParam(bodyParam4Schema, documentStream);
    simpleClient.withFormParam(bodyParam4Metadata, metadata);
    // post form and get HTTP status
    HttpStatus resource = simpleClient.putForm(); //Resource(srs, SchemaRecordSchema.class);

    if (resource.is2xxSuccessful()) {

      // get response as object
      Object responseBody;
      responseBody = simpleClient.getResponseBody(metadataClass);
      datacite = ((IMetadataMapper) metadataMapper).mapToDatacite(responseBody);

      LOGGER.trace("Build response...");
      DigitalObject dobj = new DigitalObject();
      dobj.id = datacite.getIdentifiers().iterator().next().getIdentifier();
      if (dobj.attributes == null) {
        dobj.attributes = new JsonObject();
      }
      dobj.attributes.add(DoipUtil.ATTR_DATACITE, GsonUtility.getGson().toJsonTree(datacite));
      JsonObject restHeader = new JsonObject();
      if (dobj.attributes.get("header") != null) {
        restHeader = dobj.attributes.getAsJsonObject("header");
      }
      for (String items : container.keySet()) {
        restHeader.addProperty(items, container.get(items));
      }
      dobj.attributes.add("header", restHeader);
    
      dobj.type = DoipUtil.TYPE_DO;
      dobj.elements = digitalObject.elements;
      JsonElement dobjJson = GsonUtility.getGson().toJsonTree(dobj);
      LOGGER.trace("Writing DigitalObject to output message.");
      resp.writeCompactOutput(dobjJson);
      resp.setStatus(DoipConstants.STATUS_OK);
      resp.setAttribute(DoipConstants.MESSAGE_ATT, "Successfully created!");
    } else {
      String status = null;
      switch(resource) {
        case BAD_REQUEST:
              status = DoipConstants.STATUS_BAD_REQUEST;
              break;
        case UNAUTHORIZED:
              status = DoipConstants.STATUS_UNAUTHENTICATED;
              break;
        case CONFLICT:
              status = DoipConstants.STATUS_CONFLICT;
              break;
        case FORBIDDEN:
              status = DoipConstants.STATUS_FORBIDDEN;
              break;
        case NOT_FOUND:
              status = DoipConstants.STATUS_NOT_FOUND;
              break;
        default:
              status = DoipConstants.STATUS_ERROR;
      }
      resp.setStatus(status);
      resp.setAttribute(DoipConstants.MESSAGE_ATT, resource.getReasonPhrase());
    }
    LOGGER.trace("Returning from update().");
  }

  @Override
  public void delete(DoipServerRequest req, DoipServerResponse resp) throws DoipException, IOException {
    throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
  }

  @Override
  public void listOperationsForObject(String targetId, DoipServerRequest req, DoipServerResponse resp) throws DoipException, IOException {
    throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
  }

  private void writeElementToOutput(DoipServerResponse resp, Element element) throws IOException {
    byte[] file = element.in.readAllBytes();
    if (element.in != null) {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("Add stream for Element: '{}'", element.id);
        LOGGER.debug("No of Bytes: " + file.length);
        LOGGER.debug("Content: " + new String(file));
      }
      JsonObject json = new JsonObject();
      json.addProperty("id", element.id);
      resp.getOutput().writeJson(json);
      resp.getOutput().writeBytes(file);
    }

  }
}
