FROM java:openjdk-8u111-jdk-alpine

WORKDIR /root

COPY build*.sh ./

RUN apk update && \
    apk upgrade && \
    apk add git wget && \
    wget -O grails.zip https://github.com/grails/grails-core/releases/download/v2.5.4/grails-2.5.4.zip && \
    unzip grails.zip && \
    wget -O gradle.zip https://downloads.gradle.org/distributions/gradle-4.9-bin.zip && \
    unzip gradle.zip

ENV GRAILS_HOME=/root/grails-2.5.4
ENV GRADLE_HOME=/root/gradle-4.9
ENV PATH=${GRAILS_HOME}/bin:${GRADLE_HOME}/bin:${PATH}

RUN ./build_plugins.sh && \
		./build_app.sh