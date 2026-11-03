const API_BASE = 'http://localhost:8080/api/v1';
let selectedCarId = null;
let lastBookingReference = null;

function showPage(pageId) {
    console.log(`[UI] Переключение на страницу: ${pageId}`);
    document.querySelectorAll('.page').forEach(p => p.classList.add('hidden'));
    document.getElementById(pageId).classList.remove('hidden');
}

document.getElementById('nav-catalog').addEventListener('click', () => { loadCars(); showPage('page-catalog'); });
document.getElementById('nav-admin').addEventListener('click', () => { showPage('page-admin'); });

async function loadCars() {
    const brand = document.getElementById('filter-brand').value;
    const minPrice = document.getElementById('filter-min-price').value;
    const maxPrice = document.getElementById('filter-max-price').value;

    let url = `${API_BASE}/cars?page=0&size=50`;
    if (brand) url += `&brand=${brand}`;
    if (minPrice) url += `&minPrice=${minPrice}`;
    if (maxPrice) url += `&maxPrice=${maxPrice}`;

    console.log(`[UI] Запрос списка машин. URL: ${url}`);
    try {
        const res = await fetch(url);
        const data = await res.json();
        console.log('[UI] Ответ от BFF (Список машин):', data);

        const grid = document.getElementById('cars-list');
        grid.innerHTML = '';

        if (data.responseStatus === 'SUCCESS' && data.responseBody.cars) {
            data.responseBody.cars.forEach(car => {
                const card = document.createElement('div');
                card.className = 'car-card';
                card.setAttribute('data-testid', `car-card-${car.id}`);
                card.innerHTML = `
                    <h4>${car.brand} ${car.model}</h4>
                    <p>Цена: $${car.price.toLocaleString()}</p>
                    <button data-testid="view-details-${car.id}" onclick="openCarDetails(${car.id})">Записаться</button>
                `;
                grid.appendChild(card);
            });
        }
    } catch (err) {
        console.error('[UI] КРИТИЧЕСКАЯ ОШИБКА при загрузке каталога:', err);
    }
}
document.getElementById('btn-apply-filters').addEventListener('click', loadCars);

async function openCarDetails(id) {
    selectedCarId = id;
    console.log(`[UI] Открытие карточки машины ID: ${id}`);
    document.getElementById('detail-title').innerText = `Оформление тест-драйва`;
    document.getElementById('detail-vin').innerText = 'Загрузка...';
    document.getElementById('detail-price').innerText = '';

    showPage('page-details');

    const res = await fetch(`${API_BASE}/cars?allStatuses=true`);
    const data = await res.json();
    const car = data.responseBody.cars.find(c => c.id === id);
    if (car) {
        document.getElementById('detail-vin').innerText = car.vin;
        document.getElementById('detail-price').innerText = `$${car.price}`;
        console.log(`[UI] Данные машины ID ${id} успешно отображены. VIN: ${car.vin}`);
    }
}

document.getElementById('btn-submit-booking').addEventListener('click', async () => {
    const errorBlock = document.getElementById('booking-error');
    errorBlock.classList.add('hidden');

    const dateElement = document.getElementById('book-date');
    const timeElement = document.getElementById('book-time');

    const dateVal = dateElement ? dateElement.value : '';
    const timeVal = timeElement ? timeElement.value : '';

    console.log(`[UI] Клик 'Записаться'. Данные: date='${dateVal}', time='${timeVal}'`);

    if (!dateVal || !timeVal) {
        console.warn('[UI] Валидация провалена: не выбрана дата или время');
        errorBlock.innerText = "Пожалуйста, выберите дату и время тест-драйва.";
        errorBlock.classList.remove('hidden');
        return;
    }

    const body = {
        clientName: document.getElementById('book-name').value,
        clientPhone: document.getElementById('book-phone').value,
        clientEmail: document.getElementById('book-email').value,
        date: dateVal,
        time: timeVal
    };

    const targetUrl = `${API_BASE}/cars/${selectedCarId}/test-drive`;
    console.log(`[UI] Отправка POST на BFF: ${targetUrl}`, body);

    try {
        const res = await fetch(targetUrl, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(body)
        });

        const data = await res.json();
        console.log('[UI] Ответ от BFF на запись:', data);

        if (data.responseStatus === 'SUCCESS') {
            lastBookingReference = data.responseBody.bookingReference;
            console.log(`[UI] Успешная запись! Номер брони сохранен: ${lastBookingReference}`);
            showPage('page-confirmation');
        } else {
            errorBlock.innerText = (data.responseError && data.responseError.details) ? data.responseError.details : data.responseMessage;
            errorBlock.classList.remove('hidden');
        }
    } catch (err) {
        console.error('[UI] Сбой сети при отправке записи на тест-драйв:', err);
    }
});

document.getElementById('btn-confirm-sms').addEventListener('click', async () => {
    const code = document.getElementById('sms-code').value;
    const errorBlock = document.getElementById('sms-error');
    const successBlock = document.getElementById('sms-success');

    errorBlock.classList.add('hidden');
    successBlock.classList.add('hidden');

    console.log(`[UI] Клик 'Подтвердить'. Код: ${code}, Бронь: ${lastBookingReference}`);

    try {
        const res = await fetch('http://localhost:8083/api/v1/notifications/verify', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
                bookingId: lastBookingReference,
                code: code
            })
        });

        const data = await res.json();
        console.log('[UI] Ответ от сервиса проверки кода:', data);

        if (data.responseStatus === 'SUCCESS') {
            console.log('[UI] СМС подтверждена успешно!');
            successBlock.classList.remove('hidden');
        } else {
            errorBlock.innerText = (data.responseError && data.responseError.details) ? data.responseError.details : "Неверный код!";
            errorBlock.classList.remove('hidden');
        }
    } catch (err) {
        console.error('[UI] Ошибка сети при верификации СМС:', err);
    }
});

document.getElementById('btn-save-car').addEventListener('click', async () => {
    document.getElementById('error-vin').classList.add('hidden');
    document.getElementById('error-price').classList.add('hidden');
    document.getElementById('admin-success').classList.add('hidden');

    const body = {
        vin: document.getElementById('admin-vin').value,
        brand: document.getElementById('admin-brand').value,
        model: document.getElementById('admin-model').value,
        year: parseInt(document.getElementById('admin-year').value),
        price: parseFloat(document.getElementById('admin-price').value),
        mileage: 0
    };

    console.log('[UI] Админка: отправка запроса на создание машины:', body);

    try {
        const res = await fetch(`${API_BASE}/cars`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(body)
        });

        const data = await res.json();
        console.log('[UI] Ответ от BFF (Создание машины):', data);

        if (data.responseStatus === 'SUCCESS') {
            document.getElementById('admin-success').classList.remove('hidden');
        } else {
            if (data.responseError && data.responseError.invalidFields) {
                const fields = data.responseError.invalidFields;
                if (fields.vin) {
                    document.getElementById('error-vin').innerText = fields.vin;
                    document.getElementById('error-vin').classList.remove('hidden');
                }
                if (fields.price) {
                    document.getElementById('error-price').innerText = fields.price;
                    document.getElementById('error-price').classList.remove('hidden');
                }
            } else if (data.responseError && data.responseError.errorCode === 'INVALID_VIN_FORMAT') {
                document.getElementById('error-vin').innerText = data.responseError.details;
                document.getElementById('error-vin').classList.remove('hidden');
            } else {
                alert(data.responseMessage);
            }
        }
    } catch (err) {
        console.error('[UI] Сбой сети в админке при создании авто:', err);
    }
});

loadCars();