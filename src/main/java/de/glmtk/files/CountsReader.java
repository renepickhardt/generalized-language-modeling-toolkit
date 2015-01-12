package de.glmtk.files;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;

import de.glmtk.counts.Counts;
import de.glmtk.exceptions.FileFormatException;
import de.glmtk.util.ObjectUtils;
import de.glmtk.util.StringUtils;

public class CountsReader extends AbstractFileReader {
    public static final Comparator<CountsReader> SEQUENCE_COMPARATOR = new Comparator<CountsReader>() {
        @Override
        public int compare(CountsReader lhs,
                           CountsReader rhs) {
            if (lhs == rhs)
                return 0;
            else if (lhs == null)
                return 1;
            else if (rhs == null)
                return -1;
            else
                return ObjectUtils.compare(lhs.sequence, rhs.sequence);
        }
    };

    private static long parseLong(String value) throws NumberFormatException {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            throw new NumberFormatException(String.format(
                    "Unable to parse '%s' as a floating point.", value));
        }
    }

    private String sequence;
    private Counts counts;
    private boolean fromAbsolute;

    public CountsReader(Path file,
                        Charset charset) throws IOException {
        this(file, charset, 8192);
    }

    public CountsReader(Path file,
                        Charset charset,
                        int sz) throws IOException {
        super(file, charset, sz);
        sequence = null;
        counts = null;
        fromAbsolute = true;
    }

    @Override
    protected void parseLine() {
        if (line == null) {
            sequence = null;
            counts = null;
        }

        try {
            List<String> split = StringUtils.splitAtChar(line, '\t');
            sequence = split.get(0);
            counts = new Counts();
            if (split.size() == 2) {
                fromAbsolute = true;
                counts.set(parseLong(split.get(1)), 0L, 0L, 0L);
            } else if (split.size() == 5) {
                fromAbsolute = false;
                counts.set(parseLong(split.get(1)), parseLong(split.get(2)),
                        parseLong(split.get(3)), parseLong(split.get(4)));
            } else
                throw new IllegalArgumentException(
                        "Expected line to have format '<sequence>(\\t<count>){1,4}'.");
        } catch (IllegalArgumentException e) {
            throw new FileFormatException(line, lineNo, file, "chunk",
                    e.getMessage());
        }
    }

    public String getSequence() {
        return sequence;
    }

    public long getCount() {
        return counts.getOnePlusCount();
    }

    public Counts getCounts() {
        return counts;
    }

    public boolean isFromAbsolute() {
        return fromAbsolute;
    }
}
