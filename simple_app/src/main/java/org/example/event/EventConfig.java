package org.example.event;

import jakarta.annotation.PostConstruct;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EventConfig {

    private final ApplicationEventPublisher applicationEventPublisher;

    public EventConfig(ApplicationEventPublisher applicationEventPublisher) {
        this.applicationEventPublisher = applicationEventPublisher;
    }

    @PostConstruct
    public void init() {
        Event.setPublisher(applicationEventPublisher);
    }
}