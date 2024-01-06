package myclass;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.SortedMap;
import java.util.TreeMap;

public class ConsistentHasher {
	private final TreeMap<Long, String> ring; // A sorted map to store hash values and corresponding server names
	private final int numOfVirtualNodes; // Number of virtual nodes for each server
	private final MessageDigest md; // MessageDigest for hash generation(basically is just a hash function)

	// Constructor initializes the hash ring, number of virtual nodes, and
	// MessageDigest
	public ConsistentHasher(int numOfVirtualNodes) throws NoSuchAlgorithmException {
		this.ring = new TreeMap<>();
		this.numOfVirtualNodes = numOfVirtualNodes;
		this.md = MessageDigest.getInstance("MD5");
	}

	// Remove a server and its virtual nodes from the hash ring
	public void removeServer(String server) {
		int i = 0;
		while (i < numOfVirtualNodes) {
			long hash = generateHash(server + "V" + i);
			ring.remove(hash);
			i++;
		}
	}

	// Add a server to the hash ring with multiple virtual nodes
	public void addServer(String server) {
		int i = 0;
		while (i < numOfVirtualNodes) {
			long hash = generateHash(server + i);
			ring.put(hash, server);
			i++;
		}
	}

	// Get the server that a key should be mapped to
	public String getServer(String key) {
		if (ring.isEmpty()) {
			return null;
		}
		long hash = generateHash(key);
		if (!ring.containsKey(hash)) { // if the ring does not contain the hash, we want to get the nearest server
			SortedMap<Long, String> tailMap = ring.tailMap(hash); // returns the portion of the mapping whose keys are
																	// greater than the from_Key.
			if (tailMap.isEmpty()) {
				hash = ring.firstKey(); // wrap back around the ring since there is no "greater hash"
			} else {
				hash = tailMap.firstKey(); // pick greater hash
			}
		}
		return ring.get(hash);
	}

	// Generate a long hash value for a given key using MD5
	private long generateHash(String key) {
		md.reset();
		md.update(key.getBytes());
		byte[] digest = md.digest();
		long hash = ((long) (digest[3] & 0xFF) << 24) |
				((long) (digest[2] & 0xFF) << 16) |
				((long) (digest[1] & 0xFF) << 8) |
				((long) (digest[0] & 0xFF));
		return hash;
		// return key.hashCode();
	}

}
