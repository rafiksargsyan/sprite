FROM eclipse-temurin:21-jre-noble

ENV SPRING_PROFILES_ACTIVE=web

WORKDIR /app
COPY build/libs/*.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]
