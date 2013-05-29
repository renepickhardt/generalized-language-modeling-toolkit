package de.typology.parser;

import static de.typology.parser.Token.AND;
import static de.typology.parser.Token.ASTERISK;
import static de.typology.parser.Token.AT;
import static de.typology.parser.Token.CLOSEDCURLYBRACKET;
import static de.typology.parser.Token.CLOSEDROUNDBRACKET;
import static de.typology.parser.Token.CLOSEDSQUAREDBRACKET;
import static de.typology.parser.Token.COLON;
import static de.typology.parser.Token.COMMA;
import static de.typology.parser.Token.CURLYBRACKET;
import static de.typology.parser.Token.DASH;
import static de.typology.parser.Token.EOF;
import static de.typology.parser.Token.EQUALITYSIGN;
import static de.typology.parser.Token.EXCLAMATIONMARK;
import static de.typology.parser.Token.FULLSTOP;
import static de.typology.parser.Token.GREATERTHAN;
import static de.typology.parser.Token.HYPHEN;
import static de.typology.parser.Token.LESSTHAN;
import static de.typology.parser.Token.LINESEPARATOR;
import static de.typology.parser.Token.OTHER;
import static de.typology.parser.Token.QUESTIONMARK;
import static de.typology.parser.Token.QUOTATIONMARK;
import static de.typology.parser.Token.ROUNDBRACKET;
import static de.typology.parser.Token.SEMICOLON;
import static de.typology.parser.Token.SLASH;
import static de.typology.parser.Token.SQUAREDBRACKET;
import static de.typology.parser.Token.STRING;
import static de.typology.parser.Token.UNDERSCORE;
import static de.typology.parser.Token.VERTICALBAR;
import static de.typology.parser.Token.WS;

import java.io.IOException;
import java.io.Reader;
import java.util.Iterator;

public class Tokenizer implements Iterator<Token> {
	protected Reader reader = null; // input stream

	protected Token token = null; // last token recognized
	private boolean eof = false; // reached end of file
	protected int lookahead = 0; // lookahead, if any
	protected int[] buffer = new int[10000]; // lexeme buffer
	protected int index = 0; // length of lexeme

	// Extract lexeme from buffer
	public String getLexeme() {
		return new String(this.buffer, 0, this.index);
	}

	// Reset state to begin new token
	private void reset() {
		if (this.eof) {
			throw new IllegalStateException();
		}
		this.index = 0;
		this.token = null;
		if (this.lookahead == 0) {
			this.read();
		}
	}

