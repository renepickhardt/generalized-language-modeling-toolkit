package de.glmtk.files;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;

import de.glmtk.common.Pattern;
import de.glmtk.counts.Discount;

public class DiscountWriter extends AbstractFileWriter {
    public DiscountWriter(Path file,
                          Charset charset) throws IOException {
        this(file, charset, 8192);
    }

    public DiscountWriter(Path file,
                          Charset charset,
                          int sz) throws IOException {
        super(file, charset, sz);
    }

    public void append(Pattern pattern,
                       Discount discount) throws IOException {
        writer.append(pattern.toString());
        writer.append('\t').append(Double.toString(discount.getOne()));
        writer.append('\t').append(Double.toString(discount.getTwo()));
        writer.append('\t').append(Double.toString(discount.getThree()));
        writer.append('\n');
    }
}
