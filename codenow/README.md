# bpm-app

## Setting up an IDE
https://javalin.io/tutorials/maven-setup

##Setup local environment
Enter following configuration to ~/.m2/settings.xml
```
<settings>
  <profiles>
    <profile>
      <id>codenow</id>
      <repositories>
        <repository>
          <id>codenow-releases</id>
          <url></url>
        </repository>
      </repositories>
    </profile>
  </profiles>
  <servers>
    <server>
      <id>codenow-releases</id>
      <username>ENTER YOUR USERNAME HERE</username>
      <password><![CDATA[ENTER YOUR PASSWORD HERE]]></password>
    </server>
  </servers>
  <activeProfiles>
    <activeProfile>codenow</activeProfile>
  </activeProfiles>
</settings>
```

## Application runner
To run application locally, the SERVER_PORT environment variable should be defined 
```
export SERVER_PORT=7000
```
To override, use JVM options such as
``` 
-Dlogback.configurationFile=logback-codenow.xml -Dconfig.file=codenow/config/startup-message.txt -Xmx80m -Xms80m -XX:MaxMetaspaceSize=80m
```

## Deployment configuration
All files located in `./codenow/config` directory will be deployed alongside the application and available in `/codenow/config` directory

## Curl examples
```
curl -X GET \
 http://localhost:7000/
```
