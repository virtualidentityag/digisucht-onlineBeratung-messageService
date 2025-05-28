package de.caritas.cob.messageservice.config;

import io.sentry.SentryOptions;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.client.RestTemplate;

/**
 * Contains some general spring boot application configurations
 *
 */
@Configuration
@ComponentScan(basePackages = {"de.caritas.cob.messageservice"})
public class AppConfig implements ApplicationContextAware {

  @Value("${onlineberatung.sentry.dsn}")
  private String dsn;

  @Value("${sentry.environment}")
  private String environment;

  @Value("${sentry.sample-rate:0.5}")
  private Double sampleRate;

  private ApplicationContext context;

  @PostConstruct
  public SentryOptions sentryOptions() {
    SentryOptions options = context.getBean(SentryOptions.class);
    options.setDsn(dsn);
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

  // RestTemplate Bean
  @Bean
  public RestTemplate restTemplate(RestTemplateBuilder builder) {
    return builder.build();
  }

  @Override
  public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
    this.context = applicationContext;
  }
}
