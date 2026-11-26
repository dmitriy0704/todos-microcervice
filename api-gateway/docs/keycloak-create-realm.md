# Создание realm и client

Сначала настройте Keycloak, а уже потом применяйте манифесты Istio. Когда вы
примените RequestAuthentication, Istio сразу полезет по jwksUri скачивать
публичные ключи для проверки подписей — если к этому моменту Realm еще не
создан, Istio получит 404 и не сможет проверять токены.

Шаг 1. Настройка Keycloak в админке (занимает 2 минуты)

1. Создайте новый Realm (не используйте системный master):
   • В левом верхнем углу нажмите на выпадающий список Keycloak (master) ->
   Create realm.
   • Введите имя: loadtest -> нажмите Create.
2. Отключите обязательный HTTPS для этого Realm:
   • В левом меню перейдите в Realm settings -> вкладка Login(или General).
   • В пункте Require SSL выберите None. (Без этого Keycloak откажется выдавать
   токены по внутреннему HTTP внутри кластера).
3. Увеличьте время жизни токена (чтобы он не истек во время теста):
   • Там же в Realm settings перейдите на вкладку Tokens.
   • Измените Access Token Lifespan на 30 минут (или больше, если тест
   длинный) -> нажмите Save.
4. Создайте Client для k6:
   • В левом меню перейдите в Clients -> Create client.
   • Client type: OpenID Connect.
   • Client ID: k6-client -> нажмите Next.
   • На шаге Capability config:
   • Включите Client authentication в положение On (это откроет вкладку
   Credentials с секретом).
   • В разделе Authentication flow отметьте Service accounts roles (стандартный
   flow для машинных API-запросов client_credentials).
   • Нажмите Next -> Save.
5. Скопируйте Client Secret:
   • Откройте созданный клиент k6-client -> перейдите на вкладку Credentials.
   • Скопируйте значение Client secret — оно понадобится в скрипте k6.

Шаг 2. Применение манифестов в Istio

Теперь, когда loadtest создан и его эндпоинты активны, примените политики в
namespace istio-system (обратите внимание, что адрес Keycloak теперь указывает
на default):

```yaml
apiVersion: security.istio.io/v1
kind: RequestAuthentication
metadata:
  name: jwt-auth
  namespace: istio-system
spec:
  selector:
    matchLabels:
      istio: ingressgateway
  jwtRules:
    - issuer: "http://cluster.local"
      jwksUri: "http://cluster.local/protocol/openid-connect/certs"
      forwardOriginalToken: true
---
apiVersion: security.istio.io/v1
kind: AuthorizationPolicy
metadata:
  name: require-jwt-for-tasks
  namespace: istio-system
spec:
  selector:
    matchLabels:
      istio: ingressgateway
  action: ALLOW
  rules:
    - from:
        - source:
            requestPrincipals: [ "*" ]
      to:
        - operation:
            paths: [ "/tasks/*" ]

```

Используйте код с осторожностью. (Важно: если у вас в Istio уже есть другие
сервисы, проходящие через istio-ingressgateway, учтите, что появление первой
ALLOW-политики на уровне gateway заблокирует все остальные пути, не
перечисленные в ней. Если через gateway ходят и другие пути без авторизации,
лучше вешать RequestAuthentication и AuthorizationPolicy непосредственно на поды
spring-cloud-gateway в default, а не на istio-ingressgateway).
Уточните, пожалуйста:
• Ходит ли через istio-ingressgateway какой-то еще трафик (например, метрики или
другие сервисы без авторизации), кроме /tasks/*
• Нужен ли готовый манифест Job для запуска k6 внутри вашего kind-кластера


