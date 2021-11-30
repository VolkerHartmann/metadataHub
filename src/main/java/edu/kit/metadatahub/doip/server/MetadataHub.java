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
package edu.kit.metadatahub.doip.server;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;
import java.io.File;
import java.io.FileReader;
import net.dona.doip.server.DoipServer;
import net.dona.doip.server.DoipServerConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * MetadataHub implemented based on the DOIP SDK using the Digital Object
 * Interface Protocol (DOIP). It's a facade for (multiple) implementations of
 * metadata repositories.
 */
public class MetadataHub { 

  /**
   * Logger for this class.
   */
  private static final Logger LOGGER = LoggerFactory.getLogger(MetadataHub.class);

  /**
   * @param args the command line arguments
   */
  public static void main(String[] args) throws Exception {
    // Define default configuration file.
    String configFile = "config/DoipConfiguration.json";

    // Test if there is an alternative config file defined.
    if (args.length > 1) {
      configFile = args[0];
    }
    // Test for config file
    if (!new File(configFile).exists()) {
      LOGGER.error("Config file '{}' is not available!", configFile);
      System.exit(1);
    }
    // Configure JSON parser for config file
    Gson gson = new GsonBuilder()
            .setPrettyPrinting()
            .create();
    JsonReader reader = new JsonReader(new FileReader(configFile));
    DoipServerConfig config = gson.fromJson(reader, DoipServerConfig.class);
    LOGGER.info("Found configuration: '{}'", gson.toJson(config));

    DoipServerConfig.TlsConfig tlsConfig = new DoipServerConfig.TlsConfig();
    tlsConfig.id = config.processorConfig.get("serviceId").getAsString();
    config.tlsConfig = tlsConfig;
    MetadataHubProcessor tdp = new MetadataHubProcessor();
    tdp.init(config.processorConfig);
    DoipServer server = new DoipServer(config, tdp);
    server.init();

    Runtime.getRuntime().addShutdownHook(new Thread(server::shutdown));
    LOGGER.info("Server is up and running...");
  }
}
