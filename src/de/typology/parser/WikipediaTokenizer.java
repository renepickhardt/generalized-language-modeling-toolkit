package de.typology.parser;

import static de.typology.parser.Token.EHH;
import static de.typology.parser.Token.HH;
import static de.typology.parser.Token.OTHER;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashSet;

import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;

/**
 * @author Martin Koerner
 * 
 *         derived from
 *         http://101companies.org/index.php/101implementation:javaLexer
 * 
 */
public class WikipediaTokenizer extends Tokenizer {
	private HashSet<String> disambiguations;

	public WikipediaTokenizer(String wikiInputPath)
			throws FileNotFoundException {
		InputStream input = new FileInputStream(new File(wikiInputPath));

		// TODO: check if new Bzip2 reader really does the job.
		BufferedInputStream in = new BufferedInputStream(input);
		BZip2CompressorInputStream bzIn = null;
		try {
			bzIn = new BZip2CompressorInputStream(in);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		this.reader = new BufferedReader(new InputStreamReader(bzIn));
		// BZip2InputStream cb = new BZip2InputStream(input, false);
		// this.reader = new BufferedReader(new InputStreamReader(cb));

		// use the following line for reading xml files:
		// this.reader = new BufferedReader(new FileReader(new File(s)));
		this.disambiguations = new HashSet<String>();
		boolean languageSpecified = false;

		// this part declares language specific variables
		if (wikiInputPath.contains("dewiki")) {
			this.disambiguations.add("Begriffsklärung");
			this.disambiguations.add("begriffsklärung");
			System.out.println("this is a german wikipedia xml");
			languageSpecified = true;
		}

		if (wikiInputPath.contains("barwiki")) {
			this.disambiguations.add("Begriffsklärung");
			this.disambiguations.add("begriffsklärung");
			System.out.println("this is a bavarian wikipedia xml");
			languageSpecified = true;
		}

		if (wikiInputPath.contains("enwiki")) {
			this.disambiguations.add("Disambiguation");
			this.disambiguations.add("disambiguation");
			this.disambiguations.add("Disambig");
			this.disambiguations.add("disambig");
			this.disambiguations.add("Geodis");
			this.disambiguations.add("geodis");
			// geographical location name disambiguation pages
			this.disambiguations.add("Hndis");
			this.disambiguations.add("hndis");
			// Human name disambiguation pages
			this.disambiguations.add("Roadindex");
			this.disambiguations.add("roadindex");
			// Street name disambiguation pages
			this.disambiguations.add("Shipindex");
			this.disambiguations.add("shipindex");
			// ship name disambiguation pages
			System.out.println("this is a english wikipedia xml");
			languageSpecified = true;
		}

		if (wikiInputPath.contains("eswiki")) {
			this.disambiguations.add("Desambiguación");
			this.disambiguations.add("desambiguación");
			this.disambiguations.add("Homonimia");
			this.disambiguations.add("homonimia");
			this.disambiguations.add("Idénticos");
			this.disambiguations.add("idénticos");
			System.out.println("this is a spanish wikipedia xml");
			languageSpecified = true;
		}

		if (wikiInputPath.contains("frwiki")) {
			this.disambiguations.add("Homonymie");
			this.disambiguations.add("homonymie");
			this.disambiguations.add("Patronymie");
			this.disambiguations.add("patronymie");
			this.disambiguations.add("Homonymes");
			this.disambiguations.add("homonymes");
			this.disambiguations.add("Toponymie");
			this.disambiguations.add("toponymie");
			this.disambiguations.add("Abréviation");
			this.disambiguations.add("abréviation");
			System.out.println("this is a french wikipedia xml");
			languageSpecified = true;
		}

		if (wikiInputPath.contains("itwiki")) {
			this.disambiguations.add("Disambigua");
			this.disambiguations.add("disambigua");
			this.disambiguations.add("Omonime");
			this.disambiguations.add("omonime");
			this.disambiguations.add("Mercurio");
			this.disambiguations.add("mercurio");
			this.disambiguations.add("Interprogetto");
			this.disambiguations.add("interprogetto");
			System.out.println("this is a italian wikipedia xml");
			languageSpecified = true;
		}
		if (languageSpecified == false) {
			System.out
					.println("please check naming or declare language specific variables (see WikipediaTokenizer.java)");
		}

	}

	public HashSet<String> getdisambiguations() {
		return this.disambiguations;
	}

	@Override
	public void lex() {
		super.lex();
		// Recognize -- as HH
		if (this.token == Token.HYPHEN && this.lookahead == '-') {
			this.read();
			this.token = HH;
			return;
		}
		// Recognize !-->
		if (this.token == Token.EXCLAMATIONMARK && this.lookahead == '-') {
			this.read();
			if (this.lookahead == '-') {
				this.read();
				this.token = EHH;
				return;
			}
			this.token = OTHER;
			return;

		}
		super.lexGeneral();
	}
}
