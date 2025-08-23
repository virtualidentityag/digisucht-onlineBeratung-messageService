package de.caritas.cob.messageservice.config;

import io.sentry.SentryOptions;
import javax.annotation.PostConstruct;

import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.client.RestTemplate;

/**
 * Contains some general spring boot application configurations
 *
 */
@Configuration
@ComponentScan(basePackages = {"de.caritas.cob.messageservice"})
public class AppConfig implements ApplicationContextAware {


  @Value("${sentry.environment}")
  private String environment;

  @Value("${sentry.sample-rate:0.5}")
  private Double sampleRate;

  private ApplicationContext context;

  @PostConstruct
  public SentryOptions sentryOptions() {
    SentryOptions options = context.getBean(SentryOptions.class);
    options.setEnvironment(environment);
    options.setTag("service", "MessageService");
    options.setRelease("2.0.0");
    options.setTracesSampleRate(sampleRate);
    options.setSendDefaultPii(false);
    return options;
  }

  /**
   * Activate the messages.properties for validation messages
   *
   * @param messageSource
   * @return
   */
  @Bean
  public LocalValidatorFactoryBean validator(MessageSource messageSource) {
    LocalValidatorFactoryBean validatorFactoryBean = new LocalValidatorFactoryBean();
    validatorFactoryBean.setValidationMessageSource(messageSource);
    return validatorFactoryBean;
  }

  @Bean
  public RestTemplate restTemplate(RestTemplateBuilder builder, HttpClientProperties httpClientProperties) {
    var cm = new PoolingHttpClientConnectionManager();
    cm.setMaxTotal(httpClientProperties.getMaxConnectionsTotal());
    cm.setDefaultMaxPerRoute(httpClientProperties.getMaxConnectionsPerRoute());

    CloseableHttpClient client = HttpClients.custom()
        .setConnectionManager(cm)
        .evictIdleConnections(httpClientProperties.getIdleEvict().toSeconds(), java.util.concurrent.TimeUnit.SECONDS)
        .build();

    var factory = new HttpComponentsClientHttpRequestFactory(client);

    return builder
        .setConnectTimeout(httpClientProperties.getConnectTimeout())
        .setReadTimeout(httpClientProperties.getReadTimeout())
        .requestFactory(() -> factory)
        .build();
  }

  @Override
  public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
    this.context = applicationContext;
  }
}
