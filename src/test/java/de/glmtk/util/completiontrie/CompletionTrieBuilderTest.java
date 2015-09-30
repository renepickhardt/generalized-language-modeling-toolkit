package de.glmtk.util.completiontrie;

import static org.junit.Assert.fail;

import org.junit.Test;


public class CompletionTrieBuilderTest {
    @Test
    public void testDuplicateKeySameScore() {
        CompletionTrieBuilder builder = new CompletionTrieBuilder(true);
        builder.add("foo", 1);
        builder.add("foo", 1);
    }

    @Test
    public void testDuplicateKeyOtherScore() {
        CompletionTrieBuilder builder = new CompletionTrieBuilder(true);
        builder.add("foo", 1);
        try {
            builder.add("foo", 2);
            fail("Expeced IllegalStateException.");
        } catch (IllegalStateException e) {}
    }
}
