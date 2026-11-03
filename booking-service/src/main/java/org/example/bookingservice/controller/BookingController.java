package org.example.bookingservice.controller;

import org.example.bookingservice.BookingRepository;
import org.example.bookingservice.dto.ApiResponse;
import org.example.bookingservice.dto.BookingRequest;
import org.example.bookingservice.model.Booking;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/bookings")
public class BookingController {

    private final BookingRepository bookingRepository;

    public BookingController(BookingRepository bookingRepository) {
        this.bookingRepository = bookingRepository;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Object>> createBooking(@Valid @RequestBody BookingRequest request) {
        LocalDateTime requestedTime;

        try {
            requestedTime = ZonedDateTime.parse(request.getDateTime()).toLocalDateTime();
        } catch (Exception e) {
            Map<String, String> errorDetails = new HashMap<>();
            errorDetails.put("errorCode", "INVALID_DATE_FORMAT");
            errorDetails.put("details", "Некорректный формат времени.");

            return ResponseEntity.status(400).body(new ApiResponse<>(
                    "ERROR",
                    "Ошибка валидации времени.",
                    errorDetails,
                    null)
            );
        }

        if (requestedTime.getMinute() != 0 || requestedTime.getSecond() != 0) {
            Map<String, String> errorDetails = new HashMap<>();
            errorDetails.put("errorCode", "INVALID_TIME_SLOT");
            errorDetails.put("details", "Запись возможна только на ровные часы.");

            return ResponseEntity.status(400).body(new ApiResponse<>(
                    "ERROR",
                    "Некорректный интервал.",
                    errorDetails,
                    null)
            );
        }

        int hour = requestedTime.getHour();

        if (hour < 9 || hour > 20) {
            Map<String, String> errorDetails = new HashMap<>();
            errorDetails.put("errorCode", "OUT_OF_WORKING_HOURS");
            errorDetails.put("details", "Автосалон закрыт в это время. Выберите часы с 09:00 до 20:00.");

            return ResponseEntity.status(400).body(new ApiResponse<>(
                    "ERROR",
                    "Время вне рабочих часов.",
                    errorDetails,
                    null)
            );
        }

        java.time.Instant nowUtc = java.time.Instant.now();
        java.time.ZoneId utcZone = java.time.ZoneId.of("UTC");
        LocalDateTime currentServerTimeUtc = LocalDateTime.ofInstant(nowUtc, utcZone);

        if (requestedTime.isBefore(currentServerTimeUtc)) {
            Map<String, String> errorDetails = new HashMap<>();
            errorDetails.put("errorCode", "PAST_DATE_FORBIDDEN");
            errorDetails.put("details", "Выбранное время уже прошло! Текущее время на сервере (UTC): "
                    + currentServerTimeUtc.toString().substring(11, 16) + ". Выберите будущую дату.");

            return ResponseEntity.status(400).body(new ApiResponse<>(
                    "ERROR",
                    "Отказ в резервировании.",
                    errorDetails,
                    null)
            );
        }

        Optional<Booking> existingBooking = bookingRepository.findByCarIdAndDateTime(request.getCarId(), requestedTime);

        if (existingBooking.isPresent()) {
            Map<String, String> errorDetails = new HashMap<>();
            errorDetails.put("errorCode", "SLOT_ALREADY_TAKEN");
            errorDetails.put("details", "Этот автомобиль уже забронирован на указанное время другим клиентом.");

            ApiResponse<Object> response = new ApiResponse<>(
                    "ERROR",
                    "Время уже занято.",
                    errorDetails,
                    null
            );

            return ResponseEntity.status(409).body(response);
        }

        String publicBookingId = "BK-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        Booking newBooking = new Booking(
                null,
                publicBookingId,
                request.getCarId(),
                requestedTime,
                "RESERVED",
                LocalDateTime.now()
        );

        Booking savedBooking = bookingRepository.save(newBooking);

        ApiResponse<Object> successResponse = new ApiResponse<>(
                "SUCCESS",
                "Слот времени успешно зарезервирован.",
                null,
                savedBooking
        );

        return ResponseEntity.status(200).body(successResponse);
    }
}