package org.example.mainbackendservice.controller;


import org.example.mainbackendservice.dto.ApiResponse;
import org.example.mainbackendservice.dto.CarCreateRequest;
import org.example.mainbackendservice.dto.TestDriveRequest;
import org.example.mainbackendservice.model.Car;
import jakarta.validation.Valid;
import org.example.mainbackendservice.repository.CarRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/v1/cars")
@CrossOrigin(origins = "http://localhost:3000")
public class CarController {

    private static final Logger log = LoggerFactory.getLogger(CarController.class);

    private final CarRepository carRepository;
    private final RestTemplate restTemplate;

    public CarController(CarRepository carRepository, RestTemplate restTemplate) {
        this.carRepository = carRepository;
        this.restTemplate = restTemplate;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Object>> createCar(@Valid @RequestBody CarCreateRequest request) {
        log.info(
                "[BFF] Получен запрос на создание машины: VIN={}, Brand={}, Model={}",
                request.getVin(),
                request.getBrand(),
                request.getModel()
        );

        String vin = request.getVin().toUpperCase();

        if (vin.contains("I") || vin.contains("O") || vin.contains("Q")) {
            log.warn("[BFF] Валидация провалена: VIN содержит запрещенные символы: {}", vin);
            Map<String, String> errorDetails = new HashMap<>();
            errorDetails.put("errorCode", "INVALID_VIN_FORMAT");
            errorDetails.put("details", "VIN должен состоять из 17 символов и не содержать латинские буквы I, O, Q.");

            return ResponseEntity.status(400).body(new ApiResponse<>(
                    "ERROR",
                    "Ошибка валидации параметров автомобиля.",
                    errorDetails,
                    null)
            );
        }

        Optional<Car> existingCar = carRepository.findByVinIgnoreCase(vin);

        if (existingCar.isPresent()) {
            log.warn("[BFF] Отказ записи: Машина с VIN {} уже существует в БД", vin);
            Map<String, String> errorDetails = new HashMap<>();
            errorDetails.put("errorCode", "DUPLICATE_VIN");
            errorDetails.put("details", "Автомоль с VIN-номером " + request.getVin() + " уже существует в базе данных.");

            return ResponseEntity.status(409).body(new ApiResponse<>(
                    "ERROR",
                    "Регистрация автомобиля отклонена.",
                    errorDetails,
                    null)
            );
        }

        Car newCar = new Car(
                null,
                vin,
                request.getBrand(),
                request.getModel(),
                request.getYear(),
                request.getPrice(),
                request.getMileage() != null ? request.getMileage() : 0,
                "AVAILABLE", LocalDateTime.now()
        );

        Car savedCar = carRepository.save(newCar);
        log.info("[BFF] Автомобиль успешно сохранен в БД. Назначен ID: {}", savedCar.getId());

        return ResponseEntity.status(201).body(new ApiResponse<>(
                "SUCCESS",
                "Автомобиль успешно добавлен в каталог.",
                null,
                savedCar)
        );
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Object>> getCars(
            @RequestParam(required = false) String brand,
            @RequestParam(required = false) Double minPrice,
            @RequestParam(required = false) Double maxPrice,
            @RequestParam(defaultValue = "false") Boolean allStatuses,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id,asc") String sortBy) {

        log.info(
                "[BFF] Получен запрос списка автомобилей. Фильтры: brand={}, minPrice={}, maxPrice={}",
                brand,
                minPrice,
                maxPrice
        );

        if (minPrice != null && maxPrice != null && minPrice > maxPrice) {
            log.warn("[BFF] Неверный ценовой диапазон: minPrice={} > maxPrice={}", minPrice, maxPrice);
            Map<String, String> errorDetails = new HashMap<>();
            errorDetails.put("errorCode", "INVALID_PRICE_RANGE");
            errorDetails.put("details", "Минимальная цена не может быть выше максимальной цены.");

            return ResponseEntity.status(400).body(new ApiResponse<>(
                    "ERROR",
                    "Некорректные параметры фильтрации.",
                    errorDetails,
                    null)
            );
        }

        if (size > 100) size = 100;

        List<String> allowedStatuses =
                allStatuses ? Arrays.asList("AVAILABLE", "RESERVED", "SOLD") : Collections.singletonList("AVAILABLE");
        String[] sortParams = sortBy.split(",");
        String sortField = sortParams[0];
        Sort.Direction sortDirection =
                (sortParams.length > 1 && sortParams[1].equalsIgnoreCase("desc")) ?
                        Sort.Direction.DESC : Sort.Direction.ASC;

        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortField));
        Page<Car> carPage = carRepository.findCarsWithFilters(brand, minPrice, maxPrice, allowedStatuses, pageable);
        log.info("[BFF] Из базы успешно извлечено {} записей авто", carPage.getNumberOfElements());

        Map<String, Object> paginationData = new HashMap<>();
        paginationData.put("cars", carPage.getContent());
        Map<String, Object> meta = new HashMap<>();
        meta.put("totalItems", carPage.getTotalElements());
        meta.put("currentPage", carPage.getNumber());
        meta.put("pageSize", carPage.getSize());
        meta.put("totalPages", carPage.getTotalPages());
        paginationData.put("pagination", meta);

        return ResponseEntity.status(200).body(new ApiResponse<>(
                "SUCCESS",
                "Список автомобилей успешно получен.",
                null,
                paginationData)
        );
    }

