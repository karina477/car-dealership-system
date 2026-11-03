package org.example.notificationservice.model;

import java.time.LocalDateTime;

public class SmsLog {
    private String notificationId;
    private String phoneNumber;
    private String bookingId;
    private String smsText;
    private LocalDateTime sentAt;

    public SmsLog() {
    }

    public SmsLog(String notificationId, String phoneNumber, String bookingId, String smsText, LocalDateTime sentAt) {
        this.notificationId = notificationId;
        this.phoneNumber = phoneNumber;
        this.bookingId = bookingId;
        this.smsText = smsText;
        this.sentAt = sentAt;
    }

    public String getNotificationId() {
        return notificationId;
    }

    public void setNotificationId(String notificationId) {
        this.notificationId = notificationId;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getBookingId() {
        return bookingId;
    }

    public void setBookingId(String bookingId) {
        this.bookingId = bookingId;
    }

    public String getSmsText() {
        return smsText;
    }

    public void setSmsText(String smsText) {
        this.smsText = smsText;
    }

    public LocalDateTime getSentAt() {
        return sentAt;
    }

    public void setSentAt(LocalDateTime sentAt) {
        this.sentAt = sentAt;
    }
}