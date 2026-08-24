package com.camunda;

import jakarta.annotation.PostConstruct;

import org.camunda.bpm.engine.impl.cfg.AbstractProcessEnginePlugin;
import org.camunda.bpm.engine.impl.cfg.ProcessEngineConfigurationImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
class CamundaConfigDebugPlugin extends AbstractProcessEnginePlugin {

    @Override
    public void preInit(ProcessEngineConfigurationImpl config) {

        System.out.println("========== CAMUNDA ENGINE CONFIG ==========");
        System.out.println(
            "databaseSchemaUpdate = " +
            config.getDatabaseSchemaUpdate()
        );
        System.out.println(
            "databaseSchema = " +
            config.getDatabaseSchema()
        );
        System.out.println(
            "databaseTablePrefix = " +
            config.getDatabaseTablePrefix()
        );
        System.out.println("===========================================");
    }
}

@SpringBootApplication
public class CamundaApplication {

  @Autowired
  private Environment env;

  /*
   * Tohle se spustí po vytvoření Spring bean,
   * takže už máme k dispozici Spring Environment.
   */
  @PostConstruct
  public void debugSpringConfig() {

    System.out.println("========== SPRING CONFIG DEBUG ==========");

    System.out.println(
        "camunda.bpm.database.schema-update = " +
        env.getProperty("camunda.bpm.database.schema-update")
    );

    System.out.println(
        "spring.datasource.url = " +
        env.getProperty("spring.datasource.url")
    );

    System.out.println("=========================================");
  }

  public static void main(String... args) {

    /*
     * Tohle kontroluje skutečné ENV proměnné
     * před startem Springu.
     */
    System.out.println("========== CONFIG DEBUG ==========");

    System.out.println(
        "CAMUNDA_BPM_DATABASE_SCHEMA_UPDATE env = " +
        System.getenv("CAMUNDA_BPM_DATABASE_SCHEMA_UPDATE")
    );

    System.out.println(
        "DB_URL = " +
        System.getenv("DB_URL")
    );

    System.out.println(
        "JAVAX_NET_SSL_TRUSTSTORE = " +
        System.getenv("JAVAX_NET_SSL_TRUSTSTORE")
    );

    System.out.println("==================================");


    /*
     * SSL truststore
     */
    String trustStore =
        System.getenv("JAVAX_NET_SSL_TRUSTSTORE");

    String trustStorePassword =
        System.getenv("JAVAX_NET_SSL_TRUSTSTOREPASSWORD");

    if (trustStore != null) {
      System.setProperty(
          "javax.net.ssl.trustStore",
          trustStore
      );
    }

    if (trustStorePassword != null) {
      System.setProperty(
          "javax.net.ssl.trustStorePassword",
          trustStorePassword
      );
    }


    /*
     * Start Spring Boot
     */
    SpringApplication.run(CamundaApplication.class, args);
  }
}
