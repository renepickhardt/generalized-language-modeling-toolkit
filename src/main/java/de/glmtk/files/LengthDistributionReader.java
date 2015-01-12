package de.glmtk.files;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.List;

import de.glmtk.exceptions.FileFormatException;
import de.glmtk.util.StringUtils;

public class LengthDistributionReader extends AbstractFileReader {
    private int length;
    private double frequency;

    public LengthDistributionReader(Path file,
                                    Charset charset) throws IOException {
        this(file, charset, 8192);
    }

    public LengthDistributionReader(Path file,
                                    Charset charset,
                                    int sz) throws IOException {
        super(file, charset, sz);
        length = 0;
        frequency = Double.NaN;
    }

    @Override
    protected void parseLine() {
        if (line == null) {
            length = 0;
            frequency = Double.NaN;
            return;
        }

        List<String> split = StringUtils.splitAtChar(line, '\t');

        if (split.size() != 2)
            throw new FileFormatException(line, lineNo, file,
                    "length distribution",
                    "Expected line to have format '<length>\\t<frequency>'.");

        try {
            length = Integer.parseInt(split.get(0));
        } catch (NumberFormatException e) {
            throw new FileFormatException(line, lineNo, file,
                    "length distribution",
                    "Unable to parse '%s' as an integer.", split.get(0));
        }

        try {
            frequency = Double.parseDouble(split.get(1));
        } catch (NumberFormatException e) {
            throw new FileFormatException(line, lineNo, file,
                    "length distribution",
                    "Unable to parse '%s' as a floating point number.",
                    split.get(1));
        }
    }

    public int getLength() {
        return length;
    }

    public double getFrequency() {
        return frequency;
    }
}
