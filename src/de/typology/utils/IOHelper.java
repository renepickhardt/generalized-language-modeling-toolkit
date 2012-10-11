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
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

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
	 * this function returns all files in a directory that have a certain
	 * extension. it also checks if the argument is a directory and the
	 * extension is a proper extension
	 * 
	 * @param sourcePath
	 * @param fileExtension
	 * @param calledFunctionName
	 * @return
	 */
	public static File[] getAllFilesInDirWithExtension(String sourcePath,
			String fileExtension, String calledFunctionName) {
		File dir = new File(sourcePath);
		if (!dir.isDirectory()) {
			IOHelper.strongLog("error in " + calledFunctionName
					+ " . specified argument sourcePath: " + sourcePath
					+ " is not a directory");
			return null;
		}
		if (!fileExtension.startsWith(".")) {
			IOHelper.strongLog("error in "
					+ calledFunctionName
					+ " specified argument fileExtension: "
					+ fileExtension
					+ " is not a proper fileExtension e.g. it does not start with a \".\"");
			return null;
		}
		File[] files = dir.listFiles();

		ArrayList<File> res = new ArrayList<File>();
		for (File f : files) {
			String fileName = f.getName();
			if (!fileName.endsWith(fileExtension)) {
				IOHelper.log(fileName
						+ " is not an unaggregated ngram file. process next");
				continue;
			}
			res.add(f);
		}
		// File[] retArray = new File[res.size()];
		// for (int i = 0; i < res.size(); i++) {
		// retArray[0] = res.get(i).getAbsoluteFile();
		// }
		File[] array = res.toArray(new File[res.size()]);
		return array;
	}

	/**
	 * opens buffered reader that are named after the oldFile + a letter from
	 * the most common letters. the buffered readers are stored into a hashset
	 * and returned. the memory of the buffered is set according to the flag
	 * memoryLimitForWritingFiles in the config file
	 * 
	 * @param oldFile
	 * @param letters
	 * @return
	 */
	public static HashMap<String, BufferedWriter> createWriter(String oldFile,
			String[] letters) {
		IOHelper.log("create chunks for most common letters: "
				+ letters.toString());
		HashMap<String, BufferedWriter> writers = new HashMap<String, BufferedWriter>();
		for (String letter : letters) {
			String newFileName;
			if (oldFile.contains(".")) {
				newFileName = oldFile.replace(".", letter + ".");
			} else {
				newFileName = oldFile + letter + ".chk";
			}
			BufferedWriter bw = IOHelper.openWriteFile(newFileName,
					Config.get().memoryLimitForWritingFiles);
			writers.put(letter, bw);
		}
		String newFileName;
		if (oldFile.contains(".")) {
			newFileName = oldFile.replace(".", "other.");
		} else {
			newFileName = oldFile + "other.chk";
		}

		BufferedWriter bw = IOHelper.openWriteFile(newFileName,
				Config.get().memoryLimitForWritingFiles);
		writers.put("other", bw);

		IOHelper.log("all chunks are created");
		return writers;
	}

	public static BufferedReader openReadFile(String filename, int bufferSize) {
		FileInputStream fstream;
		BufferedReader br = null;
		try {
			fstream = new FileInputStream(filename);
			DataInputStream in = new DataInputStream(fstream);
			br = new BufferedReader(new InputStreamReader(in), bufferSize);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
		return br;
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

	public static BufferedWriter openWriteFile(String filename, int bufferSize) {
		FileWriter filestream = null;
		try {
			filestream = new FileWriter(filename);
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
		return new BufferedWriter(filestream, bufferSize);
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