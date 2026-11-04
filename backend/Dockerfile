FROM node:20-alpine AS fe
WORKDIR /fe
RUN npm config set registry https://registry.npmmirror.com
COPY frontend/package*.json ./
RUN npm ci || npm install
COPY frontend/ ./
RUN npm run build

FROM maven:3.9-eclipse-temurin-17 AS be
WORKDIR /app
COPY backend/pom.xml .
RUN mvn dependency:go-offline -B || true
COPY backend/src ./src
COPY --from=fe /fe/dist ./src/main/resources/static/
RUN mvn package -DskipTests -B

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=be /app/target/*.jar app.jar
EXPOSE 8080
CMD ["java", "-jar", "app.jar"]
