package org.example.notificationservice.dto;

public class NotificationRequest {
    private String phoneNumber;
    private String bookingId;

    public NotificationRequest() {
    }

    public NotificationRequest(String phoneNumber, String bookingId) {
        this.phoneNumber = phoneNumber;
        this.bookingId = bookingId;
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
}