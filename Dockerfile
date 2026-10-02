# 编译阶段：前端静态文件随 Spring Boot 一起打入 JAR。
FROM maven:3.9.13-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml ./
RUN mvn -q -DskipTests dependency:go-offline
COPY src ./src
RUN mvn -q -DskipTests package

# 运行阶段只保留 JRE 和应用，不需要把 Maven 放进最终镜像。
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/target/learn-backend-*.jar /app/app.jar
EXPOSE 8080
USER 10001:10001
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
