FROM arm32v7/adoptopenjdk:16-jre-hotspot
RUN addgroup --system spring && adduser --system -ingroup spring spring
USER spring:spring
ARG JAR_FILE=*.jar
COPY ${JAR_FILE} app.jar
ENTRYPOINT ["java", "-jar", "/app.jar"]
