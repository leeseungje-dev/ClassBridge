# 앱을 배포 서버에서 실행하기 위한 Docker 설정
# 1. Java 21 환경에서 앱 빌드
FROM eclipse-temurin:21-jdk AS build

WORKDIR /app

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
COPY src/ src/

# Windows 줄바꿈을 정리하고 Maven 실행 권한 부여
RUN sed -i 's/\r$//' mvnw \
	&& chmod +x mvnw \
	&& ./mvnw -B -DskipTests package

# 2. 빌드된 앱을 실행할 환경
FROM eclipse-temurin:21-jre

RUN apt-get update \
	&& apt-get install -y --no-install-recommends unzip \
	&& rm -rf /var/lib/apt/lists/*

WORKDIR /app

COPY --from=build /app/target/classbridge-0.0.1-SNAPSHOT.jar app.jar
COPY start.sh start.sh

RUN sed -i 's/\r$//' start.sh \
    && chmod +x start.sh

ENTRYPOINT ["/app/start.sh"]