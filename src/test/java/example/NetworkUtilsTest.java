package example;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Session;
import org.neo4j.driver.Values;
import org.neo4j.harness.Neo4j;
import org.neo4j.harness.Neo4jBuilders;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("NetworkUtils IP CIDR Check Tests")
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

    @ParameterizedTest(name = "IP {0} should {2} belong to network {1}")
    @DisplayName("Test IP address membership in CIDR networks")
    @CsvSource({
            // Standard cases - should belong
            "10.10.0.12, 10.10.0.0/8, true",
            "10.255.255.255, 10.0.0.0/8, true",
            "10.10.10.1, 10.10.0.0/16, true",
            "10.10.10.1, 10.10.10.0/24, true",
            "172.16.0.130, 172.16.0.0/24, true",
            "192.168.1.100, 192.168.1.0/24, true",
            "192.168.1.1, 192.168.0.0/16, true",
            // Standard cases - should NOT belong
            "192.168.1.10, 10.10.0.0/8, false",
            "255.255.255.255, 255.255.255.0, false",
            "172.16.1.1, 172.16.0.0/28, false",
            "10.11.0.1, 10.10.0.0/16, false",
            // Edge cases
            "10.10.0.0, 10.10.0.0/24, true",  // Network address itself
            "10.10.0.255, 10.10.0.0/24, true", // Broadcast address
            "127.0.0.1, 127.0.0.0/8, true",    // Loopback
            "0.0.0.0, 0.0.0.0/0, true"         // Any IP in entire space
    })
    void testIpBelongsToNetwork(String ip, String network, boolean expectedResult) {
        try (Driver driver = GraphDatabase.driver(embeddedDatabaseServer.boltURI());
             Session session = driver.session()) {

            boolean result = session.run(
                    "RETURN example.ipBelongsToNetwork($ip, $network) AS result",
                    Values.parameters("ip", ip, "network", network))
                    .single()
                    .get("result")
                    .asBoolean();

            assertThat(result)
                    .as("IP %s should %s belong to network %s", ip, expectedResult ? "" : "NOT ", network)
                    .isEqualTo(expectedResult);
        }
    }

    @ParameterizedTest(name = "Invalid case: {0}")
    @DisplayName("Test invalid IP addresses")
    @CsvSource({
            "'invalid_ip', '10.10.0.0/8'",
            "'999.999.999.999', '10.10.0.0/8'",
            "'10.10.0.256', '10.10.0.0/24'",
            "'10.10.-1.1', '10.10.0.0/24'"
    })
    void testInvalidIp(String ip, String network) {
        try (Driver driver = GraphDatabase.driver(embeddedDatabaseServer.boltURI());
             Session session = driver.session()) {

            assertThatThrownBy(() -> session.run(
                    "RETURN example.ipBelongsToNetwork($ip, $network)",
                    Values.parameters("ip", ip, "network", network)))
                    .hasMessageContaining("Invalid IP or network format");
        }
    }

    @ParameterizedTest(name = "Invalid network: {1}")
    @DisplayName("Test invalid CIDR networks")
    @CsvSource({
            "'10.10.0.12', 'invalid_network'",
            "'10.10.0.12', '10.10.0.0/33'",  // Prefix > 32
            "'10.10.0.12', '10.10.0.0/-1'",  // Negative prefix
            "'10.10.0.12', '10.10.0.0/abc'", // Non-numeric prefix
            "'10.10.0.12', '999.999.999.999/24'"
    })
    void testInvalidNetwork(String ip, String network) {
        try (Driver driver = GraphDatabase.driver(embeddedDatabaseServer.boltURI());
             Session session = driver.session()) {

            assertThatThrownBy(() -> session.run(
                    "RETURN example.ipBelongsToNetwork($ip, $network)",
                    Values.parameters("ip", ip, "network", network)))
                    .hasMessageContaining("Invalid IP or network format");
        }
    }

    @Test
    @DisplayName("Test empty IP address")
    void testEmptyIp() {
        try (Driver driver = GraphDatabase.driver(embeddedDatabaseServer.boltURI());
             Session session = driver.session()) {

            assertThatThrownBy(() -> session.run(
                    "RETURN example.ipBelongsToNetwork($ip, $network)",
                    Values.parameters("ip", "", "network", "10.10.0.0/8")))
                    .hasMessageContaining("Invalid IP or network format");
        }
    }

    @Test
    @DisplayName("Test empty network")
    void testEmptyNetwork() {
        try (Driver driver = GraphDatabase.driver(embeddedDatabaseServer.boltURI());
             Session session = driver.session()) {

            assertThatThrownBy(() -> session.run(
                    "RETURN example.ipBelongsToNetwork($ip, $network)",
                    Values.parameters("ip", "10.10.0.12", "network", "")))
                    .hasMessageContaining("Invalid IP or network format");
        }
    }

    @Test
    @DisplayName("Test null IP address")
    void testNullIp() {
        try (Driver driver = GraphDatabase.driver(embeddedDatabaseServer.boltURI());
             Session session = driver.session()) {

            assertThatThrownBy(() -> session.run(
                    "RETURN example.ipBelongsToNetwork($ip, $network)",
                    Values.parameters("ip", Values.NULL, "network", "10.10.0.0/8")))
                    .hasMessageContaining("Invalid IP or network format");
        }
    }

    @Test
    @DisplayName("Test null network")
    void testNullNetwork() {
        try (Driver driver = GraphDatabase.driver(embeddedDatabaseServer.boltURI());
             Session session = driver.session()) {

            assertThatThrownBy(() -> session.run(
                    "RETURN example.ipBelongsToNetwork($ip, $network)",
                    Values.parameters("ip", "10.10.0.12", "network", Values.NULL)))
                    .hasMessageContaining("Invalid IP or network format");
        }
    }

    @Test
    @DisplayName("Test function with Neo4j nodes (simulated)")
    void testFunctionInCypherQuery() {
        try (Driver driver = GraphDatabase.driver(embeddedDatabaseServer.boltURI());
             Session session = driver.session()) {

            // Create test data: nodes with IP properties
            session.run(
                    "CREATE (server:Server {name: 'web-server-1', ip: '10.10.10.50'}), " +
                    "(server2:Server {name: 'web-server-2', ip: '10.20.0.100'}), " +
                    "(server3:Server {name: 'db-server', ip: '192.168.1.10'})");

            // Query to find servers in a specific network
            var result = session.run(
                    "MATCH (s:Server) " +
                    "WHERE example.ipBelongsToNetwork(s.ip, '10.10.0.0/16') = true " +
                    "RETURN s.name AS serverName, s.ip AS serverIp " +
                    "ORDER BY s.name")
                    .list();

            assertThat(result)
                    .as("Should find servers in 10.10.0.0/16 network")
                    .hasSize(1)
                    .extracting(r -> r.get("serverName").asString())
                    .containsExactly("web-server-1");

            // Query to find all servers NOT in a specific network
            var result2 = session.run(
                    "MATCH (s:Server) " +
                    "WHERE example.ipBelongsToNetwork(s.ip, '10.10.0.0/16') = false " +
                    "RETURN s.name AS serverName, s.ip AS serverIp " +
                    "ORDER BY s.name")
                    .list();

            assertThat(result2)
                    .as("Should find servers NOT in 10.10.0.0/16 network")
                    .hasSize(2);
        }
    }
}
