import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ProxyServerSelector {
    private final List<String> hosts;
    private int round;

    public ProxyServerSelector(List<String> hosts) {
        this.hosts = hosts;
        this.round = 0;
    }

    public ProxyServerSelector(String fileName) {
        this.hosts = getHostsFromFile(fileName);
        this.round = 0;
    }

    public static List<String> getHostsFromFile(String fileString) {
        ArrayList<String> hosts = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(fileString))) {
            String line;
            while (((line = reader.readLine()) != null) && (!line.isEmpty())) {
                hosts.add(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }

        return hosts;
    }

    public String getNextHost() {
        String curHost = this.hosts.get(this.round);
        this.round = (this.round + 1) % hosts.size();
        return curHost;
    }
}
