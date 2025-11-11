package example;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Session;
import org.neo4j.harness.Neo4j;
import org.neo4j.harness.Neo4jBuilders;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class NetworkUtilsTest {

    private Neo4j embeddedDatabaseServer;

    @BeforeAll
    void initializeNeo4j() {
        this.embeddedDatabaseServer = Neo4jBuilders.newInProcessBuilder()
                .withDisabledServer()
                .withFunction(NetworkUtils.class)
                .build();
    }

    @AfterAll
    void closeNeo4j() {
        this.embeddedDatabaseServer.close();
    }

    @ParameterizedTest
    @CsvSource({
            "10.10.0.12, 10.10.0.0/8, true",
            "192.168.1.10, 10.10.0.0/8, false",
            "10.10.10.1, 10.10.0.0/16, true",
            "255.255.255.255, 255.255.255.0, false",
            "10.10.10.1, 10.10.0.0/23, false"
    })
    void testIpBelongsToNetwork(String ip, String network, boolean expectedResult) {
        try (Driver driver = GraphDatabase.driver(embeddedDatabaseServer.boltURI());
             Session session = driver.session()) {

            boolean result = session.run("RETURN example.ipBelongsToNetwork($ip, $network) AS result", 
                                           org.neo4j.driver.Values.parameters("ip", ip, "network", network))
                                 .single()
                                 .get("result")
                                 .asBoolean();

            assertThat(result).isEqualTo(expectedResult);
        }
    }

    @Test
    void testInvalidIp() {
        try (Driver driver = GraphDatabase.driver(embeddedDatabaseServer.boltURI());
             Session session = driver.session()) {

            assertThatThrownBy(() -> session.run("RETURN example.ipBelongsToNetwork('invalid_ip', '10.10.0.0/8')"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Invalid IP or network format");
        }
    }

    @Test
    void testInvalidNetwork() {
        try (Driver driver = GraphDatabase.driver(embeddedDatabaseServer.boltURI());
             Session session = driver.session()) {

            assertThatThrownBy(() -> session.run("RETURN example.ipBelongsToNetwork('10.10.0.12', 'invalid_network')"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Invalid IP or network format");
        }
    }

    @Test
    void testEmptyIp() {
        try (Driver driver = GraphDatabase.driver(embeddedDatabaseServer.boltURI());
             Session session = driver.session()) {

            assertThatThrownBy(() -> session.run("RETURN example.ipBelongsToNetwork('', '10.10.0.0/8')"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Invalid IP or network format");
        }
    }

    @Test
    void testEmptyNetwork() {
        try (Driver driver = GraphDatabase.driver(embeddedDatabaseServer.boltURI());
             Session session = driver.session()) {

            assertThatThrownBy(() -> session.run("RETURN example.ipBelongsToNetwork('10.10.0.12', '')"))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("Invalid IP or network format");
        }
    }
}
