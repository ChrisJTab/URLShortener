package myclass;

import java.io.*;
import java.net.*;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Queue;
import java.util.LinkedList;
import java.nio.charset.StandardCharsets;


import configs.GeneralConfigs;
import configs.PortConfigs;

public class ReverseProxy {
	private static final String SERVER_NAME = "Reverse Proxy";
	private static final Integer BUFFER_SIZE = 4096;

	private static Integer URL_SHORTNER_SERVER_PORT;
	private static Integer REVERSE_PROXY_SERVER_PORT;


	private static void startLogSendingThread(String host, ProxyServerSelector serverSelector) {
		// we want to send the logs over to the server specified in host, over port 7776
		Integer port = 7776;
        Thread logSendingThread = new Thread(() -> {
			// we simply want to send the contents of the logs.txt file to the server
			if (serverSelector.serverStatus.get(host) == true) {
				try {
					// read the contents of the logs.txt file
					File file = new File(Resources.LOGS);
					BufferedReader br = new BufferedReader(new FileReader(file));
					String line;
					while ((line = br.readLine()) != null) {
						// send the line to the server
						Socket socket = new Socket(host, port);
						OutputStream out = socket.getOutputStream();
						out.write(line.getBytes());
						out.flush();
						socket.close();
					}
					br.close();
					// whipe the logs
					FileWriter myWriter = new FileWriter(Resources.LOGS);
					myWriter.write("");
					myWriter.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
        });

        // Start the health check thread
        logSendingThread.start();
    }
	

	

	public static void main(String[] args) {
		if (args.length != 2) {
			System.out.println("Usage: ReverseProxy ReverseProxyServerPort URLShortnerServerPort");
			return;
		}

		REVERSE_PROXY_SERVER_PORT = Integer.parseInt(args[0]);
		URL_SHORTNER_SERVER_PORT = Integer.parseInt(args[1]);

		List<String> hosts = ProxyServerSelector.getHostsFromFile(GeneralConfigs.HOST_FILE_LOCATION);


		// instead of having previous server status be a hashtable, we will have a file called server_status.txt which will contain this hashtable
		// we will have a function that will read the file and return the hashtable
		// The file already exists, lets now initialize it by writing the hashtable to the file: 
		try {
			FileWriter myWriter = new FileWriter(Resources.SERVER_STATUS);
			for (String host : hosts) {
				myWriter.write(host + ":true\n");
			}
			myWriter.close();
		} catch (IOException e) {
			e.printStackTrace();
		}




		Map<String, String> hostToBackupHost = new HashMap<String, String>();
		for (int i = 0; i < hosts.size(); i++) {
			hostToBackupHost.put(hosts.get(i), hosts.get((i + 1) % hosts.size()));
		}
		System.out.println(hostToBackupHost);

		// create a map from the hosts to the numbers we will send to the servers, i.e. host1 -> 1, host2 -> 2, etc.
		Map<String, Byte> hostToNumber = new HashMap<String, Byte>();
		for (int i = 0; i < hosts.size(); i++) {
			hostToNumber.put(hosts.get(i), (byte) (i + 1));
		}
		System.out.println(hostToNumber);

		int numOfVirtualNodes = 100;
		ConsistentHasher consistentHasher;
		try {
			consistentHasher = new ConsistentHasher(numOfVirtualNodes);
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
			return;
		}
		for (String host : hosts) {
			consistentHasher.addServer(host);
		}

		// With hashing this need to be moved into the threaded code.
		// because the host is determined after reading the client request
		// which will be used to generate the hash
		ProxyServerSelector serverSelector = new ProxyServerSelector(hosts);

		HeartBeatMonitor heartBeatMonitor = new HeartBeatMonitor(serverSelector, hosts);
		heartBeatMonitor.startHealthCheckThreadRP();

		ExecutorService executorService = Executors.newFixedThreadPool(GeneralConfigs.THREAD_POOL_SIZE);

		try (ServerSocket serverSocket = new ServerSocket(REVERSE_PROXY_SERVER_PORT)) {
			System.out.printf(
					"Reverse Proxy Server started.\nListening for connections on %s:%d ...\n",
					InetAddress.getLocalHost().getHostName(),
					REVERSE_PROXY_SERVER_PORT);

			while (true) {
				Socket clientSocket = serverSocket.accept();
				executorService
						.submit(new ProxyThread(clientSocket, URL_SHORTNER_SERVER_PORT, consistentHasher, serverSelector, hostToBackupHost, hostToNumber));
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			executorService.shutdown();
		}
	}

	private static class ProxyThread implements Runnable {
		private static Pattern GET_PATTERN = Pattern.compile("^\\/(.+)$");
		private static Pattern PUT_PATTERN = Pattern.compile("^\\/\\?short=(.*)\\&long=(.*)$");

		private static Map<String, Boolean> ALLOWED_METHODS;

		// Static initializer block
		static {
			ALLOWED_METHODS = new HashMap<>();
			ALLOWED_METHODS.put("GET", true);
			ALLOWED_METHODS.put("PUT", true);
		}

		private Socket clientSocket;
		private Socket targetSocket;
		private Socket targetSocket2;
		private Integer targetPort;
		private ConsistentHasher consistentHasher;
		private ProxyServerSelector serverSelector;
		private Map<String, String> hostToBackupHost;
		private Map<String, Byte> hostToNumber;

		public ProxyThread(Socket clientSocket, Integer targetPort, ConsistentHasher consistentHasher,
				ProxyServerSelector serverSelector, Map<String, String> hostToBackupHost, Map<String, Byte> hostToNumber) {
			this.clientSocket = clientSocket;
			this.targetPort = targetPort;
			this.consistentHasher = consistentHasher;
			this.serverSelector = serverSelector;
			this.hostToBackupHost = hostToBackupHost;
			this.hostToNumber = hostToNumber;
		}

		@Override
		public void run() {
			try {
				// Create input and output streams for client and target server
				InputStream clientIn = clientSocket.getInputStream();
				OutputStream clientOut = clientSocket.getOutputStream();

				// Read the first line (e.g., the HTTP request line) from the client
				String firstLine = readFirstLine(clientIn);

				System.out.println(firstLine);

				String[] splitLine = firstLine.split(" ");
				String HTTP_METHOD = splitLine[0];
				String HTTP_PATH = splitLine[1];

				if (!ALLOWED_METHODS.getOrDefault(HTTP_METHOD, false)) {
					// ERROR. Direct the error back to client
					HttpSocketHelper.sendHttp404(clientOut, SERVER_NAME);
					clientSocket.close();
					return;
				}

				Matcher matcher;
				if (HTTP_METHOD.equals("GET")) {
					matcher = GET_PATTERN.matcher(HTTP_PATH);
				} else {
					matcher = PUT_PATTERN.matcher(HTTP_PATH);
				}

				if (!matcher.matches()) {
					// Error malformed URL or something like that
					HttpSocketHelper.sendHttp400(clientOut, SERVER_NAME);
					clientSocket.close();
					return;
				}

				String shortURL = matcher.group(1);


				// Get target URLShortner sever
				String targetHost = this.serverSelector.getNextHost(shortURL, consistentHasher);
				String backupHost = hostToBackupHost.get(targetHost);
				
				byte targetNumber = hostToNumber.get(targetHost);

				System.out.printf("Next host: %s:%d\n", targetHost, targetPort);

				File file = new File(Resources.LOGS);
				BufferedReader br = new BufferedReader(new FileReader(file));
				String firstLineInLogs = br.readLine();
				String[] splitFirstLineInLogs;
				String targetNumberInLogs = "";
				String targetHostInLogs = null;
				if (firstLineInLogs == null) {
					System.out.println("LOGS.TXT IS EMPTY");
				} else{
					splitFirstLineInLogs = firstLineInLogs.split("\\|");
					targetNumberInLogs = splitFirstLineInLogs[2];
					System.out.println("targetNumberInLogs: " + targetNumberInLogs);
					
					// Now we need to check if the server that went down is back up, and if so, send the requests to the server
					// Lets start by finding out what host the targetNumberInLogs maps to:
					for (Map.Entry<String, Byte> entry : hostToNumber.entrySet()) {
						if (entry.getValue().toString().equals(targetNumberInLogs)) {
							targetHostInLogs = entry.getKey();
						}
					}
					System.out.println("targetHostInLogs: " + targetHostInLogs);
				}
				br.close();

				// Now we need to check if the server that went down is back up, and if so, send the requests to the server
				if (targetHostInLogs != null && serverSelector.serverStatus.get(targetHostInLogs) == true){
					// Since the server is now up, we can send the logs directly to the database by running the bash file sendRequests.sh with the targetNumberInLogs as an argument
					// Simply run bash file called sendRequests.sh, which executes commands in logs.txt
					// convert targetNumberInLogs to int before passing in:
					System.out.println("SERVER UP, SENDING LOGGED REQUESTS");
					

					startLogSendingThread(targetHostInLogs, serverSelector);
					int targetNumberInLogsInt = Integer.parseInt(targetNumberInLogs);
					
				}

				// now we want to handle the case where the server is temporarily down
				// if the server is down, we want to send the request to the backup server instead aswell as logging the requests
				// When the server is back up, we want to send the logged requests to the server
				// we can know that a server is down if the serverStatus is false
				Boolean targetServerStatus = serverSelector.serverStatus.get(targetHost);
				

				if (targetServerStatus == false) { // if the server is down
					System.out.println("SERVER DOWN, REDIRECTING TO BACKUP SERVER");
					// if the server is down, we want to send the request to the backup server instead
					// we will do this by creating a new socket to the backup server
					// we will also log the request in the database
					targetHost = backupHost;
					
				} 
				targetSocket = new Socket(targetHost, targetPort);
				InputStream targetIn = targetSocket.getInputStream();
				OutputStream targetOut = targetSocket.getOutputStream();
				
				InputStream targetIn2; 
				OutputStream targetOut2;

			
				if (targetServerStatus == true){
					if (HTTP_METHOD.equals("PUT")){
						System.out.println("PUT + SERVER UP");
						targetOut.write(targetNumber);
						targetOut.flush();
						targetOut.write(firstLine.getBytes());
						targetOut.flush();

						targetSocket2 = new Socket(backupHost, targetPort);
						targetIn2 = targetSocket2.getInputStream();
						targetOut2 = targetSocket2.getOutputStream();
						// write the request to the secondary server
						targetOut2.write(targetNumber);
						targetOut.flush();
						targetOut2.write(firstLine.getBytes());
						targetOut2.flush();
						forward2HTTP(clientIn, targetOut, targetOut2);
					} else {
						System.out.println("GET + SERVER UP");
						targetOut.write(targetNumber);
						targetOut.flush();
						targetOut.write(firstLine.getBytes());
						targetOut.flush();
						forwardHTTP(clientIn, targetOut);
					}
				} else {
					if (HTTP_METHOD.equals("PUT")){
						System.out.println("PUT + SERVER DOWN");
						targetOut.write(targetNumber);
						targetOut.flush();
						targetOut.write(firstLine.getBytes());
						targetOut.flush();
						forwardHTTP(clientIn, targetOut);
						// We need to log the request in the database HEEERRREEE
						// we will put the request in a file called logs.txt
						System.out.println("SUPPOSEDLY LOGGING REQUEST");
						FileWriter myWriter = new FileWriter(Resources.LOGS, true);
						String[] splitFirstLine = firstLine.split(" ");
						String shortURL2 = splitFirstLine[1].split("=")[1].split("&")[0];
						String longURL2 = splitFirstLine[1].split("=")[2];
						String RPhostname = InetAddress.getLocalHost().getHostName();
						String curlCommand = shortURL2 + "|" + longURL2 + "|" + targetNumber ;

						System.out.println("curlCommand: " + curlCommand);
						myWriter.write(curlCommand + "\n");
						myWriter.close();
						System.out.println("SUPPOSEDLY FINISHED LOGGING REQUEST");
					} else {
						System.out.println("GET + SERVER DOWN");
						targetOut.write(targetNumber);
						targetOut.flush();
						targetOut.write(firstLine.getBytes());
						targetOut.flush();
						forwardHTTP(clientIn, targetOut);
						// we dont need to log the request.
					}
				}

				forwardHTTP(targetIn, clientOut);
				

				// Close sockets
				clientSocket.close();
				targetSocket.close();
				targetSocket2.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		// Helper method to read the first line from an InputStream
		private String readFirstLine(InputStream inputStream) throws IOException {
			StringBuilder firstLine = new StringBuilder();
			int ch;
			while ((ch = inputStream.read()) != -1 && ch != '\r' && ch != '\n') {
				firstLine.append((char) ch);
			}
			return firstLine.toString();
		}

		private void forwardHTTP(InputStream in, OutputStream out) throws IOException {
			byte[] buffer = new byte[BUFFER_SIZE];
			int bytesRead;
			while ((bytesRead = in.read(buffer)) != -1) {
				out.write(buffer, 0, bytesRead);
				out.flush();

				String request = new String(buffer, 0, bytesRead);
				if (request.contains("\r\n\r\n")) {
					return;
				}
			}
		}

		private void forward2HTTP(InputStream in, OutputStream out1, OutputStream out2) throws IOException {
			byte[] buffer = new byte[BUFFER_SIZE];
			int bytesRead;
			while ((bytesRead = in.read(buffer)) != -1) {
				out1.write(buffer, 0, bytesRead);
				out1.flush();

				out2.write(buffer, 0, bytesRead);
				out2.flush();

				String request = new String(buffer, 0, bytesRead);
				if (request.contains("\r\n\r\n")) {
					return;
				}
			}
		}

	}
}
