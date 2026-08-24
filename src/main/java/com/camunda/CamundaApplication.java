package com.camunda;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class CamundaApplication {
  public static void main(String... args) {

    System.out.println("========== CONFIG DEBUG ==========");
    System.out.println( "CAMUNDA_BPM_DATABASE_SCHEMA_UPDATE env = " + System.getenv("CAMUNDA_BPM_DATABASE_SCHEMA_UPDATE") );
    System.out.println( "DB_URL = " + System.getenv("DB_URL") );
    System.out.println( "JAVAX_NET_SSL_TRUSTSTORE = " + System.getenv("JAVAX_NET_SSL_TRUSTSTORE") );
    System.out.println("==================================");

    String trustStore = System.getenv("JAVAX_NET_SSL_TRUSTSTORE");
    String trustStorePassword = System.getenv("JAVAX_NET_SSL_TRUSTSTOREPASSWORD");

    if (trustStore != null) { System.setProperty("javax.net.ssl.trustStore", trustStore); }
    if (trustStorePassword != null) { System.setProperty("javax.net.ssl.trustStorePassword", trustStorePassword); }

    SpringApplication.run(CamundaApplication.class, args);
  }

}
