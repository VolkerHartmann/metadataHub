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
package edu.kit.metadatahub.doip.server.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import edu.kit.turntable.mapping.Datacite43Schema;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Level;
import net.dona.doip.DoipConstants;
import net.dona.doip.InDoipSegment;
import net.dona.doip.client.DigitalObject;
import net.dona.doip.client.DoipException;
import net.dona.doip.server.DoipServerRequest;
import net.dona.doip.util.GsonUtility;
import net.dona.doip.util.InDoipMessageUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for generating, parsing the data from/to a DOIP object.
 */
public class DoipUtil {

  /**
   * Label of the attribute containing datacite (in JSON format) metadata about
   * the DO.
   */
  public static final String ATTR_DATACITE = "datacite";
  public static final String ID_SCHEMA = "schema";
  public static final String ID_METADATA = "metadata";
  public static final String ID_APPLICATION_PROFILE = "application_profile";
  /**
   * Label for the general DO type.
   */
  public static final String TYPE_DO = "0.TYPE/DO";

  private static final Logger LOGGER = LoggerFactory.getLogger(DoipUtil.class);

  private DigitalObject digitalObject = null;

  public DigitalObject getDigitalObject(DoipServerRequest req) throws DoipException, IOException {
    if (digitalObject == null) {
      InDoipSegment firstSegment = InDoipMessageUtil.getFirstSegment(req.getInput());
      LOGGER.trace("Deserializing digital object from first segment.");
      digitalObject = GsonUtility.getGson().fromJson(firstSegment.getJson(), DigitalObject.class);
    }
    return digitalObject;
  }

  public Datacite43Schema getDatacite(DoipServerRequest req) throws DoipException, IOException {
    digitalObject = getDigitalObject(req);
    JsonElement metadata = digitalObject.attributes.get(ATTR_DATACITE);
    LOGGER.debug(metadata.toString());
    LOGGER.debug(metadata.getAsString());
    Datacite43Schema resource = GsonUtility.getGson().fromJson(metadata.getAsString(), Datacite43Schema.class);

    return resource;
  }

  public static DigitalObject ofDataResource(Datacite43Schema resource) throws DoipException {
    net.dona.doip.client.DigitalObject digitalObject = new DigitalObject();
    digitalObject.id = resource.getIdentifiers().iterator().next().getIdentifier();
    digitalObject.type = "0.TYPE/DO";
    digitalObject.attributes = new JsonObject();
    JsonElement dobjJson = GsonUtility.getGson().toJsonTree(resource);
    digitalObject.attributes.add(ATTR_DATACITE, dobjJson);
    return digitalObject;
  }

  public static boolean getBooleanAttributeFromRequest(DoipServerRequest req, String att) {
    JsonElement el = req.getAttribute(att);
    if (el == null || !el.isJsonPrimitive()) {
      return false;
    }

    JsonPrimitive priv = el.getAsJsonPrimitive();
    if (priv.isBoolean()) {
      return priv.getAsBoolean();
    }
    if (priv.isString()) {
      return "true".equalsIgnoreCase(priv.getAsString());
    }
    return false;
  }

  public static Map<String, byte[]> getStreams(DoipServerRequest req) throws DoipException {
    Map<String, byte[]> streamMap = new HashMap<>();
    Iterator<InDoipSegment> iterator = req.getInput().iterator();
    while (iterator.hasNext()) {
      if (LOGGER.isTraceEnabled()) {
        LOGGER.trace("*************************************************************");
        LOGGER.trace("Next Segment.....");
        LOGGER.trace("*************************************************************");
      }
      InDoipSegment segment = iterator.next();
      if (segment.isJson() == false) {
        throw new DoipException(DoipConstants.STATUS_BAD_REQUEST, "Segment should be a JSON!");
      } else {
        try {
          // Read first part of segment which should contain JSON.
          // Read id of element
          LOGGER.trace("Content: '{}'", segment.getJson().toString());
          String id = segment.getJson().getAsJsonObject().get("id").getAsString();
          LOGGER.trace("ID: '{}'", id);

          segment = iterator.next();
          
          streamMap.put(id, segment.getInputStream().readNBytes(65536));
          segment.getInputStream().close();
        } catch (IOException ex) {
          throw new DoipException(DoipConstants.STATUS_BAD_REQUEST, "Error while reading JSON!");
        }
      }
    }
    for (String key: streamMap.keySet())
      LOGGER.trace("Found stream: '{}' -> '{}'", key, streamMap.get(key).length);
    return streamMap;
  }

}
