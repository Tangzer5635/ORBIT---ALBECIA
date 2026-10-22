# --- Étape 1 : compiler le jar avec Maven ---
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml .
COPY src ./src
# le cache évite de re-télécharger toutes les dépendances à chaque build
RUN --mount=type=cache,target=/root/.m2 mvn -q -B package -DskipTests

# --- Étape 2 : image finale, juste le JRE + le jar ---
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /build/target/orbit-1.0-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
