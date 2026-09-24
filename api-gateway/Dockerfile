# --- Этап 1: Сборка приложения ---
FROM maven:3.9-eclipse-temurin-25 AS builder
WORKDIR /app

# Копируем файлы конфигурации Maven для кеширования зависимостей
COPY pom.xml .
# Скачиваем зависимости (если pom.xml не менялся, этот шаг будет взят из кеша)
RUN mvn dependency:go-offline -B

# Копируем исходный код
COPY src ./src

# Собираем проект (пропуская тесты, так как их лучше гонять в CI/CD)
RUN mvn clean package -DskipTests

# --- Этап 2: Финальный образ для запуска ---
FROM eclipse-temurin:25-jre-noble
WORKDIR /app

# Создаем безопасного (не-root) пользователя для запуска приложения
RUN useradd -m springuser && chown -R springuser:springuser /app
USER springuser

# Копируем собранный jar-файл из предыдущего этапа
# Замените 'tasks-0.0.1-SNAPSHOT.jar' на имя вашего jar-файла из pom.xml
COPY --from=builder /app/target/*.jar app.jar

# Открываем стандартный порт Spring Boot
EXPOSE 8080

# Оптимальные параметры запуска для контейнера
ENTRYPOINT ["java", "-jar", "app.jar"]
