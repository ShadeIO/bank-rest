# ---------- build stage ----------
FROM maven:3.9.8-eclipse-temurin-17 AS build
WORKDIR /build
COPY pom.xml .
RUN mvn -q -B -e -DskipTests dependency:go-offline
COPY src ./src
RUN mvn -q -DskipTests clean package

# ---------- run stage ----------
FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

# (необязательно) часовой пояс
ENV TZ=Europe/Amsterdam

# профиль "docker" можно активировать переменной окружения
ENV SPRING_PROFILES_ACTIVE=docker

# оптимизация памяти JVM в контейнере
ENV JAVA_TOOL_OPTIONS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

# копируем с билд-стейджа — имя jar мы не угадываем, берём единственный *.jar
COPY --from=build /build/target/*.jar /app/app.jar

EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]
