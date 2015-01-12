package de.glmtk.files;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.List;

import de.glmtk.common.Pattern;
import de.glmtk.common.Patterns;
import de.glmtk.counts.NGramTimes;
import de.glmtk.exceptions.FileFormatException;
import de.glmtk.util.StringUtils;

public class NGramTimesReader extends AbstractFileReader {
    private Pattern pattern;
    private NGramTimes nGramTimes;

    public NGramTimesReader(Path file,
                            Charset charset) throws IOException {
        this(file, charset, 8192);
    }

    public NGramTimesReader(Path file,
                            Charset charset,
                            int sz) throws IOException {
        super(file, charset, sz);
    }

    @Override
    protected void parseLine() {
        if (line == null) {
            pattern = null;
            nGramTimes = null;
            return;
        }

        List<String> split = StringUtils.splitAtChar(line, '\t');
        if (split.size() != 5)
            throw new FileFormatException(line, lineNo, file, "ngraim times",
                    "Expected line to have format '<pattern>(\\t<count>){4}'.");

        try {
            pattern = Patterns.get(split.get(0));
        } catch (RuntimeException e) {
            throw new FileFormatException(line, lineNo, file, "ngram times",
                    "Unable to parse '%s' as a pattern.", split.get(0));
        }

        try {
            nGramTimes = new NGramTimes(parseNumber(split.get(1)),
                    parseNumber(split.get(2)), parseNumber(split.get(3)),
                    parseNumber(split.get(4)));
        } catch (NumberFormatException e) {
            throw new FileFormatException(line, lineNo, file, "ngram times",
                    e.getMessage());
        }
    }

    public Pattern getPattern() {
        return pattern;
    }

    public NGramTimes getNGramTimes() {
        return nGramTimes;
    }
}
