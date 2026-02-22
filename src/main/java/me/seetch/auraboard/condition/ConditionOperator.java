package me.seetch.auraboard.condition;

public enum ConditionOperator {
    EQUALS, NOT_EQUALS, CONTAINS, STARTS_WITH, ENDS_WITH, REGEX, GT, LT, GTE, LTE;

    public boolean evaluate(String actual, String expected) {
        if (actual == null) actual = "";
        return switch (this) {
            case EQUALS -> actual.equalsIgnoreCase(expected);
            case NOT_EQUALS -> !actual.equalsIgnoreCase(expected);
            case CONTAINS -> actual.contains(expected);
            case STARTS_WITH -> actual.startsWith(expected);
            case ENDS_WITH -> actual.endsWith(expected);
            case REGEX -> actual.matches(expected);
            case GT -> parseDouble(actual) > parseDouble(expected);
            case LT -> parseDouble(actual) < parseDouble(expected);
            case GTE -> parseDouble(actual) >= parseDouble(expected);
            case LTE -> parseDouble(actual) <= parseDouble(expected);
        };
    }

    private double parseDouble(String s) {
        try {
            return Double.parseDouble(s.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}