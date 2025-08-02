FROM eclipse-temurin:21-jre-jammy
LABEL maintainer="Julian"

WORKDIR /app
COPY target/chinaport-data-signature-webapp.jar /app/chinaport-data-signature.jar

EXPOSE 8080
ENV TZ=PRC
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ >/etc/timezone

ENV JAVA_OPTS=""
ENV SPRING_ARGS=""

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar chinaport-data-signature.jar $SPRING_ARGS"]
