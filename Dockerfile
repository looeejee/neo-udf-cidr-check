FROM neo4j:2025-community

USER root

# Copy the plugin
COPY ./target/neo-udf-cidr-check-1.0.0.jar /var/lib/neo4j/plugins/neo-udf-cidr-check-1.0.0.jar
RUN chown neo4j:neo4j /var/lib/neo4j/plugins/neo-udf-cidr-check-1.0.0.jar

# Create a backup directory
RUN mkdir -p /var/lib/neo4j/backup
COPY ./data/network-management-50.dump /var/lib/neo4j/backup/network-management-50.dump
RUN mv /var/lib/neo4j/backup/network-management-50.dump /var/lib/neo4j/backup/neo4j.dump
RUN chown neo4j:neo4j -R /var/lib/neo4j/backup/neo4j.dump

# Switch to the neo4j user
USER neo4j

# Load the database
RUN /var/lib/neo4j/bin/neo4j-admin database load --from-path=/var/lib/neo4j/backup --overwrite-destination=true --verbose neo4j
