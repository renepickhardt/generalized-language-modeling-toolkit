package de.typology.utils;

import java.io.Reader;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.WhitespaceTokenizer;
import org.apache.lucene.util.Version;

public class TypologyAnalyzer extends Analyzer {
	private Version matchVersion;

	public TypologyAnalyzer(Version matchVersion) {
		this.matchVersion = matchVersion;
	}

	@Override
	protected TokenStreamComponents createComponents(String arg0, Reader arg1) {

		final Tokenizer source = new WhitespaceTokenizer(this.matchVersion,
				arg1);

		return new TokenStreamComponents(source, source);
	}

}