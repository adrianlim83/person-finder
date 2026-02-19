#
# Build stage
#
FROM maven:3.9-eclipse-temurin-21-alpine AS build
COPY settings.xml ~/.m2
COPY . /home/app/person-finder
RUN mvn -f /home/app/person-finder/pom.xml clean package -DskipTests

#
# Package stage
#
FROM eclipse-temurin:21-jre-alpine
COPY --from=build /home/app/person-finder/target/person-finder.jar /usr/local/lib/person-finder.jar
ENTRYPOINT ["sh", "-c", "java -Xms${heap_min:-512m} -Xmx${heap_max:-2g} $JAVA_OPTS -jar /usr/local/lib/person-finder.jar"]