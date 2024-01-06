package myclass;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class HtmlResources { // Singleton
	// Static resources
	private static HtmlResources instance = null;
	public static final File WEB_ROOT = new File("./html/");

	private Map<String, byte[]> htmlCache;

	private HtmlResources() {
		this.htmlCache = new HashMap<String, byte[]>();
	}

	public static HtmlResources getInstance() {
		if (instance == null) {
			instance = new HtmlResources();
		}
		return instance;
	}

	private synchronized byte[] loadFromDisk(String filename) throws IOException {
		File file = new File(WEB_ROOT, filename);
		byte[] fileContent = new byte[(int) file.length()];
		try (FileInputStream inputStream = new FileInputStream(file)) {
			inputStream.read(fileContent);
		}
		this.htmlCache.put(filename, fileContent);
		return this.htmlCache.get(filename);
	}

	public byte[] loadHTML(String filename) throws IOException {
		if (this.htmlCache.containsKey(filename)) {
			return this.htmlCache.get(filename);
		}

		byte[] fileContent = loadFromDisk(filename);
		this.htmlCache.put(filename, fileContent);
		return this.htmlCache.get(filename);
	}

	public byte[] load_DEFAULT_FILE() throws IOException {
		return this.loadHTML("index.html");
	}

	public byte[] load_NOT_FOUND() throws IOException {
		return this.loadHTML("not_found.html");
	}

	public byte[] load_REDIRECT() throws IOException {
		return this.loadHTML("redirect.html");
	}

	public byte[] load_REDIRECT_RECORDED() throws IOException {
		return this.loadHTML("redirect_recorded.html");
	}

	public byte[] load_BAD_REQUEST() throws IOException {
		return this.loadHTML("bad_request.html");
	}

	public byte[] load_CONFLICT_REQUEST() throws IOException {
		return this.loadHTML("conflict.html");
	}

}
