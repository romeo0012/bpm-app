package com.camunda.costcalc;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.logging.Logger;

import javax.sql.DataSource;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component("config")
public class Config implements JavaDelegate {

    private static final Logger LOGGER = Logger.getLogger("PROJECT-REQUESTS");

    @Autowired
    private DataSource dataSource;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void execute(DelegateExecution execution) throws Exception {
        LOGGER.info("Loading config from DB...");

        try (
            Connection conn = dataSource.getConnection();
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery("SELECT data FROM costcalc_config LIMIT 1")
        ) {
            if (rs.next()) {
                String json = rs.getString("data");

                if (json == null || json.isBlank()) {
                    throw new IllegalStateException("Column 'data' in table costcalc_config is empty");
                }

                JsonNode cfg = objectMapper.readTree(json);

                execution.setVariable("configJson", json);
                pushJsonToExecution(cfg, execution);

                LOGGER.info("Config loaded OK");
                LOGGER.info("Config JSON loaded to process variable 'configJson'");
            } else {
                LOGGER.warning("No config found in table costcalc_config");
            }
        }
    }

    private void pushJsonToExecution(JsonNode json, DelegateExecution execution) {
        Iterator<Entry<String, JsonNode>> properties = json.properties().iterator(); // ✅ Set → Iterator

        while (properties.hasNext()) {
            Entry<String, JsonNode> field = properties.next();
            String key = field.getKey();
            JsonNode value = field.getValue();

            if (value == null || value.isNull()) {
                execution.setVariable(key, null);
            } else if (value.isIntegralNumber()) {
                execution.setVariable(key, value.asLong());
            } else if (value.isFloatingPointNumber()) {
                execution.setVariable(key, value.asDouble());
            } else if (value.isBoolean()) {
                execution.setVariable(key, value.asBoolean());
            } else if (value.isTextual()) {
                execution.setVariable(key, value.asText());
            } else {
                execution.setVariable(key, value.toString());
            }
        }
    }
}