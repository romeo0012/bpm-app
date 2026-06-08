package com.camunda.security;

import java.util.Collections;

import org.camunda.bpm.webapp.impl.security.auth.ContainerBasedAuthenticationFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.context.request.RequestContextListener;

import org.springframework.security.config.Customizer;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

    @Bean
    public SecurityFilterChain httpSecurity(HttpSecurity http) throws Exception {
        http
            // 1. Zapne OAuth2 login (automaticky přidá OAuth2LoginAuthenticationFilter + /oauth2/authorization/keycloak + /login/oauth2/code/keycloak)
            .oauth2Login(oauth2 -> {})
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))

            // 2. Zakáž klasický form login, aby nepřebíral requesty
            .formLogin(form -> form.disable())

            // 3. ACL pro OAuth2, Camunda webapp a OAuth2 endpoint
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/app/**", "/api/**", "/lib/**", "/oauth2/**", "/login/oauth2/code/**")
                    .authenticated()
                .requestMatchers("/engine-rest/version")
                    .permitAll()
                .requestMatchers("/engine-rest/**")
                    .authenticated()
                .anyRequest().permitAll()
            )

            // 4. Logout
            .logout(logout -> logout
                .logoutSuccessUrl("/app/")
            )

            // 5. CSRF vypnuté (Camunda webapp většinou bez formulářových POST, jinak lze přenastavit)
            .csrf(AbstractHttpConfigurer::disable);

        return http.build();
    }

    @Bean
    public FilterRegistrationBean<ContainerBasedAuthenticationFilter> containerBasedAuthenticationFilter() {
        FilterRegistrationBean<ContainerBasedAuthenticationFilter> filterRegistration =
            new FilterRegistrationBean<>();

        filterRegistration.setFilter(new ContainerBasedAuthenticationFilter());
        filterRegistration.setInitParameters(Collections.singletonMap(
            "authentication-provider",
            "com.camunda.security.KeycloakAuthenticationProvider"
        ));
        filterRegistration.setOrder(201);
        filterRegistration.addUrlPatterns("/app/*");
        return filterRegistration;
    }

    @Bean
    public RequestContextListener requestContextListener() {
        return new RequestContextListener();
    }
}