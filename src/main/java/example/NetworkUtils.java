package example;

import org.neo4j.procedure.Description;
import org.neo4j.procedure.Name;
import org.neo4j.procedure.UserFunction;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class NetworkUtils {
    
    @UserFunction
    @Description("example.ipBelongsToNetwork(ip, network) - returns true if the given IP belongs to the specified CIDR network.")
    public boolean ipBelongsToNetwork(
            @Name("ip") String ip,
            @Name("network") String network) {
        try {
            InetAddress addr = InetAddress.getByName(ip);
            InetAddress networkAddr = InetAddress.getByName(network.substring(0, network.indexOf('/')));
            int prefixLength = Integer.parseInt(network.substring(network.indexOf('/') + 1));

            return checkIpInNetwork(addr, networkAddr, prefixLength);
        } catch (Exception e) {
            throw new RuntimeException("Invalid IP or network format: " + e.getMessage());
        }
    }

    private boolean checkIpInNetwork(InetAddress ip, InetAddress network, int prefixLength) {
        byte[] ipBytes = ip.getAddress();
        byte[] networkBytes = network.getAddress();

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
