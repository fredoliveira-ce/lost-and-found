FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /build
COPY pom.xml .
RUN mvn -B dependency:go-offline
COPY src ./src
RUN mvn -B -o -DskipTests -Dmaven.test.skip=true clean package

FROM eclipse-temurin:25-jre
WORKDIR /app
COPY --from=build /build/target/lost-and-found-*.jar app.jar
EXPOSE 8081 8082
ENTRYPOINT ["java", "-jar", "app.jar"]
