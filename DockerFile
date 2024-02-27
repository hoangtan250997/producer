FROM openjdk:17
USER root
WORKDIR /tmp
ADD target/*.jar ./tmp/app.jar
ENTRYPOINT [ "java","-jar","./tmp/app.jar" ]
