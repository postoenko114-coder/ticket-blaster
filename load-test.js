import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
    stages: [
        { duration: '30s', target: 300 },
        { duration: '1m', target: 300 },
    ],
    thresholds: {
        http_req_duration: ['p(95)<300'],
        http_req_failed: ['rate<0.01'],
    },
};

const BASE_URL = 'http://host.docker.internal:8080/api/book';


export function setup() {
    const eventId = 1;
    const count = 1000000;

    const initUrl = `${BASE_URL}/init?eventId=${eventId}&count=${count}`;
    const res = http.post(initUrl);

    console.log(`[Setup] Инициализация билетов: статус ${res.status}`);

    return { eventId: eventId };
}


export default function (data) {
    const url = BASE_URL;

    const randomUserId = Math.floor(Math.random() * 1000000);
    const randomQuantity = Math.floor(Math.random() * 4) + 1;

    const payload = JSON.stringify({
        userId: randomUserId,
        userEmail: `testuser${randomUserId}@gmail.com`,
        eventId: data.eventId,
        quantity: randomQuantity
    });
    const params = {
        headers: {
            'Content-Type': 'application/json',
        },
    };

    const res = http.post(url, payload, params);

    check(res, {
        'status is 200': (r) => r.status === 200,
    });

    sleep(0.01);
}