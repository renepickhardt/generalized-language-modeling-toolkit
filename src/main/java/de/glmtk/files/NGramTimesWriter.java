package de.glmtk.files;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;

import de.glmtk.common.Pattern;
import de.glmtk.counts.NGramTimes;

public class NGramTimesWriter extends AbstractFileWriter {
    public NGramTimesWriter(Path file,
                            Charset charset) throws IOException {
        this(file, charset, 8192);
    }

    public NGramTimesWriter(Path file,
                            Charset charset,
                            int sz) throws IOException {
        super(file, charset, sz);
    }

    public void append(Pattern pattern,
                       NGramTimes nGramTimes) throws IOException {
        writer.append(pattern.toString());
        writer.append('\t').append(Long.toString(nGramTimes.getOneCount()));
        writer.append('\t').append(Long.toString(nGramTimes.getTwoCount()));
        writer.append('\t').append(Long.toString(nGramTimes.getThreeCount()));
        writer.append('\t').append(Long.toString(nGramTimes.getFourCount()));
        writer.append('\n');
    }
}
