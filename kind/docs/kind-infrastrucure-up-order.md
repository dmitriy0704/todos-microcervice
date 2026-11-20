# Порядок установки и настройки кластере Kubernetes в kind.

## Компоненты

1. Config: config-server
2. Microservice: tasks-service, postgres
3. Gateway:
    - gateway/gateway-deployment.yaml
    - gateway/gateway-rbac.yaml
4. Istio:
    - istio/istio-gateway.yaml
    - istio/istio-security.yaml
    - istio/istio-virtualservice.yaml
5. Ratelimit:
    - ratelimit/redis-deployment.yaml
    - ratelimit/ratelimit-config.yaml
    - ratelimit/ratelimit-deployment.yaml
    - ratelimit/ratelimit-envoyfilter.yaml
    - message/kafka.yaml

## Настройка кластера:

1. Установка kind в систему
2. Конфигурация kind
3. Установка и настройка istio


