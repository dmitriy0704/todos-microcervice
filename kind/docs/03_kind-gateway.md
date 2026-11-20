# Порядок установки компонентов Gateway из api-gateway

Отлично, что Istio установился и `tasks-service` стал `2/2` — метка
`istio-injection=enabled` сработала, потому что вебхук в итоге активировался
корректно. Теперь — порядок для Gateway и Istio-ресурсов.

## 🎯 Правильный порядок применения

Порядок **не совсем** такой, как вы перечислили. Ключевое правило: **RBAC — до
`gateway-deployment`**, иначе под стартует без прав и упадёт с `403` (та самая
ошибка, что была раньше).

### Шаг 1: RBAC (Role + RoleBinding)

```bash
kubectl apply -f gateway-rbac.yaml
```

**Почему первым:** `spring-cloud-gateway` с `lb://tasks-service` при старте
обращается к Kubernetes API за списком endpoints. Без прав — `403 Forbidden`,
под падает. RBAC должен существовать **до** создания Deployment.

Проверьте:

```bash
kubectl get role,rolebinding | grep gateway
```

### Шаг 2: `gateway-deployment` + Service

```bash
kubectl apply -f gateway-deployment.yaml
```

**Что произойдёт:**

- Namespace уже помечен `istio-injection=enabled` → под создастся **сразу с
  sidecar** (`2/2`).
- Gateway с `lb://` получит права из RBAC (Шаг 1) и найдёт `tasks-service`.

Проверьте:

```bash
kubectl get pods -l app=spring-cloud-gateway
# Ожидаемо: 2/2 Running
```

**Не переходите к Шагу 3, пока Gateway не `2/2`** — иначе `VirtualService`
укажет в «никуда».

### Шаг 3: Istio `Gateway` (CRD)

```bash
kubectl apply -f istio-gateway.yaml
```

**Что это:** манифест `kind: Gateway` с `selector: istio: ingressgateway`. Он
говорит поду `istio-ingressgateway` слушать порт 80 и принимать HTTP.

**Почему до `VirtualService`:** `VirtualService` ссылается на `Gateway` по имени
(`gateways: - tasks-app-gateway`). Если Gateway ещё нет — VirtualService
создастся, но не привяжется.

Проверьте:

```bash
kubectl get gateway -n default
```

### Шаг 4: `VirtualService`

```bash
kubectl apply -f istio-virtualservice.yaml
```

**Что это:** правила маршрутизации — «трафик с Gateway → на Service
`spring-cloud-gateway:80`».

**Важно проверить:**

```yaml
http:
  - name: gateway-route          # ← имя нужно для HTTP_ROUTE-патча ratelimit
    route:
      - destination:
          host: spring-cloud-gateway
          port:
            number: 80           # ← порт Service, НЕ targetPort
```

Если `name: gateway-route` нет — позже `EnvoyFilter` для ratelimit не сматчится.

Проверьте:

```bash
kubectl get virtualservice
istioctl analyze -n default
```

### Шаг 5: `PeerAuthentication` (security)

```bash
kubectl apply -f istio-security.yaml
```

**Можно в любой момент** — `PERMISSIVE` безопасен, не ломает существующий
трафик. Но логичнее **после** того, как маршрутизация работает — если что-то
сломается, вы будете знать, что это не mTLS.

## 📋 Итоговая последовательность

```
1. gateway-rbac.yaml              ← Role + RoleBinding
2. gateway-deployment.yaml        ← под 2/2 (с sidecar)
3. istio-gateway.yaml             ← CRD Gateway
4. istio-virtualservice.yaml      ← CRD VirtualService
5. istio-security.yaml            ← PeerAuthentication (PERMISSIVE)
```

## ⚠️ Что проверить после каждого шага

| Шаг               | Проверка                                       | Ожидаемо                      |
|-------------------|------------------------------------------------|-------------------------------|
| 1. RBAC           | `kubectl get role,rolebinding`                 | `spring-cloud-gateway-reader` |
| 2. Deployment     | `kubectl get pods -l app=spring-cloud-gateway` | `2/2 Running`                 |
| 3. Gateway        | `kubectl get gateway`                          | `tasks-app-gateway`           |
| 4. VirtualService | `istioctl analyze`                             | `No validation issues`        |
| 5. PeerAuth       | `kubectl get peerauthentication`               | `default-permissive`          |

## 🚪 Доступ к Gateway снаружи (важно!)

`istio-ingressgateway` — `Service type=LoadBalancer`, но в `kind` **без
MetalLB** `EXTERNAL-IP` будет `<pending>`. Поэтому:

```bash
kubectl port-forward -n istio-system svc/istio-ingressgateway 8080:80
```

Теперь `http://localhost:8080/tasks/create` идёт **через Istio Ingress Gateway →
Spring Cloud Gateway → tasks-service**.

**Проверка всей цепочки:**

```bash
curl -v -X POST http://localhost:8080/tasks/create \
  -H "Content-Type: application/json" \
  -d '{"title":"test"}'
```

Ожидаемо: `201 Created`. Если `503` — Gateway не достучался до tasks-service
(проверьте RBAC и `lb://`). Если `404` — VirtualService не маршрутизирует
`/tasks/**`.

## 💡 Что сказать на собеседовании

> «Порядок развёртывания критичен: RBAC — до Deployment (иначе 403 при
> Discovery), Deployment — до VirtualService (иначе маршрут в никуда), Gateway
> CRD — до VirtualService (иначе нет привязки). Namespace помечен
> `istio-injection=enabled` **до** создания подов — они сразу рождаются с sidecar
> `2/2`, без перезапуска. PeerAuthentication в режиме PERMISSIVE применяю
> последним — он безопасен, но логичнее изолировать изменения.»

## 💎 Итог

| Вопрос                         | Ответ                                |
|--------------------------------|--------------------------------------|
| RBAC до или после Deployment?  | **ДО** — иначе 403 при старте        |
| Deployment до Istio Gateway?   | ✅ Да — Gateway должен быть `2/2`    |
| Gateway CRD до VirtualService? | ✅ Да — VS ссылается на Gateway      |
| PeerAuthentication когда?      | **Последним** — PERMISSIVE безопасен |
| Под будет `2/2` сразу?         | ✅ Да — namespace уже помечен        |

**Пошагово:**

1. `kubectl apply -f gateway-rbac.yaml`
2. `kubectl apply -f gateway-deployment.yaml` → дождаться `2/2`
3. `kubectl apply -f istio-gateway.yaml`
4. `kubectl apply -f istio-virtualservice.yaml`
5. `kubectl apply -f istio-security.yaml`
6. `kubectl port-forward -n istio-system svc/istio-ingressgateway 8080:80`
7. `curl localhost:8080/tasks/create` → `201`

Пришлите `kubectl get pods` после Шага 2 — убедимся, что Gateway `2/2`, и пойдём
дальше.