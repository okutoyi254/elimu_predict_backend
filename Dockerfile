#Build stage
FROM maven:3.9.6-eclipse-temurin-21-alpine AS build

WORKDIR /app

COPY pom.xml .
RUN mvn dependency:go-offline -q

COPY src ./src

RUN mvn clean package -DskipTests -q

#Runtime stage
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

RUN addgroup -S elimu && adduser -S elimu -G elimu

COPY --from=build /app/target/*.jar app.jar

RUN chown elimu:elimu app.jar

USER elimu

EXPOSE 8080

#JVM tuning to optimize RAM and CPU
ENTRYPOINT ["java", \
  "-Xms128m", \
  "-Xmx384m", \
  "-XX:+UseSerialGC", \
  "-XX:MaxMetaspaceSize=128m", \
  "-XX:+OptimizeStringConcat", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]
