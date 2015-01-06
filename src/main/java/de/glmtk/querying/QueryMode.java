package de.glmtk.querying;

import de.glmtk.exceptions.SwitchCaseNotImplementedException;
import de.glmtk.util.ObjectUtils;

public class QueryMode {

    public static enum QueryType {
        SEQUENCE,
        FIXED,
        MARKOV,
        COND;
    }

    public static QueryMode forString(String string) {
        String stringLower = string.toLowerCase();
        int pos = posOfFirstNumber(stringLower);
        if (pos == -1) {
            String type = stringLower;
            if (!type.isEmpty() && "sequence".startsWith(type))
                return newSequence();
        } else {
            String type = stringLower.substring(0, pos);
            String orderStr = stringLower.substring(pos);
            try {
                int order = Integer.parseInt(orderStr);
                if (type.isEmpty())
                    return newFixed(order);
                else if ("markov".startsWith(type))
                    return newMarkov(order);
                else if ("cond".startsWith(type))
                    return newCond(order);
            } catch (NumberFormatException e) {
            }
        }
        throw new RuntimeException(String.format(
                "Illegal Query Mode string '%s'.", string));
    }

    public static void main(String[] args) {
        System.out.println(forString("fmar2"));
    }

    private static int posOfFirstNumber(String string) {
        for (int i = 0; i != string.length(); ++i)
            if (Character.isDigit(string.charAt(i)))
                return i;
        return -1;
    }

    public static QueryMode newSequence() {
        QueryMode queryMode = new QueryMode();
        queryMode.type = QueryType.SEQUENCE;
        queryMode.withLengthFreq = true;
        return queryMode;
    }

    public static QueryMode newFixed(int order) {
        QueryMode queryMode = new QueryMode();
        queryMode.type = QueryType.FIXED;
        queryMode.order = order;
        return queryMode;
    }

    public static QueryMode newMarkov(int order) {
        QueryMode queryMode = new QueryMode();
        queryMode.type = QueryType.MARKOV;
        queryMode.withLengthFreq = true;
        queryMode.order = order;
        return queryMode;
    }

    public static QueryMode newCond(int order) {
        QueryMode queryMode = new QueryMode();
        queryMode.type = QueryType.COND;
        queryMode.order = order;
        return queryMode;
    }

    private QueryType type = null;
    private boolean withLengthFreq = false;
    private Integer order = null;

    private QueryMode() {
    }

    public QueryType getType() {
        return type;
    }

    public Boolean isWithLengthFreq() {
        return withLengthFreq;
    }

    public Integer getOrder() {
        return order;
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        switch (type) {
            case SEQUENCE:
                result.append("Sequence");
                break;
            case FIXED:
                break;
            case MARKOV:
                result.append("Markov");
                break;
            case COND:
                result.append("Cond");
                break;
            default:
                throw new SwitchCaseNotImplementedException();
        }
        if (order != null)
            result.append(order);
        return result.toString();
    }

    public boolean equals(QueryMode other) {
        if (other == this)
            return true;
        else if (other == null || getClass() != other.getClass())
            return false;

        QueryMode o = other;
        return type.equals(o.type) && withLengthFreq == o.withLengthFreq
                && ObjectUtils.equals(order, o.order);
    }
}
