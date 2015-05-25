package de.glmtk.options;

import static de.glmtk.options.PathOption.parsePath;
import static de.glmtk.options.QueryModeOption.parseQueryMode;
import static de.glmtk.util.revamp.MapUtils.map;
import static de.glmtk.util.revamp.SetUtils.set;
import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

import java.nio.file.Path;
import java.util.Map;
import java.util.Set;

import de.glmtk.querying.probability.QueryMode;

public class QueryModeFilesOption extends Option {
    public static final String QUERY_MODE_DEFAULT_ARGNAME = QueryModeOption.DEFAULT_ARGNAME;
    public static final String FILES_DEFAULT_ARGNAME = "FILE";

    private String queryModeArgname;
    private String filesArgname;
    private Map<QueryMode, Set<Path>> value = map();
    private boolean explicitDefault = false;

    public QueryModeFilesOption(String shortopt,
                                String longopt,
                                String desc) {
        this(shortopt, longopt, desc, QUERY_MODE_DEFAULT_ARGNAME,
                FILES_DEFAULT_ARGNAME);
    }

    public QueryModeFilesOption(String shortopt,
                                String longopt,
                                String desc,
                                String queryModeArgname,
                                String filesArgname) {
        super(shortopt, longopt, desc);

        requireNonNull(queryModeArgname);
        requireNonNull(filesArgname);

        this.queryModeArgname = queryModeArgname;
        this.filesArgname = filesArgname;
    }

    public QueryModeFilesOption defaultValue(Map<QueryMode, Set<Path>> defaultValue) {
        value = defaultValue;
        explicitDefault = true;
        return this;
    }

    @Override
    /* package */org.apache.commons.cli.Option createCommonsCliOption() {
        org.apache.commons.cli.Option commonsCliOption = new org.apache.commons.cli.Option(
                shortopt, longopt, true, desc);
        commonsCliOption.setArgName(format("%s> <%s%s", queryModeArgname,
                filesArgname, MULTIPLE_ARG_SUFFIX));
        commonsCliOption.setArgs(org.apache.commons.cli.Option.UNLIMITED_VALUES);
        return commonsCliOption;
    }

    @Override
    /* package */void parse(org.apache.commons.cli.Option commonsCliOption) throws OptionException {
        if (explicitDefault) {
            explicitDefault = false;
            value = map();
        }

        String[] args = commonsCliOption.getValues();
        if (args.length < 2)
            throw new OptionException("Option %s requires at least a "
                    + "query mode and one file.", this);

        QueryMode queryMode = parseQueryMode(args[0], this);
        Set<Path> paths = value.get(queryMode);
        if (paths == null) {
            paths = set();
            value.put(queryMode, paths);
        }

        for (String pathString : commonsCliOption.getValues())
            paths.add(parsePath(pathString, false, true, true, false, this));
    }

    public Map<QueryMode, Set<Path>> getQueryModeFiles() {
        return value;
    }
}