	// Read one more char.
	// Add previous char, if any, to the buffer.
	//
	protected void read() {
		if (this.eof) {
			throw new IllegalStateException();
		}
		if (this.lookahead != 0) {
			this.buffer[this.index] = this.lookahead;
			this.index++;
			if (this.index == 10000) {
				this.index = 0;
				System.out.println("String longer than 100000 characters");
				// reset buffer if token gets too big (very unlikely to happen)
			}
		}
		try {
			this.lookahead = this.reader.read();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// Recognize a token
	protected boolean lex() {
		this.reset();
		// Recognize end of file
		if (this.lookahead == -1) {
			this.eof = true;
			this.token = EOF;
			return true;
		}

		// Recognize newline
		if (this.lookahead == 13) {
			this.token = LINESEPARATOR;
			this.read();
			if (this.lookahead == 10) {
				this.token = LINESEPARATOR;
				this.read();
			}
			return true;
		}
		// Recognize newline
		if (this.lookahead == 10) {
			this.token = LINESEPARATOR;
			this.read();
			return true;
		}

		// Recognize newline
		if (this.lookahead == '\r') {
			this.token = LINESEPARATOR;
			this.read();
			if (this.lookahead == '\n') {
				this.token = LINESEPARATOR;
				this.read();
				return true;
			}
			return true;
		}
		// Recognize newline
		if (this.lookahead == '\n') {
			this.token = LINESEPARATOR;
			this.read();
			return true;
		}
		// Recognize whitespace
		if (Character.isWhitespace(this.lookahead)) {
			this.read();
			this.token = WS;
			return true;
		}

		// Recognize fullstop
		if (this.lookahead == '.') {
			this.read();
			this.token = FULLSTOP;
			return true;
		}

		// Recognize comma
		if (this.lookahead == ',') {
			this.read();
			this.token = COMMA;
			return true;
		}
		// Recognize semicolon
		if (this.lookahead == ';') {
			this.read();
			this.token = SEMICOLON;
			return true;
		}
		// Recognize colon
		if (this.lookahead == ':') {
			this.read();
			this.token = COLON;
			return true;
		}

		// recognize quotation mark
		if (this.lookahead == 39) {// 39='
			this.read();
			this.token = QUOTATIONMARK;
			return true;
		}

		// Recognize underscore
		if (this.lookahead == '_') {
			this.read();
			this.token = UNDERSCORE;
			return true;
		}
		// Recognize hyphen
		if (this.lookahead == '-') {
			this.read();
			this.token = HYPHEN;
			return true;
		}
		// Recognize dash
		if (this.lookahead == '-') {
			this.read();
			this.token = DASH;
			return true;
		}

		// Recognize exclamation mark
		if (this.lookahead == '!') {
			this.read();
			this.token = EXCLAMATIONMARK;
			return true;
		}

		// Recognize question mark
		if (this.lookahead == '?') {
			this.read();
			this.token = QUESTIONMARK;
			return true;
		}

		// Recognize vertical bar
		if (this.lookahead == '|') {
			this.read();
			this.token = VERTICALBAR;
			return true;
		}

		// Recognize slash
		if (this.lookahead == '/') {
			this.read();
			this.token = SLASH;
			return true;
		}
		// Recognize asterisk
		if (this.lookahead == '*') {
			this.read();
			this.token = ASTERISK;
			return true;
		}
		// Recognize equality sign
		if (this.lookahead == '=') {
			this.read();
			this.token = EQUALITYSIGN;
			return true;
		}

		// recognize @
		if (this.lookahead == '@') {
			this.read();
			this.token = AT;
			return true;
		}

		if (this.lookahead == '&') {
			this.read();
			// remove &amp;
			if (this.lookahead == 'a') {
				this.read();
				if (this.lookahead == 'm') {
					this.read();
					if (this.lookahead == 'p') {
						this.read();
						if (this.lookahead == ';') {
							this.read();
							this.token = AND;
							return true;
						}
					}
				}
			}
			if (this.lookahead == 'q') {
				this.read();
				if (this.lookahead == 'u') {
					this.read();
					if (this.lookahead == 'o') {
						this.read();
						if (this.lookahead == 't') {
							this.read();
							if (this.lookahead == ';') {
								this.read();
								this.token = QUOTATIONMARK;
								return true;
							}
						}
					}
				}
			}
			// Recognize and chance &lt; to <
			if (this.lookahead == 'l') {
				this.read();
				if (this.lookahead == 't') {
					this.read();
					if (this.lookahead == ';') {
						this.read();
						this.reset();
						this.buffer[0] = '<';
						this.index++;
						this.token = LESSTHAN;
						return true;
					}
				}
				this.token = OTHER;
				return true;
			}
			// Recognize and chance &gt; to >
			if (this.lookahead == 'g') {
				this.read();
				if (this.lookahead == 't') {
					this.read();
					if (this.lookahead == ';') {
						this.read();
						this.reset();
						this.buffer[0] = '>';
						this.index++;
						this.token = GREATERTHAN;
						return true;
					}
				}
				this.token = OTHER;
				return true;
			}

			// // Recognize &nbsp; = whitespace
			if (this.lookahead == 'n') {
				this.read();
				if (this.lookahead == 'b') {
					this.read();
					if (this.lookahead == 's') {
						this.read();
						if (this.lookahead == 'p') {
							this.read();
							if (this.lookahead == ';') {
								this.token = WS;
								this.read();
								return true;
							}
						}
					}
				}
				this.token = OTHER;
				return true;
			}
			// Recognize &quot; = " (as other)
			if (this.lookahead == 'q') {
				this.read();
				if (this.lookahead == 'u') {
					this.read();
					if (this.lookahead == 'o') {
						this.read();
						if (this.lookahead == 't') {
							this.read();
							if (this.lookahead == ';') {
								this.token = OTHER;
								this.read();
								return true;
							}
						}
					}
				}
				this.token = OTHER;
				return true;
			}

			this.token = OTHER;
			return true;
		}

		// Recognize squared bracket open
		if (this.lookahead == '[') {
			this.read();
			this.token = SQUAREDBRACKET;
			return true;
		}

		// Recognize squared bracket close
		if (this.lookahead == ']') {
			this.read();
			this.token = CLOSEDSQUAREDBRACKET;
			return true;
		}

		// Recognize round bracket open
		if (this.lookahead == '(') {
			this.read();
			this.token = ROUNDBRACKET;
			return true;
		}

		// Recognize round bracket close
		if (this.lookahead == ')') {
			this.read();
			this.token = CLOSEDROUNDBRACKET;
			return true;
		}
		// Recognize curly bracket open
		if (this.lookahead == '{') {
			this.read();
			this.token = CURLYBRACKET;
			return true;
		}

		// Recognize curly bracket close
		if (this.lookahead == '}') {
			this.read();
			this.token = CLOSEDCURLYBRACKET;
			return true;
		}
		// Recognize lower than sign
		if (this.lookahead == '<') {
			this.read();
			this.token = LESSTHAN;
			return true;
		}

		// Recognize curly bracket close
		if (this.lookahead == '>') {
			this.read();
			this.token = GREATERTHAN;
			return true;
		}
		return false;
	}

	protected void lexGeneral() {
		if (this.token == null) {
			// Recognize String
			if (Character.isLetterOrDigit(this.lookahead)) {
				do {
					this.read();
				} while (!Character.isWhitespace(this.lookahead)
						&& Character.isLetterOrDigit(this.lookahead));

				this.token = STRING;
				return;
			}

			// Recognize other
			if (!Character.isLetterOrDigit(this.lookahead)) {
				this.read();
				this.token = OTHER;
				return;
			}
		}
	}

	@Override
	public boolean hasNext() {
		if (this.token != null) {
			return true;
		}
		if (this.eof) {
			return false;
		} else {
			return true;
		}
	}

	@Override
	public Token next() {
		if (this.hasNext()) {
			Token result = this.token;
			this.token = null;
			return result;
		} else {
			throw new IllegalStateException();
		}
	}

	public void close() {
		try {
			this.reader.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public void remove() {
		throw new UnsupportedOperationException();
	}

	// Stress test: lex until end-of-file
	public void lexall() {
		while (this.hasNext()) {
			Token t = this.next();
			System.out.println(t + " : " + this.getLexeme());
		}
	}

}
