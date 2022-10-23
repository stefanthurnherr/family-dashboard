package org.bondor.dashboard;

import java.io.IOException;
import java.util.Collections;
import java.text.SimpleDateFormat;
import java.util.Date;
import com.drew.metadata.Directory;
import com.drew.metadata.Tag;
import java.net.URI;
import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.jpeg.JpegMetadataReader;
import com.drew.metadata.Metadata;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.Comparator;

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

    return "slideshowTemplate";
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
