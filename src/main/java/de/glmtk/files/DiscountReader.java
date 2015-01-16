package de.glmtk.files;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.List;

import de.glmtk.common.Pattern;
import de.glmtk.counts.Discount;
import de.glmtk.exceptions.FileFormatException;
import de.glmtk.util.StringUtils;

public class DiscountReader extends AbstractFileReader {
    private Pattern pattern;
    private Discount discount;

    public DiscountReader(Path file,
                          Charset charset) throws IOException {
        this(file, charset, 8192);
    }

    public DiscountReader(Path file,
                          Charset charset,
                          int sz) throws IOException {
        super(file, charset, sz);
    }

    @Override
    protected void parseLine() {
        if (line == null) {
            pattern = null;
            discount = null;
            return;
        }

        List<String> split = StringUtils.splitAtChar(line, '\t');
        if (split.size() != 4)
            throw new FileFormatException(line, lineNo, file, "discount",
                    "Expected line to have format '<pattern>(\\t<discount>}{3}'.");

        try {
            pattern = parsePattern(split.get(0));
            discount = new Discount(parseFloatingPoint(split.get(1)),
                    parseFloatingPoint(split.get(2)),
                    parseFloatingPoint(split.get(3)));
        } catch (IllegalArgumentException e) {
            throw new FileFormatException(line, lineNo, file, "discount",
                    e.getMessage());
        }
    }

    public Pattern getPattern() {
        return pattern;
    }

    public Discount getDiscount() {
        return discount;
    }
}
