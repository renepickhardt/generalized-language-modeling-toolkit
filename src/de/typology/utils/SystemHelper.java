package de.typology.utils;

import java.io.IOException;

public class SystemHelper {

	/*
	 * from stack overflow discussion:
	 * http://stackoverflow.com/a/3403256/1512538
	 */
	public static void runUnixCommand(String cmd) {
		Process p;
		try {
			p = Runtime.getRuntime()
					.exec(new String[] { "/bin/sh", "-c", cmd });
			p.waitFor();
			p.destroy();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}
}
