FROM openjdk:8-jdk-alpine
ADD target/webflux-demo-0.0.1-SNAPSHOT.jar app.jar
ENV JAVA_OPTS=""
CMD ["java", "-Djava.security.egd=file:/dev/./urandom", "-jar", "/app.jar" ]