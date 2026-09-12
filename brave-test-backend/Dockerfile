# ===== 构建阶段：Maven + JDK17 =====
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY mvn-settings.xml /root/.m2/settings.xml
COPY pom.xml .
RUN mvn -q -B dependency:go-offline
COPY src ./src
RUN mvn -q -B -DskipTests package

# ===== 运行阶段：JRE17 =====
FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
