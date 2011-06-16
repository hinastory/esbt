package org.scalastuff.esbt;

import static org.scalastuff.esbt.Utils.indexOfLineContaining;
import static org.scalastuff.esbt.Utils.read;
import static org.scalastuff.esbt.Utils.write;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class SbtPluginCreator {
	
	private static File userHome;
	
	public static void createSbtPlugin() throws IOException {
		// for debugging purposes
		if (userHome == null || true) {
			userHome = new File(System.getProperty("user.home"));
			File pluginDir = new File(userHome, ".sbt/plugins");
			pluginDir.mkdirs();
			createBuildSbt(pluginDir);
			createSbtEclipsePluginFile(pluginDir);
		}
	}
	
	private static void createBuildSbt(File pluginDir) throws IOException {
		File file = new File(pluginDir, "build.sbt");
		if (!file.exists()) {
			file.createNewFile();
		}
		List<String> lines = read(file);
		if (indexOfLineContaining(lines, "sbtPlugin := true") < 0) {
			lines.add("");
			lines.add("sbtPlugin := true");
			write(new FileOutputStream(file), lines);
		}
	}
	
	private static void createSbtEclipsePluginFile(File pluginDir) throws IOException {
		InputStream is = SbtPluginCreator.class.getResourceAsStream("SbtEclipsePlugin.scala.source");
		if (is == null) {
			throw new IOException("Coulnd't find SbtEclipsePlugin.scala.source");
		}
		List<String> content = read(is);
		File destFile = new File(pluginDir, "SbtEclipsePlugin.scala");
		List<String> existingContent = read(destFile);
		if (!content.equals(existingContent)) {
			write(new FileOutputStream(destFile), content);
		}
	}
}