FROM arm32v7/eclipse-temurin:19
#
RUN addgroup --system spring && adduser --system -ingroup spring spring
#
# create empty dir with correct permissions to be used for volume mount
RUN mkdir /opt/family-dashboard-images
RUN chown spring:spring /opt/family-dashboard-images
#
USER spring:spring
#
ARG JAR_FILE=*.jar
COPY ${JAR_FILE} app.jar
ENTRYPOINT ["java", "-jar", "/app.jar"]
