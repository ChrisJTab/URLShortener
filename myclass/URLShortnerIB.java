package myclass;
import java.io.*;
import java.net.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import caching.URLCache;
import configs.GeneralConfigs;

public class URLShortnerIB {
	private static String SERVER_NAME = "URLShortner";

	public static Connection dbConnection;

	public static void main(String[] args) {
		if (args.length != 1) {
			System.out.println("Usage: URLShortner URLShortnerServerPort");
			return;
		}

		// System.out.println("before");
		// recreateDatabaseFromBackup();
		// System.out.println("after");

		// HeartBeatMonitor heartBeatMonitor = new HeartBeatMonitor();
		// heartBeatMonitor.startHealthCheckThreadURLDB();

		Integer urlShortnerServerPort = Integer.parseInt(args[0]);

		ExecutorService executorService = Executors.newFixedThreadPool(GeneralConfigs.THREAD_POOL_SIZE);

		URLCache<String, String> urlCache = new URLCache<String, String>(GeneralConfigs.CACHE_SIZE);
		dbConnection = initializeDatabaseConnection();

		try (ServerSocket serverSocket = new ServerSocket(urlShortnerServerPort)) {
			System.out.printf(
					"URLShortner Server started.\nListening for connections on %s:%d ...\n",
					InetAddress.getLocalHost().getHostName(),
					urlShortnerServerPort);

			while (true) {
				Socket clientSocket = serverSocket.accept();
				executorService.submit(new ProxyThread(clientSocket, urlCache, dbConnection));
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			closeDatabaseConnection(dbConnection);
			executorService.shutdown();
		}
	}

	private static Connection initializeDatabaseConnection() {
		try {
			// Create and return a single SQLite database connection
			// handleDatabaseError();
			return DriverManager.getConnection(Resources.BACKUP_DATABASE);
		} catch (SQLException e) {
			System.out.println("Problem with connecting to the database");
			// handleDatabaseError();
			e.printStackTrace();
			return null;
		}
	}

	private static void resetDatabaseConnection() {
		try { // Close db connection
			if (dbConnection != null && !dbConnection.isClosed()) {
				dbConnection.close();
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}

		try {
			// Create and return a single SQLite database connection
			// handleDatabaseError();
			dbConnection = DriverManager.getConnection(Resources.BACKUP_DATABASE);
		} catch (SQLException e) {
			System.out.println("Problem with connecting to the database");
			handleDatabaseError();
			e.printStackTrace();
		}
	}

	private static void closeDatabaseConnection(Connection connection) {
		try {
			if (connection != null) {
				connection.close();
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}



	private static void handleDatabaseError() {
        File databaseFile = new File(Resources.BACKUP_DATABASE.substring(12)); // Removing "jdbc:sqlite:" prefix
		System.out.println("handleDatabaseError has been called");
        if (databaseFile.exists()) {
            if (databaseFile.delete()) {
            System.out.println("Deleted existing database file.");
        	} else {
            System.err.println("Failed to delete existing database file.");
        	}
        }

        recreateDatabaseFromBackup();
    }


	private static void recreateDatabaseFromBackup() {
        // Run a bash script to recreate the database file and copy content from the backup
        String scriptPath = "./scripts/create_DB_from_backup.sh";
        try {
            ProcessBuilder processBuilder = new ProcessBuilder(scriptPath);

            // Redirect error stream to output stream to capture errors in the same place
            // processBuilder.redirectErrorStream(true);

            Process process = processBuilder.start();
            int exitCode = process.waitFor();
			System.out.println("exitCode:" + exitCode);

            if (exitCode == 0) {
                System.out.println("script ran successfully");
            } else {
                System.err.println("Failed to run the script.");
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
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
		private URLCache<String, String> urlCache;
		private Connection dbConnection;

		public ProxyThread(Socket clientSocket, URLCache<String, String> urlCache, Connection dbConnection) {
			this.clientSocket = clientSocket;
			this.urlCache = urlCache;
			this.dbConnection = dbConnection;
		}

		private void handleGET(OutputStream outputStream, String shortURL) throws IOException {
			String longURL;

			longURL = this.urlCache.get(shortURL); // TODO CHECK IF THIS IS SYNCRONIZED PROPERLY
			if (longURL != null) {
				System.out.printf(
						"Short='%s' & Long='%s' were found in CACHE\n",
						shortURL,
						longURL);
				HttpSocketHelper.sendHttp307(outputStream, SERVER_NAME, longURL);
				return;
			}

			longURL = readRecord(shortURL);
			if (longURL != null) {
				System.out.printf(
						"Short='%s' & Long='%s' were found in DISK\n",
						shortURL,
						longURL);
				this.urlCache.put(shortURL, longURL);
				HttpSocketHelper.sendHttp307(outputStream, SERVER_NAME, longURL);
				return;
			}

			HttpSocketHelper.sendHttp404(outputStream, SERVER_NAME);
			return;
		}

		private void handlePUT(OutputStream outputStream, String shortURL, String longURL, Integer node) throws IOException {
			boolean success = insertRecord(shortURL, longURL, node);
			if (success) {
				// On success, cache results and return success message
				this.urlCache.put(shortURL, longURL);
				HttpSocketHelper.sendHttp201(outputStream, SERVER_NAME);
			} else {
				HttpSocketHelper.sendHttp409(outputStream, SERVER_NAME);
			}
			return;
		}

		@Override
		public void run() {
			InputStream inputStream;
			OutputStream outputStream;

			try {
				System.out.println("Connected!");
				// Create input and output streams for client and target server
				inputStream = clientSocket.getInputStream();
				outputStream = clientSocket.getOutputStream();

				// Read the first line (e.g., the HTTP request line) from the client
				// before reading the firstline request, we need to read the server number that we are being sent,
				// as we will later on store this number into the database.
				// we will use this number to determine whether it is a backup input or not
				byte b = (byte) inputStream.read();
				System.out.println("THIS: " + b);  // FIX TO ObjectStrea READER/WRITER
				String firstLine = readFirstLine(inputStream);

				System.out.println(firstLine);

				String[] splitLine = firstLine.split(" ");
				String HTTP_METHOD = splitLine[0];
				String HTTP_PATH = splitLine[1];

				if (!ALLOWED_METHODS.getOrDefault(HTTP_METHOD, false)) {
					// ERROR. Direct the error back to client
					HttpSocketHelper.sendHttp404(outputStream, SERVER_NAME);
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
					HttpSocketHelper.sendHttp400(outputStream, SERVER_NAME);
					clientSocket.close();
					return;
				}

				String shortURL = matcher.group(1);
				String longURL = null;

				if (HTTP_METHOD.equals("PUT")) {
					longURL = matcher.group(2);
				}

				System.out.printf(
						"Short='%s' | Long='%s' | Node='%d'\n",
						shortURL,
						longURL,
						(int) b);

				if (HTTP_METHOD.equals("GET")) {
					this.handleGET(outputStream, shortURL);
				} else {
					this.handlePUT(outputStream, shortURL, longURL, (int) b);
				}

				// Close sockets
				clientSocket.close();
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

		// -------------- DATABASE STUFF --------------
		public synchronized boolean insertRecord(String key, String value, Integer node) {
			Boolean success = false;

			try {
				String insertSQL = "INSERT OR REPLACE INTO stuff (short, long, node) VALUES (?, ?, ?);";
				PreparedStatement ps = dbConnection.prepareStatement(insertSQL);
				ps.setString(1, key);
				ps.setString(2, value);
				ps.setInt(3, node);
				ps.executeUpdate();

				success = true;
			} catch (SQLException e) {
				System.out.println("insertRecord failuer");
				System.out.println(e.getMessage());
				handleDatabaseError();
				resetDatabaseConnection();
				// reset the whole thing 
			}
			// } finally {
			// 	try {
			// 		if (conn != null) {
			// 			conn.close();
			// 		}
			// 	} catch (SQLException ex) {
			// 		System.out.println(ex.getMessage());
			// 	}
			// }

			return success;
		}

		public synchronized String readRecord(String key) {
			try {
				String selectSQL = "SELECT * FROM stuff WHERE short = ?";
				PreparedStatement ps = dbConnection.prepareStatement(selectSQL);
				ps.setString(1, key);
				ResultSet rs = ps.executeQuery();
				if (rs.next()) {
					return rs.getString("long");
				}
			} catch (SQLException e) {
				System.out.println(e.getMessage());
			}
			// } finally {
			// 	try {
			// 		if (conn != null) {
			// 			conn.close();
			// 		}
			// 	} catch (SQLException ex) {
			// 		System.out.println(ex.getMessage());
			// 	}
			// }
			return null; // Return null if the record is not found
		}
		// -------------- DATABASE STUFF --------------
	}
}
