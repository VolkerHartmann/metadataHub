/*
 * Copyright 2019 Karlsruhe Institute of Technology.
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
package edu.kit.rest.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.kit.turntable.mapping.SchemaRecordSchema;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RequestCallback;
import org.springframework.web.client.ResponseExtractor;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

/**
 *
 * @author jejkal
 */
public class SimpleServiceClient {

  /**
   * Logger.
   */
  private final static Logger LOGGER = LoggerFactory.getLogger(SimpleServiceClient.class);
  /**
   * Define url path separator.
   */
  private static final String SLASH = "/";
  /**
   * Key for header field holding content range
   */
  private static final String CONTENT_RANGE = "Content-Range";
  /**
   * Key for header field holding location URI
   */
  private static final String LOCATION = "Location";
  /**
   * Key for header field holding ETag
   */
  private static final String ETAG = "ETag";

  private RestTemplate restTemplate = new RestTemplate();

  private final String resourceBaseUrl;
  private String resourcePath = null;
  private String bearerToken = null;
  private HttpHeaders headers;
  private Map<String, String> requestedResponseHeaders = null;
  // Contains the response of the request.
  private String responseBody;
  private HttpStatus responseStatus;

  MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
  MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();

  SimpleServiceClient(String resourceBaseUrl) {
    this.resourceBaseUrl = resourceBaseUrl;
    headers = new HttpHeaders();
  }

  /**
   * Set template for REST access.
   *
   * @param restTemplate Template for REST Access.
   */
  public void setRestTemplate(RestTemplate restTemplate) {
    this.restTemplate = restTemplate;

  }

  /**
   * Create service client.
   *
   * @param baseUrl Base URL of the service.
   * @return Service client.
   */
  public static SimpleServiceClient create(String baseUrl) {
    SimpleServiceClient client = new SimpleServiceClient(baseUrl);
    return client;
  }

  /**
   * Add bearer token to service client.
   *
   * @param bearerToken Bearer token.
   * @return Service client with authentication.
   */
  public SimpleServiceClient withBearerToken(String bearerToken) {
    this.bearerToken = bearerToken;
    if (bearerToken != null) {
      return withHeader("Authorization", "Bearer " + bearerToken);
    }
    headers.remove("Authorization");
    return this;
  }

  /**
   * Add header to service client.
   *
   * @param field Key of the header field.
   * @param value Value of the header field.
   * @return Service client with header.
   */
  public SimpleServiceClient withHeader(String field, String value) {
    this.headers.add(field, value);
    return this;
  }

  /**
   * Set accepted mimetypes.
   *
   * @param mediaType Array of valid mimetypes.
   * @return Service client with accept-Header.
   */
  public SimpleServiceClient accept(MediaType... mediaType) {
    headers.setAccept(Arrays.asList(mediaType));
    return this;
  }

  /**
   * Add map for response header.
   *
   * @param container Map for response header.
   * @return Service client.
   */
  public SimpleServiceClient collectResponseHeader(Map<String, String> container) {
    requestedResponseHeaders = container;
    return this;
  }

  /**
   * Set content type.
   *
   * @param contentType Content type.
   * @return Service client.
   */
  public SimpleServiceClient withContentType(MediaType contentType) {
    headers.setContentType(contentType);
    return this;
  }

  /**
   * Add path for resources.
   *
   * @param resourcePath Resource path.
   * @return Service client.
   */
  public SimpleServiceClient withResourcePath(String resourcePath) {
    LOGGER.warn("Creating SingleResourceAccessClient with resourcePath {}.", resourcePath);
    this.resourcePath = resourcePath;
    return this;
  }

  /**
   * Add form parameter.
   *
   * @param name Name of the parameter.
   * @param object Object containing parameter.
   * @return Service client.
   * @throws IOException Error while reading parameter.
   */
  public SimpleServiceClient withFormParam(String name, Object object) throws IOException {
    if (name == null || object == null) {
      throw new IllegalArgumentException("Form element key and value must not be null.");
    }
    if (object instanceof File) {
      body.add(name, new FileSystemResource((File) object));
    } else if (object instanceof InputStream) {
      body.add(name, new ByteArrayResource(IOUtils.toByteArray((InputStream) object)) {
        //overwriting filename required by spring (see https://medium.com/@voziv/posting-a-byte-array-instead-of-a-file-using-spring-s-resttemplate-56268b45140b)
        @Override
        public String getFilename() {
          return "stream#" + UUID.randomUUID().toString();
        }
      });
    } else {
      String metadataString = new ObjectMapper().writeValueAsString(object);
      LOGGER.warn("Adding argument from JSON document {}.", metadataString);
      body.add(name, new ByteArrayResource(metadataString.getBytes()) {
        //overwriting filename required by spring (see https://medium.com/@voziv/posting-a-byte-array-instead-of-a-file-using-spring-s-resttemplate-56268b45140b)
        @Override
        public String getFilename() {
          return "metadata#" + UUID.randomUUID().toString() + ".json";
        }
      });
    }
    return this;
  }

