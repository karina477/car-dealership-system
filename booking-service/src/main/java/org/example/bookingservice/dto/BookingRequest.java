package org.example.bookingservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class BookingRequest {

    @NotNull(message = "ID автомобиля должен быть указан")
    private Long carId;

    @NotBlank(message = "Дата и время должны быть указаны")
    private String dateTime;

    public BookingRequest() {
    }

    public BookingRequest(Long carId, String dateTime) {
        this.carId = carId;
        this.dateTime = dateTime;
    }

    public Long getCarId() {
        return carId;
    }

    public void setCarId(Long carId) {
        this.carId = carId;
    }

    public String getDateTime() {
        return dateTime;
    }

    public void setDateTime(String dateTime) {
        this.dateTime = dateTime;
    }
}