package org.bondor.dashboard;

import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;
import org.springframework.context.annotation.Configuration;
//import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;


@Configuration
//@EnableWebMvc // not allowed/needed for Spring Boot!
public class MySpringConfiguration
  //implements WebMvcConfigurer
  extends WebMvcConfigurerAdapter
{

  @Override
  public void addResourceHandlers(final ResourceHandlerRegistry registry) {
    System.out.println("Adding resource handler for /fdimages ...");
    registry.addResourceHandler("/fdimages/**").addResourceLocations("file:/opt/family-dashboard-images/");
  }

}
