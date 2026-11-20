> я создал kind кластер, с таким конфигом

```yaml
kind: Cluster
apiVersion: kind.x-k8s.io/v1alpha4
nodes:
  - role: control-plane
    extraPortMappings:
  - containerPort: 80
    hostPort: 80
  - containerPort: 443
    hostPort: 443
```

> помоги в нем разобраться

Этот конфиг создает одноузловой кластер Kubernetes, где стандартные веб-порты
вашего Mac (80 и 443) напрямую соединены с портами внутри кластера. Это
позволяет принимать HTTP и HTTPS запросы (например, от k6 или браузера) по
простому адресу http://localhost без указания номера порта.

Разбор каждой строки конфига

• kind: Cluster и apiVersion: kind.x-k8s.io/v1alpha4 — служебные заголовки,
которые говорят утилите kind, что этот файл описывает конфигурацию нового
кластера.
• nodes: — список виртуальных узлов (нод), из которых будет состоять кластер.
• - role: control-plane — создает один главный узел. В Kind он выполняет сразу
две роли: управляет всем кластером и запускает ваши поды с микросервисами.
• extraPortMappings: — настройка проброса сетевых портов между вашим Mac
(хостом) и Docker-контейнером ноды Kind.
• hostPort: 80 -> containerPort: 80 — весь обычный веб-трафик (HTTP), который
приходит на localhost:80 вашего Mac, перенаправляется на порт 80 внутри ноды
Kind.
• hostPort: 443 -> containerPort: 443 — то же самое для защищенного трафика
(HTTPS) на порту 443.

Важный нюанс для вашего стека (Spring Cloud Gateway / Istio)

В Kubernetes обычные сервисы типа NodePort по умолчанию могут занимать только
порты в диапазоне 30000–32767. Порты 80 и 443 являются системными (ниже 1024),
поэтому обычный Service с типом NodePort не сможет занять порт 80 внутри
кластера.
Чтобы трафик с hostPort: 80 дошел до вашего приложения, у вас есть два рабочих
пути:

1. Использовать Istio Ingress Gateway или Nginx Ingress. Если вы поставите Istio
   с профилем default (или установите Ingress-контроллер с поддержкой
   hostNetwork: true), он сможет привязаться напрямую к порту 80 ноды и
   перенаправлять запросы в ваш Spring Cloud Gateway.
2. Использовать hostPort в поде Spring Cloud Gateway. Если вы не ставите Istio
   Ingress Gateway, в манифесте Deployment вашего Spring Cloud Gateway в секции
   ports нужно добавить hostPort: 80 (тогда под шлюза напрямую «сядет» на 80-й
   порт ноды и примет трафик с Mac).

Как проверить, что проброс портов работает

Выполните в терминале Mac команду docker ps. В колонке PORTS у вашего контейнера
...-control-plane вы должны увидеть запись вида:
0.0.0.0:80->80/tcp, 0.0.0.0:443->443/tcp
Это подтверждает, что Docker Desktop успешно открыл порты 80 и 443 на вашем Mac
и направил их в кластер.
Каким способом вы планируете принимать внешний трафик от k6 — через Istio
Ingress Gateway или напрямую в ваш Spring Cloud Gateway? Подскажу точный
YAML-манифест под выбранный вариант.

> через istio

Чтобы принимать внешний трафик от k6 через Istio Ingress Gateway на портах 80 и
443 в вашем кластере Kind, нужно установить Istio с профилем default и настроить
проброс портов на Ingress Gateway. В вашем текущем конфиге Kind порты 80 и 443
ноды уже открыты, поэтому достаточно связать их с Istio.

Шаг 1. Установка Istio с Ingress Gateway

Перейдите в вашу папку с Istio (cd ~/istio-*) и запустите установку с профилем
default (он включает и istiod, и istio-ingressgateway). Чтобы не тратить память
на сложные балансировщики в локальном кластере, сразу переведите тип сервиса в
NodePort с фиксированными портами:

```bash
./bin/istioctl install --set profile=default \
--set components.ingressGateways[0].name=istio-ingressgateway \
--set components.ingressGateways[0].enabled=true \
--set values.gateways.istio-ingressgateway.type=NodePort \
--set values.gateways.istio-ingressgateway.ports[0].name=http2 \
--set values.gateways.istio-ingressgateway.ports[0].port=80 \
--set values.gateways.istio-ingressgateway.ports[0].targetPort=8080 \
--set values.gateways.istio-ingressgateway.ports[0].nodePort=30080 \
--set values.gateways.istio-ingressgateway.ports[1].name=https \
--set values.gateways.istio-ingressgateway.ports[1].port=443 \
--set values.gateways.istio-ingressgateway.ports[1].targetPort=8443 \
--set values.gateways.istio-ingressgateway.ports[1].nodePort=30443 -y
```

Шаг 2. Важная поправка в конфиге Kind

Поскольку istio-ingressgateway в Kubernetes по умолчанию слушает порты 30080 и
30443 (как NodePort), в вашем конфиге Kind нужно направлять трафик с hostPort:
80 на containerPort: 30080 (а не на 80).
Если вы еще не развернули микросервисы, пересоздайте кластер с исправленным
kind-config.yaml:

```yaml
kind: Cluster
apiVersion: kind.x-k8s.io/v1alpha4
nodes:
  - role: control-plane
    extraPortMappings:
      - containerPort: 30080
        hostPort: 80
        protocol: TCP
      - containerPort: 30443
        hostPort: 443
        protocol: TCP
```

(Если пересоздавать кластер не хочется, можно в Deployment самого
istio-ingressgateway добавить hostNetwork: true, но пересоздание кластера с
правильным конфигом работает надежнее и чище).

Шаг 3. Связка Istio Gateway и Spring Cloud Gateway

Чтобы запросы, пришедшие на http://localhost (порт 80), попадали в ваш Spring
Cloud Gateway, примените следующий YAML-манифест (kubectl apply -f
gateway-routing.yaml):

```yaml
apiVersion: networking.istio.io/v1
kind: Gateway
metadata:
   name: main-gateway
spec:
   selector:
      istio: ingressgateway # привязка к стандартному Istio Ingress Gateway
   servers:
      - port:
           number: 80
           name: http
           protocol: HTTP
        hosts:
           - "*" # принимать запросы с любого хоста (включая localhost)
---
apiVersion: networking.istio.io/v1
kind: VirtualService
metadata:
   name: spring-gateway-route
spec:
   hosts:
      - "*"
   gateways:
      - main-gateway
   http:
      - route:
           - destination:
                host: spring-gateway # имя Service вашего Spring Cloud Gateway
                port:
                   number: 8080 # порт, на котором слушает Spring Cloud Gateway
```

Как будет идти трафик при нагрузке

1. k6 отправляет запрос на http://localhost/api/... (порт 80 на Mac).
2. Docker пробрасывает его на порт 30080 ноды Kind.
3. istio-ingressgateway принимает запрос, применяет глобальные политики
   (например, Rate Limit) и передает его по внутренней сети в Spring Cloud
   Gateway.
4. Spring Cloud Gateway маршрутизирует запрос дальше в ваши микросервисы, а
   sidecar-прокси Envoy между ними обеспечивают Circuit Breaker и Bulkhead.
   Нужен ли готовый шаблон DestinationRule для Istio, чтобы настроить Circuit
   Breaker (ограничение одновременных соединений и отключение сбойных подов) для
   ваших микросервисов?
