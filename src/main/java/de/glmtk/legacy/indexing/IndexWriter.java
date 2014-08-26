package de.glmtk.legacy.indexing;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import de.glmtk.pattern.Pattern;
import de.glmtk.pattern.PatternElem;

public class IndexWriter implements AutoCloseable {

    private Index index;

    private Pattern pattern;

    private Path outputDir;

    private int bufferSizes;

    private PatternElem type;

    private List<BufferedWriter> writers;

    /* package */IndexWriter(
            Index index,
            Pattern pattern,
            Path outputDir,
            int bufferSizes) throws IOException {
        this.index = index;
        this.pattern = pattern;
        this.outputDir = outputDir;
        this.bufferSizes = bufferSizes;

        writers = new ArrayList<BufferedWriter>();
        type = pattern.getFirstNonSkp();
        switch (type) {
            case CNT:
                openNWriters(index.getWordIndex().size());
                break;

            case POS:
                openNWriters(index.getPosIndex().size());
                break;

            default:
                openNWriters(1);
        }
    }

    private void openNWriters(int n) throws IOException {
        for (Integer i = 0; i != n; ++i) {
            writers.add(new BufferedWriter(new OutputStreamWriter(Files
                    .newOutputStream(outputDir.resolve(i.toString()))),
                    bufferSizes));
        }
    }

    @Override
    public void close() throws IOException {
        for (BufferedWriter writer : writers) {
            writer.close();
        }
    }

    public BufferedWriter get(String[] words, String[] poses, int p) {
        if (!type.equals(PatternElem.CNT) && !type.equals(PatternElem.POS)) {
            return writers.get(0);
        }

        // get first word of sequence that isn't PatternElem.SKIPPED_WORD
        String indexWord = PatternElem.SKIPPED_WORD;
        for (int i = 0; indexWord.equals(PatternElem.SKIPPED_WORD)
                && i != pattern.length(); ++i) {
            indexWord = pattern.get(i).apply(words[p + i], poses[p + i]);
        }

        return get(indexWord);
    }

    public BufferedWriter get(Object[] words) {
        if (!type.equals(PatternElem.CNT) && !type.equals(PatternElem.POS)) {
            return writers.get(0);
        }

        String indexWord = PatternElem.SKIPPED_WORD;
        for (int i = 0; indexWord.equals(PatternElem.SKIPPED_WORD)
                && i != pattern.length(); ++i) {
            indexWord = pattern.get(i).apply((String) words[i]);
        }

        return get(indexWord);
    }

    private BufferedWriter get(String indexWord) {
        switch (type) {
            case CNT:
                return writers.get(rank(index.getWordIndex(), indexWord));

            case POS:
                return writers.get(rank(index.getPosIndex(), indexWord));

            default:
                throw new IllegalStateException();
        }
    }

    private int rank(List<String> index, String indexWord) {
        int lo = 0;
        int hi = index.size() - 1;
        while (lo <= hi) {
            int mid = lo + (hi - lo) / 2;
            if (indexWord.compareTo(index.get(mid)) < 0) {
                hi = mid - 1;
            } else if (indexWord.compareTo(index.get(mid)) > 0) {
                lo = mid + 1;
            } else {
                return mid;
            }
        }
        // the following return statement is not the standard return result for
        // binary search
        return (lo + hi) / 2;
    }

}
