package org.bondor.dashboard;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.math.BigInteger;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.Comparator;

import com.drew.metadata.Directory;
import com.drew.metadata.Tag;
import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.jpeg.JpegMetadataReader;
import com.drew.metadata.Metadata;

import org.springframework.core.io.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.info.BuildProperties;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.stereotype.Controller;

@Controller
public class DashboardSlideshow {

  @Autowired
  BuildProperties buildProperties;

  @GetMapping(value = "/slideshow")
  public String slideshow(final Model model) throws Exception {
    System.out.println("Gathering slideshow...");
    final List<Resource> resources = new DashboardApplication().allImageResources();

    // Signal strips files of all EXIF data (as of 2022-oct), so no point in trying to read it.
    // Some related resources to read:
    //  https://github.com/signalapp/Signal-Android/issues/7865
    //  https://community.signalusers.org/t/add-option-to-keep-or-strip-meta-data/3005
    //sortByExifCreatedDate(resources);

    final Map<Resource, Long> resourceToTimestamp = new HashMap<>();
    for (final Resource resource : resources){
      resourceToTimestamp.put(resource, resource.lastModified());
    }
    resources.sort(Comparator.comparing(r -> resourceToTimestamp.get(r)));

    System.out.println("Found " + resources.size() + " available images:");
    final List<String> imageUrls = new ArrayList<String>();
    final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    for (Resource resource : resources) {
      final String imageTimestamp = dateFormat.format(resource.getFile().lastModified());
      System.out.println("  Found image file " + resource.getFilename()
                        + " created at " + imageTimestamp
                        + " located at " + resource.getURI());
      imageUrls.add("images/" + resource.getFilename());
    }

    model.addAttribute("imageUrls", imageUrls);

    model.addAttribute("intervalMsecs", 3000);

    model.addAttribute("slideshowChecksum", calculateChecksum(resources));

    return "slideshowTemplate";
  }

  @GetMapping(value = "/slideshowChecksum")
  public String slideshowChecksum(final Model model) throws Exception {
    final List<Resource> resources = new DashboardApplication().allImageResources();
    return calculateChecksum(resources);
  }

  private static String calculateChecksum(List<Resource> resources) throws Exception {
    final MessageDigest shaDigest = MessageDigest.getInstance("SHA3-256");

    byte[] byteArray = new byte[1024];
    int bytesCount = 0;
    for (final Resource resource : resources) {
      try (InputStream inputStream = resource.getInputStream()) {
        while ((bytesCount = inputStream.read(byteArray)) != -1) {
          shaDigest.update(byteArray, 0, bytesCount);
        };
      }
    }

    final String hashText = convertToHex(shaDigest.digest());
    return hashText;
  }

  private static String convertToHex(final byte[] bytes) {
      final BigInteger bigint = new BigInteger(1, bytes);
      String hexText = bigint.toString(16);
      while (hexText.length() < 32) {
         hexText = "0".concat(hexText);
      }
      return hexText;
   }

  private void sortByExifCreatedDate(List<Resource> unsortedResources) throws Exception {
    for (final Resource resource : unsortedResources) {
      System.out.println("\nProcessing file " + resource.getFilename());
      //final Metadata metadata = ImageMetadataReader.readMetadata(resource.getFile());
      final Metadata metadata = JpegMetadataReader.readMetadata(resource.getFile());
      for (final Directory directory : metadata.getDirectories()) {
        for (Tag tag : directory.getTags()) {
          System.out.println(tag.getDirectoryName() + " -- " + tag);
        }
      }
    }
  }

}
