# AIP to DIP converter

## Using the library

1. Add the dependency to your pom.xml

   ```xml
   <dependency>
       <groupId>fi.disec.eark</groupId>
       <artifactId>aip-to-dip-converter</artifactId>
       <version>1.0.0</version>
   </dependency>
   ```

2. Create a new instance of SIPToAIPConverter and just use the convert methods

   ```java
      final var aipToDipConverter = new AIPToDIPConverter();
      final var pathToAip = Path.of("aip.zip");
      final var pathToDip = Path.of("dip.zip");
      final var dip = aipToDipConverter.convert(pathToAip, pathToDip);
      System.out.println(dip.getId());
   ```

## Authenticating to the GitHub Maven repository

Some of the dependencies of this library are not published in Maven central, but only in GitHub packages maven repository.

The GitHub Maven repository does not (atleast for now) provide public access without authentication.
Use the following steps to authenticate:

1. Create a new personal access token with `read:packages` scope on your personal GitHub account
2. Add the following to your Maven settings.xml (the ids should match the ones in pom.xml)

   ```xml
   <servers>
     <server>
       <id>github-roda</id>
       <username>YOUR GITHUB USERNAME</username>
       <password>PERSONAL ACCESS TOKEN</password>
     </server>
     <server>
       <id>github-commons-ip</id>
       <username>YOUR GITHUB USERNAME</username>
       <password>PERSONAL ACCESS TOKEN</password>
     </server>
   </servers>
   ```
   