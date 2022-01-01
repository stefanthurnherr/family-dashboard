package org.bondor.dashboard;

import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import java.io.InputStream;
import java.io.File;
import java.time.format.DateTimeFormatter;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.info.BuildProperties;
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@SpringBootApplication
@RestController
public class DashboardApplication {

  @Autowired
  BuildProperties buildProperties;

  @Autowired
  private ResourcePatternResolver resourcePatternResolver;

  @RequestMapping(value = "/", method = RequestMethod.GET)
  public String home() {
      return "Hello Docker World - built " + DateTimeFormatter.ISO_INSTANT.format(buildProperties.getTime());
  }

  @RequestMapping(value = "/images", method = RequestMethod.GET)
  public ResponseEntity<String> listImages() throws Exception {
    final ClassPathResource imagesDirectory = new ClassPathResource("static/");
    if (!imagesDirectory.exists()) {
      return ResponseEntity.notFound().build();
    }

    System.out.println("Listing images in " + imagesDirectory);
    final StringBuilder sb = new StringBuilder();
    //Resource[] resources = resourcePatternResolver.getResources("static/*.png");
    final ClassLoader cl = this.getClass().getClassLoader();
    final ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver(cl);
    final Resource[] resources = resolver.getResources("classpath*:/static/*.png") ;
    for (Resource resource : resources) {
      System.out.println("  Found image file: " + resource.getFilename());
      sb.append(resource.getFilename()).append("\n");
    }

    return ResponseEntity.ok()
            .contentType(MediaType.TEXT_PLAIN)
            .body(sb.toString());
  }

  @RequestMapping(value = "/images/{id:[0-9]+}", method = RequestMethod.GET)
  public ResponseEntity<byte[]> image(@PathVariable("id") long id) throws Exception {
    //FIXME prevent string code injection/validate it is numeric
    final ClassPathResource imageFile = new ClassPathResource("static/" + id + ".png");
    System.out.println("Serving up image " + id + " as " + imageFile);

    if (!imageFile.exists()) {
      return ResponseEntity.notFound().build();
    }

    InputStream inputStream = null;
    try {
      inputStream = imageFile.getInputStream();
      final byte[] imageBytes = StreamUtils.copyToByteArray(inputStream);
      return ResponseEntity.ok()
            .contentType(MediaType.IMAGE_PNG)
            .body(imageBytes);
    } finally {
      if (inputStream != null) {
        try {
          inputStream.close();
        } catch (Exception e) {}
      }
    }
  }

  public static void main(String[] args) {
      SpringApplication.run(DashboardApplication.class, args);
  }

}
