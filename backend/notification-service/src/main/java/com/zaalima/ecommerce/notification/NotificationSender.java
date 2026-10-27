package com.zaalima.ecommerce.notification;

/** Port for outbound channels. Swap the logging adapter for SES/SendGrid/Twilio/FCM. */
public interface NotificationSender {
    void send(String customerId, String subject, String message);
}
