# Установка и настройка kind

Установить Kind на macOS проще всего через менеджер пакетов Homebrew, а Istio
разворачивается в созданном кластере с помощью утилиты istioctl из вашей
существующей папки. Так как в качестве API-шлюза вы используете Java Spring
Cloud Gateway, вам не нужен тяжелый Istio Ingress Gateway, что еще сильнее
сэкономит память.

## Шаг 1. Установка Kind на macOS

Откройте терминал и выполните установку через Homebrew (убедитесь, что Docker
Desktop при этом запущен):

```bash
brew install kind
```

Используйте код с осторожностью.

```bash
kind version
```

## Шаг 2. Создание кластера с пробросом портов

Поскольку трафик от k6 пойдет напрямую в ваш Spring Cloud Gateway, нужно
пробросить порт с вашего Mac (например, 8080) на NodePort внутри кластера Kind
(например, 30080).

1. Создайте файл kind-config.yaml:

```yaml
   kind: Cluster
   apiVersion: kind.x-k8s.io/v1alpha4
   nodes:

     - role: control-plane
       extraPortMappings:
         - containerPort: 30080
           hostPort: 8080
           protocol: TCP
   ```

2. Создайте кластер с этим конфигом:

```bash
   kind create cluster --name microservices --config kind-config.yaml
 ```

После этого kubectl автоматически переключится на новый кластер
kind-microservices.

## Шаг 3. Установка легкого Istio из вашей папки

Так как роль входного шлюза выполняет Spring Cloud Gateway, вам нужен только
контроллер Istio (istiod) для управления трафиком, Circuit Breaker и Rate Limit
между сервисами.

1. Перейдите в вашу папку с Istio (обычно она называется istio-1.x.x):

```shell
   cd ~/istio-*
```   

2. Установите Istio с профилем minimal (он ставит только легкий istiod без
   лишних компонентов):

```sh
   ./bin/istioctl install --set profile=minimal -y
```

3. Включите автоматическое добавление Envoy-прокси (sidecar) в ваш namespace,
   где будут жить микросервисы (например, default):

```sh
   kubectl label namespace default istio-injection=enabled
 ```  

## Шаг 4. Подключение Spring Cloud Gateway и загрузка образов

- Загрузка локальных образов. В Kind не нужно настраивать локальный registry.
Чтобы загрузить ваши собранные Docker-образы (Spring Cloud Gateway и
микросервисы) внутрь кластера, используйте команду:bash
kind load docker-image имя-вашего-образа:тег --name microservices
Используйте код с осторожностью.
- Настройка Service для Spring Cloud Gateway. Чтобы принимать запросы от k6 по
адресу http://localhost:8080, в манифесте Service вашего Spring Cloud Gateway
укажите тип NodePort и порт 30080:yaml

```yaml
apiVersion: v1
kind: Service
metadata:
  name: spring-gateway
spec:
  type: NodePort
  ports:
  - port: 8080
    targetPort: 8080
    nodePort: 30080
  selector:
    app: spring-gateway

```

Используйте код с осторожностью.
  Нужна ли помощь с настройкой DestinationRule и VirtualService в Istio, чтобы
  перенести туда ваши правила для Circuit Breaker и Rate Limit?
