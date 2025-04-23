package de.caritas.cob.messageservice.filter;

import io.sentry.ITransaction;
import io.sentry.Sentry;
import io.sentry.SpanStatus;
import java.io.IOException;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@ConditionalOnExpression("${sentry.performance.monitoring.enabled:true}")
public class PerformanceMonitoringFilter implements Filter {

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException {
    HttpServletRequest httpRequest = (HttpServletRequest) request;
    String operation = httpRequest.getMethod() + " " + httpRequest.getRequestURI();
    ITransaction transaction = Sentry.startTransaction(operation, "http-request");
    log.debug("Started sentry transaction for operation {}", operation);

    try {
      chain.doFilter(request, response);
      transaction.setStatus(SpanStatus.OK);
    } catch (Exception e) {
      transaction.setStatus(SpanStatus.INTERNAL_ERROR);
      throw e;
    } finally {
      transaction.finish();
      log.debug("Sentry transaction for operation {} finished", operation);

    }
  }
}

