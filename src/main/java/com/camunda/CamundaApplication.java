package com.camunda;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
//import org.camunda.bpm.spring.boot.starter.annotation.EnableProcessApplication;

@SpringBootApplication
//@EnableProcessApplication
public class CamundaApplication {

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
