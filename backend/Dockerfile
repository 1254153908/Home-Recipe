# ============ 构建阶段 ============
FROM maven:3.8-openjdk-11-slim AS builder

WORKDIR /app
COPY pom.xml .
# 先下载依赖（利用 Docker 缓存层）
RUN mvn dependency:go-offline -B

COPY src ./src
RUN mvn package -DskipTests -B

# ============ 运行阶段 ============
FROM openjdk:11-jre-slim

WORKDIR /app

# 时区设置
ENV TZ=Asia/Shanghai
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

COPY --from=builder /app/target/*.jar app.jar

EXPOSE 4993

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
