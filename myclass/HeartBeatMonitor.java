package myclass;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.List;

import configs.PortConfigs;

public class HeartBeatMonitor {

    private final ProxyServerSelector serverSelector;
    private final List<String> hosts;

    public HeartBeatMonitor(ProxyServerSelector serverSelector, List<String> hosts) {
        this.serverSelector = serverSelector;
        this.hosts = hosts;
    }

    // make an initializer with no arguments required
    public HeartBeatMonitor() {
        this.serverSelector = null;
        this.hosts = null;
    }

    public void startHealthCheckThreadRP() {
		// note that we cannot use the same port as the one used for the proxy server
        // Create and start a thread for health checks
        Thread healthCheckThread = new Thread(() -> {
            while (true) {
				for (String host : this.hosts) {
					try {
						sendHealthCheck(host, PortConfigs.HEARTBEAT, serverSelector);
					} catch (IOException e) {
						System.err.println("28" + e);
					}
				}
				// do this every 1 second
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
            }
        });

        // Start the health check thread
        healthCheckThread.start();
    }

    public static void sendHealthCheck(String host, int port, ProxyServerSelector serverSelector) throws IOException {
		String heartBeatMsg = "HBC";
		try{
			Socket Serversocket = new Socket(host, port);
			OutputStream out = Serversocket.getOutputStream();
			InputStream in = Serversocket.getInputStream();
			out.write(heartBeatMsg.getBytes(), 0, heartBeatMsg.length());
			out.flush();
			Serversocket.shutdownOutput();

			boolean isHealthy = false;
			byte[] reply = new byte[4096];
			int bytesRead;
			try {
				bytesRead = in.read(reply);

				String sr = new String(reply, StandardCharsets.UTF_8);
				//System.out.println("Receiving:  :)" + sr);
				if (!sr.equals("HBC") || sr == "-1") {
					isHealthy = false;
				}
				isHealthy = true;

			} catch (IOException e) {
				// we dont want to print this because it will print a lot of errors
				// System.err.println(e);
			}
			in.close();
			out.close();
			Serversocket.close();
			
			serverSelector.UpdateServerStatus(host, isHealthy);
		} catch (IOException e) {
			serverSelector.UpdateServerStatus(host, false);
			
		}

	}

    public void startHealthCheckThreadURLDB() {
		// Create and start a thread for health checks

		Thread healthCheckThread = new Thread(() -> {
			BufferedReader inn = null;

			try (ServerSocket healthCheckServerSocket = new ServerSocket(PortConfigs.HEARTBEAT)) {
				while (true) {
					try(Socket clientSocket = healthCheckServerSocket.accept()) {
						inn = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
						OutputStream outt = clientSocket.getOutputStream();
						// Read the request from the client
						String request = inn.readLine();

						if ("HBC".equals(request)) {
							// If the request is a health check, send back a response
							outt.write("HBC".getBytes(), 0, "HBC".length());
							
						} else {
							// If the request is not a health check, send back an error
							outt.write("Error: Invalid request".getBytes(), 0, "Error: Invalid request".length());
						}

						// Close the connection
						inn.close();
						outt.close();
						clientSocket.close();
						
					} catch (IOException e) {

						e.printStackTrace();
					}

				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		});

		// Start the health check thread
		healthCheckThread.start();
	}


    
}
