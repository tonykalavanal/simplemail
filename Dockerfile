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
# Links the image in GitHub Container Registry to this repo, so the CI token may push to it.
LABEL org.opencontainers.image.source="https://github.com/tonykalavanal/simplemail"
WORKDIR /app

# Run as a non-root user (security best practice).
# A numeric UID lets Kubernetes verify the user is not root (runAsNonRoot needs a number, not a name).
RUN groupadd --system --gid 10001 app && useradd --system --uid 10001 --gid app app
COPY --from=build /app/target/simplemail.jar /app/simplemail.jar
USER 10001:10001

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/simplemail.jar"]
