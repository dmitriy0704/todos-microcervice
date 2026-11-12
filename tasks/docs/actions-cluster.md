# Работа с Kind

## Начало работы:

```shell
# 1. Создать kind-кластер
kind create cluster --config kind-config.yaml

# 2. Загрузить образы
kind load docker-image myorg/tasks-service:1.0 --name tasks-cluster

# 3. Применить ТОЛЬКО:
kubectl apply -f k8s/postgres/
kubectl apply -f k8s/tasks-service/

# 4. Проверить
kubectl get pods
kubectl get svc

# port-forward на tasks-service
kubectl port-forward svc/tasks-service 8091:8091

# curl
curl -X POST http://localhost:8091/tasks/create \
  -H "Content-Type: application/json" \
  -d '{"title":"test"}'
```

## Управление

### Основные команды самого Kind

```bash
# Создать кластер:
kind create cluster --name microservices --config kind-config.yaml

# Посмотреть список запущенных кластеров:
kind get clusters

# Загрузить локальный Docker-образ в кластер:
kind load docker-image my-service:1.0 --name microservices

# Удалить кластер полностью:
kind delete cluster --name microservices

# Переключить контекст kubectl (если у вас несколько кластеров):
kind export kubeconfig --name microservices
```

### Как остановить и запустить кластер без удаления

Поскольку каждая нода кластера в Kind — это просто контейнер в Docker Desktop,
вы можете управлять его питанием напрямую через Docker:

• Остановить кластер (аналог minikube stop):

```bash
docker stop microservices-control-plane
```

Примечание: имя контейнера всегда формируется по шаблону <имя-кластера>
-control-plane (если у вас кластер по умолчанию, он называется
kind-control-plane).

```bash
# Запустить остановленный кластер обратно:
docker start microservices-control-plane
```

Все ваши развернутые поды, базы данных, настройки Istio и микросервисы
восстановятся в течение 15–20 секунд.

```shell
# Поставить на паузу (заморозить процессы без выгрузки из памяти):bash
docker pause microservices-control-plane
```

Для снятия с паузы используйте `docker unpause microservices-control-plane`

