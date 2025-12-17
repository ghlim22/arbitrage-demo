package org.example.info;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;

@RequiredArgsConstructor
@Configuration
public class InfoConfig {
   private final Info info;

   @PostConstruct
   public void init() {
       InfoUtil.setInfo(info);
   }
}
