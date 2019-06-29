FROM openjdk:13-ea-19-alpine3.9

LABEL maintainer=alexvaut@github.com
ENV WORK_DIR /var/www

WORKDIR $WORK_DIR

#### Can be used to run directly from the sources instead of the .jar
# ENV LEIN_ROOT 1
# RUN apk add --update wget ca-certificates bash && \
#     wget -q "https://raw.githubusercontent.com/technomancy/leiningen/stable/bin/lein" \
#          -O /usr/local/bin/lein && \
#     chmod 0755 /usr/local/bin/lein && \
#     lein && \
#     apk del wget ca-certificates && \
#     rm -rf /tmp/* /var/cache/apk/*
# COPY . $WORK_DIR
# ENTRYPOINT ["lein", "ring", "server-headless"]

EXPOSE 3000
COPY ./target $WORK_DIR
ENTRYPOINT java -jar ${WORK_DIR}/server.jar