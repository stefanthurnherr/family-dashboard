package org.bondor.dashboard;

import java.util.ArrayList;
import java.util.List;

import org.springframework.core.io.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.info.BuildProperties;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.stereotype.Controller;

@SpringBootApplication
@Controller
public class DashboardSlideshow {

  @Autowired
  BuildProperties buildProperties;

  @GetMapping(value = "/slideshow")
  public String slideshow(final Model model) throws Exception {
    System.out.println("Gathering slideshow...");
    final List<Resource> resources = new DashboardApplication().allImageResources();

    System.out.println("Found " + resources.size() + " available images:");
    final List<String> imageUrls = new ArrayList<String>();
    for (Resource resource : resources) {
      System.out.println("  Found image file: " + resource.getFilename() + " at " + resource.getURI());
      imageUrls.add(resource.getFilename());
    }

    model.addAttribute("imageUrls", imageUrls);

    return "slideshowTemplate";
  }

}
