package org.example.mainbackendservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class TestDriveRequest {

    @NotBlank(message = "Имя клиента не может быть пустым")
    private String clientName;

    @NotBlank(message = "Телефон не может быть пустым")
    @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Некорректный формат номера телефона")
    private String clientPhone;

    @NotBlank(message = "Email не может быть пустым")
    @Email(message = "Некорректный формат Email")
    private String clientEmail;

    @NotBlank(message = "Дата должна быть указана")
    private String date; // "2026-08-15"

    @NotBlank(message = "Время должно быть указано")
    private String time; // "12:00"

    public TestDriveRequest() {
    }

    public TestDriveRequest(String clientName, String clientPhone, String clientEmail, String date, String time) {
        this.clientName = clientName;
        this.clientPhone = clientPhone;
        this.clientEmail = clientEmail;
        this.date = date;
        this.time = time;
    }

    public String getClientName() {
        return clientName;
    }

    public void setClientName(String clientName) {
        this.clientName = clientName;
    }

    public String getClientPhone() {
        return clientPhone;
    }

    public void setClientPhone(String clientPhone) {
        this.clientPhone = clientPhone;
    }

    public String getClientEmail() {
        return clientEmail;
    }

    public void setClientEmail(String clientEmail) {
        this.clientEmail = clientEmail;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }
}