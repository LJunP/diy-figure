# 多阶段构建:先编译打包,再只带 JRE 运行,镜像体积更小
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
# 先只拉依赖,利用 Docker 缓存,代码变动时不用重新下载
RUN mvn -B -q dependency:go-offline
COPY src ./src
RUN mvn -B -q -DskipTests package

FROM eclipse-temurin:17-jre
WORKDIR /app
# 非 root 用户运行
RUN useradd -m -u 10001 appuser
COPY --from=build /app/target/*.jar app.jar
RUN mkdir -p /app/data/uploads && chown -R appuser:appuser /app
USER appuser
EXPOSE 8080
# 容器编排通过环境变量注入端口、数据库与密钥
ENTRYPOINT ["java","-jar","/app/app.jar"]
