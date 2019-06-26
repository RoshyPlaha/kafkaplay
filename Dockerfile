#
# Build stage
#
FROM maven:3.6.0-jdk-11-slim AS build
COPY src /home/app/src
COPY pom.xml /home/app
RUN mvn -f /home/app/pom.xml clean compile assembly:single # Run Producer in our case


#
# Package stage
#
FROM openjdk:8-jre-slim
COPY --from=build /home/app/target/tutorial-kafka-java-1.0-SNAPSHOT-jar-with-dependencies.jar /usr/local/lib/demo.jar
EXPOSE 9092 2181
ENTRYPOINT ["java","-jar","/usr/local/lib/demo.jar"]