FROM neo4j:2025.09.0-enterprise

COPY ./target/NetworkUtils-1.0-SNAPSHOT.jar /var/lib/neo4j/plugins/NetworkUtils-1.0-SNAPSHOT.jar

RUN chown neo4j:neo4j /var/lib/neo4j/plugins/NetworkUtils-1.0-SNAPSHOT.jar