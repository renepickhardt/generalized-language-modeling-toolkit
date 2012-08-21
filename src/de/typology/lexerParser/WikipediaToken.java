package de.typology.lexerParser;

public enum WikipediaToken {
	OBJECT, STRING, OTHER, WS, LINESEPERATOR, URI, EOL, EOF, FULLSTOP, COMMA, EM, QM,
	//
	INFOBOX,
	//
	LINK, LABELEDLINK,
	//
	PAGE, CLOSEDPAGE,
	//
	TITLE, CLOSEDTITLE,
	//
	TEXT, CLOSEDTEXT
}
