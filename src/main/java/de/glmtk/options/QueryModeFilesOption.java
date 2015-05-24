package de.glmtk.options;

import static de.glmtk.util.revamp.ListUtils.list;
import static java.util.Objects.requireNonNull;

import java.nio.file.Path;
import java.util.List;

import de.glmtk.querying.probability.QueryMode;

public class QueryModeFilesOption extends Option {
    public static final String QUERY_MODE_DEFAULT_ARGNAME = QueryModeOption.DEFAULT_ARGNAME;
    public static final String FILES_DEFAULT_ARGNAME = "FILE";

    private String queryModeArgname;
    private String filesArgname;
    private QueryMode queryModeDefaultValue = null;
    private List<Path> filesDefaultValue = list();

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

    public QueryModeFilesOption queryModeDefaultValue(QueryMode defaultValue) {
        queryModeDefaultValue = defaultValue;
        return this;
    }

    public QueryModeFilesOption filesDefaultValue(List<Path> defaultValue) {
        filesDefaultValue = defaultValue;
        return this;
    }

    public QueryMode getQueryMode() {
        throw new UnsupportedOperationException();
    }

    public List<Path> getFiles() {
        throw new UnsupportedOperationException();
    }
}
