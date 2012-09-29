package de.typology.Keystrokes;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;

import de.typology.utils.Config;

/**
 * This class is designed to learn a typology like graph on a character level
 * from loads of input text the idea is that the letter like graph is so small
 * that a complete edge Hashmap with probabilities can be stored in memory. This
 * hashmap can be created for severl n's of n distance.
 * 
 * 
 * IMPORTANT config variables:
 * 
 * * keystrokesWindowSize = n the n from n - distance * TODO keystrokesInputFile
 * = the file from which text will be learnt.
 * 
 * @author rpickhardt
 * 
 */
public class KeystrokesLearner extends Keystrokes {
	public static void main(String[] args) {

		nodes = new HashMap<String, Long>();
		edges = new HashMap<String, Long>();

		goThroughText();

		// KeystrokesHelper.loadEdges(edges, nodes);
		// generateAlphabet(nodes);
		// OutEdges();
	}

	public static void goThroughText() {
		String filePath = Config.get().germanWikiText;
		KeystrokesHelper.generateAlphabet(nodes);
		BufferedReader br = null;
		try {
			String sCurrentLine;
			br = new BufferedReader(new FileReader(filePath));
			int cnt = 4000;
			while ((sCurrentLine = br.readLine()) != null) {
				putInLetterGraph(sCurrentLine);
				if (cnt % 1000 == 0) {
					System.out.println(cnt);
					if (cnt % 5000 == 0) {
						OutEdges(0);
						OutEdges(1);
						OutEdges(2);
						KeystrokesHelper.OutEdges(edges, nodes);
					}
				}
				cnt++;
			}

		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				if (br != null) {
					br.close();
				}
			} catch (IOException ex) {
				ex.printStackTrace();
			}
		}
	}

	private static void OutEdges(int norm) {
		for (int i = 1; i < Config.get().keystrokesWindowSize; i++) {
			File file = new File(Config.get().letterGraph + norm + "." + i
					+ ".txt");
			try {
				FileWriter out = new FileWriter(file, false);
				for (String k1 : nodes.keySet()) {
					out.write("#" + k1 + "\t" + nodes.get(k1) + "\n");
					for (String k2 : nodes.keySet()) {
						out.write(k2 + " ");
						String edgeKey = k1 + k2 + "#" + i;
						if (edges.containsKey(edgeKey)) {
							float cut = edges.get(edgeKey);
							float a = nodes.get(k1);
							float b = nodes.get(k2);
							float result = 0;
							switch (norm) {
							case 0:
								result = cut / b;
								break;
							case 1:
								result = cut / a;
								break;
							case 2:
								result = cut / (a + b - cut);
								break;
							}
							out.write(result + "\t");
						} else {
							out.write("0\t");
						}
					}
					out.write("\n\n");
				}
				out.flush();
				out.close();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}
	}

	/**
	 * 
	 * @param sCurrentLine
	 * @author Rene Pickhardt
	 */
	private static void putInLetterGraph(String sCurrentLine) {
		for (int i = 0; i < sCurrentLine.length()
				- Config.get().keystrokesWindowSize; i++) {
			String curI = "" + sCurrentLine.charAt(i);
			if (nodes.containsKey(curI)) {
				nodes.put(curI, nodes.get(curI) + 1);
				for (int j = 1; j < Config.get().keystrokesWindowSize; j++) {
					String curJ = sCurrentLine.charAt(i + j) + "";
					if (nodes.containsKey(curJ)) {
						String edge = curI + curJ + "#" + j;
						if (edges.containsKey(edge)) {
							Long count = edges.get(edge) + 1;
							edges.put(edge, count);
						} else {
							edges.put(edge, new Long(1));
						}
					}
				}
			}
		}
	}
}
