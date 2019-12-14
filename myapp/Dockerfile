FROM openjdk:8
ADD target/myapp-notifier.jar myapp-notifier.jar
EXPOSE 8085
ENTRYPOINT ["java","-jar", "myapp-notifier.jar"]