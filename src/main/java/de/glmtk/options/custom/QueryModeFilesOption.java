package de.glmtk.options.custom;

import static com.google.common.collect.Maps.newHashMap;
import static com.google.common.collect.Sets.newHashSet;
import static de.glmtk.options.PathOption.parsePath;
import static de.glmtk.options.custom.QueryModeOption.EXPLANATION;
import static de.glmtk.options.custom.QueryModeOption.parseQueryMode;
import static java.util.Objects.requireNonNull;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableList;

import de.glmtk.options.Option;
import de.glmtk.options.OptionException;
import de.glmtk.querying.probability.QueryMode;

public class QueryModeFilesOption extends Option {
    public static final String QUERY_MODE_DEFAULT_ARGNAME = QueryModeOption.DEFAULT_ARGNAME;
    public static final String FILES_DEFAULT_ARGNAME = "FILE";

    private Arg queryModeArg = new Arg(QUERY_MODE_DEFAULT_ARGNAME, 1,
            EXPLANATION);
    private Arg filesArg = new Arg(FILES_DEFAULT_ARGNAME, MORE_THAN_ONE);
    private Map<QueryMode, Set<Path>> value = newHashMap();

    public QueryModeFilesOption(String shortopt,
                                String longopt,
                                String desc) {
        super(shortopt, longopt, desc);
        mayBeGivenRepeatedly = true;
    }

    public QueryModeFilesOption queryModeArgName(String argName) {
        requireNonNull(argName);
        queryModeArg.name = argName;
        return this;
    }

    public QueryModeFilesOption filesArgName(String argName) {
        requireNonNull(argName);
        filesArg.name = argName;
        return this;
    }

    public QueryModeFilesOption defaultValue(Map<QueryMode, Set<Path>> defaultValue) {
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
            value = newHashMap();

        QueryMode queryMode = parseQueryMode(queryModeArg.values.get(0), this);
        Set<Path> paths = value.get(queryMode);
        if (paths == null) {
            paths = newHashSet();
            value.put(queryMode, paths);
        }

        for (String pathString : filesArg.values)
            paths.add(parsePath(pathString, false, true, true, false, this));
    }

    public Map<QueryMode, Set<Path>> getQueryModeFiles() {
        return value;
    }
}