    @PostMapping("/{id}/test-drive")
    public ResponseEntity<ApiResponse<Object>> bookTestDrive(
            @PathVariable Long id,
            @Valid @RequestBody TestDriveRequest request
    ) {
        log.info(
                "[BFF] >>> Сквозной процесс: Заявка на тест-драйв авто ID {}. Для клиента: {}",
                id,
                request.getClientName()
        );

        Optional<Car> carOptional = carRepository.findById(id);

        if (carOptional.isEmpty()) {
            log.warn("[BFF] Машина с ID {} не найдена", id);
            Map<String, String> errorDetails = new HashMap<>();
            errorDetails.put("errorCode", "CAR_NOT_FOUND");
            errorDetails.put("details", "Автомобиль с ID " + id + " не найден в системе.");

            return ResponseEntity.status(404).body(new ApiResponse<>(
                    "ERROR",
                    "Ошибка оформления записи.",
                    errorDetails,
                    null));
        }

        Car car = carOptional.get();
        String formattedDateTime = request.getDate() + "T" + request.getTime() + ":00Z";
        log.info("[BFF] Проверки пройдены. Склеенная дата-время UTC: {}", formattedDateTime);

        String bookingUrl = "http://car-booking:8080/api/v1/bookings";
        Map<String, Object> bookingBody = new HashMap<>();
        bookingBody.put("carId", car.getId());
        bookingBody.put("dateTime", formattedDateTime);

        log.info("[BFF] HTTP Вызов -> booking-service: URL={}, Body={}", bookingUrl, bookingBody);
        String bookingId;

        try {
            ResponseEntity<ApiResponse> bookingResult =
                    restTemplate.postForEntity(bookingUrl, bookingBody, ApiResponse.class);
            log.info(
                    "[BFF] booking-service вернул статус HTTP: {}, Тело: {}",
                    bookingResult.getStatusCode(),
                    bookingResult.getBody()
            );

            if (bookingResult.getBody() == null || !"SUCCESS".equals(bookingResult.getBody().getResponseStatus())) {
                String serverReason = "Выбранное время недоступно.";

                if (bookingResult.getBody() != null && bookingResult.getBody().getResponseError() != null) {
                    Map<String, Object> err = (Map<String, Object>) bookingResult.getBody().getResponseError();

                    if (err.containsKey("details")) serverReason = (String) err.get("details");
                }

                log.warn("[BFF] Бронирование отклонено внутренней логикой: {}", serverReason);
                Map<String, String> errorDetails = new HashMap<>();
                errorDetails.put("errorCode", "TIME_SLOT_REJECTED");
                errorDetails.put("details", serverReason);

                return ResponseEntity.status(409).body(new ApiResponse<>(
                        "ERROR",
                        "Отказ бронирования.",
                        errorDetails,
                        null)
                );
            }

            Map<String, Object> bodyMap = (Map<String, Object>) bookingResult.getBody().getResponseBody();
            bookingId = (String) bodyMap.get("bookingId");
            log.info("[BFF] Успешная бронь в booking-service. ID Бронирования: {}", bookingId);
        } catch (Exception e) {
            log.error("[BFF] ИСКЛЮЧЕНИЕ СВЯЗИ при вызове booking-service: ", e);
            Map<String, String> errorDetails = new HashMap<>();
            errorDetails.put("errorCode", "BOOKING_SERVICE_UNAVAILABLE");
            errorDetails.put("details", "Вспомогательный сервис бронирования недоступен.");

            return ResponseEntity.status(503).body(new ApiResponse<>(
                    "ERROR",
                    "Ошибка связи между сервисами.",
                    errorDetails,
                    null)
            );
        }

        String notificationUrl = "http://car-notification:8080/api/v1/notifications/send";
        Map<String, Object> notificationBody = new HashMap<>();
        notificationBody.put("phoneNumber", request.getClientPhone());
        notificationBody.put("bookingId", bookingId);

        log.info("[BFF] HTTP Вызов -> notification-service: URL={}", notificationUrl);
        String notificationStatus = "PENDING";

        try {
            ResponseEntity<ApiResponse> ntfResult =
                    restTemplate.postForEntity(notificationUrl, notificationBody, ApiResponse.class);
            log.info("[BFF] notification-service вернул ответ: {}", ntfResult.getBody().getResponseStatus());

            if (ntfResult.getBody() != null && "SUCCESS".equals(ntfResult.getBody().getResponseStatus())) {
                notificationStatus = "SENT";
            }
        } catch (Exception e) {
            log.error("[BFF] Ошибка отправки уведомления (не прерывает основной процесс): ", e);
            notificationStatus = "FAILED_TO_SEND";
        }

        Map<String, Object> testDriveSummary = new HashMap<>();
        testDriveSummary.put("bookingReference", bookingId);
        testDriveSummary.put("carId", car.getId());
        testDriveSummary.put("dateTime", formattedDateTime);
        testDriveSummary.put("clientName", request.getClientName());
        Map<String, String> serviceStatuses = new HashMap<>();
        serviceStatuses.put("bookingStatus", "CONFIRMED");
        serviceStatuses.put("notificationStatus", notificationStatus);
        testDriveSummary.put("externalServices", serviceStatuses);
        log.info("[BFF] <<< Сквозной процесс завершен успешно для клиента: {}", request.getClientName());

        return ResponseEntity.status(200).body(new ApiResponse<>(
                "SUCCESS",
                "Запись на тест-драйв успешно оформлена.",
                null,
                testDriveSummary)
        );
    }
}