  /**
   * Add form parameter.
   *
   * @param name Name of the parameter.
   * @param string Object containing parameter.
   * @return Service client.
   * @throws IOException Error while reading parameter.
   */
  public SimpleServiceClient withFormParam(String name, String string) throws IOException {
    if (name == null || string == null) {
      throw new IllegalArgumentException("Form element key and value must not be null.");
    }
    String metadataString = string;
    LOGGER.warn("Adding argument from JSON document {}.", metadataString);
    body.add(name, new ByteArrayResource(metadataString.getBytes()) {
      //overwriting filename required by spring (see https://medium.com/@voziv/posting-a-byte-array-instead-of-a-file-using-spring-s-resttemplate-56268b45140b)
      @Override
      public String getFilename() {
        return "metadata#" + UUID.randomUUID().toString() + ".json";
      }
    });
    return this;
  }

  /**
   * Add query parameter.
   *
   * @param name Name of query parameter.
   * @param value Value of query parameter.
   * @return Service client.
   */
  public SimpleServiceClient withQueryParam(String name, String value) {
    queryParams.add(name, value);
    return this;
  }

  /**
   * Get Resource of response.
   *
   * @param <C> Type of response.
   * @param responseType Class of response.
   * @return Instance of response class.
   */
  public <C> C getResource(Class<C> responseType) {
    LOGGER.warn("Calling getResource().");
    String destinationUri = buildUri();
    UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromHttpUrl(destinationUri).queryParams(queryParams);
    LOGGER.warn("Obtaining resource from resource URI {}.", uriBuilder.toUriString());
    ResponseEntity<C> response = restTemplate.exchange(uriBuilder.toUriString(), HttpMethod.GET, new HttpEntity<>(headers), responseType);
    responseStatus = response.getStatusCode();
    collectResponseHeaders(response.getHeaders());
    LOGGER.warn("Request returned with status {}. Returning response body.", response.getStatusCodeValue());
    return response.getBody();
  }
//
//  /**
//   * Get multiple resources.
//   *
//   * @param <C> Type of response.
//   * @param responseType Class of response.
//   * @return Page holding all responses.
//   */
//  public <C> ResultPage<C> getResources(Class<C[]> responseType) {
//    LOGGER.warn("Calling getResource().");
//    String destinationUri = resourceBaseUrl + ((resourcePath != null) ? resourcePath : "");
//    UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromHttpUrl(destinationUri).queryParams(queryParams);
//    LOGGER.warn("Obtaining resource from resource URI {}.", uriBuilder.toUriString());
//    ResponseEntity<C[]> response = restTemplate.exchange(uriBuilder.toUriString(), HttpMethod.GET, new HttpEntity<>(headers), responseType);
//    LOGGER.warn("Request returned with status {}. Returning response body.", response.getStatusCodeValue());
//    ContentRange contentRange = ControllerUtils.parseContentRangeHeader(response.getHeaders().getFirst("Content-Range"));
//    collectResponseHeaders(response.getHeaders());
//    return new ResultPage<>(response.getBody(), contentRange);
//  }

//  /**
//   * Find resource using provided example.
//   *
//   * @param <C> Type of response.
//   * @param resource Example instance.
//   * @param responseType Class of response.
//   * @return Page holding all responses.
//   */
//  public <C> ResultPage<C> findResources(C resource, Class<C[]> responseType) {
//    LOGGER.warn("Calling getResource().");
//    String destinationUri = resourceBaseUrl + "search";
//    UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromHttpUrl(destinationUri).queryParams(queryParams);
//    LOGGER.warn("Obtaining resource from resource URI {}.", uriBuilder.toUriString());
//    ResponseEntity<C[]> response = restTemplate.exchange(uriBuilder.toUriString(), HttpMethod.POST, new HttpEntity<>(resource, headers), responseType);
//    LOGGER.warn("Request returned with status {}. Returning response body.", response.getStatusCodeValue());
//    ContentRange contentRange = ControllerUtils.parseContentRangeHeader(response.getHeaders().getFirst("Content-Range"));
//    collectResponseHeaders(response.getHeaders());
//    return new ResultPage<>(response.getBody(), contentRange);
//  }
  /**
   * Get resource.
   *
   * @param outputStream Outputstream for the resource.
   * @return Status.
   */
  public int getResource(OutputStream outputStream) {
    String sourceUri = buildUri();

    UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromHttpUrl(sourceUri).queryParams(queryParams);
    LOGGER.warn("Downloading content from source URI {}.", uriBuilder.toUriString());

    RequestCallback requestCallback = request -> {
      Set<Entry<String, List<String>>> entries = headers.entrySet();
      entries.forEach((entry) -> {
        request.getHeaders().addAll(entry.getKey(), entry.getValue());
      });
    };

    ResponseExtractor<ClientHttpResponse> responseExtractor = response -> {
      IOUtils.copy(response.getBody(), outputStream);

      return response;
    };

    ClientHttpResponse response = restTemplate.execute(uriBuilder.toUriString(), HttpMethod.GET, requestCallback, responseExtractor);
    int status = -1;
    try {
    responseStatus = response.getStatusCode();
      status = responseStatus.value();
      LOGGER.warn("Download returned with status {}.", status);
      collectResponseHeaders(response.getHeaders());
    } catch (IOException ex) {
      LOGGER.error("Failed to extract raw status from response.", ex);
    }
    return status;
  }

