package com.camunda.security;

import org.camunda.bpm.extension.keycloak.plugin.KeycloakIdentityProviderPlugin;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "plugin.identity.keycloak")
@Profile("!test")
public class CamundaKeycloakIdentityProvider extends KeycloakIdentityProviderPlugin {
}