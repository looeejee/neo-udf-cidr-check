package example;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.neo4j.procedure.Description;
import org.neo4j.procedure.Name;
import org.neo4j.procedure.UserFunction;

public class NetworkUtils {
    
    @UserFunction
    @Description("example.ipBelongsToNetwork(ip, network) - returns true if the given IP belongs to the specified CIDR network.")
    public boolean ipBelongsToNetwork(
            @Name("ip") String ip,
            @Name("network") String network) {
              
        // Validate inputs
        if (ip == null || network == null || ip.isEmpty() || network.isEmpty()) {
            throw new RuntimeException("Invalid IP or network format: IP and network must not be null or empty.");
        }

        try {
            InetAddress addr = InetAddress.getByName(ip);
            String[] networkParts = network.split("/");

            // Validate network format
            if (networkParts.length != 2) {
                throw new RuntimeException("Invalid network format.");
            }

            InetAddress networkAddr = InetAddress.getByName(networkParts[0]);
            int prefixLength = Integer.parseInt(networkParts[1]);

            // Validate prefix length
            if (prefixLength < 0 || prefixLength > 32) {
                throw new RuntimeException("Invalid prefix length.");
            }

            return checkIpInNetwork(addr, networkAddr, prefixLength);
        } catch (UnknownHostException e) {
            throw new RuntimeException("Invalid IP or network format: " + ip + " or " + network);
        } catch (NumberFormatException e) {
            throw new RuntimeException("Invalid IP or network format: Invalid prefix length in " + network);
        } catch (Exception e) {
            throw new RuntimeException("Invalid IP or network format: " + e.getMessage());
        }
    }

    private boolean checkIpInNetwork(InetAddress ip, InetAddress network, int prefixLength) {
        byte[] ipBytes = ip.getAddress();
        byte[] networkBytes = network.getAddress();

        // Create mask based on prefix length
        int mask = 0xFFFFFFFF << (32 - prefixLength);
        int networkInt = byteArrayToInt(networkBytes) & mask;
        int ipInt = byteArrayToInt(ipBytes) & mask;

        return networkInt == ipInt;
    }

    private int byteArrayToInt(byte[] bytes) {
        int result = 0;
        for (byte b : bytes) {
            result = (result << 8) | (b & 0xFF);
        }
        return result;
    }
}
