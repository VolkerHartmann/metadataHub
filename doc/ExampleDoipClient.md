# Code examples how to access DOIP client
```

...
    DoipClient client = new DoipClient();
    AuthenticationInfo authInfo = new PasswordAuthenticationInfo("admin", "password");
    ServiceInfo serviceInfo = new ServiceInfo(TARGET_ONE, "localhost", 8880);

    printHeader("HELLO");

    DigitalObject result = client.hello(TARGET_ONE, authInfo, serviceInfo);

    printResult(result);
    printHeader("LIST_OPERATIONS");
    List<String> listOperations = client.listOperations(TARGET_ONE, authInfo, serviceInfo);
    System.out.println(listOperations);
    dobj = createSchema();
    result = client.create(dobj, authInfo, serviceInfo);
    printHeader("Create...");
    printResult(result);
    String id = result.id;
...
 private static DigitalObject createSchema() throws IOException {
    Datacite43Schema datacite = new Datacite43Schema();
    Title title = new Title();
    title.setTitle("schema_2");
    datacite.getTitles().add(title);
    datacite.setPublisher("NFDI4Ing");
    datacite.getFormats().add("JSON");//application/json");
    String json = new Gson().toJson(datacite);
    DigitalObject dobj = new DigitalObject();
    dobj.attributes = new JsonObject();
    dobj.attributes.addProperty("datacite", json);
    dobj.elements = new ArrayList<>();
    Element element = new Element();
    element.id = "schema";
    element.type = "application/json";
    element.in = new ByteArrayInputStream(JSON_SCHEMA.getBytes());
    element.length = (long) JSON_SCHEMA.getBytes().length;
    dobj.elements.add(element);

    return dobj;
  }

  private static void printHeader(String header) {
    System.out.println("********************************************************************");
    System.out.println("***" + header);
    System.out.println("********************************************************************");

  }

  private static void printResult(DigitalObject result) throws IOException {
    System.out.println(result.id);
    System.out.println(result.type);
    System.out.println(result.attributes);
    if (result.elements != null) {
      System.out.println("No of elements: " + result.elements.size());
      for (Element item : result.elements) {
        System.out.println("Element...");
        System.out.println(item.id);
        System.out.println(item.attributes);
        System.out.println(item.type);
        System.out.println(item.length);
        StringBuilder textBuilder = new StringBuilder("stream: '");
        if (item.in != null) {
          char[] input = new char[8196];
          System.out.println("Inputstream not null");
          try (Reader reader = new BufferedReader(new InputStreamReader(item.in, Charset.forName(StandardCharsets.UTF_8.name())))) {
            int c = 0;
            while ((c = reader.read(input)) > 0) {
              System.out.println("Read '" + c + "' bytes!");
              textBuilder.append(new String(input, 0, c));
            }
          }
        }
        textBuilder.append("'");
        System.out.println(textBuilder.toString());
      }
    }

```
