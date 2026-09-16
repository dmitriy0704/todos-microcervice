# Настройка приложения для запуска в Kubernetes

Отличный шаг! Minikube идеально подходит для локальной разработки и тестирования манифестов перед деплоем в облако.
Отвечая на ваш вопрос: да, файлы конфигурации Kubernetes обычно хранят прямо в корневой папке проекта (рядом с Dockerfile и pom.xml). Это стандартная практика, так как манифесты инфраструктуры версиируются в Git вместе с кодом приложения.
Для удобства рекомендуется создать в корне проекта отдельную папку k8s/ и положить манифесты туда.
------------------------------
## Шаг 1. Готовим структуру проекта
Создайте следующую структуру:

ваше-приложение/
├── k8s/
│   ├── mongodb.yaml      # База данных: Deployment, Service + PVC
│   └── tasks-app.yaml    # Приложение: Deployment + Service
├── src/
├── Dockerfile
└── pom.xml

------------------------------
## Шаг 2. Манифест для MongoDB (k8s/mongodb.yaml)
В Kubernetes база данных требует постоянного диска (PersistentVolumeClaim), чтобы данные не пропадали при перезапуске Пода.
Создайте файл k8s/mongodb.yaml:

apiVersion: v1kind: PersistentVolumeClaimmetadata:
  name: mongo-pvcspec:
  accessModes:
    - ReadWriteOnce
  resources:
    requests:
      storage: 1Gi
---apiVersion: apps/v1kind: Deploymentmetadata:
  name: mongodbspec:
  replicas: 1
  selector:
    matchLabels:
      app: mongodb
  template:
    metadata:
      labels:
        app: mongodb
    spec:
      containers:
        - name: mongodb
          image: mongo:7.0
          ports:
            - containerPort: 27017
          environment:
            - name: MONGO_INITDB_ROOT_USERNAME
              value: rootadmin
            - name: MONGO_INITDB_ROOT_PASSWORD
              value: supersecretpassword
            - name: MONGO_INITDB_DATABASE
              value: tasks_db
          volumeMounts:
            - name: mongo-storage
              mountPath: /data/db
      volumes:
        - name: mongo-storage
          persistentVolumeClaim:
            claimName: mongo-pvc
---apiVersion: v1kind: Servicemetadata:
  name: mongodb # ⚠️ Важно: имя сервиса станет DNS-именем для Spring Bootspec:
  ports:
    - port: 27017
  selector:
    app: mongodb

------------------------------
## Шаг 3. Манифест для Spring Boot приложения (k8s/tasks-app.yaml)
Обратите внимание, что URL подключения к базе остается точно таким же, как в Docker Compose (mongodb://...), потому что K8s внутри себя свяжет имя сервиса mongodb с базой данных.
Создайте файл k8s/tasks-app.yaml:

apiVersion: apps/v1kind: Deploymentmetadata:
  name: tasks-appspec:
  replicas: 1 # Количество запущенных экземпляров вашего приложения
  selector:
    matchLabels:
      app: tasks-app
  template:
    metadata:
      labels:
        app: tasks-app
    spec:
      containers:
        - name: tasks-app
          # ⚠️ Локальный образ, который мы соберем внутри окружения Minikube
          image: tasks-app:latest 
          imagePullPolicy: Never # Говорим K8s не искать образ в Docker Hub
          ports:
            - containerPort: 8091
          env:
            - name: SERVER_PORT
              value: "8091"
            - name: MONGO_URI
              value: "mongodb://rootadmin:supersecretpassword@mongodb:27017/tasks_db?authSource=admin"
---apiVersion: v1kind: Servicemetadata:
  name: tasks-app-servicespec:
  type: NodePort # Позволяет открыть порт приложения наружу для вашего ПК
  ports:
    - port: 8091
      targetPort: 8091
      nodePort: 30091 # Приложение будет доступно по этому порту на IP-адресе Minikube
  selector:
    app: tasks-app

------------------------------
## Шаг 4. Пошаговый запуск в Minikube
Чтобы всё заработало, нужно выполнить команды в строгом порядке:
1. Запускаем Minikube:

minikube start

2. Переключаем терминал на Docker-окружение Minikube:
Это критически важный шаг! По умолчанию ваш Docker собирает образы на вашем ПК, и K8s их не увидит. Эта команда перенаправит ваш Docker-клиент внутрь виртуальной машины Minikube:

eval $(minikube docker-env)

3. Собираем Docker-образ приложения внутри Minikube:
Теперь собираем образ прямо там (находясь в корне проекта):

docker build -t tasks-app:latest .

4. Применяем манифесты Kubernetes:

kubectl apply -f k8s/mongodb.yaml
kubectl apply -f k8s/tasks-app.yaml

5. Проверяем, что все Поды поднялись (статус Running):

kubectl get pods

------------------------------
## Шаг 5. Как теперь отправить запрос к приложению?
Так как Minikube работает внутри своей виртуальной сети, localhost:8091 работать не будет. Чтобы узнать адрес, по которому доступно ваше приложение, выполните команду:

minikube service tasks-app-service --url

Она вернет вам адрес вида http://192.168.49.2:30091. Используйте его в Postman или браузере для отправки POST и GET запросов!
Получилось ли у вас переключить окружение Docker и собрать образ внутри Minikube? Если возникнут ошибки на этапе запуска подов, дайте знать, я помогу их отладить.

# Запуск приложений

## Запуск minikube

```shell
# Старт minicube
minikube start

# Эта команда перенаправит Docker-клиент внутрь виртуальной машины Minikube:
eval $(minikube docker-env)

# Собираем Docker-образ приложения внутри Minikube:Теперь собираем образ прямо там (находясь в корне проекта):
docker build -t tasks-app:latest .

# Применяем манифесты Kubernetes: 
kubectl apply -f k8s/mongodb.yaml
kubectl apply -f k8s/tasks-app.yaml

# Проверяем, что все Поды поднялись (статус Running):
kubectl get pods # или svc

# Подключение к подам, если в service.yaml spec.type - ClusterIP
kubectl port-forward service/tasks-service 8091:8091

# Можно подключиться к поду:
kubectl exec -it имя_пода -- bash

# Удалить некорректные конфииги для подов - удалтся поды
kubectl delete deployment mongodb
kubectl delete service mongodb

# Примените два новых раздельных манифеста:
kubectl apply -f k8s/tasks-mongodb.yaml
kubectl apply -f k8s/users-mongodb.yaml


```
