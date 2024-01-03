package org.bondor.dashboard;

import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;


@Configuration
public class MySpringConfiguration implements WebMvcConfigurer {

  @Override
  public void addResourceHandlers(final ResourceHandlerRegistry registry) {
    System.out.println("Adding resource handler for /fdimages ...");
    registry.addResourceHandler("/fdimages/**").addResourceLocations("file:/opt/family-dashboard-images/");
  }

}
