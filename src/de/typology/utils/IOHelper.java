/*
 * Helper class for input and output
 *
 * @author René Pickhardt
 *
 */
package de.typology.utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Date;

import de.typology.trainers.NGramReader;

public class IOHelper {
	// d = debug mode set true if debugg messages should be displayed
	private static boolean d = true;
	private static BufferedWriter logFile = openAppendFile("Complet.log");
	private static BufferedWriter strongLogFile = openAppendFile("Complet.strong.log");

	/**
	 * faster access to a buffered reader
	 * 
	 * @param filename
	 * @return buffered reader for file input
	 */
	public static BufferedReader openReadFile(String filename) {
		FileInputStream fstream;
		BufferedReader br = null;
		try {
			fstream = new FileInputStream(filename);
			DataInputStream in = new DataInputStream(fstream);
			br = new BufferedReader(new InputStreamReader(in));
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
		return br;
	}

	/**
	 * faster access to a ngram reader
	 * 
	 * @param filename
	 * @return ngram reader for file input
	 */
	public static NGramReader openReadNGrams(String filename) {
		FileInputStream fstream;
		NGramReader nr = null;
		try {
			fstream = new FileInputStream(filename);
			DataInputStream in = new DataInputStream(fstream);
			nr = new NGramReader(new InputStreamReader(in));
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
		return nr;
	}

	/**
	 * Faster access to a bufferedWriter
	 * 
	 * @param filename
	 * @return buffered writer which can be used for output
	 */
	public static BufferedWriter openWriteFile(String filename) {
		FileWriter filestream = null;
		try {
			filestream = new FileWriter(filename);
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
		return new BufferedWriter(filestream);
	}

	/**
	 * Faster access to a bufferedWriter that appands to a fil
	 * 
	 * @param filename
	 * @return buffered writer which can be used for output
	 */
	public static BufferedWriter openAppendFile(String filename) {
		FileWriter filestream = null;
		try {
			filestream = new FileWriter(filename, true);
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
		return new BufferedWriter(filestream);
	}

	/**
	 * function for output that is only displayed in debugmode
	 * 
	 * @param out
	 */
	public static void pln(Object out) {
		if (d) {
			System.out.println(out);
		}
	}

	/**
	 * function for output that is only displayed in debugmode
	 * 
	 * @param out
	 */
	public static void p(Object out) {
		if (d) {
			System.out.print(out);
		}
	}

	/*
	 * function for error-output that is only displayed in debugmode
	 * 
	 * @param out
	 */
	public static void perr(Object out) {
		if (d) {
			System.err.println(out);
		}
	}

	/**
	 * function for output that is only displayed in debugmode
	 * 
	 * @param out
	 */
	public static void log(Object out) {
		if (d) {
			System.out.println(out);
		}
		try {
			Date dt = new Date();
			logFile.write(dt + " - " + out + "\n");
			logFile.flush();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * function for output that is only displayed in debugmode
	 * 
	 * @param out
	 */
	public static void logError(Object out) {
		if (d) {
			System.err.println(out);
		}
		try {
			Date dt = new Date();
			logFile.write(dt + " - ERROR:" + out + "\n");
			logFile.flush();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * function for output that is only displayed in debugmode
	 * 
	 * @param out
	 */
	public static void strongLog(Object out) {
		log("!!!!!" + out);
		try {
			Date dt = new Date();
			strongLogFile.write(dt + " - " + out + "\n");
			strongLogFile.flush();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static boolean deleteDirectory(String path) {
		return deleteDirectory(new File(path));
	}

	/**
	 * @param path
	 *            : filepath to the directory that needs to be deleted
	 * @return true if successful
	 */
	public static boolean deleteDirectory(File path) {
		if (path.exists()) {
			File[] files = path.listFiles();
			for (File file : files) {
				if (file.isDirectory()) {
					deleteDirectory(file);
				} else {
					file.delete();
				}
			}
		}
		return path.delete();
	}
}