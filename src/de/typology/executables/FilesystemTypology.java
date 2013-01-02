package de.typology.executables;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;

public class FilesystemTypology {
	public HashMap<String, File> fileIndex;
	public HashMap<String, HashMap<String, Long>> wordOffsetIndex;

	public FilesystemTypology() {
		this.fileIndex = new HashMap<String, File>();
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String input = "/var/lib/datasets/out/wikipedia/typoEdgesDENormalizedGer7095/";
		// String input = "/dev/shm/typo/";
		FilesystemTypology fst = new FilesystemTypology();
		long start = System.currentTimeMillis();
		for (int i = 0; i < 1; i++) {
			fst.run(input);
		}
		// fst.display();
		//
		// start = System.currentTimeMillis();
		// for (int i = 0; i < 1; i++) {
		// fst.openStreams();
		// }
		// System.out.println("" + (System.currentTimeMillis() - start));
		start = System.currentTimeMillis();
		fst.buildIndices();
		System.out.println("BUILT index in "
				+ (System.currentTimeMillis() - start));

		// start = System.currentTimeMillis();
		// final File testFile = new
		// File("/home/rpickhardt/Desktop/test/out.test");
		// HashMap<String, Long> index = fst.buildIndexforFile(testFile);
		// System.out.println("DONE" + (System.currentTimeMillis() - start));

		start = System.currentTimeMillis();
		for (int i = 0; i < 1; i++) {
			String q = "das ist";
			String p = "A";
			int k = 5;
			int hit = 0;
			// DUMMY TEST RIGHT NOW
			// fst.fileIndex.get("1-i");
			long seek = fst.wordOffsetIndex.get("1-i").get("ist");
			try {
				RandomAccessFile raf = new RandomAccessFile(
						fst.fileIndex.get("1-i"), "r");
				raf.seek(seek);
				int len = 162040;
				byte[] b = new byte[len];
				raf.read(b, 0, len);
				String res = new String(b, "UTF8");
				String[] lines = res.split("\n");
				for (String line : lines) {
					String[] fields = line.split("\t");
					if (fields.length < 3) {
						break;
					}
					if (!fields[0].equals("ist")) {
						break;
					}
					if (!fields[1].startsWith(p)) {
						continue;
					}
					Float f = Float.parseFloat(fields[2]);
					System.out.println(fields[1] + "\t" + f);
					if (hit++ >= k) {
						break;
					}
				}

				hit = 0;
				seek = fst.wordOffsetIndex.get("2-d").get("das");
				raf = new RandomAccessFile(fst.fileIndex.get("2-d"), "r");
				raf.seek(seek);
				len = 162040;
				b = new byte[len];
				raf.read(b, 0, len);
				res = new String(b, "UTF8");
				lines = res.split("\n");
				for (String line : lines) {
					String[] fields = line.split("\t");
					if (fields.length < 3) {
						break;
					}
					if (!fields[0].equals("das")) {
						break;
					}
					if (!fields[1].startsWith(p)) {
						continue;
					}
					Float f = Float.parseFloat(fields[2]);
					System.out.println(fields[1] + "\t" + f);
					if (hit++ >= k) {
						break;
					}

				}
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}
		System.out.println("DONE in " + (System.currentTimeMillis() - start));
		// fst.display();
	}

	private void buildIndices() {
		this.wordOffsetIndex = new HashMap<String, HashMap<String, Long>>();

		long size = 0;

		for (String key : this.fileIndex.keySet()) {
			if (key.startsWith("4")) {
				continue;
			}
			if (key.startsWith("3")) {
				continue;
			}
			HashMap<String, Long> index = this.buildIndexforFile(this.fileIndex
					.get(key));
			size += index.size();
			System.out.println(size + "\t" + key + "\t "
					+ this.fileIndex.get(key));
			this.wordOffsetIndex.put(key, index);

		}
	}

	HashMap<String, Long> buildIndexforFile(File f) {
		RandomAccessFile raf;

		HashMap<String, Long> index = new HashMap<String, Long>(50000);
		int bocnt = 0;

		try {
			raf = new RandomAccessFile(f, "r");
			long size = f.length();
			int buffsize = 1;
			int cnt = 0;
			raf.seek(0);
			while (buffsize != -1) {
				int len = 1000000;
				byte[] b = new byte[len];
				buffsize = raf.read(b, 0, len);
				for (int i = 0; i < len; i++) {
					if (b[i] == '\n') {
						int start = i + 1;
						boolean bufferOverflow = true;
						for (int j = i + 1; j < len; j++) {
							if (b[j] == '\t') {
								int end = j - 1;
								bufferOverflow = false;
								String firstWord = this.getWord(b, start, end);
								if (index.containsKey(firstWord)) {
									break;
								} else {
									index.put(firstWord,
											(long) (len * cnt + start));
									break;
								}
							}
						}
						if (bufferOverflow) {
							bocnt++;
							// System.out.println("BO:@\t"
							// + (long) (len * cnt + start));
						}
					}
				}
				cnt++;
			}
			raf.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("index size: " + index.size() + " numbo:" + bocnt);
		return index;
	}

	private String getWord(byte[] b, int start, int end) {
		byte[] wordArray = new byte[end - start + 1];
		for (int i = start; i <= end; i++) {
			wordArray[i - start] = b[i];
		}
		try {
			String roundTrip = new String(wordArray, "UTF8");
			return roundTrip;
			// System.out.println(len - roundTrip.length());
			// System.out.println("roundTrip = " + roundTrip);
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return null;
	}

	public void openStreams() {
		int cnt = 0;
		for (String k : this.fileIndex.keySet()) {
			if (cnt >= 3) {
				break;
			}
			if (Math.random() < 0.1) {
				try {
					RandomAccessFile raf = new RandomAccessFile(
							this.fileIndex.get(k), "r");
					long size = this.fileIndex.get(k).length();
					// System.out.println(size);
					int len = 1000;
					byte[] b = new byte[len];
					int off = (int) (len + Math.random() * size - len);
					raf.seek(off);
					raf.read(b, 0, len);

					try {
						String roundTrip = new String(b, "UTF8");
						// System.out.println("roundTrip = " + roundTrip);
					} catch (UnsupportedEncodingException e) {
						e.printStackTrace();
					}

					// for (byte e : b) {
					// System.out.print("" + (char) e);
					// }
					// System.out.println("\n");
					raf.close();
					cnt++;
				} catch (FileNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
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
					String[] val = tmp.split("\\.");
					if (val[1].endsWith("es")) {
						continue;
					}
					tmp = val[0];
					this.fileIndex.put(path + "-" + tmp, indexFile);
					// System.out.println(indexFile.getAbsolutePath());
				}
			}
		}
	}
}
