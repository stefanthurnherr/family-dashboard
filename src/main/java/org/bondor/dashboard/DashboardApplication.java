package org.bondor.dashboard;

import java.time.LocalDateTime;
import java.net.URI;
import java.util.Random;
import java.io.IOException;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import java.io.InputStream;
import java.time.format.DateTimeFormatter;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
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

  private Random random = new Random();

  @RequestMapping(value = "/", method = RequestMethod.GET)
  public String home() {
      return "Hello Docker World - built " + DateTimeFormatter.ISO_INSTANT.format(buildProperties.getTime());
  }

  @RequestMapping(value = "/tryredirect", method = RequestMethod.GET)
  public ResponseEntity<Void> tryRedirect() {
    return ResponseEntity.status(HttpStatus.FOUND).location(URI.create("/fdimages/one.jpg")).build();
  }

  @RequestMapping(value = "/images", method = RequestMethod.GET)
  public ResponseEntity<String> listImages() throws Exception {
    System.out.println("Listing all available images...");
    final Resource[] resources = allImageResources();
    if (resources == null) {
      return ResponseEntity.notFound().build();
    }
    System.out.println("Found " + resources.length + " available images:");
    final StringBuilder sb = new StringBuilder();
    for (Resource resource : resources) {
      System.out.println("  Found image file: " + resource.getFilename());
      sb.append(resource.getFilename()).append("\n");
    }

    return ResponseEntity.ok()
            .contentType(MediaType.TEXT_PLAIN)
            .body(sb.toString());
  }

  @RequestMapping(value = "/images/{id:[1-9a-z][0-9a-z]*}", method = RequestMethod.GET)
  public ResponseEntity<byte[]> image(@PathVariable("id") String id) throws Exception {
    final Resource imageResource = findImageResource(id, allImageResources());
    if (imageResource == null) {
      return ResponseEntity.notFound().build();
    }

    final String nowString = DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(LocalDateTime.now());
    System.out.println(nowString + " Serving image " + imageResource + " for index " + id);
    InputStream inputStream = null;
    try {
      inputStream = imageResource.getInputStream();
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

  private Resource findImageResource(final String id, final Resource[] resources) {
    try {
      final long idLong = Long.parseLong(id);
      for (final Resource resource : resources){
        System.out.println("  Checking whether " + resource.getFilename() + " matches...");
        if (resource.getFilename().equals(Long.toString(idLong) + ".png")) {
          return resource;
        }
      }
    } catch (NumberFormatException e) {
      if ("any".equals(id)) {
        final int randomIndex = random.nextInt(resources.length);
        return resources[randomIndex];
      }
    }
    System.out.println("  No matching resource found for id=" + id);
    return null;
  }

  private Resource[] allImageResources() throws IOException {
    final ClassPathResource imagesDirectory = new ClassPathResource("static/");
    if (!imagesDirectory.exists()) {
      return null;
    }
    final ClassLoader cl = this.getClass().getClassLoader();
    final ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver(cl);
    return resolver.getResources("classpath*:/static/*.png") ;
  }

  public static void main(String[] args) {
      SpringApplication.run(DashboardApplication.class, args);
  }

}
