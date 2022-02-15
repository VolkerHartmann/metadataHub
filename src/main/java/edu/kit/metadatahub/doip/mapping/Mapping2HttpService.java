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
import edu.kit.turntable.mapping.HttpCall;
import edu.kit.turntable.mapping.HttpMapping;
import edu.kit.turntable.mapping.Pid;
import edu.kit.turntable.mapping.SchemaRecordSchema;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.dona.doip.DoipConstants;
import net.dona.doip.client.DigitalObject;
import net.dona.doip.client.DoipException;
import net.dona.doip.client.Element;
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
  private static final Logger LOGGER = LoggerFactory.getLogger(Mapping2HttpService.class);

  private static final String ATTRIBUTE_ALL_ELEMENTS = "includeElementData";
  private static final String ATTRIBUTE_ELEMENT = "element";

  HttpMapping mappingSchema;

  IHandleManager handleManager;

  @Override
  public void initMapping(HttpMapping mapping) {
    mappingSchema = mapping;
    LOGGER.trace("Initialise mapping service with mapping: '{}'", mappingSchema);
    handleManager = new HandleMockup();
  }

  @Override
  public void listOperationsForService(DoipServerRequest req, DoipServerResponse resp) throws DoipException, IOException {
    throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
  }

  @Override
  public void create(DoipServerRequest req, DoipServerResponse resp) throws DoipException, IOException {
    LOGGER.debug("Repo: Create...");
    doRestCall(req, resp, mappingSchema.getMappings().getDoipOpCreate());
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
    LOGGER.debug("Repo: Retrieve...");
    boolean retrieveElementOnly = false;
    // Check for correct syntax...
    DoipUtil doipUtil = new DoipUtil(req);
    if (doipUtil.getDigitalObject() != null) {
      LOGGER.error("First segment is not null!?");
      resp.setStatus(DoipConstants.STATUS_BAD_REQUEST);
      resp.setAttribute(DoipConstants.MESSAGE_ATT, "Input is not allowed for retrieving a digital object!");
      throw new DoipException(DoipConstants.MESSAGE_ATT, "Input is not allowed for retrieving a digital object!");
    }
    Set<String> elementSet = new HashSet<>();
    for (HttpCall call : mappingSchema.getMappings().getDoipOpRetrieve()) {
      elementSet.add(call.getLabel());
    }
    String[] allElements = elementSet.toArray(new String[1]);
    DigitalObject digitalObject = new DigitalObject();
    digitalObject.elements = new ArrayList<>();
    digitalObject.id = doipUtil.getTargetId();
    String[] selectedElements = new String[1];
    selectedElements[0] = "metadata";
    boolean retrieveNoElements = true;
    if (req.getAttribute(ATTRIBUTE_ALL_ELEMENTS) != null && req.getAttribute(ATTRIBUTE_ALL_ELEMENTS).getAsBoolean()) {
      selectedElements = allElements;
      retrieveNoElements = false;
    }
    if (req.getAttribute(ATTRIBUTE_ELEMENT) != null) {
      retrieveElementOnly = true;
      selectedElements = new String[1];
      selectedElements[0] = req.getAttributeAsString(ATTRIBUTE_ELEMENT);
      retrieveNoElements = false;
    }
    // Collect all elements.
    List<HttpCall> httpCall = new ArrayList<>();
    for (String element : selectedElements) {
      for (HttpCall singleCall : mappingSchema.getMappings().getDoipOpRetrieve()) {
        if (singleCall.getLabel().equals(element)) {
          httpCall.add(singleCall);
          break;
        }
      }
    }
    // Fetch all elements 
    DigitalObject collectDigitalObject = new DigitalObject();
    for (HttpCall restCall : httpCall) {
      HttpStatus resource = doPartialRestCall(doipUtil, collectDigitalObject, restCall);
    }
    JsonElement digitalObjectAsJson = GsonUtility.getGson().toJsonTree(collectDigitalObject);
    LOGGER.debug("JSON element: '{}'", digitalObjectAsJson.toString());
    if (retrieveElementOnly) {
      LOGGER.trace("Write element directly to output...");
      resp.getOutput().writeBytes(collectDigitalObject.elements.get(0).in);
    } else {
      resp.getOutput().writeJson(digitalObjectAsJson);
      // attach elements
      if (!retrieveNoElements) {
        for (Element singleElement : collectDigitalObject.elements) {
          writeElementToOutput(resp, singleElement);
        }
      } else {
        collectDigitalObject.elements = new ArrayList<>();
      }
    }
    resp.setStatus(DoipConstants.STATUS_OK);
    resp.setAttribute(DoipConstants.MESSAGE_ATT, "Successfully submitted!");
    resp.getOutput().close();
    resp.commit();
    LOGGER.debug("Finished retrieve!");
  }

  @Override
  public void update(DoipServerRequest req, DoipServerResponse resp) throws DoipException, IOException {
    LOGGER.debug("Repo: Update...");

    doRestCall(req, resp, mappingSchema.getMappings().getDoipOpUpdate());
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
    byte[] elementContent = element.in.readAllBytes();
    if (element.in != null) {
      if (LOGGER.isDebugEnabled()) {
        LOGGER.debug("Add stream for Element: '{}'", element.id);
        LOGGER.debug("No of Bytes: " + elementContent.length);
        LOGGER.debug("Content: " + new String(elementContent));
      }
      JsonObject json = new JsonObject();
      json.addProperty("id", element.id);
      resp.getOutput().writeJson(json);
      resp.getOutput().writeBytes(elementContent);
    }

  }

  /**
   * Make REST call based on defined mapping.
   *
   * @param mapping
   * @return
   */
  private HttpStatus doRestCall(DoipServerRequest req, DoipServerResponse resp, HttpCall... mapping) throws DoipException, IOException {
    HttpStatus resource = null;
    LOGGER.debug("Repo: do REST call ...");
    DoipUtil doipUtil = new DoipUtil(req);
    DigitalObject collectDigitalObject = new DigitalObject();
    for (HttpCall singleMapping : mapping) {
      resource = doPartialRestCall(doipUtil, collectDigitalObject, singleMapping);
      evaluateHttpStatus(resource, resp);
      JsonElement dobjJson = GsonUtility.getGson().toJsonTree(collectDigitalObject);
      LOGGER.trace("Writing DigitalObject to output message.");
      resp.writeCompactOutput(dobjJson);
      resp.setStatus(DoipConstants.STATUS_OK);
    }
    LOGGER.trace("Returning from do REST call.");
    return resource;
  }

  /**
   * Make REST call based on defined mapping.
   *
   * @param mapping
   * @return
   */
  private HttpStatus doPartialRestCall(DoipUtil doipUtil, DigitalObject collectDigitalObject, HttpCall mapping) throws DoipException, IOException {
    HttpStatus resource = null;
    LOGGER.debug("Repo: prepare REST call ...");
    // First of all get targetId.
    String targetId = doipUtil.getTargetId();
    DigitalObject digitalObject = doipUtil.getDigitalObject();
    Map<String, byte[]> streamMap = doipUtil.getStreams();
    Datacite43Schema datacite = doipUtil.getDatacite();
    // There should be an implementation class inside the mapping...
//    SchemaRecordSchema metadata = metadataMapper.mapFromDatacite(datacite);
    // for Metastore handle is created outside (yet)
    Pid pid = new Pid();
    pid.setIdentifier(handleManager.createHandle());
    pid.setIdentifierType("HANDLE");
//    metadata.setPid(pid);

    String baseUrl = mapping.getRequestUrl();//"http://localhost:8040/api/v1/schemas";
    baseUrl = baseUrl.replace("{targetId}", URLEncoder.encode(targetId, Charset.forName("UTF-8")));
    LOGGER.trace("baseURL: '{}'", baseUrl);
    String acceptType = mapping.getMimetype(); //"application/json";

    SimpleServiceClient simpleClient = SimpleServiceClient.create(baseUrl);
    simpleClient.accept(MediaType.parseMediaType(acceptType));
    // Add authentication if available
    if (doipUtil.getAuthentication() != null) {
      JsonElement authentication = doipUtil.getAuthentication();
      LOGGER.trace("Authentication available: " + authentication.toString());
      if (authentication.isJsonObject()) {
        JsonObject object = (JsonObject) authentication;
        if (object.has("token")) {
          simpleClient.withBearerToken(object.get("token").getAsString());
        } else {
          LOGGER.warn("Only authorization via token supported yet!");
        }
      }
    }
    ///////////////////////////////////////////////////////////////
    // Prepare metadata
    ///////////////////////////////////////////////////////////////
    IMetadataMapper metadataMapper = null;
    Object metadata = datacite;
    if (mapping.getMetadata() != null) {
      try {
        metadataMapper = (IMetadataMapper) Class.forName(mapping.getMetadata().getMapperClass()).getDeclaredConstructor().newInstance();
        // There should be an implementation class inside the mapping...
        metadata = metadataMapper.mapFromDatacite(datacite);
        LOGGER.trace("Transformed datacite metadata to '{}'.", metadata.getClass());
      } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException | IllegalArgumentException | InstantiationException | SecurityException | InvocationTargetException ex) {
        LOGGER.error(null, ex);
      }
    }
    Class<?> metadataClassResponse = Datacite43Schema.class;
    IMetadataMapper metadataMapperResponse = null;
    if ((mapping.getResponse() != null)
            && (mapping.getResponse().getClassName() != null)) {
      LOGGER.trace("Get mapper for '{}'", mapping.getResponse().getMapperClass());
      LOGGER.trace("Get class for '{}'", mapping.getResponse().getClassName());
      try {
        metadataClassResponse = Class.forName(mapping.getResponse().getClassName());
        metadataMapperResponse = (IMetadataMapper) Class.forName(mapping.getResponse().getMapperClass()).getDeclaredConstructor().newInstance();
      } catch (ClassNotFoundException | NoSuchMethodException | SecurityException | InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
        LOGGER.error(null, ex);
      }
    }
    ///////////////////////////////////////////////////////////////
    // Prepare headers
    // header with values will be assigned to header send.
    // header without values will be assigned to collect headers.
    ///////////////////////////////////////////////////////////////
    Map<String, String> container = new HashMap<>();
    if (mapping.getHeader() != null) {
      for (String attr : mapping.getHeader().getAdditionalProperties().keySet()) {
        LOGGER.trace("Add header: '{}'", attr);
        String value = null;
        if ((digitalObject != null)
                && (digitalObject.attributes != null)
                && (digitalObject.attributes.getAsJsonObject("header") != null)
                && (digitalObject.attributes.getAsJsonObject("header").get(attr) != null)
                && (!digitalObject.attributes.getAsJsonObject("header").get(attr).isJsonNull())) {
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
    ///////////////////////////////////////////////////////////////
    // Prepare request
    ///////////////////////////////////////////////////////////////
    switch (mapping.getVerb()) {
      case "GET":
        collectDigitalObject.id = doipUtil.getTargetId();
        break;
      case "POST":
      case "PUT":
        // add entries to form
        for (String key : streamMap.keySet()) {
          LOGGER.trace("Found stream: '{}' -> '{}'", key, streamMap.get(key).length);
          String mappingKey = key;
          if (mapping.getBody().getAdditionalProperties().containsKey(key)) {
            mappingKey = mapping.getBody().getAdditionalProperties().get(key);
          }
          InputStream documentStream = new ByteArrayInputStream(streamMap.get("schema"));
          simpleClient.withFormParam(mappingKey, documentStream);
        }
        String mappingKey = DoipUtil.ID_METADATA;
        if (mapping.getBody() != null) {
          if (mapping.getBody().getMetadata() != null) {
            mappingKey = mapping.getBody().getMetadata();
          }
          simpleClient.withFormParam(mappingKey, metadata);
        }
        break;
      default:
        throw new DoipException(DoipConstants.STATUS_BAD_REQUEST, "Mapping verb is not correct!");

    }
    ///////////////////////////////////////////////////////////////
    // Make request
    ///////////////////////////////////////////////////////////////
    Object responseBody = null;
    switch (mapping.getVerb()) {
      case "GET":
        if (metadataMapperResponse != null) {
          responseBody = simpleClient.getResource(metadataClassResponse);
          if (!(responseBody instanceof Datacite43Schema)) {
            datacite = ((IMetadataMapper) metadataMapperResponse).mapToDatacite(responseBody);
          }
          responseBody = datacite;
        } else {
          responseBody = simpleClient.getResource(String.class);
        }
        resource = simpleClient.getResponseStatus();

        break;
      case "POST":
        // post form and get HTTP status
        resource = simpleClient.postForm(); //Resource(srs, SchemaRecordSchema.class);
        break;
      case "PUT":
        resource = simpleClient.putForm(); //Resource(srs, SchemaRecordSchema.class);
        break;
    }

    ///////////////////////////////////////////////////////////////
    // Collect response
    ///////////////////////////////////////////////////////////////
    if ((resource != null) && resource.is2xxSuccessful()) {
      // get response as object
      if (responseBody == null) {
        responseBody = simpleClient.getResponseBody(metadataClassResponse);
        if (metadataMapperResponse != null) {
          datacite = ((IMetadataMapper) metadataMapperResponse).mapToDatacite(responseBody);
        }
        responseBody = (Datacite43Schema) datacite;
      }
      LOGGER.trace("Build response...");
      if (collectDigitalObject.id == null) {
        collectDigitalObject.id = datacite.getIdentifiers().iterator().next().getIdentifier();
      }
      if (collectDigitalObject.attributes == null) {
        collectDigitalObject.attributes = new JsonObject();
      }
      collectDigitalObject.attributes.add(DoipUtil.ATTR_DATACITE, GsonUtility.getGson().toJsonTree(datacite));
      collectDigitalObject.type = DoipUtil.TYPE_DO;
      if (collectDigitalObject.elements == null) {
        collectDigitalObject.elements = new ArrayList<>();
      } //= digitalObject.elements;
      switch (mapping.getVerb()) {
        case "GET":
          LOGGER.trace("Add element '{}' to digital object. ", mapping.getLabel());
          // Add response to response object
          Element doipElement = new Element();
          doipElement.id = mapping.getLabel();
          if (responseBody instanceof String) {
            doipElement.in = new ByteArrayInputStream(((String) responseBody).getBytes());
          } else {
            JsonElement jsonElement = GsonUtility.getGson().toJsonTree(responseBody);
            LOGGER.trace("Writing DigitalObject to output message.");
            doipElement.in = new ByteArrayInputStream(jsonElement.toString().getBytes());
          }
          collectDigitalObject.elements.add(doipElement);
          break;
        default:
      }
      JsonObject restHeader = new JsonObject();
      if (collectDigitalObject.attributes.get("header") != null) {
        restHeader = collectDigitalObject.attributes.getAsJsonObject("header");
      }
      for (String items : container.keySet()) {
        restHeader.addProperty(items, container.get(items));
      }
      collectDigitalObject.attributes.add("header", restHeader);
    }
    LOGGER.trace("Returning digital object from REST call.");
    return resource;
  }

  private void evaluateHttpStatus(HttpStatus httpStatus, DoipServerResponse resp) throws DoipException {
    if ((httpStatus == null) || !httpStatus.is2xxSuccessful()) {
      // do some error handling
      String status = null;
      switch (httpStatus) {
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
      resp.setAttribute(DoipConstants.MESSAGE_ATT, httpStatus.getReasonPhrase());
      throw new DoipException(DoipConstants.MESSAGE_ATT, httpStatus.getReasonPhrase());
    }

  }
}
