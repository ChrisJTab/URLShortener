import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;

// 1- round robin...
// 2-  partition -> hash

public class ReverseProxyServer {
    public static void main(String[] args) throws IOException {
        List<String> hosts = ProxyServerSelector.getHostsFromFile("./configs/hosts");

        ProxyServerSelector serverSelector = new ProxyServerSelector(hosts);

        System.out.println(args[0]);

        try {
            int remotePort = 4000;
            int localPort = 4500;

            System.out.println("Starting proxy on port " + localPort);
            // And start running the server
            runServer(serverSelector, remotePort, localPort); // never returns
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * runs a single-threaded proxy server on
     * the specified local port. It never returns.
     */
    public static void runServer(ProxyServerSelector serverSelector, int remotePort, int localPort)
            throws IOException {
        // Create a ServerSocket to listen for connections with
        ServerSocket proxyServerSocket = new ServerSocket(localPort);

        final byte[] request = new byte[1024];
        byte[] reply = new byte[4096];

        while (true) {
            Socket clientSocket = null, remoteServerSocket = null;
            try {
                // Wait for a connection on the local port
                clientSocket = proxyServerSocket.accept();
                System.out.println("Connected!");

                final InputStream clientInputStream = clientSocket.getInputStream();
                final OutputStream clientOutputStream = clientSocket.getOutputStream();

                // Make a connection to the real server.
                // If we cannot connect to the server, send an error to the
                // client, disconnect, and continue waiting for connections.\

                String curHost = serverSelector.getNextHost();
                try {
                    remoteServerSocket = new Socket(curHost, remotePort);
                } catch (IOException e) {
                    PrintWriter pw = new PrintWriter(clientOutputStream);
                    pw.print("Proxy server cannot connect to " + curHost + ":"
                            + remotePort + ":\n" + e + "\n");
                    pw.flush();
                    clientSocket.close();
                    continue;
                }

                // Get remote server streams.
                final InputStream remoteServerInputStream = remoteServerSocket.getInputStream();
                final OutputStream remoteServerOutputStream = remoteServerSocket.getOutputStream();

                // a thread to read the client's requests and pass them
                // to the server. A separate thread for asynchronous.
                Thread t = new Thread() {
                    public void run() {
                        int bytesRead;
                        try {
                            while ((bytesRead = clientInputStream.read(request)) != -1) {
                                remoteServerOutputStream.write(request, 0, bytesRead);
                                remoteServerOutputStream.flush();
                            }
                        } catch (IOException e) {
                        }

                        // the client closed the connection to us, so close our
                        // connection to the server.
                        try {
                            remoteServerOutputStream.close();
                        } catch (IOException e) {
                        }
                    }
                };

                // Start the client-to-server request thread running
                t.start();

                // Read the server's responses
                // and pass them back to the client.
                int bytesRead;
                try {
                    while ((bytesRead = remoteServerInputStream.read(reply)) != -1) {
                        clientOutputStream.write(reply, 0, bytesRead);
                        clientOutputStream.flush();
                    }
                } catch (IOException e) {
                }

                // The server closed its connection to us, so we close our
                // connection to our client.
                clientOutputStream.close();
            } catch (IOException e) {
                System.err.println(e);
            } finally {
                try {
                    if (remoteServerSocket != null)
                        remoteServerSocket.close();
                    if (clientSocket != null)
                        clientSocket.close();
                } catch (IOException e) {
                }
            }
        }
    }
}
