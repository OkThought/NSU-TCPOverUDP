package ru.nsu.ccfit.bogush.tcptransfer;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FilenameProcessor {
	private static final String REGEXP_FILE_PATTERN = "(?<name>.+?)(?>-(?<copies>\\d+))?(?<extension>\\..+)";

	private Matcher m;
	private String name = null;
	private String extension = null;
	private Path path;
	private int copies = 0;

	public FilenameProcessor(String filename) {
		Pattern pattern = Pattern.compile(REGEXP_FILE_PATTERN);
		m = pattern.matcher(filename);
		if (m.matches()) {
			name = m.group("name");
			String copiesGroup = m.group("copies");
			copies = copiesGroup == null ? 0 : Integer.parseInt(copiesGroup);
			extension = m.group("extension");
			path = Paths.get(getFullName());
		}
	}

	public void slideThroughExistingCopies() {
		while (Files.exists(path)) {
			path = getNext();
		}
	}

	private Path getNext() {
		return Paths.get(name + "-" + (++copies) + extension);
	}

	public String getName() {
		return name;
	}

	public String getFullName() {
		return name + (copies == 0 ? "" : "-" + copies) + extension;
	}

	public String getExtension() {
		return extension;
	}

	public int getCopies() {
		return copies;
	}

	public Path getPath() {
		return path;
	}

	public File getFile() {
		return path.toFile();
	}
}
