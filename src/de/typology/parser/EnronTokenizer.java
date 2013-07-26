package de.typology.parser;

import static de.typology.parser.Token.BCC;
import static de.typology.parser.Token.CC;
import static de.typology.parser.Token.CONTENTTRANSFERENCODING;
import static de.typology.parser.Token.CONTENTTYPE;
import static de.typology.parser.Token.DATE;
import static de.typology.parser.Token.FROM;
import static de.typology.parser.Token.HEADER;
import static de.typology.parser.Token.MESSAGEID;
import static de.typology.parser.Token.MIMEVERSION;
import static de.typology.parser.Token.RE;
import static de.typology.parser.Token.STRING;
import static de.typology.parser.Token.SUBJECT;
import static de.typology.parser.Token.TO;
import static de.typology.parser.Token.XBCC;
import static de.typology.parser.Token.XCC;
import static de.typology.parser.Token.XFILENAME;
import static de.typology.parser.Token.XFOLDER;
import static de.typology.parser.Token.XFROM;
import static de.typology.parser.Token.XORIGIN;
import static de.typology.parser.Token.XTO;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import de.typology.utils.IOHelper;

/**
 * @author Martin Koerner
 * 
 *         derived from
 *         http://101companies.org/index.php/101implementation:javaLexer
 * 
 */
public class EnronTokenizer extends Tokenizer {

	// Keywords to token mapping
	private static Map<String, Token> keywords;

	public EnronTokenizer(File f) {
		this.reader = IOHelper.openReadFile(f.getAbsolutePath());

		keywords = new HashMap<String, Token>();

		keywords.put("Message-ID", MESSAGEID);
		keywords.put("Date", DATE);
		keywords.put("Re", RE);
		keywords.put("From", FROM);
		keywords.put("To", TO);
		keywords.put("cc", CC);
		keywords.put("bcc", BCC);
		keywords.put("Cc", CC);
		keywords.put("Bcc", BCC);
		keywords.put("Subject", SUBJECT);
		keywords.put("Mime-Version", MIMEVERSION);
		keywords.put("Content-Type", CONTENTTYPE);
		keywords.put("Content-Transfer-Encoding", CONTENTTRANSFERENCODING);
		keywords.put("X-From", XFROM);
		keywords.put("X-To", XTO);
		keywords.put("X-cc", XCC);
		keywords.put("X-bcc", XBCC);
		keywords.put("X-Folder", XFOLDER);
		keywords.put("X-Origin", XORIGIN);
		keywords.put("X-FileName", XFILENAME);
	}

	// Recognize a token
	@Override
	public boolean lex() {
		if (super.lex()) {
			return true;
		}
		// Recognize String
		if (Character.isLetterOrDigit(this.lookahead)) {
			do {
				this.read();
			} while (!Character.isWhitespace(this.lookahead)
					&& Character.isLetterOrDigit(this.lookahead)
					|| this.lookahead == '-');
			// - and : to recognize header
			if (this.lookahead == ':') {
				if (keywords.containsKey(this.getLexeme())) {
					this.token = HEADER;
					return true;
				}
			}
			this.token = STRING;
			return true;
		}
		super.lexGeneral();
		return true;
	}
}
