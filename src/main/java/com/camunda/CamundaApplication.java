package com.camunda;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
//import org.camunda.bpm.spring.boot.starter.annotation.EnableProcessApplication;

@SpringBootApplication
//@EnableProcessApplication
public class CamundaApplication {

  public static void main(String... args) {
    SpringApplication.run(CamundaApplication.class, args);
  }

}
