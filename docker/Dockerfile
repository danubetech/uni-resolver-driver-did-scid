# Dockerfile for universalresolver/driver-did-scid

FROM maven:3-eclipse-temurin-21-alpine AS build
MAINTAINER Markus Sabadello <markus@danubetech.com>

# build driver-did-scid

ADD pom.xml /opt/driver-did-scid/
RUN cd /opt/driver-did-scid/ && mvn org.apache.maven.plugins:maven-dependency-plugin:3.8.1:go-offline
ADD . /opt/driver-did-scid/
RUN cd /opt/driver-did-scid/ && mvn clean package -P war -N -DskipTests


FROM jetty:12.0.18-jre21-eclipse-temurin
MAINTAINER Markus Sabadello <markus@danubetech.com>

# variables

ENV uniresolver_driver_did_scid_didResolverUri=http://localhost:8080/1.0/identifiers/
ENV uniresolver_driver_did_scid_wrapperHttpDomain=http://localhost:8080/1.0/identifiers/
ENV uniresolver_driver_did_scid_wrapperFilesPath=/opt/wrapper-files/

# copy from build stage

COPY --from=build --chown=jetty /opt/driver-did-scid/target/*.war /var/lib/jetty/webapps/ROOT.war

# done

EXPOSE 8080
RUN java -Djetty.http.port=8080 -jar "$JETTY_HOME/start.jar" --add-modules=ee10-webapp,ee10-deploy,ee10-jsp,ee10-jstl
CMD java -Djetty.http.port=8080 -jar "$JETTY_HOME/start.jar"
