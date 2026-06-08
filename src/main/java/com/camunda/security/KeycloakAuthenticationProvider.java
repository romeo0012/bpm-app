package com.camunda.security;

import java.util.ArrayList;
import java.util.List;

import jakarta.servlet.http.HttpServletRequest;

import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.engine.rest.security.auth.AuthenticationResult;
import org.camunda.bpm.engine.rest.security.auth.impl.ContainerBasedAuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.util.StringUtils;

public class KeycloakAuthenticationProvider extends ContainerBasedAuthenticationProvider {

    @Override
    public AuthenticationResult extractAuthenticatedUser(HttpServletRequest request, ProcessEngine engine) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (!(authentication instanceof OAuth2AuthenticationToken) ||
            !(authentication.getPrincipal() instanceof OidcUser oidcUser)) {
            return AuthenticationResult.unsuccessful();
        }

        String userId = oidcUser.getPreferredUsername();

        if (!StringUtils.hasText(userId)) {
            return AuthenticationResult.unsuccessful();
        }

        AuthenticationResult result = new AuthenticationResult(userId, true);
        result.setGroups(getGroups(userId, engine));
        return result;
    }

    private List<String> getGroups(String userId, ProcessEngine engine) {
        List<String> groups = new ArrayList<>();
        engine.getIdentityService()
            .createGroupQuery()
            .groupMember(userId)
            .list()
            .forEach(group -> groups.add(group.getId()));
        return groups;
    }
}