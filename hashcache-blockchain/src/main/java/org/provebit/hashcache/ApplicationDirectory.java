package org.provebit.hashcache;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public enum ApplicationDirectory implements DirectoryAccessor {
	INSTANCE;
	
	private File applicationDirectory;

	private ApplicationDirectory() {
		final Logger ilog = LoggerFactory.getLogger(ApplicationDirectory.class);
		String workingDirectory;
		String OS = (System.getProperty("os.name")).toUpperCase();

		// get system's default application data folder
		if (OS.contains("WIN")) {
			workingDirectory = System.getenv("AppData");
			// TODO check windows works
			workingDirectory += "\\..\\Local\\HashCache";
		} else {
			// linux
			workingDirectory = System.getProperty("user.home");
			// for mac
			if (OS.contains("MAC")) {
				// TODO check mac works
				workingDirectory += "/Library/Application Support";
			}
			workingDirectory += "/.hashcache";
		}

		// create application folder if it does not exist
		applicationDirectory = new File(workingDirectory);
		if (!applicationDirectory.exists()) {
			if (!applicationDirectory.mkdir()) {
				//log.error("Error creating the application folder");
				ilog.error("Wat");
			}
		}
		//ilog.error("asdf");
	}

	public File getRoot() {
		return applicationDirectory;
	}
}
