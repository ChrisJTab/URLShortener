import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class URLShortner {
    // port to listen connection
    // static final int PORT = 8080;

    // verbose mode
    static final boolean verbose = true;

    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Usage: java URLShortner.java PORT");
            return;
        }

        // port to listen connection
        int PORT = Integer.parseInt(args[0]);


        ExecutorService executor = Executors.newFixedThreadPool(8);

        ServerSocket serverSocket = null;
        try {
            serverSocket = new ServerSocket(PORT);
            System.out.println("Server started.\nListening for connections on port : " + PORT + " ...\n");

            // we listen until user halts server execution
            while (true) {
                Socket workerSocket = null;
                if (verbose) {
                    System.out.println("Connection opened. (" + new Date() + ")");
                }
                workerSocket = serverSocket.accept();
                executor.execute(new RQWorker(workerSocket));

                // Wait until all threads are finish
                // executor.awaitTermination();
            }
        } catch (IOException e) {

            // This will make the executor accept no new threads
            // and finish all existing threads in the queue
            // executor.shutdown();
            System.err.println("Server Connection error : " + e.getMessage());
        }

        try {
            if (serverSocket != null) {
                serverSocket.close();
            }
        } catch (IOException e) {
        }
    }
}

class RQWorker implements Runnable {
    static final File WEB_ROOT = Resources.WEB_ROOT;
    static final String DEFAULT_FILE = Resources.DEFAULT_FILE;
    static final String FILE_NOT_FOUND = Resources.FILE_NOT_FOUND;
    static final String METHOD_NOT_SUPPORTED = Resources.METHOD_NOT_SUPPORTED;
    static final String REDIRECT_RECORDED = Resources.REDIRECT_RECORDED;
    static final String REDIRECT = Resources.REDIRECT;
    static final String NOT_FOUND = Resources.NOT_FOUND;
    static final String DATABASE = Resources.DATABASE;
    private Socket workerSocket;
    static final boolean verbose = true;

    RQWorker(Socket workerSocket) {
        this.workerSocket = workerSocket;
    }

    public void run() {
        BufferedReader in = null;
        PrintWriter out = null;
        BufferedOutputStream dataOut = null;

        try {
            in = new BufferedReader(new InputStreamReader(workerSocket.getInputStream()));
            out = new PrintWriter(workerSocket.getOutputStream());
            dataOut = new BufferedOutputStream(workerSocket.getOutputStream());

            String input = in.readLine();

            if (verbose)
                System.out.println("first line: " + input);
            Pattern pput = Pattern.compile("^PUT\\s+/\\?short=(\\S+)&long=(\\S+)\\s+(\\S+)$");
            Matcher mput = pput.matcher(input);
            if (mput.matches()) {
                String shortResource = mput.group(1);
                String longResource = mput.group(2);
                String httpVersion = mput.group(3);

                save(shortResource, longResource);

                File file = new File(WEB_ROOT, REDIRECT_RECORDED);
                int fileLength = (int) file.length();
                String contentMimeType = "text/html";
                // read content to return to client
                byte[] fileData = readFileData(file, fileLength);

                out.println("HTTP/1.1 200 OK");
                out.println("Server: Java HTTP Server/Shortner : 1.0");
                out.println("Date: " + new Date());
                out.println("Content-type: " + contentMimeType);
                out.println("Content-length: " + fileLength);
                out.println();
                out.flush();

                dataOut.write(fileData, 0, fileLength);
                dataOut.flush();
            } else {
                Pattern pget = Pattern.compile("^(\\S+)\\s+/(\\S+)\\s+(\\S+)$");
                Matcher mget = pget.matcher(input);
                if (mget.matches()) {
                    String method = mget.group(1);
                    String shortResource = mget.group(2);
                    String httpVersion = mget.group(3);

                    String longResource = find(shortResource);
                    if (longResource != null) {
                        File file = new File(WEB_ROOT, REDIRECT);
                        int fileLength = (int) file.length();
                        String contentMimeType = "text/html";

                        // read content to return to client
                        byte[] fileData = readFileData(file, fileLength);

                        // out.println("HTTP/1.1 301 Moved Permanently");
                        out.println("HTTP/1.1 307 Temporary Redirect");
                        out.println("Location: " + longResource);
                        out.println("Server: Java HTTP Server/Shortner : 1.0");
                        out.println("Date: " + new Date());
                        out.println("Content-type: " + contentMimeType);
                        out.println("Content-length: " + fileLength);
                        out.println();
                        out.flush();

                        dataOut.write(fileData, 0, fileLength);
                        dataOut.flush();
                    } else {
                        File file = new File(WEB_ROOT, FILE_NOT_FOUND);
                        int fileLength = (int) file.length();
                        String content = "text/html";
                        byte[] fileData = readFileData(file, fileLength);

                        out.println("HTTP/1.1 404 File Not Found");
                        out.println("Server: Java HTTP Server/Shortner : 1.0");
                        out.println("Date: " + new Date());
                        out.println("Content-type: " + content);
                        out.println("Content-length: " + fileLength);
                        out.println();
                        out.flush();

                        dataOut.write(fileData, 0, fileLength);
                        dataOut.flush();
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Server error");
        } finally {
            try {
                in.close();
                out.close();
                workerSocket.close(); // we close socket connection
            } catch (Exception e) {
                System.err.println("Error closing stream : " + e.getMessage());
            }

            if (verbose) {
                System.out.println("Connection closed.\n");
            }
        }
    }

    private static String find(String shortURL) {
        String longURL = null;
        try {
            File file = new File(DATABASE);
            FileReader fileReader = new FileReader(file);
            BufferedReader bufferedReader = new BufferedReader(fileReader);
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                String[] map = line.split("\t");
                if (map[0].equals(shortURL)) {
                    longURL = map[1];
                    break;
                }
            }
            fileReader.close();
        } catch (IOException e) {

        }
        return longURL;
    }

    private static void save(String shortURL, String longURL) {
        try {
            File file = new File(DATABASE);
            FileWriter fw = new FileWriter(file, true);
            BufferedWriter bw = new BufferedWriter(fw);
            PrintWriter pw = new PrintWriter(fw);
            pw.println(shortURL + "\t" + longURL);
            pw.close();
        } catch (IOException e) {

        }
        return;
    }

    private static byte[] readFileData(File file, int fileLength) throws IOException {
        FileInputStream fileIn = null;
        byte[] fileData = new byte[fileLength];

        try {
            fileIn = new FileInputStream(file);
            fileIn.read(fileData);
        } finally {
            if (fileIn != null)
                fileIn.close();
        }

        return fileData;
    }
}
