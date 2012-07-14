package de.typology.parser;

/*
 * Parser for Wikipedia dumps
 * 
 * @version 1.1
 * @author Till Speicher
 * @author Rene Pickhardt
 */

import de.typology.utils.Config;
import edu.jhu.nlp.wikipedia.PageCallbackHandler;
import edu.jhu.nlp.wikipedia.WikiPage;
import edu.jhu.nlp.wikipedia.WikiXMLParser;
import edu.jhu.nlp.wikipedia.WikiXMLParserFactory;

public class WikipediaParser {
    //constants
    public static int cnt = 0;
    public static void main(String[] args) {
     	WikiXMLParser wxsp = WikiXMLParserFactory.getSAXParser(Config.get().wikiXmlPath);
        try {
            wxsp.setPageCallback(new PageCallbackHandler() {
                public void process(WikiPage page) {
                    if(page.isSpecialPage()){
                        return;
                    }
                    cnt++;
                    if (cnt%100==0)
                    	System.out.println("Processing: " + page.getTitle() + " - " + cnt);
                }
            });
            wxsp.parse();
        } catch (Exception e) {
            e.printStackTrace();
        }	
    }
}