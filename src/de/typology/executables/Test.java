package de.typology.executables;

import java.io.BufferedReader;

import de.typology.utils.Config;
import de.typology.utils.IOHelper;

public class Test {

	/**
	 * @param args
	 */
	public static void main(String[] args) {

		// BufferedReader br = IOHelper.openReadFile("python/test.txt");
		// String line = "";
		// BufferedWriter bw = IOHelper.openWriteFile("python/test.csv",
		// Config.get().memoryLimitForWritingFiles);
		// try {
		// while ((line = br.readLine()) != null) {
		// String[] values = line.split("\t#");
		// values[0] = values[0].replace("\t", " ");
		// Float f = Float.parseFloat(values[1]);
		// bw.write("\"" + values[0] + "\";\"" + f + "\"\n");
		// }
		// bw.flush();
		// bw.close();
		// br.close();
		// } catch (IOException e) {
		// // TODO Auto-generated catch block
		// e.printStackTrace();
		// }

		long start = System.currentTimeMillis();
		for (int i = 0; i < 2000000; i++) {
			BufferedReader br = IOHelper.openReadFile("python/test.txt");
			try {
				String line = "";
				int cnt = 0;
				while ((line = br.readLine()) != null) {
					break;
					// if (line.startsWith("\"fantastische")) {
					// cnt++;
					// if (cnt % 50 == 0) {
					// System.out.println(cnt);
					// }
				}
				if (i % 1000 == 0) {
					System.out.println(i + "DONE"
							+ (System.currentTimeMillis() - start) + "\n\n");
				}

				br.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		System.out.println(Config.get().aggregateNGramChunks);

		// String edgeDir =
		// "/var/lib/datasets/out/wikipedia/typoEdgesDENormalizedGer7095/";
		// String edgeIndex = "/var/lib/datasets/out/wikipedia/indexEdge7095/";
		//
		// try {
		// LuceneTypologyIndexer.run(edgeIndex, edgeDir);
		// } catch (IOException e) {
		// // TODO Auto-generated catch block
		// e.printStackTrace();
		// }

		// NGramBuilder.run("/var/lib/datasets/out/wikipedia/new7095NGRAMS/",
		// "/var/lib/datasets/out/wikipedia/trainingGer7095.file");

		// String nGramDir =
		// "/var/lib/datasets/out/wikipedia/new7095NGRAMS/nGramsNormalized/";
		// String nGramIndex =
		// "/var/lib/datasets/out/wikipedia/indexNGram7095/";
		// try {
		// LuceneNGramIndexer.run(nGramDir, nGramIndex);
		// } catch (NumberFormatException e) {
		// // TODO Auto-generated catch block
		// e.printStackTrace();
		// } catch (IOException e) {
		// // TODO Auto-generated catch block
		// e.printStackTrace();
		// }
	}
}
