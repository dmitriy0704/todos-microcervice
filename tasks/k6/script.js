import http from 'k6/http';
import {check, sleep} from 'k6';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8091';


export const options = {
    // vus: 10,          // 10 виртуальных пользователей (Virtual Users)
    // duration: '30s',  // в течение 30 секунд(10/s)

    stages: [
        { duration: '30s', target: 400 },
        { duration: '30s', target: 600 },
        { duration: '30s', target: 800 },
        { duration: '30s', target: 0 },
    ],

    thresholds: {
        http_req_duration: ['p(95)<500'],   // 95% запросов быстрее 200ms
        http_req_failed: ['rate<0.05'],     // меньше 1% ошибок
    },
};

export default function () {
    const url = 'http://127.0.0.1:8091/tasks/create';
    const payload = JSON.stringify({
        title: `task-${__VU}-${__ITER}`, // Уникальное имя для каждой задачи
        description: `NEW task-${__VU}-${__ITER}`,
        completed: true
    });

    const params = {
        headers: {
            'Content-Type': 'application/json',
            'X-User-ID': `user-${__VU}`,   // каждый VU — отдельный пользователь
        },
    };

    // Отправляем POST-запрос
    const res = http.post(url, payload, params);
    // Проверяем, что статус ответа — 201 Created
    check(res, {'status is 201': (r) => r.status === 201,});
    // Пауза в 1 секунду, имитируя пользователя, читающего ответ. Используется для stage
    sleep(1);
}