  /**
   * Post resource.
   *
   * @param <C> Type of response.
   * @param resource Instance to post.
   * @param responseType Class of response.
   * @return Posted resource.
   */
  public <C> C postResource(C resource, Class<C> responseType) {
    LOGGER.warn("Calling createResource(#DataResource).");

    String destinationUri = buildUri();
    UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromHttpUrl(destinationUri).queryParams(queryParams);

    LOGGER.warn("Sending POST request for resource.");
    ResponseEntity<C> response = restTemplate.exchange(uriBuilder.toUriString(), HttpMethod.POST, new HttpEntity<>(resource, headers), responseType);
    responseStatus = response.getStatusCode();
    LOGGER.warn("Request returned with status {}. Returning response body.", response.getStatusCodeValue());
    collectResponseHeaders(response.getHeaders());
    return response.getBody();
  }

  /**
   * Post form.
   *
   * @return Status of post.
   */
  public HttpStatus postForm() {
    return postForm(MediaType.MULTIPART_FORM_DATA);
  }

  /**
   * Post form with given content type.
   *
   * @param contentType Content type.
   * @return Status of post.
   */
  public HttpStatus postForm(MediaType contentType) {
    LOGGER.warn("Adding content type header with value {}.", contentType);
    headers.setContentType(contentType);

    String destinationUri = buildUri();

    UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromHttpUrl(destinationUri).queryParams(queryParams);

    LOGGER.warn("Uploading content to destination URI {}.", uriBuilder.toUriString());
    ResponseEntity<String> response = restTemplate.postForEntity(uriBuilder.toUriString(), new HttpEntity<>(body, headers), String.class);
    responseStatus = response.getStatusCode();
    LOGGER.warn("Upload returned with status {}.", response.getStatusCodeValue());
    responseBody = response.getBody();
    collectResponseHeaders(response.getHeaders());
    return responseStatus;

  }

  /**
   * Post form.
   *
   * @return Status of post.
   */
  public HttpStatus putForm() {
    return putForm(MediaType.MULTIPART_FORM_DATA);
  }

  /**
   * Post form with given content type.
   *
   * @param contentType Content type.
   * @return Status of post.
   */
  public HttpStatus putForm(MediaType contentType) {
    LOGGER.warn("Adding content type header with value {}.", contentType);
    headers.setContentType(contentType);
    // Check for eTag and set ifMatch if available
    String etag = headers.getFirst(ETAG);
    if (etag != null) {
      LOGGER.warn("Sending PUT request for resource with ETag {}.", etag);
      headers.setIfMatch(etag);
    }

    String destinationUri = buildUri();

    UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromHttpUrl(destinationUri).queryParams(queryParams);

    LOGGER.warn("Uploading content to destination URI {}.", uriBuilder.toUriString());
    ResponseEntity<String> response = restTemplate.exchange(uriBuilder.toUriString(), HttpMethod.PUT, new HttpEntity<>(body, headers), String.class);
    responseStatus = response.getStatusCode();
    LOGGER.warn("Upload returned with status {}.", response.getStatusCodeValue());
    responseBody = response.getBody();
    collectResponseHeaders(response.getHeaders());
    return responseStatus;

  }

