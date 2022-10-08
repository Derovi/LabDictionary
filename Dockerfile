FROM openjdk:17-alpine3.14

COPY "/build/libs/*.jar" "lindeck.jar"
ENTRYPOINT ["java", "-jar", "/lindeck.jar"]

EXPOSE 8080