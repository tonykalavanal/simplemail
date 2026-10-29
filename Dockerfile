# ---------- Stage 1: build the jar with Maven ----------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Copy the POM first and download dependencies.
# Docker caches this layer, so dependencies are only re-downloaded when pom.xml changes.
COPY pom.xml .
RUN mvn -B -q dependency:go-offline

# Now copy the source and build. Tests run in the CI pipeline before the image is built.
COPY src ./src
RUN mvn -B -q package -DskipTests

# ---------- Stage 2: small runtime image with only the JRE and the jar ----------
FROM eclipse-temurin:21-jre
WORKDIR /app

# Run as a non-root user (security best practice)
RUN groupadd --system app && useradd --system --gid app app
COPY --from=build /app/target/simplemail.jar /app/simplemail.jar
USER app

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/simplemail.jar"]