  /**
   * Put resource.
   *
   * @param <C> Type of response.
   * @param resource Instance to put.
   * @param responseType Class of response.
   * @return Puted resource.
   */
  public <C> C putResource(C resource, Class<C> responseType) {
    LOGGER.warn("Calling updateResource(#DataResource).");

    String destinationUri = buildUri();
    UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromHttpUrl(destinationUri).queryParams(queryParams);
    LOGGER.warn("Obtaining resource from resource URI {}.", uriBuilder.toUriString());
    ResponseEntity<C> response = restTemplate.exchange(uriBuilder.toUriString(), HttpMethod.GET, new HttpEntity<>(headers), responseType);
    LOGGER.warn("Reading ETag from response header.");
    String etag = response.getHeaders().getFirst("ETag");
    LOGGER.warn("Sending PUT request for resource with ETag {}.", etag);
    headers.setIfMatch(etag);
    response = restTemplate.exchange(uriBuilder.toUriString(), HttpMethod.PUT, new HttpEntity<>(resource, headers), responseType);
    responseStatus = response.getStatusCode();
    collectResponseHeaders(response.getHeaders());
    LOGGER.warn("Request returned with status {}. Returning response body.", response.getStatusCodeValue());
    return response.getBody();
  }
//
//  /**
//   * Delete a resource. This call, if supported and authorized, should always
//   * return without result.
//   */
//  public void deleteResource() {
//    LOGGER.warn("Calling delete().");
//    String destinationUri = resourceBaseUrl + ((resourcePath != null) ? resourcePath : "");
//    UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromHttpUrl(destinationUri).queryParams(queryParams);
//    LOGGER.warn("Obtaining resource from resource URI {}.", uriBuilder.toUriString());
//    ResponseEntity<DataResource> response = restTemplate.exchange(uriBuilder.toUriString(), HttpMethod.GET, new HttpEntity<>(headers), DataResource.class);
//    LOGGER.warn("Reading ETag from response header.");
//    String etag = response.getHeaders().getFirst("ETag");
//    LOGGER.warn("Obtained ETag value {}.", etag);
//
//    LOGGER.warn("Sending DELETE request for resource with ETag {}.", etag);
//    headers.setIfMatch(etag);
//    response = restTemplate.exchange(uriBuilder.toUriString(), HttpMethod.DELETE, new HttpEntity<>(headers), DataResource.class);
//    collectResponseHeaders(response.getHeaders());
//    LOGGER.warn("Request returned with status {}. No response body expected.", response.getStatusCodeValue());
//  }

  /**
   * Collect all response headers.
   *
   * @param responseHeaders Response headers.
   */
  private void collectResponseHeaders(HttpHeaders responseHeaders) {
    if (LOGGER.isWarnEnabled()) {
      for (String keys : responseHeaders.keySet()) {
        LOGGER.warn("HTTP header: " + keys + " -> " + responseHeaders.get(keys));
      }
    }
    if (requestedResponseHeaders != null) {
      Set<Entry<String, String>> entries = requestedResponseHeaders.entrySet();

      entries.forEach((entry) -> {
        requestedResponseHeaders.put(entry.getKey(), responseHeaders.getFirst(entry.getKey()));
      });
    }
  }

