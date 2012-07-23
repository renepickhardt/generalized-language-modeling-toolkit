package de.typology.parser;

/*
 * Parser for Wikipedia dumps
 * 
 * @version 1.1
 * @author Till Speicher
 * @author Rene Pickhardt
 */

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;

import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.parser.ParserDelegator;

import net.java.textilej.parser.MarkupParser;
import net.java.textilej.parser.builder.HtmlDocumentBuilder;
import net.java.textilej.parser.markup.mediawiki.MediaWikiDialect;
import de.typology.utils.Config;
import edu.jhu.nlp.wikipedia.PageCallbackHandler;
import edu.jhu.nlp.wikipedia.WikiPage;
import edu.jhu.nlp.wikipedia.WikiXMLParser;
import edu.jhu.nlp.wikipedia.WikiXMLParserFactory;

public class WikipediaParser {
	// constants
	public static int cnt = 0;
	public static FileWriter out;

	public static void main(String[] args) {
		WikiXMLParser wxsp = WikiXMLParserFactory
				.getSAXParser(Config.get().wikiXmlPath);
		File file = new File(Config.get().parsedWikiOutputPath);
		try {
			out = new FileWriter(file, true);
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		try {
			wxsp.setPageCallback(new PageCallbackHandler() {
				@Override
				public void process(WikiPage page) {
					if (page.isSpecialPage()) {
						return;
					}
					if (page.getWikiText().startsWith("#REDIRECT ")) {
						// System.out.println("redirect...");
						return;
					}
					if (page.getWikiText().startsWith("#WEITERLEITUNG")) {
						return;
					}

					String out = getCleanText(page);
					output(out + "\n");
					if (cnt % 1000 == 0) {
						System.out.println("Processing: " + page.getTitle()
								+ " - " + cnt);
					}
					cnt++;
				}
			});
			wxsp.parse();
		} catch (Exception e) {
			e.printStackTrace();
		}
		try {
			out.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	/**
	 * categories and translations at the very end of the text have to be thrown
	 * away
	 * 
	 * @param page
	 * @return
	 */
	protected static String getCleanText(WikiPage page) {
		String markup = page.getWikiText();
		StringWriter writer = new StringWriter();

		HtmlDocumentBuilder builder = new HtmlDocumentBuilder(writer);
		builder.setEmitAsDocument(false);

		MarkupParser parser = new MarkupParser(new MediaWikiDialect());
		parser.setBuilder(builder);
		parser.parse(markup);

		final String html = writer.toString();

		final StringBuilder cleaned = new StringBuilder();

		HTMLEditorKit.ParserCallback callback = new HTMLEditorKit.ParserCallback() {
			@Override
			public void handleText(char[] data, int pos) {
				cleaned.append(new String(data)).append(' ');
			}
		};
		try {
			new ParserDelegator()
					.parse(new StringReader(html), callback, false);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// TODO: more hast to be done... this is very dirty
		String result = cleaned.toString();
		result = ParserModifyText.removeChildNodes(result, "<ref", "</ref>");
		result = ParserModifyText.removeChildNodes(result, "<math>", "</math>");
		result = ParserModifyText.removeChildNodes(result, "{{", "}}");
		result = ParserModifyText.removeChildNodes(result, "{|", "|}");
		result = result.split("Kategorie:")[0];
		return result;
	}

	public static void output(String result) {
		try {
			out.write(result);
			out.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
