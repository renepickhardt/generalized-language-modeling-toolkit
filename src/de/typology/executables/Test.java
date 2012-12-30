package de.typology.executables;

import de.typology.utils.Config;

public class Test {

	/**
	 * @param args
	 */
	public static void main(String[] args) {

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
