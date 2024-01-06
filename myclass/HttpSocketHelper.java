package myclass;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class HttpSocketHelper {
	private static HtmlResources htmlResources = HtmlResources.getInstance();

	public static void sendRequest(OutputStream outputStream, List<String> lines, byte[] content) throws IOException {
		PrintWriter printWriter = new PrintWriter(outputStream);
		lines.forEach(line -> printWriter.println(line));
		printWriter.flush();

		BufferedOutputStream dataOut = new BufferedOutputStream(outputStream);
		dataOut.write(content, 0, content.length);
		dataOut.flush();
	}

	public static void sendHttp404(OutputStream outputStream, String serverName) throws IOException {
		byte[] content = htmlResources.load_NOT_FOUND();

		List<String> lines = new ArrayList<String>();
		lines.add("HTTP/1.1 404 Not Found");
		lines.add("Server: " + serverName);
		lines.add("Date: " + new Date());
		lines.add("Content-type: " + " text/html");
		lines.add("Content-length: " + content.length);
		lines.add("");

		sendRequest(outputStream, lines, content);
	}

	public static void sendHttp400(OutputStream outputStream, String serverName) throws IOException {
		byte[] content = htmlResources.load_BAD_REQUEST();

		List<String> lines = new ArrayList<String>();
		lines.add("HTTP/1.1 400 Bad Request");
		lines.add("Server: " + serverName);
		lines.add("Date: " + new Date());
		lines.add("Content-type: " + " text/html");
		lines.add("Content-length: " + content.length);
		lines.add("");

		sendRequest(outputStream, lines, content);
	}

	public static void sendHttp307(OutputStream outputStream, String serverName, String location) throws IOException {
		byte[] content = htmlResources.load_REDIRECT();

		List<String> lines = new ArrayList<String>();
		lines.add("HTTP/1.1 307 Temporary Redirect");
		lines.add("Location: " + location);
		lines.add("Server: " + serverName);
		lines.add("Date: " + new Date());
		lines.add("Content-type: " + " text/html");
		lines.add("Content-length: " + content.length);
		lines.add("");

		sendRequest(outputStream, lines, content);
	}

	public static void sendHttp201(OutputStream outputStream, String serverName) throws IOException {
		byte[] content = htmlResources.load_REDIRECT_RECORDED();

		List<String> lines = new ArrayList<String>();
		lines.add("HTTP/1.1 201 Created");
		lines.add("Server: " + serverName);
		lines.add("Date: " + new Date());
		lines.add("Content-type: " + " text/html");
		lines.add("Content-length: " + content.length);
		lines.add("");

		sendRequest(outputStream, lines, content);
	}

	public static void sendHttp409(OutputStream outputStream, String serverName) throws IOException {
		byte[] content = htmlResources.load_CONFLICT_REQUEST();

		List<String> lines = new ArrayList<String>();
		lines.add("HTTP/1.1 201 Conflict");
		lines.add("Server: " + serverName);
		lines.add("Date: " + new Date());
		lines.add("Content-type: " + " text/html");
		lines.add("Content-length: " + content.length);
		lines.add("");

		sendRequest(outputStream, lines, content);
	}
}
