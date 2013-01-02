package de.typology.executables;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;

public class FilesystemTypology {
	private HashMap<String, File> fileIndex;

	public FilesystemTypology() {
		this.fileIndex = new HashMap<String, File>();
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String input = "/var/lib/datasets/out/wikipedia/typoEdgesDENormalizedGer7095/";
		FilesystemTypology fst = new FilesystemTypology();
		long start = System.currentTimeMillis();
		for (int i = 0; i < 100; i++) {
			fst.run(input);
			System.out.println("" + (System.currentTimeMillis() - start));
		}
		fst.display();

		start = System.currentTimeMillis();
		for (int i = 0; i < 100; i++) {
			fst.openStreams();
			System.out.println("" + (System.currentTimeMillis() - start));
		}
		// fst.display();
	}

	public void openStreams() {
		for (String k : this.fileIndex.keySet()) {
			try {
				FileInputStream stream = new FileInputStream(
						this.fileIndex.get(k));
				stream.close();
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	private void display() {
		for (String k : this.fileIndex.keySet()) {
			System.out.println(k + "\t" + this.fileIndex.get(k));
		}
		System.out.println(this.fileIndex.size());
	}

	public void run(String input) {
		File f = new File(input);
		this.fileIndex = new HashMap<String, File>();
		for (String path : f.list()) {
			File typeDir = new File(input + path + "/");
			// System.out.println(typeDir.getAbsolutePath());
			for (String indexPath : typeDir.list()) {
				File indexFile = new File(input + path + "/" + indexPath);
				String tmp = indexFile.getName();
				if (tmp.contains(".")) {
					tmp = tmp.split("\\.")[0];
					this.fileIndex.put(path + "-" + tmp, indexFile);
					// System.out.println(indexFile.getAbsolutePath());
				}
			}
		}
	}
}
