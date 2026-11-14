# Инфраструктурные слои кластера для тестирования

## Слой 1: tasks-service + PostgreSQL (baseline, который вы уже измерили)


### Нужно:

Сервисы запускаются по порядку:

**PostgreSQL**:

- postgres-deployment.yaml:
  `tasks-service/k8s/db/tasks-postgres-deployment.yaml`,
- postgres-service.yaml: `tasks-postgres-deployment.yaml`,
- postgres-secret.yaml: `tasks/k8s/db/tasks-postgres-secret.yaml`,
- postgres-pvc.yaml:`tasks-postgres-deployment.yaml`

**Config Server**(подтягиваются конфиги для подключения tasks-service к БД) :

- config-server-deployment.yaml:
  `config-server/k8s/config-server-deployment.yaml`,
- config-server-service.yaml :
  `config-server/k8s/config-server-deployment.yaml`


**tasks-service**(запускается и получает настройки подключения из config server):

- tasks-service-deployment.yaml: `tasks-service/k8s/tasks-deployment.yaml`,
- tasks-service-service.yaml:`tasks-service/k8s/tasks-deployment.yaml`,
- tasks-service-secret.yaml:`tasks/k8s/db/tasks-postgres-secret.yaml`


**НЕ нужно:**

- ❌ Spring Cloud Gateway — он **внешний** слой, не влияет на tasks-service
- ❌ Istio (Gateway, VirtualService, PeerAuthentication, RequestAuthentication) —
  это **сеть**, не код
- ❌ Rate limit (ratelimit-config, deployment, envoyfilter, redis) — это **слой
  4**
- ❌ Kafka — **вы его уже убрали** из кода, зачем он в кластере?

**Итог:** 4–6 YAML, ~500MB RAM. **Этого достаточно** для baseline.

## Слой 2: + Spring Cloud Gateway

**Добавляете:**

**RBAC** (если используете `lb://`, сначала запустить его):

- -gateway-role.yaml: `../k8s/gateway/gateway-rbac.yaml`
- gateway-rolebinding.yaml: `../k8s/gateway/gateway-rbac.yaml`


**Gateway(потом его)**:

- gateway-deployment.yaml: `../k8s/gateway/gateway-deployment.yaml`
- gateway-service.yaml: `../k8s/gateway/gateway-deployment.yaml`


**Config Server** (если не было):

- config-server-deployment.yaml:
  `config-server/k8s/config-server-deployment.yaml`
- config-server-service.yaml: `config-server/k8s/config-server-deployment.yaml)`

**Про RBAC:** нужен **только если** в `application.yaml` Gateway использует
`lb://tasks-service` (Spring Cloud Kubernetes Discovery). Если используете
прямой `http://tasks-service:8091` — **RBAC не нужен**.

**НЕ нужно пока:**

- ❌ Istio (Gateway, VirtualService) — это слой 3
- ❌ Rate limit — слой 4
- ❌ Kafka — не влияет

## Слой 3: + Istio

**Добавляете:**

`istioctl install` - Устанавливает Istio в кластер

`tasks-service-vs.yaml` (VirtualService) - Маршрутизация от Ingress к Gateway
../k8s/istio/istio-virtualservice.yaml

`tasks-service-gateway.yaml`(Gateway) - Точка входа на порту 80/443
../k8s/istio/istio-gateway.yaml

`tasks-service-dr.yaml`(DestinationRule) - Circuit breaker, subsets (если нужно)

**`PeerAuthentication`** — **только если** тестируете mTLS (`STRICT` режим). Для
первого теста — **не нужно**, оставьте `PERMISSIVE` (по умолчанию) или вообще
без него.

**`RequestAuthentication` / `AuthorizationPolicy`** — **только если** тестируете
JWT. Для нагрузочного теста без аутентификации — **не нужно**.

## Слой 4: + Rate limit

**Добавляете:**

Redis:

- redis-deployment.yaml: `../k8s/ratelimit/redis-deployment.yaml`
- redis-service.yaml: `../k8s/ratelimit/redis-deployment.yaml`

ratelimit

- ratelimit-config.yaml: `../k8s/ratelimit/ratelimit-config.yaml`
- ratelimit-deployment.yaml: `../k8s/ratelimit/ratelimit-deployment.yaml`
- ratelimit-service.yaml: `../k8s/ratelimit/ratelimit-deployment.yaml`

EnvoyFilter

- ratelimit-envoyfilter.yaml: `../k8s/ratelimit/ratelimit-envoyfilter.yaml`

rateLimits в VirtualService - Обновляете `VirtualService` (или EnvoyFilter —
как вы решили)

**Только сейчас** появляются все эти ratelimit-манифесты. **До слоя 4 — они не
нужны.**

### Слой 5: + Kafka (если вернёте)

**Добавляете:**

Kafka

- `kafka-deployment.yaml`, `kafka-service.yaml`: ../k8s/message/kafka.yaml
  Обновляете `tasks-service` - Возвращаете `KafkaTemplate.send()` в код

**Помните:** вы **убрали** Kafka из `createTask`, потому что она **блокировала**
запрос. **Пока не вернёте** — Kafka в кластере **не нужна**.

## 📊 Итоговая таблица — что когда применять

| Слой  | Что применяете                             | RAM    | Что тестируете              |
|-------|--------------------------------------------|--------|-----------------------------|
| **1** | postgres + tasks-service (+ config-server) | ~500MB | Baseline capacity           |
| **2** | + gateway (+ RBAC, если `lb://`)           | ~800MB | Overhead Gateway            |
| **3** | + Istio (Gateway, VirtualService)          | ~2GB   | Overhead Istio              |
| **4** | + redis + ratelimit + EnvoyFilter          | ~2.5GB | Overhead Rate Limit + 429   |
| **5** | + Kafka                                    | ~3GB   | Async events (если вернёте) |

**Ключевое:** каждый слой **добавляется только когда предыдущий стабилен и
измерен**. Не раньше.

