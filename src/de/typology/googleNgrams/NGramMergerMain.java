package de.typology.googleNgrams;

import java.io.IOException;

import de.typology.utils.Config;
import de.typology.utils.SystemHelper;

public class NGramMergerMain {

	/**
	 * @param args
	 * @throws IOException
	 * 
	 * @author Martin Koerner
	 */
	public static void main(String[] args) throws IOException {
		System.out.println("delete old output file");
		SystemHelper
				.runUnixCommand("rm " + Config.get().googleNgramsMergedPath);
		System.out.println("start merging");
		NGramMerger merger = new NGramMerger();
		merger.merge(Config.get().googleNgramsPath,
				Config.get().googleNgramsMergedPath);

	}

}
