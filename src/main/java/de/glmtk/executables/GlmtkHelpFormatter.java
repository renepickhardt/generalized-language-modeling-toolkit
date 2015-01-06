package de.glmtk.executables;

import static de.glmtk.common.Output.OUTPUT;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;

import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

public class GlmtkHelpFormatter extends HelpFormatter {
    @SuppressWarnings({"unchecked", "deprecation"})
    @Override
    protected StringBuffer renderOptions(StringBuffer sb,
                                         int width,
                                         Options options,
                                         int leftPad,
                                         int descPad) {
        final String lpad = createPadding(leftPad);
        final String dpad = createPadding(descPad);

        List<Option> optList;
        try {
            Method helpOptions = Options.class.getDeclaredMethod("helpOptions");
            helpOptions.setAccessible(true);
            optList = (List<Option>) helpOptions.invoke(options);
        } catch (NoSuchMethodException | SecurityException
                 | IllegalAccessException | IllegalArgumentException
                 | InvocationTargetException e) {
            // lets hope this doesnt happen
            throw new RuntimeException(e);
        }

        Collections.sort(optList, getOptionComparator());

        boolean first = true;
        for (Option option : optList) {
            if (first)
                first = false;
            else
                sb.append(defaultNewLine);

            StringBuffer optBuf = new StringBuffer(8);

            if (option.getOpt() == null)
                optBuf.append(lpad).append("   ").append(defaultLongOptPrefix).append(
                        OUTPUT.bold(option.getLongOpt()));
            else {
                optBuf.append(lpad).append(defaultOptPrefix).append(
                        OUTPUT.bold(option.getOpt()));
                if (option.hasLongOpt())
                    optBuf.append(',').append(defaultLongOptPrefix).append(
                            OUTPUT.bold(option.getLongOpt()));
            }

            if (option.hasArg())
                if (option.hasArgName())
                    optBuf.append(' ').append(option.getArgName());
                else
                    optBuf.append(' ');
            renderWrappedText(sb, width, width, optBuf.toString());

            if (option.getDescription() != null) {
                sb.append(defaultNewLine);
                StringBuffer descBuf = new StringBuffer();
                descBuf.append(lpad).append(dpad).append(
                        option.getDescription());
                renderWrappedText(sb, width, leftPad + descPad,
                        descBuf.toString());
            }
        }

        return sb;
    }
}
