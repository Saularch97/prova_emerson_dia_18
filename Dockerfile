# Estágio 1: compila o WAR (pom.xml primeiro para aproveitar o cache das dependências)
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY pom.xml .
RUN mvn -q dependency:go-offline
COPY src ./src
RUN mvn -q -DskipTests package

# Estágio 2: Tomcat serve o WAR no contexto /crud-manager
FROM tomcat:10.1-jdk21
RUN rm -rf /usr/local/tomcat/webapps/*
COPY --from=build /build/target/crud-manager.war /usr/local/tomcat/webapps/crud-manager.war
EXPOSE 8080
