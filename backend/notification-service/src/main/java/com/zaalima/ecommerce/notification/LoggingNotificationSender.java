package com.zaalima.ecommerce.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class LoggingNotificationSender implements NotificationSender {

    private static final Logger log = LoggerFactory.getLogger(LoggingNotificationSender.class);

    @Override
    public void send(String customerId, String subject, String message) {
        log.info("NOTIFY customer={} subject=\"{}\" message=\"{}\"", customerId, subject, message);
    }
}
