package org.example.notificationservice.controller;

import org.example.notificationservice.dto.ApiResponse;
import org.example.notificationservice.dto.NotificationRequest;
import org.example.notificationservice.model.SmsLog;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

@RestController
@RequestMapping("/api/v1/notifications")
@CrossOrigin(origins = "http://localhost:3000")
public class NotificationController {

    private final List<SmsLog> smsStorage = new CopyOnWriteArrayList<>();

    @PostMapping("/send")
    public ResponseEntity<ApiResponse<Object>> sendNotification(@RequestBody NotificationRequest request) {

        int secretCode = 1000 + new Random().nextInt(9000);
        String smsText = "Код для подтверждения тест-драйва: " + secretCode;
        String notificationId = "NTF-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        SmsLog log = new SmsLog(
                notificationId,
                request.getPhoneNumber(),
                request.getBookingId(),
                smsText,
                LocalDateTime.now()
        );

        smsStorage.add(log);

        Map<String, String> responseData = new HashMap<>();
        responseData.put("notificationId", notificationId);
        responseData.put("status", "QUEUED");

        ApiResponse<Object> response = new ApiResponse<>(
                "SUCCESS",
                "Запрос на отправку SMS принят.",
                null,
                responseData
        );

        return ResponseEntity.status(200).body(response);
    }

    @GetMapping("/history")
    public ResponseEntity<ApiResponse<Object>> getHistory(@RequestParam(required = false) String phoneNumber) {
        List<SmsLog> filteredLogs = new ArrayList<>();

        for (SmsLog log : smsStorage) {

            if (phoneNumber == null || log.getPhoneNumber().equals(phoneNumber)) {
                filteredLogs.add(log);
            }
        }

        Collections.reverse(filteredLogs);

        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("logs", filteredLogs);

        ApiResponse<Object> response = new ApiResponse<>(
                "SUCCESS",
                "История уведомлений успешно выгружена.",
                null,
                responseBody
        );

        return ResponseEntity.status(200).body(response);
    }

    @PostMapping("/verify")
    public ResponseEntity<ApiResponse<Object>> verifyCode(@RequestBody Map<String, String> request) {
        String bookingId = request.get("bookingId");
        String inputCode = request.get("code");

        for (SmsLog log : smsStorage) {
            if (log.getBookingId().equals(bookingId)) {
                String realCode = log.getSmsText().replaceAll("[^0-9]", "");

                if (realCode.equals(inputCode)) {
                    return ResponseEntity.ok(new ApiResponse<>(
                            "SUCCESS",
                            "Код успешно подтвержден.",
                            null,
                            true)
                    );
                } else {
                    Map<String, String> err = new HashMap<>();
                    err.put("errorCode", "WRONG_SMS_CODE");
                    err.put("details", "Введен неверный код подтверждения из SMS.");

                    return ResponseEntity.status(400).body(new ApiResponse<>(
                            "ERROR",
                            "Ошибка проверки кода.",
                            err,
                            null)
                    );
                }
            }
        }

        Map<String, String> err = new HashMap<>();
        err.put("errorCode", "BOOKING_NOT_FOUND");

        return ResponseEntity.status(404).body(new ApiResponse<>(
                "ERROR",
                "Запись не найдена.",
                err,
                null)
        );
    }
}