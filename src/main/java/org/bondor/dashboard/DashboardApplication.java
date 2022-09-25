package org.bondor.dashboard;

import java.util.ArrayList;
import java.net.URI;
import java.util.Arrays;
import java.util.Random;
import java.util.List;
import java.io.IOException;
import java.io.InputStream;
import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;

import javax.servlet.http.HttpServletRequest;

import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
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
    final List<Resource> resources = allImageResources();
    if (resources == null) {
      return ResponseEntity.notFound().build();
    }
    System.out.println("Found " + resources.size() + " available images:");
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
  public ResponseEntity<byte[]> image(HttpServletRequest request,
                                      @PathVariable("id") String id) throws Exception {
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

  private Resource findImageResource(final String id, final List<Resource> resources) {
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
        final int randomIndex = random.nextInt(resources.size());
        return resources.get(randomIndex);
      }
    }
    System.out.println("  No matching resource found for id=" + id);
    return null;
  }

  protected List<Resource> allImageResources() throws IOException {
    final ClassLoader cl = this.getClass().getClassLoader();
    final ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver(cl);

    final List<Resource> resourceList = new ArrayList<Resource>();

    final Resource[] fdimagesResources = resolver.getResources("file:/opt/family-dashboard-images/**") ;
    resourceList.addAll(Arrays.asList(fdimagesResources));

    if (resourceList.isEmpty()) {
      resourceList.addAll(allStaticResources(resolver));
      System.out.println("No images found, choosing from " + resourceList.size() + " default images instead.");
    }

    return resourceList;
  }

  private List<Resource> allStaticResources(final ResourcePatternResolver resolver) throws IOException {
    final List<Resource> resourceList = new ArrayList<Resource>();
    final ClassPathResource imagesDirectory = new ClassPathResource("static/");
    if (!imagesDirectory.exists()) {
      return resourceList;
    }

    final Resource[] staticResources = resolver.getResources("classpath*:/static/*.png") ;
    resourceList.addAll(Arrays.asList(staticResources));
    return resourceList;
  }

  public static void main(String[] args) {
      SpringApplication.run(DashboardApplication.class, args);
  }

  private static String getRequestIpAddress(final HttpServletRequest request) {
    final List<String> IP_HEADERS = Arrays.asList(
        "X-Forwarded-For",
        "Proxy-Client-IP",
        "WL-Proxy-Client-IP",
        "HTTP_X_FORWARDED_FOR",
        "HTTP_X_FORWARDED",
        "HTTP_X_CLUSTER_CLIENT_IP",
        "HTTP_CLIENT_IP",
        "HTTP_FORWARDED_FOR",
        "HTTP_FORWARDED",
        "HTTP_VIA",
        "REMOTE_ADDR");
    for (String header: IP_HEADERS) {
      final String value = request.getHeader(header);
      if (value == null || value.isEmpty()) {
        continue;
      }
      final String[] parts = value.split("\\s*,\\s*");
      final String requestIpAddress = parts[0];
      System.out.println("  from " + requestIpAddress + " (header: " + header + ")");
      return requestIpAddress;
    }
    return request.getRemoteAddr();
  }

}
