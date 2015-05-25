package de.glmtk.options.custom;

import static de.glmtk.options.PathOption.parsePath;
import static de.glmtk.options.custom.QueryModeOption.EXPLANATION;
import static de.glmtk.options.custom.QueryModeOption.parseQueryMode;
import static de.glmtk.util.Strings.requireNotEmpty;
import static java.util.Objects.requireNonNull;

import java.nio.file.Path;
import java.util.List;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;

import de.glmtk.options.Option;
import de.glmtk.options.OptionException;
import de.glmtk.querying.probability.QueryMode;

public class QueryModeFilesOption extends Option {
    public static final String QUERY_MODE_DEFAULT_ARGNAME = QueryModeOption.DEFAULT_ARGNAME;
    public static final String FILES_DEFAULT_ARGNAME = "FILE";

    private Arg queryModeArg = new Arg(QUERY_MODE_DEFAULT_ARGNAME, 1,
            EXPLANATION);
    private Arg filesArg = new Arg(FILES_DEFAULT_ARGNAME, GREATER_ONE);
    private Multimap<QueryMode, Path> value = LinkedHashMultimap.create();

    public QueryModeFilesOption(String shortopt,
                                String longopt,
                                String desc) {
        super(shortopt, longopt, desc);
        mayBeGivenRepeatedly = true;
    }

    public QueryModeFilesOption queryModeArgName(String argName) {
        requireNonNull(argName);
        requireNotEmpty(argName);
        queryModeArg.name = argName;
        return this;
    }

    public QueryModeFilesOption filesArgName(String argName) {
        requireNonNull(argName);
        requireNotEmpty(argName);
        filesArg.name = argName;
        return this;
    }

    public QueryModeFilesOption defaultValue(Multimap<QueryMode, Path> defaultValue) {
        value = defaultValue;
        return this;
    }

    @Override
    protected List<Arg> arguments() {
        return ImmutableList.of(queryModeArg, filesArg);
    }

    @Override
    protected void parse() throws OptionException {
        if (!given)
            value = LinkedHashMultimap.create();

        QueryMode queryMode = parseQueryMode(queryModeArg.value, this);
        for (String pathString : filesArg.values)
            value.put(queryMode, parsePath(pathString, false, true, true,
                    false, this));
    }

    public Multimap<QueryMode, Path> getQueryModeFiles() {
        return value;
    }
}
