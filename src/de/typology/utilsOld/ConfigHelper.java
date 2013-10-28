/*
 * Helper to handle constants and config files
 *
 * All values are global static values used in complet
 * To use config file instead of standard values call loadConfigFile()
 *
 * Note that PATH variables can be overwritten by calling classes!
 *
 * @author Paul Wagner
 *
 */
package de.typology.utilsOld;

import java.io.FileInputStream;
import java.util.Properties;

public class ConfigHelper {

	// MAIN CONFIG
	public static String NAME_KEY = "word";
	public static String COUNT_KEY = "cnt";
	public static String RELLOC_KEY = "rel_loc";
	public static String RELSUM_KEY = "rel_sum";
	public static int MAX_RELATIONS = 4;

	public static int RELATIONSHIPSTORE_MEM = 100;
	public static int PROPERTYSTORE_MEM = 150;
	public static String CACHE_TYPE = "strong";
	public static Boolean BUFFER = true;

	public static String IN_PATH = "";
	public static String OUT_PATH = "";
	public static String DB_PATH = "";

	// SERVER CONFIG
	public static int SRV_PORT = 1337;
	public static int P1 = 5;
	public static int P2 = 2;
	public static int P3 = 2;
	public static int P4 = 0;

	public static void loadConfigFile(String config_file) {
		if (!config_file.isEmpty()) {
			try {
				Properties p = new Properties();
				p.load(new FileInputStream(config_file));
				IN_PATH = p.getProperty("IN_PATH", IN_PATH);
				OUT_PATH = p.getProperty("OUT_PATH", OUT_PATH);
				DB_PATH = p.getProperty("DB_PATH", DB_PATH);
				NAME_KEY = p.getProperty("NAME_KEY", NAME_KEY);
				COUNT_KEY = p.getProperty("COUNT_KEY", COUNT_KEY);
				RELLOC_KEY = p.getProperty("RELLOC_KEY", RELLOC_KEY);
				RELSUM_KEY = p.getProperty("RELLOC_KEY", RELSUM_KEY);
				SRV_PORT = Integer.parseInt(p.getProperty("SRV_PORT", SRV_PORT
						+ ""));
				P1 = Integer.parseInt(p.getProperty("P1", P1 + ""));
				P2 = Integer.parseInt(p.getProperty("P2", P2 + ""));
				P3 = Integer.parseInt(p.getProperty("P3", P3 + ""));
				P4 = Integer.parseInt(p.getProperty("P4", P4 + ""));
				MAX_RELATIONS = Integer.parseInt(p.getProperty("MAX_RELATIONS",
						MAX_RELATIONS + ""));
				RELATIONSHIPSTORE_MEM = Integer.parseInt(p.getProperty(
						"RELATIONSHIPSTORE_MEM", RELATIONSHIPSTORE_MEM + ""));
				PROPERTYSTORE_MEM = Integer.parseInt(p.getProperty(
						"PROPERTYSTORE_MEM", PROPERTYSTORE_MEM + ""));
				CACHE_TYPE = p.getProperty("CACHE_TYPE", CACHE_TYPE);
				if (p.getProperty("BUFFER", "TRUE").toUpperCase()
						.equals("TRUE")) {
					BUFFER = true;
				} else {
					BUFFER = false;
				}
			} catch (Exception e) {
				IOHelper.logError("(ConfigHelper) Error parsing config file...");
			}
		}

	}
}
