# Local:

# Baseline: tasks-service + PostgreSQL

## Условия

- tasks-service: Spring Boot 4.1.1, HikariCP 30, Tomcat 400
- PostgreSQL: 16-alpine, Docker
- k6: ramp-up 30s на каждый stage

## Результаты

| VU  | RPS | p(95)   | failed | max   |
|-----|-----|---------|--------|-------|
| 100 | 42  | 10.43ms | 0%     | 40ms  |
| 300 | 165 | 11.63ms | 0%     | 67ms  |
| 500 | 278 | 14.59ms | 0%     | 83ms  |
| 800 | 448 | 11.01ms | 0%     | 122ms |

## Метрики при 800 VU

- hikaricp_acquire_max: 38ms (нет перегрузки)
- hikaricp_pending: 0 (нет очереди)
- jvm_gc_pause_max: 7ms (GC не влияет)
- pg_stat_activity: 30 idle, 1 active (PG не напряжён)

## Вывод

Capacity > 800 VU. Точка деградации не достигнута.
Это baseline для сравнения с Gateway, Istio, Rate Limit.

# Подключаем Gateway

Baseline: tasks-service + PostgreSQL (без Gateway)

- 800 VU:  RPS = 448, p (95) = 11.01ms, 0% failed
- 2500 VU: RPS = 1353, p (95) = 8.34ms, 0% failed ← ЛУЧШИЙ результат
- HikariCP: 15 active, 0 pending (запас 2x)
- Capacity > 2500 VU

Layer 2: + Spring Cloud Gateway

- 800 VU:  RPS = 446, p (95) = 15.45ms, 0% failed
- Gateway overhead: +4.44ms на запрос
- HikariCP: 30 active, 15 pending (пул ИСЧЕРПАН)
- Gateway создаёт в 3 раза больше давления на пул

# Kind

## Слой 1: tasks-service + postgres

Baseline (kind): tasks-service + PostgreSQL

- 400 RPS, p (95) = 2.88ms, 0% failed (48397 запросов)
- max = 35ms (без JIT/port-forward overhead)
- k6 внутри кластера, DNS-резолвинг по имени Service

## Слой 2: + Spring Cloud Gateway

Результаты теста

VU:
500 - 1000 -1000 -0

HTTP
http_req_duration..............: avg=19.3ms min=626.48µs med=2.39ms max=1.27s p
(90)=11.8ms p (95)=29.34ms
{ expected_response:true }...: avg=19.3ms min=626.48µs med=2.39ms max=1.27s p
(90)=11.8ms p (95)=29.34ms
http_req_failed................: 0.00% 0 out of 74019
http_reqs......................: 74019 612.408189/s

VU:
1000 - 1250 -1250 -0

HTTP
http_req_duration..............: avg=22.89ms min=657.08µs med=2.34ms max=1.31s p
(90)=25.32ms p (95)=91.72ms
{ expected_response:true }...: avg=22.89ms min=657.08µs med=2.34ms max=1.31s p
(90)=25.32ms p (95)=91.72ms
http_req_failed................: 0.00% 0 out of 103212
http_reqs......................: 103212 854.700019/s

VU:
1000 - 1250 -1500 -0

HTTP
http_req_duration..............: avg=32.97ms min=358.4µs med=2.58ms max=1.81s p
(90)=55.74ms p (95)=124.34ms
{ expected_response:true }...: avg=32.97ms min=358.4µs med=2.58ms max=1.81s p
(90)=55.74ms p (95)=124.34ms
http_req_failed................: 0.00% 0 out of 109544
http_reqs......................: 109544 906.60288/s

VU:
1000 - 1500 -2000 -0

HTTP
http_req_duration..............: avg=111.59ms min=636.58µs med=11.57ms max=1.64s
p (90)=333.63ms p (95)=507.59ms
{ expected_response:true }...: avg=111.59ms min=636.58µs med=11.57ms max=1.64s p
(90)=333.63ms p (95)=507.59ms
http_req_failed................: 0.00% 0 out of 122292
http_reqs......................: 122292 1011.816797/s

#### Итог
Layer 2 (Spring Cloud Gateway):
- 900 VU:  p(95) = 6.22ms, 560 RPS, +3.3ms overhead  ← чисто
- 1250 VU: p(95) = 91.72ms, 854 RPS                   ← деградация начинается
- 1500 VU: p(95) = 124.34ms, 906 RPS                  ← серая зона
- 2000 VU: p(95) = 507.59ms, 1011 RPS                 ← потолок RPS
- Точка деградации: ~1000 VU (между 900 и 1250)
- Capacity: ~800–900 RPS (потолок ~1000 RPS)
- 0% ошибок во всех тестах
