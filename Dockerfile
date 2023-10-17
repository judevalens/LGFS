FROM amazoncorretto:20-alpine3.18

WORKDIR ./
RUN mkdir "app"
ADD app/build/install/ .
#RUN cd app
RUN cd app && mkdir conf && cd conf #&& touch application.conf && touch chunk.conf
ENTRYPOINT cd "app/bin" && ./app
