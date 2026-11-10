import http from 'k6/http';
import {check, sleep} from 'k6';

export const options = {
    // vus: 10,          // 10 виртуальных пользователей (Virtual Users)
    // duration: '30s',  // в течение 30 секунд(10/s)

    // stages: [
    //     { duration: '30s', target: 100 },
    //     { duration: '30s', target: 150 },
    //     { duration: '30s', target: 200 },
    //     { duration: '30s', target: 0 },
    // ],

    thresholds: {
        http_req_duration: ['p(95)<500'],   // 95% запросов быстрее 200ms
        http_req_failed: ['rate<0.05'],     // меньше 1% ошибок
    },
    scenarios: {
        load_test: {
            executor: 'constant-arrival-rate',
            rate: 120,
            timeUnit: '1s',
            duration: '2m',
            preAllocatedVUs: 150,
            maxVUs: 250,
        },
    },
};

function safeParseJson(str) {
    try {
        return JSON.parse(str);
    } catch {
        return null;
    }
}

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

    // Временно выводим первые 10 ответов
    if (__ITER < 10 && __VU <= 2) {
        console.log(JSON.stringify({
            vu: __VU,
            iteration: __ITER,
            status: res.status,
            error: res.error,
            error_code: res.error_code,
            body: res.body ? res.body.substring(0, 500) : null
        }));
    }

    if (res.status !== 201) {
        console.error(JSON.stringify({
            vu: __VU,
            iteration: __ITER,
            status: res.status,
            body: res.body,
            duration: res.timings.duration
        }));
    }
    // Проверяем, что статус ответа — 201 Created
    check(res, {
        'status is 201': (r) => r.status === 201,
        'has task id': (r) => {
            if (r.status !== 201) return false;   // ← не парсим тело на ошибках
            const body = safeParseJson(r.body);
            return body !== null && body.id !== undefined;
        },
        'fast enough': (r) => r.timings.duration < 500,
        // k6 покажет, сколько проверок прошло/провалилось. Если «has task id»
        // провалилась — значит, tasks-service возвращает некорректный JSON.
    });

    // Пауза в 1 секунду, имитируя пользователя, читающего ответ. Используется для stage
    // sleep(1);
}