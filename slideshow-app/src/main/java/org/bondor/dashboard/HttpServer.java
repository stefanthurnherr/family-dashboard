package org.bondor.dashboard;

import org.apache.catalina.connector.Connector;

import org.springframework.beans.factory.annotation.Value;

import org.springframework.stereotype.Component;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.servlet.server.ServletWebServerFactory;
import org.springframework.context.annotation.Bean;


@Component
public class HttpServer {

  @Bean
  public ServletWebServerFactory servletContainer(@Value("${server.http.port}") int httpPort) {
      final Connector connector = new Connector(TomcatServletWebServerFactory.DEFAULT_PROTOCOL);
      connector.setPort(httpPort);

      final TomcatServletWebServerFactory tomcat = new TomcatServletWebServerFactory();
      tomcat.addAdditionalTomcatConnectors(connector);
      return tomcat;
  }
}
