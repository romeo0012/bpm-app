package com.camunda.service;

import io.javalin.Javalin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class Application {

    public static void main(String[] args) throws IOException {
        Logger logger = LoggerFactory.getLogger(Application.class);
        logger.info("Hello World!");

        int serverPort;
        if (System.getenv("SERVER_PORT") != null) {
            serverPort = Integer.parseInt(System.getenv("SERVER_PORT"));
        } else {
            serverPort = 7000;
            logger.warn("The SERVER_PORT environment variable was not set. The default port is set to " + serverPort + ".");
        }

        if (System.getProperty("config.file") != null) {
            String configFilePath = System.getProperty("config.file");
            if (new File(configFilePath).isFile()) {
                Path fileName = Path.of(configFilePath);
                String actual = Files.readString(fileName);
                logger.info(actual);
            } else {
                logger.error("A configuration file doesn't exist!");
            }
        } else {
            logger.info("Do you know, that you can use your configuration file with -Dconfig.file flag?");
        }

        Javalin app = Javalin.create().start(serverPort);
        app.get("/", ctx -> ctx.result("Hello World"));
        app.get("/health", ctx -> ctx.result("OK"));
    }
}
