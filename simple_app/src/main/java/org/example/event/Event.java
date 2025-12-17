package org.example.event;

import org.springframework.context.ApplicationEventPublisher;

public class Event {

    private static ApplicationEventPublisher publisher;

    public static void setPublisher(ApplicationEventPublisher publisher) {
        Event.publisher = publisher;
    }

    public static void raise(Object event) {
        if (publisher == null) {
            return;
        }
        publisher.publishEvent(event);
    }
}