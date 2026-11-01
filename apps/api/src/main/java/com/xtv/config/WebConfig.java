package com.xtv.config;

import com.xtv.service.MediaStore;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
  private final MediaStore media;

  public WebConfig(MediaStore media) {
    this.media = media;
  }

  @Override
  public void addResourceHandlers(ResourceHandlerRegistry registry) {
    String location = media.root().resolve("public").toUri().toString();
    if (!location.endsWith("/")) {
      location = location + "/";
    }
    // Only public marketing assets. HLS streams go through MediaController with signed paths.
    registry.addResourceHandler("/media/covers/**").addResourceLocations(location + "covers/");
    registry.addResourceHandler("/media/avatars/**").addResourceLocations(location + "avatars/");
    registry.addResourceHandler("/media/ads/**").addResourceLocations(location + "ads/");
  }
}