  /**
   * Main method for quick tests.
   *
   * @param args Not used.
   */
  public static void main(String[] args) throws IOException {
    ///////////////////////////////////////////////////////////////////////
    // Create (Schema) Resource
    ///////////////////////////////////////////////////////////////////////
    // Example with posting a form with two entries:
    // 1. schema document with its assigned key 'schema'
    // 2. schema record with its assigned key 'record'
    // Accept "application/json"

    String schemaId = "test" + UUID.randomUUID();
    // If there are generated classes for your JSON you may use them directily.
    // Otherwise you have to create an approbriate  string 
    // Example for generated classes:
    // simple_example_schema.json (located in src/main/resources/json/schema) 
    // will generate a class edu.kit.turntable.mapping.SimpleExampleSchema
    SchemaRecordSchema srs = new SchemaRecordSchema();
    srs.setSchemaId(schemaId);
    srs.setType(SchemaRecordSchema.Type.XML);
    String schemaV1 = "<xs:schema targetNamespace=\"http://www.example.org/schema/xsd/\"\n"
            + "                xmlns=\"http://www.example.org/schema/xsd/\"\n"
            + "                xmlns:xs=\"http://www.w3.org/2001/XMLSchema\"\n"
            + "                elementFormDefault=\"qualified\" attributeFormDefault=\"unqualified\">\n"
            + "      <xs:element name=\"metadata\">\n"
            + "        <xs:complexType>\n"
            + "          <xs:sequence>\n"
            + "            <xs:element name=\"title\" type=\"xs:string\"/>\n"
            + "            <xs:element name=\"date\" type=\"xs:date\"/>\n"
            + "          </xs:sequence>\n"
            + "        </xs:complexType>\n"
            + "      </xs:element>\n"
            + "    </xs:schema>";
    String schemaV2 = "<xs:schema targetNamespace=\"http://www.example.org/schema/xsd/\"\n"
            + "                xmlns=\"http://www.example.org/schema/xsd/\"\n"
            + "                xmlns:xs=\"http://www.w3.org/2001/XMLSchema\"\n"
            + "                elementFormDefault=\"qualified\" attributeFormDefault=\"unqualified\">\n"
            + "      <xs:element name=\"metadata\">\n"
            + "        <xs:complexType>\n"
            + "          <xs:sequence>\n"
            + "            <xs:element name=\"title\" type=\"xs:string\"/>\n"
            + "            <xs:element name=\"date\" type=\"xs:date\"/>\n"
            + "            <xs:element name=\"note\" type=\"xs:string\" minOccurs=\"0\"/>\n"
            + "          </xs:sequence>\n"
            + "        </xs:complexType>\n"
            + "      </xs:element>\n"
            + "    </xs:schema>";
    SimpleServiceClient simpleClient = SimpleServiceClient.create("http://localhost:8040/api/v1/schemas");
    simpleClient.accept(MediaType.parseMediaType("application/json"));
    System.out.println(srs);
    System.out.println(srs.toString());
    // add entries to form
    simpleClient.withFormParam("schema", schemaV1);
    simpleClient.withFormParam("record", srs);
    // post form and get HTTP status
    HttpStatus resource = simpleClient.postForm(); //Resource(srs, SchemaRecordSchema.class);
    System.out.println(resource);
    // get response as string
    System.out.println(simpleClient.getResponseBody());
    // get response as object
    srs = simpleClient.getResponseBody(SchemaRecordSchema.class);
    System.out.println(srs);

    ///////////////////////////////////////////////////////////////////////
    // Get single entry (record)
    ///////////////////////////////////////////////////////////////////////
    simpleClient = SimpleServiceClient.create("http://localhost:8040/api/v1/schemas");
    // append id to base path. 
    // Alternatively: SSC.create(basePath + "/" + schemaId);
    simpleClient.withResourcePath(schemaId);
    simpleClient.accept(MediaType.parseMediaType("application/vnd.datamanager.schema-record+json"));
    // Configure client to fetch ETag...
    Map<String, String> responseHeader = new HashMap<>();
    responseHeader.put(ETAG, null);
    simpleClient.collectResponseHeader(responseHeader);
    SchemaRecordSchema resource1 = simpleClient.getResource(SchemaRecordSchema.class);
    System.out.println(resource1);
    // Read ETag from header.
    String eTag = responseHeader.get(ETAG);

    ///////////////////////////////////////////////////////////////////////
    // Get single entry (schema document)
    ///////////////////////////////////////////////////////////////////////
    simpleClient = SimpleServiceClient.create("http://localhost:8040/api/v1/schemas");
    // append id to base path. 
    // Alternatively: SSC.create(basePath + "/" + schemaId);
    simpleClient.withResourcePath(schemaId);
    ByteArrayOutputStream stream = new ByteArrayOutputStream();
    int statusCode = simpleClient.getResource(stream);
    System.out.println("Status Code: " + statusCode);
    System.out.println(stream.toString());

    ///////////////////////////////////////////////////////////////////////
    // Get all entries
    ///////////////////////////////////////////////////////////////////////
    simpleClient = SimpleServiceClient.create("http://localhost:8040/api/v1/schemas");
    responseHeader = new HashMap<>();
    responseHeader.put(CONTENT_RANGE, null);
    simpleClient.collectResponseHeader(responseHeader);
    SchemaRecordSchema[] resource2 = simpleClient.getResource(SchemaRecordSchema[].class);
    for (SchemaRecordSchema item : resource2) {
      System.out.println(item);
    }
    // Print collected headers
    for (String key : responseHeader.keySet()) {
      System.out.println(key + "--->" + responseHeader.get(key));
    }
    ///////////////////////////////////////////////////////////////////////
    // Update an entry via PUT (etags may be a problem)
    ///////////////////////////////////////////////////////////////////////
    simpleClient = SimpleServiceClient.create("http://localhost:8040/api/v1/schemas");
    simpleClient.withResourcePath(schemaId);
    // add ETag to Header
    simpleClient.withHeader(ETAG, eTag);
    // add entry to form
    simpleClient.withFormParam("schema", schemaV2);
    // add a response header we want to fetch
    responseHeader = new HashMap<>();
    responseHeader.put(LOCATION, null);
    simpleClient.collectResponseHeader(responseHeader);
    // post form and get HTTP status
    resource = simpleClient.putForm(); //Resource(srs, SchemaRecordSchema.class);
    System.out.println(resource);
    // get response as string
    System.out.println(simpleClient.getResponseBody());
    // get response as object
    srs = simpleClient.getResponseBody(SchemaRecordSchema.class);
    System.out.println(srs);
    //       resource = ssc.getResource(SchemaRecordSchema[].class);
    //       for (SchemaRecordSchema item : resource)
    //      System.out.println(item);
    //    ResultPage<SchemaRecordSchema> result = SimpleServiceClient.create("http://localhost:8090/api/v1/dataresources1/").getResources(DataResource[].class);
    //    System.out.println(result.getContentRange());OK
    //    for (DataResource r : result.getResources()) {
    //      System.out.println(r);
    //    }
  }

//  /**
//   * Resul page holding instance of type 'C'.
//   * @param <C> Type of response:
//   */
//  @Data
//  public static class ResultPage<C> {
//
//    /**
//     *  Constructor.
//     * @param resources Array holding all resources.
//     * @param range Given range if numer of resources is restricted.
//     */
//    public ResultPage(C[] resources, ControllerUtils.ContentRange range) {
//      this.resources = resources;
//      this.contentRange = range;
//    }
//
//    ControllerUtils.ContentRange contentRange;
//    C[] resources;
//  }
//
//  /**
//   * Sort respose.
//   */
//  @Data
//  public static class SortField {
//
//    /**
//     * Select direction for sorting.
//     */
//    public enum DIR {
//
//      /**
//       * Ascending
//       */
//      ASC,
//      /**
//       * Descending
//       */
//      DESC;
//    }
//
//    String fieldName;
//    DIR direction;
//
//    /**
//     * Define field for sorting!
//     * @param fieldName Field to sort.
//     * @param direction Ascending or descending.
//     */
//    public SortField(String fieldName, DIR direction) {
//      this.fieldName = fieldName;
//      this.direction = direction;
//    }
//
//    /**
//     * Transform sort to query parameter.
//     * @return Query part of URL.
//     */
//    public String toQueryParam() {
//      return fieldName + ((direction != null) ? "," + direction.toString().toLowerCase() : "");
//    }
//
//  }
  /**
   * @return the responseBody
   */
  public String getResponseBody() {
    return responseBody;
  }

  /**
   * Get response as object (if possible)
   *
   * @param <C> Class of response
   * @param responseType Class of response
   * @return response as object
   */
  public <C> C getResponseBody(Class<C> responseType) {
    C response = null;
    try {
      response = new ObjectMapper().readValue(responseBody, responseType);
    } catch (JsonProcessingException ex) {
      LOGGER.error(null, ex);
    }
    return response;
  }

  private String buildUri() {
    StringBuffer destinationUri = new StringBuffer(resourceBaseUrl);
    if (resourcePath != null) {
      if (!resourceBaseUrl.endsWith(SLASH)) {
        destinationUri.append(SLASH);
      }
      if (resourcePath.startsWith(SLASH)) {
        destinationUri.append(resourcePath.substring(1));
      } else {
        destinationUri.append(resourcePath);
      }
    }
    return destinationUri.toString();
  }

  /**
   * @return the responseStatus
   */
  public HttpStatus getResponseStatus() {
    return responseStatus;
  }
}
