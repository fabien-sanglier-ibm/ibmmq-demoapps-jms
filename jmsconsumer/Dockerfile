FROM maven:3.9.9-eclipse-temurin-17 AS build

WORKDIR /workspace

COPY pom.xml .
COPY src ./src

RUN mvn -Popenshift -DskipTests package

FROM eclipse-temurin:17-jre

WORKDIR /app

COPY --from=build /workspace/target/*-jar-with-dependencies.jar /app/app.jar

ENV MQ_APP_PASSWORD=""

# MQ v10 JVM options:
# - Disable MQ client logging to avoid file access issues in containers
# - Set TCP buffer sizes to avoid warnings (32KB send, 64KB receive)
# - Set working directory for any MQ temp files
ENV APP_JAVA_OPTS="\
    -Duser.dir=/app \
    -Dcom.ibm.msg.client.commonservices.log.outputName=/dev/stdout \
    -Dcom.ibm.msg.client.commonservices.trace.outputName=SYSTEM.OUT \
    -Dcom.ibm.mq.cfg.TCP.ClntSndBuffSize=32768 \
    -Dcom.ibm.mq.cfg.TCP.ClntRcvBuffSize=65536"

# Ensure your entrypoint uses both the standard JAVA_TOOL_OPTIONS 
# (which the JVM picks up automatically) AND your custom variable
ENTRYPOINT ["sh", "-c", "java $APP_JAVA_OPTS $JAVA_TOOL_OPTIONS -jar /app/app.jar"]

# Made with Bob
