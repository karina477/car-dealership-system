package org.example.mainbackendservice.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public class CarCreateRequest {

    @NotBlank(message = "VIN-номер не может быть пустым")
    @Size(min = 17, max = 17, message = "VIN-номер должен состоять строго из 17 символов")
    private String vin;

    @NotBlank(message = "Бренд не может быть пустым")
    private String brand;

    @NotBlank(message = "Модель не может быть пустой")
    private String model;

    @NotNull(message = "Год выпуска должен быть указан")
    @Min(value = 1900, message = "Год выпуска не может быть меньше 1900")
    @Max(value = 2027, message = "Год выпуска не может превышать текущий + 1")
    private Integer year;

    @NotNull(message = "Цена должна быть указана")
    @Positive(message = "Цена должна быть строго больше 0")
    private Double price;

    private Integer mileage;

    public CarCreateRequest() {
    }

    public CarCreateRequest(String vin, String brand, String model, Integer year, Double price, Integer mileage) {
        this.vin = vin;
        this.brand = brand;
        this.model = model;
        this.year = year;
        this.price = price;
        this.mileage = mileage;
    }

    public String getVin() {
        return vin;
    }

    public void setVin(String vin) {
        this.vin = vin;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;
    }

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }

    public Integer getMileage() {
        return mileage;
    }

    public void setMileage(Integer mileage) {
        this.mileage = mileage;
    }
}