package de.typology.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

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
			BufferedReader stdInput = new BufferedReader(new InputStreamReader(
					p.getInputStream()));
			String s = "";
			while ((s = stdInput.readLine()) != null) {
				IOHelper.log(s);
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
