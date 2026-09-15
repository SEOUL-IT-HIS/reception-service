from openjdk:17-ea-jdk-slim

run mkdir /app
workdir /app

add ./build/libs/*.jar /app/app.jar

expose 8083
entrypoint ["java", "-jar", "app.jar", "--server.port=8083"]