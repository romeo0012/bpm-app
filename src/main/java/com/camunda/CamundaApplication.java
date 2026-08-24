package com.camunda;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.env.Environment;
//import org.camunda.bpm.spring.boot.starter.annotation.EnableProcessApplication;

import jakarta.annotation.PostConstruct;

@SpringBootApplication
//@EnableProcessApplication
public class CamundaApplication {

  @Autowired
    private Environment env;

    @PostConstruct
    public void debugConfig() {
        System.out.println("========== CONFIG DEBUG ==========");

        System.out.println(
            "camunda.bpm.database.schema-update = " +
            env.getProperty("camunda.bpm.database.schema-update")
        );

        System.out.println(
            "CAMUNDA_BPM_DATABASE_SCHEMA_UPDATE env = " +
            System.getenv("CAMUNDA_BPM_DATABASE_SCHEMA_UPDATE")
        );

        System.out.println(
            "SPRING_CONFIG_LOCATION env = " +
            System.getenv("SPRING_CONFIG_LOCATION")
        );

        System.out.println(
            "SPRING_CONFIG_ADDITIONAL_LOCATION env = " +
            System.getenv("SPRING_CONFIG_ADDITIONAL_LOCATION")
        );

        System.out.println(
            "DB_URL = " +
            System.getenv("DB_URL")
        );

        System.out.println("==================================");
    }
  public static void main(String... args) {

    String trustStore = System.getenv("JAVAX_NET_SSL_TRUSTSTORE");
    String trustStorePassword = System.getenv("JAVAX_NET_SSL_TRUSTSTOREPASSWORD");

    if (trustStore != null) {
      System.setProperty("javax.net.ssl.trustStore", trustStore);
    }

    if (trustStorePassword != null) {
      System.setProperty("javax.net.ssl.trustStorePassword", trustStorePassword);
    }

    SpringApplication.run(CamundaApplication.class, args);
  }

}
