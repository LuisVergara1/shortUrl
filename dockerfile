# Utiliza una imagen base de Eclipse Temurin JRE 17
FROM eclipse-temurin:17-jre

# Copia el archivo JAR compilado al contenedor
COPY target/*.jar app.jar

# Expone el puerto en el que la aplicación se ejecuta
EXPOSE 8080

# Comando para ejecutar la aplicación
ENTRYPOINT ["java", "-jar", "/app.jar"]
