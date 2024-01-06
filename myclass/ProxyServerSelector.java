package myclass;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Hashtable;

public class ProxyServerSelector {
	private final List<String> hosts;
	private int round;
	public Hashtable<String, Boolean> serverStatus;
	public Hashtable<String, Boolean> PreviousServerStatus;
	private int counter;

	public ProxyServerSelector(List<String> hosts) {
		this.hosts = hosts;
		this.round = 0;
		this.serverStatus = new Hashtable<String, Boolean>();
		this.PreviousServerStatus = new Hashtable<String, Boolean>();
		this.counter = 0;
		for (String host : hosts) {
			serverStatus.put(host, true);
		}
		for (String host : hosts) {
			PreviousServerStatus.put(host, true);
		}
	}

	public ProxyServerSelector(String fileName) {
		this.hosts = getHostsFromFile(fileName);
		this.round = 0;
		this.serverStatus = new Hashtable<String, Boolean>();
		this.PreviousServerStatus = new Hashtable<String, Boolean>();
		this.counter = 0;
		for (String host : hosts) {
			serverStatus.put(host, true);
		}
		for (String host : hosts) {
			PreviousServerStatus.put(host, true);
		}
	}

	public void UpdateServerStatus(String host, boolean status) {
		serverStatus.put(host, status);
		// print the status of all servers neatly, but only do so every 5 times this
		// function is called using a counter
		// make sure to print out "Healthy" or "Unhealthy" instead of true or false
		counter++;
		if (counter == 5) {
			System.out.println("--------------------");
			for (String key : serverStatus.keySet()) {
			System.out.println(key + ": " + (serverStatus.get(key) ? "Healthy" :
			"Unhealthy"));
			}
			counter = 0;
		}
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

	public String getNextHost(String stringToHash, ConsistentHasher consistentHasher) {
		String curHost = consistentHasher.getServer(stringToHash);
		return curHost;
	}
}
