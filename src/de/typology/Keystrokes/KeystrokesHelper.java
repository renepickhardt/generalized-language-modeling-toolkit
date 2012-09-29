package de.typology.Keystrokes;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;

import de.typology.utils.Config;

/**
 * this class contains some helper functions for learning an predicting
 * keystrokes from large amounts of text e.g. Wikipedia
 * 
 * @author Rene Pickhardt
 * 
 */
public class KeystrokesHelper {
	/**
	 * @param edges
	 * @param nodes
	 */
	public static void OutEdges(HashMap<String, Long> edges,
			HashMap<String, Long> nodes) {
		File file = new File(Config.get().letterGraph);
		try {
			FileOutputStream f = new FileOutputStream(file);
			ObjectOutputStream s = new ObjectOutputStream(f);

			s.writeObject(edges);
			s.writeObject(nodes);
			s.flush();
			s.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * @param edges
	 * @param nodes
	 */
	public static void loadEdges(HashMap<String, Long> edges,
			HashMap<String, Long> nodes) {
		try {
			File file = new File(Config.get().letterGraph);
			FileInputStream f = new FileInputStream(file);
			ObjectInputStream s;
			s = new ObjectInputStream(f);
			HashMap<String, Long> e = (HashMap<String, Long>) s.readObject();
			for (String k : e.keySet()) {
				edges.put(k, e.get(k));
			}
			HashMap<String, Long> n = (HashMap<String, Long>) s.readObject();
			for (String k : n.keySet()) {
				nodes.put(k, n.get(k));
			}
			s.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * @param alphabet
	 * @author Rene Pickhardt
	 */
	public static void generateAlphabet(HashMap<String, Long> alphabet) {
		for (int i = 0; i < 26; i++) {
			alphabet.put(new String((char) (i + 65) + ""), new Long(0));
			alphabet.put(new String((char) (i + 97) + ""), new Long(0));
		}
		for (int i = 0; i < 10; i++) {
			alphabet.put(new String(i + ""), new Long(0));
		}
		alphabet.put("ä", new Long(0));
		alphabet.put("ü", new Long(0));
		alphabet.put("ö", new Long(0));
		alphabet.put(" ", new Long(0));
		alphabet.put(".", new Long(0));
		alphabet.put(",", new Long(0));
		alphabet.put(":", new Long(0));
		alphabet.put(";", new Long(0));
		alphabet.put("-", new Long(0));
		alphabet.put("\"", new Long(0));
		alphabet.put("@", new Long(0));
		alphabet.put("(", new Long(0));
		alphabet.put(")", new Long(0));
		alphabet.put("!", new Long(0));
		alphabet.put("?", new Long(0));
		alphabet.put("ß", new Long(0));
		alphabet.put("Ü", new Long(0));
		alphabet.put("Ä", new Long(0));
		alphabet.put("Ö", new Long(0));
		alphabet.put("\'", new Long(0));
		alphabet.put("§", new Long(0));
		alphabet.put("&", new Long(0));
		alphabet.put("%", new Long(0));
		alphabet.put("/", new Long(0));
		alphabet.put("\\", new Long(0));
		alphabet.put("<", new Long(0));
		alphabet.put(">", new Long(0));
	}
}
