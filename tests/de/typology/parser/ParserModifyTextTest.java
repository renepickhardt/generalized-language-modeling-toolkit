package de.typology.parser;
import static org.junit.Assert.*;

import org.junit.Test;
import org.junit.Assert.*;

public class ParserModifyTextTest {
	@org.junit.Test public void removeSingleCharacters(){	
		String input = "hello this is a test";
		String expectedOutput = input;
		String output = ParserModifyText.removeSingleCharacters(input);
		assertFalse(output.equals(expectedOutput));

		expectedOutput = "hello this is test";
		output = ParserModifyText.removeSingleCharacters(input);
		assertTrue(output.equals(expectedOutput));
		
		input = "this is tests does not modify the original string";
		expectedOutput = input;
		output = ParserModifyText.removeSingleCharacters(input);
		assertTrue(output.equals(expectedOutput));
		
		input = "the method should also work for other characters that letters for example the number 3 will be removed";
		expectedOutput = "the method should also work for other characters that letters for example the number will be removed";
		output = ParserModifyText.removeSingleCharacters(input);
		assertTrue(output.equals(expectedOutput));
		
		input = "1 2 3 b a d k";
		expectedOutput = "";
		output = ParserModifyText.removeSingleCharacters(input);
		assertTrue(output.equals(expectedOutput));
	}
	@org.junit.Test public void removeMultipleWhitespaces(){	
		String input;
		String expectedOutput;
		String output;
		
		input = "1 2 3 b a d k";
		expectedOutput = "1 2 3 b a d k";
		output = ParserModifyText.removeMultipleWhitespaces(input);
		assertTrue(output.equals(expectedOutput));

		
		input = "    1 2 3 b a d k";
		expectedOutput = "1 2 3 b a d k";
		output = ParserModifyText.removeMultipleWhitespaces(input);
		assertTrue(output.equals(expectedOutput));

		input = "1 2     3   b   a  d  k";
		expectedOutput = "1 2 3 b a d k";
		output = ParserModifyText.removeMultipleWhitespaces(input);
		assertTrue(output.equals(expectedOutput));
		
		input = "1 2 3\t b a d k";
		expectedOutput = "1 2 3\t b a d k";
		output = ParserModifyText.removeMultipleWhitespaces(input);
		assertTrue(output.equals(expectedOutput));

		input = "   ";
		expectedOutput = "";
		output = ParserModifyText.removeMultipleWhitespaces(input);
		assertTrue(output.equals(expectedOutput));
		
		
	}
		
	@org.junit.Test public void removeNumbers(){
		String input;
		String expectedOutput;
		String output;
		
		input = "b 1 2 3 b a d k";
		expectedOutput = "b b a d k";
		output = ParserModifyText.removeNumbers(input);
		assertTrue(output.equals(expectedOutput));
		
		input = "1 2 3 b a d k";
		expectedOutput = "b b a d k";
		output = ParserModifyText.removeNumbers(input);
		assertTrue(output.equals(expectedOutput));
	}
	
	@Test(expected= NullPointerException.class) public void ExceptionTest() { 
	    ParserModifyText.removeMultipleWhitespaces(null); 
	}

}
