package de.caritas.cob.messageservice.config;

import static de.caritas.cob.messageservice.api.authorization.Authority.AuthorityValue.ANONYMOUS_DEFAULT;
import static de.caritas.cob.messageservice.api.authorization.Authority.AuthorityValue.CONSULTANT_DEFAULT;
import static de.caritas.cob.messageservice.api.authorization.Authority.AuthorityValue.TECHNICAL_DEFAULT;
import static de.caritas.cob.messageservice.api.authorization.Authority.AuthorityValue.USER_DEFAULT;
import static de.caritas.cob.messageservice.api.authorization.Authority.AuthorityValue.USE_FEEDBACK;

import de.caritas.cob.messageservice.config.security.AuthorisationService;
import de.caritas.cob.messageservice.config.security.JwtAuthConverter;
import de.caritas.cob.messageservice.config.security.JwtAuthConverterProperties;
import de.caritas.cob.messageservice.filter.PerformanceMonitoringFilter;
import de.caritas.cob.messageservice.filter.StatelessCsrfFilter;
import org.keycloak.adapters.springsecurity.KeycloakConfiguration;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CsrfFilter;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Provides the Keycloak/Spring Security configuration.
 */
@Configuration
@KeycloakConfiguration
public class SecurityConfig implements WebMvcConfigurer {

  public static final String[] WHITE_LIST =
      new String[]{"/agencies/docs", "/agencies/docs/**", "/v2/api-docs", "/configuration/ui",
          "/swagger-resources/**", "/configuration/security", "/swagger-ui.html", "/swagger-ui/**", "/webjars/**", "/actuator/health", "/actuator/health/**"};


  @Autowired
  AuthorisationService authorisationService;
  @Autowired
  JwtAuthConverterProperties jwtAuthConverterProperties;


  @Value("${csrf.cookie.property}")
  private String csrfCookieProperty;

  @Value("${csrf.header.property}")
  private String csrfHeaderProperty;

  @Value("${csrf.whitelist.header.property}")
  private String csrfWhitelistHeaderProperty;

  @Value("${sentry.peformance.monitoring.enabled:false}")
  private boolean sentryPerformanceMonitoringEnabled;


  /**
   * Configure spring security filter chain: disable default Spring Boot CSRF token behavior and add
   * custom {@link StatelessCsrfFilter}, set all sessions to be fully stateless, define necessary
   * Keycloak roles for specific REST API paths.
   */
  @Bean
  public SecurityFilterChain configure(HttpSecurity http) throws Exception {


    @SuppressWarnings("java:S1075") // URIs should not be hardcoded
    final var SINGLE_MESSAGE_PATH = "/messages/{messageId:[0-9A-Za-z]{17}}";

    var httpSecurity = http.csrf(csrf -> csrf.disable())
        .addFilterBefore(new StatelessCsrfFilter(csrfCookieProperty, csrfHeaderProperty, csrfWhitelistHeaderProperty),
            CsrfFilter.class);
    if (sentryPerformanceMonitoringEnabled) {
      http.addFilterBefore(new PerformanceMonitoringFilter(), CsrfFilter.class);
    }

    httpSecurity.sessionManagement(management -> management.sessionCreationPolicy(SessionCreationPolicy.STATELESS)).authorizeHttpRequests(requests -> requests
        .requestMatchers(SecurityConfig.WHITE_LIST).permitAll()
        .requestMatchers("/messages/key")
        .hasAuthority(TECHNICAL_DEFAULT)
        .requestMatchers("/messages", "/messages/draft", "/messages/videohint/new")
        .hasAnyAuthority(USER_DEFAULT, CONSULTANT_DEFAULT, ANONYMOUS_DEFAULT)
        .requestMatchers(HttpMethod.PATCH, SINGLE_MESSAGE_PATH)
        .hasAnyAuthority(USER_DEFAULT)
        .requestMatchers(HttpMethod.GET, SINGLE_MESSAGE_PATH)
        .hasAnyAuthority(USER_DEFAULT, CONSULTANT_DEFAULT, ANONYMOUS_DEFAULT)
        .requestMatchers(HttpMethod.DELETE, SINGLE_MESSAGE_PATH)
        .hasAnyAuthority(USER_DEFAULT, CONSULTANT_DEFAULT, ANONYMOUS_DEFAULT)
        .requestMatchers("/messages/new")
        .hasAnyAuthority(USER_DEFAULT, CONSULTANT_DEFAULT, TECHNICAL_DEFAULT, ANONYMOUS_DEFAULT)
        .requestMatchers("/messages/forward", "/messages/feedback/new")
        .hasAuthority(USE_FEEDBACK)
        .requestMatchers("/messages/aliasonly/new")
        .hasAnyAuthority(USER_DEFAULT, CONSULTANT_DEFAULT, TECHNICAL_DEFAULT)
        .requestMatchers("/messages/aliasWithContent/new")
        .hasAnyAuthority(USER_DEFAULT, CONSULTANT_DEFAULT, TECHNICAL_DEFAULT)
        .anyRequest()
        .denyAll());

    httpSecurity.oauth2ResourceServer(server -> server.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthConverter())));
    return httpSecurity.build();
  }


  /**
   * Configure trailing slash match for all endpoints (needed as Spring Boot 3.0.0 changed default behaviour for trailing slash match)
   * https://www.baeldung.com/spring-boot-3-migration (section 3.1)
   */
  @Override
  public void configurePathMatch(PathMatchConfigurer configurer) {
    configurer.setUseTrailingSlashMatch(true);
  }

  @Bean
  public JwtAuthConverter jwtAuthConverter() {
    return new JwtAuthConverter(jwtAuthConverterProperties, authorisationService);
  }


}
