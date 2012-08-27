package de.typology.lexerParser;

import static de.typology.lexerParser.WikipediaToken.BR;
import static de.typology.lexerParser.WikipediaToken.CLOSEDELEMENT;
import static de.typology.lexerParser.WikipediaToken.CLOSEDPAGE;
import static de.typology.lexerParser.WikipediaToken.CLOSEDREF;
import static de.typology.lexerParser.WikipediaToken.CLOSEDTEXT;
import static de.typology.lexerParser.WikipediaToken.CLOSEDTITLE;
import static de.typology.lexerParser.WikipediaToken.EHH;
import static de.typology.lexerParser.WikipediaToken.ELEMENT;
import static de.typology.lexerParser.WikipediaToken.GREATERTHAN;
import static de.typology.lexerParser.WikipediaToken.LESSTHAN;
import static de.typology.lexerParser.WikipediaToken.OTHER;
import static de.typology.lexerParser.WikipediaToken.PAGE;
import static de.typology.lexerParser.WikipediaToken.REF;
import static de.typology.lexerParser.WikipediaToken.SLASH;
import static de.typology.lexerParser.WikipediaToken.STRING;
import static de.typology.lexerParser.WikipediaToken.TEXT;
import static de.typology.lexerParser.WikipediaToken.TITLE;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class WikipediaRecognizer implements Iterator<WikipediaToken> {

	private WikipediaTokenizer tokenizer;
	private String lexeme;
	private WikipediaToken current;
	private WikipediaToken previous;
	private WikipediaToken label;

	// Keywords to token mapping
	private static Map<String, WikipediaToken> keywords;

	static {
		keywords = new HashMap<String, WikipediaToken>();
		keywords.put("!--", EHH);
		keywords.put("page", PAGE);
		keywords.put("title", TITLE);
		keywords.put("text", TEXT);
		keywords.put("ref", REF);
		keywords.put("br", BR);
		keywords.put("/page", CLOSEDPAGE);
		keywords.put("/title", CLOSEDTITLE);
		keywords.put("/text", CLOSEDTEXT);
		keywords.put("/ref", CLOSEDREF);
	}

	public WikipediaRecognizer(WikipediaTokenizer tokenizer) {
		this.tokenizer = tokenizer;
	}

	public String getLexeme() {
		return this.lexeme;
	}

	public WikipediaTokenizer getTokenizer() {
		return this.tokenizer;
	}

	public void reset() {
		this.lexeme = "";
		this.current = null;
		this.previous = null;
		this.label = null;
	}

	public void read() {
		if (this.tokenizer.hasNext()) {
			this.lexeme += this.tokenizer.getLexeme();
			this.previous = this.current;
			this.current = this.tokenizer.next();

		} else {
			throw new IllegalStateException();
		}
	}

	@Override
	public boolean hasNext() {
		return this.tokenizer.hasNext();
	}

	@Override
	public WikipediaToken next() {
		this.reset();
		this.read();
		// Recognize <...>
		if (this.current == LESSTHAN) {
			this.read();
			if (this.current == EHH) {
				return EHH;
			}

			if (this.current == STRING) {
				this.label = ELEMENT;
				if (keywords.containsKey(this.lexeme.substring(1))) {
					this.label = keywords.get(this.lexeme.substring(1));
				}
				if (this.lexeme.startsWith("ref")) {
					this.label = REF;
				}
			}
			if (this.current == SLASH) {
				this.label = CLOSEDELEMENT;
				this.read();
				if (keywords.containsKey(this.lexeme.substring(1))) {
					this.label = keywords.get(this.lexeme.substring(1));
				}
			}
			while (this.current != GREATERTHAN) {
				this.read();
			}
			if (this.current == GREATERTHAN) {
				if (this.previous == SLASH) {
					return OTHER;
				}
				return this.label;
			}

		}

		return this.current;
	}

	@Override
	public void remove() {
		throw new UnsupportedOperationException();
	}

}
