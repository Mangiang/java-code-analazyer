FROM python:3.6-alpine AS cloner
RUN apk add --no-cache git
RUN git clone https://github.com/Mangiang/test-spring-boot.git


# Step : Test and package
FROM maven:3.6.1-jdk-8-alpine as target
WORKDIR /build
COPY pom.xml .
RUN mvn dependency:go-offline
COPY src/ /build/src/
RUN mvn clean package

# Step : Package image
#FROM openjdk:11.0.8-jdk-slim-buster
EXPOSE 4567
CMD exec java $JAVA_OPTS -jar /app/test-batch-1.0-SNAPSHOT.jar
WORKDIR /app
ENV SRC_DIR /helloworld-client
#COPY --from=target /build/target/test-batch-1.0-SNAPSHOT.jar /app/test-batch-1.0-SNAPSHOT.jar
RUN cp /build/target/test-batch-1.0-SNAPSHOT.jar /app/test-batch-1.0-SNAPSHOT.jar
COPY --from=cloner /test-spring-boot/apps/helloworld-client /helloworld-client/