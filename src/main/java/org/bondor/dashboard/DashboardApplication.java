package org.bondor.dashboard;

import java.io.InputStream;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;

import org.springframework.core.io.ClassPathResource;
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

  @RequestMapping(value = "/", method = RequestMethod.GET)
  public String home() {
      return "Hello Docker World - built " + DateTimeFormatter.ISO_INSTANT.format(buildProperties.getTime());
  }

  @RequestMapping(value = "/images", method = RequestMethod.GET)
  public ResponseEntity<String> listImages() throws Exception {
    final ClassPathResource imageFile = new ClassPathResource("static/");
    System.out.println("Listing images in " + imageFile);
    return ResponseEntity.ok()
            .contentType(MediaType.TEXT_PLAIN)
            .body("Not implemented yet!");
  }

  @RequestMapping(value = "/images/{id:[0-9]+}", method = RequestMethod.GET)
  public ResponseEntity<byte[]> image(@PathVariable("id") long id) throws Exception {
    //FIXME prevent string code injection/validate it is numeric
    final ClassPathResource imageFile = new ClassPathResource("static/" + id + ".jpg");
    System.out.println("Serving up image " + id + " as " + imageFile);

    if (!imageFile.exists()) {
      return ResponseEntity.notFound().build();
    }

    final byte[] imageBytes = StreamUtils.copyToByteArray(imageFile.getInputStream());
    return ResponseEntity.ok()
            .contentType(MediaType.IMAGE_JPEG)
            .body(imageBytes);
  }

  public static void main(String[] args) {
      SpringApplication.run(DashboardApplication.class, args);
  }

}
