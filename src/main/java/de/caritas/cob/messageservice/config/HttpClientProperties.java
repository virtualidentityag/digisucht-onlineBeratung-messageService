package de.caritas.cob.messageservice.config;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.convert.DurationUnit;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

@Data
@Validated
@Component
@ConfigurationProperties(prefix = "http.client")
public class HttpClientProperties {

  @Min(1)
  private int maxConnectionsTotal = 200;

  @Min(2)
  private int maxConnectionsPerRoute = 100;

  @NotNull
  @DurationUnit(ChronoUnit.SECONDS)
  private Duration idleEvict = Duration.ofSeconds(30);

  @NotNull
  @DurationUnit(ChronoUnit.SECONDS)
  private Duration connectTimeout = Duration.ofSeconds(2);

  @NotNull
  @DurationUnit(ChronoUnit.SECONDS)
  private Duration readTimeout = Duration.ofSeconds(8);

  @DurationUnit(ChronoUnit.SECONDS)
  private Duration connectionTtl;
}

