package de.typology.tester;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import de.typology.patterns.PatternBuilder;
import de.typology.patterns.PatternTransformer;

/**
 * This class extracts all sequences that are needed for computing the
 * Kneser-Ney smoothed values for a set of given test sequences.
 * 
 * @author Martin Koerner
 * 
 */
public class TestSequenceExtractor {
	private File testSequenceFile;
	private File absoluteDirectory;
	private File _absoluteDirectory;
	private File _absolute_Directory;
	private File absolute_Directory;
	private File outputDirectory;

	private String delimiter;

	public TestSequenceExtractor(File testSequenceFile, File absoluteDirectory,
			File _absoluteDirectory, File _absolute_Directory,
			File absolute_Directory, File outputDirectory, String delimiter) {
		this.testSequenceFile = testSequenceFile;
		this.absoluteDirectory = absoluteDirectory;
		this._absoluteDirectory = _absoluteDirectory;
		this._absolute_Directory = _absolute_Directory;
		this.absolute_Directory = absolute_Directory;
		this.outputDirectory = outputDirectory;
		this.delimiter = delimiter;

	}

	public void extractSequences(int maxModelLength, int cores) {

		// read test sequences into HashSet
		ArrayList<String> sequences = new ArrayList<String>();
		try {
			BufferedReader testSequenceReader = new BufferedReader(
					new FileReader(this.testSequenceFile));
			String line;
			while ((line = testSequenceReader.readLine()) != null) {
				sequences.add(line);
			}
			testSequenceReader.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		ArrayList<boolean[]> absolutePatterns = PatternBuilder
				.getGLMForSmoothingPatterns(maxModelLength);

		// call SequenceExtractorTasks

		// initialize executerService
		// int cores = Runtime.getRuntime().availableProcessors();
		ExecutorService executorService = Executors.newFixedThreadPool(cores);
		for (boolean[] absolutePattern : absolutePatterns) {
			// extract absolute sequences
			if (absolutePattern.length <= maxModelLength) {
				String absoluteStringPattern = PatternTransformer
						.getStringPattern(absolutePattern);
				File absoluteInputDirectory = new File(
						this.absoluteDirectory.getAbsolutePath() + "/"
								+ absoluteStringPattern);
				File absoluteOutputDirectory = new File(this.outputDirectory
						+ "/" + this.absoluteDirectory.getName() + "/"
						+ absoluteStringPattern);
				SequenceExtractorTask absoluteSET = new SequenceExtractorTask(
						sequences, absolutePattern, absolutePattern,
						absoluteInputDirectory, absoluteOutputDirectory,
						this.delimiter);
				executorService.execute(absoluteSET);
			}

			// extract _absolute sequences
			if (absolutePattern.length > 0) {
				boolean[] _absolutePattern = Arrays.copyOfRange(
						absolutePattern, 1, absolutePattern.length);
				String _absoluteStringPattern = "_"
						+ PatternTransformer.getStringPattern(_absolutePattern);
				File _absoluteInputDirectory = new File(
						this._absoluteDirectory.getAbsolutePath() + "/"
								+ _absoluteStringPattern);
				File _absoluteOutputDirectory = new File(this.outputDirectory
						+ "/" + this._absoluteDirectory.getName() + "/"
						+ _absoluteStringPattern);
				SequenceExtractorTask _absoluteSET = new SequenceExtractorTask(
						sequences, absolutePattern, _absolutePattern,
						_absoluteInputDirectory, _absoluteOutputDirectory,
						this.delimiter);
				executorService.execute(_absoluteSET);
			}

			if (absolutePattern.length > 1
					&& absolutePattern[absolutePattern.length - 1]) {
				// extract _absolute_ sequences
				boolean[] _absolute_Pattern = Arrays.copyOfRange(
						absolutePattern, 1, absolutePattern.length - 1);
				String _absolute_StringPattern = "_"
						+ PatternTransformer
								.getStringPattern(_absolute_Pattern) + "_";
				File _absolute_InputDirectory = new File(
						this._absolute_Directory.getAbsolutePath() + "/"
								+ _absolute_StringPattern);
				File _absolute_OutputDirectory = new File(this.outputDirectory
						+ "/" + this._absolute_Directory.getName() + "/"
						+ _absolute_StringPattern);
				SequenceExtractorTask _absolute_SET = new SequenceExtractorTask(
						sequences, absolutePattern, _absolute_Pattern,
						_absolute_InputDirectory, _absolute_OutputDirectory,
						this.delimiter);
				executorService.execute(_absolute_SET);

				// extract absolute_ sequences
				boolean[] absolute_Pattern = Arrays.copyOfRange(
						absolutePattern, 0, absolutePattern.length - 1);
				String absolute_StringPattern = PatternTransformer
						.getStringPattern(absolute_Pattern) + "_";
				File absolute_InputDirectory = new File(
						this.absolute_Directory.getAbsolutePath() + "/"
								+ absolute_StringPattern);
				File absolute_OutputDirectory = new File(this.outputDirectory
						+ "/" + this.absolute_Directory.getName() + "/"
						+ absolute_StringPattern);
				SequenceExtractorTask absolute_SET = new SequenceExtractorTask(
						sequences, absolutePattern, absolute_Pattern,
						absolute_InputDirectory, absolute_OutputDirectory,
						this.delimiter);
				executorService.execute(absolute_SET);

			}
		}

		executorService.shutdown();
		try {
			executorService.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
