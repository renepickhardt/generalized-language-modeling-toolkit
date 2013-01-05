package de.typology.trainers;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashSet;

import de.typology.utils.Config;
import de.typology.utils.IOHelper;

public class TreeServerIndexer {

	public static HashSet<String> treeMap;


	/**
	 * @param args
	 * @throws IOException
	 */
	public static void main(String[] args) throws IOException {
		TreeServerIndexer tti = new TreeServerIndexer();
		tti.run(Config.get().normalizedEdges);
		int mb = 1024 * 1024;
		Runtime runtime = Runtime.getRuntime();
		System.out.println("Used Memory:"
				+ (runtime.totalMemory() - runtime.freeMemory()) / mb);
	}

	public void run(String normalizedTypologyEdgesPath) throws IOException {
		treeMap=new HashSet<String>();
		long startTime = System.currentTimeMillis();

		Writer writer = new OutputStreamWriter(new FileOutputStream(
				"startServer.sh"));
		for (int edgeType = 1; edgeType < 6; edgeType++) {
			ArrayList<File> files = IOHelper.getDirectory(new File(normalizedTypologyEdgesPath + edgeType + "/"));
			for (File file : files) {
				System.out.println(file.getAbsolutePath());
				if (file.getName().contains("distribution")) {
					IOHelper.log("skipping " + file.getAbsolutePath());
					continue;
				}
				writer.write("java -jar TreeServer.jar "+file.getAbsolutePath()+" &\n");
				treeMap.add(file.getName());
			}
			writer.flush();
		}
		writer.close();
		//SystemHelper.runUnixCommand("sh startServer.sh");
		long endTime = System.currentTimeMillis();
		IOHelper.strongLog((endTime - startTime) / 1000
				+ " seconds for indexing " + normalizedTypologyEdgesPath);
		return;
	}
}