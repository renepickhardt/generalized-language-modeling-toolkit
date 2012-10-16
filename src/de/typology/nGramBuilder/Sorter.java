package de.typology.nGramBuilder;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.util.HashSet;
import java.util.TreeMap;

import de.typology.utils.IOHelper;

public class Sorter {

	public void sort(String sourcePath, String fileExtension) {
		File[] files = IOHelper.getAllFilesInDirWithExtension(sourcePath,
				fileExtension, this.getClass().getName() + "aggregateNGrams");

		for (File f : files) {
			String fileName = f.getName();
			if (!fileName.endsWith(fileExtension)) {
				IOHelper.log(fileName
						+ " is not an aggregated ngram file. process next");
				continue;
			}
			System.out.println(sourcePath + File.separator + fileName);

			String fullQualifiedFileName = sourcePath + File.separator
					+ fileName;

			BufferedReader br = IOHelper.openReadFile(fullQualifiedFileName,
					32 * 1024 * 1024);

			TreeMap<Integer, HashSet<String>> data = new TreeMap<Integer, HashSet<String>>();
			String line = "";
			int lCnt = 0;
			try {
				while ((line = br.readLine()) != null) {
					String[] values = line.split("\t#");
					if (values.length != 2) {
						IOHelper.log("bad format for: " + line + " in file: "
								+ fullQualifiedFileName);
						continue;
					}
					String strData = values[0];
					Integer count = Integer.parseInt(values[1]);

					HashSet<String> tmp = data.get(count);

					if (tmp == null) {
						tmp = new HashSet<String>();
					}

					tmp.add(strData);

					data.put(count, tmp);

					if (lCnt++ % 5000000 == 0) {
						IOHelper.log(fullQualifiedFileName + "\t" + lCnt
								+ " nGrams processed for sorting");
					}
				}
				IOHelper.log("sorting done for: " + fullQualifiedFileName
						+ "\nstart writing to file");

				String aggregatedFileExtension = fileExtension
						.replace("a", "s");

				String outFileName = fullQualifiedFileName.replaceFirst(
						fileExtension, aggregatedFileExtension);
				BufferedWriter bw = IOHelper.openWriteFile(outFileName,
						32 * 1024 * 1024);
				int nCnt = 0;

				for (Integer key : data.descendingKeySet()) {
					HashSet<String> tmp = data.get(key);
					for (String s : tmp) {
						bw.write(s + "\t#" + key + "\n");
						nCnt++;
						if (nCnt % 1000000 == 0) {
							bw.flush();
							IOHelper.log(fullQualifiedFileName + "\t" + nCnt
									+ " written to file");
						}
					}
				}
				bw.flush();
				bw.close();
				br.close();
				// TODO: comment in the following line to DELETE THE
				// UNAGGREGATED FILE AND SAVE DISKSPACE
				f.delete();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}