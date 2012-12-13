package de.typology.nGramBuilder;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

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

			String fullQualifiedFileName = sourcePath + File.separator
					+ fileName;

			IOHelper.log("start sorting " + fullQualifiedFileName);
			String aggregatedFileExtension = fileExtension.replace("a", "s");

			String outFileName = fullQualifiedFileName.replaceFirst(
					fileExtension, aggregatedFileExtension);

			String sortCommand = "sort -r -t\'#\' -k 2 -n -S3G \""
					+ fullQualifiedFileName + "\" --output=\"" + outFileName
					+ "\"";
			Runtime rt = Runtime.getRuntime();
			Process p = null;
			try {
				p = rt.exec(new String[] { "bash", "-c", sortCommand });
				System.out.println(sortCommand);
			} catch (IOException ioe) {
				IOHelper.log("Error executing: " + sortCommand);
			}
			InputStream output = p.getInputStream();

			BufferedReader br = new BufferedReader(
					new InputStreamReader(output));
			String line = "";
			try {
				while ((line = br.readLine()) != null) {
					System.out.println(line);
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			// SystemHelper.runUnixCommand(sortCommand);
			// TODO: comment out to delete aggregated files after sorting
			f.delete();
		}
	}
}