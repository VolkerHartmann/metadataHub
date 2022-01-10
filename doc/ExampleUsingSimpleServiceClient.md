# Code examples how to use SimpeServiceClient for HTTP requests
## Prerequisites
### Generate Java classes for easier handling of json records
If the service uses json metadata records you may use generated java classes to
interact with them. Therefore you have to store the JSON schema of the record
in the folder 'src/main/resources/json/schema' with a unique name. After the first 
build generated classes will be available in package 'edu.kit.turntable.mapping'.
The main class has the same name then the stored schema file.
(example_schema.json -> ExampleSchema.java)
 

```
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
```
