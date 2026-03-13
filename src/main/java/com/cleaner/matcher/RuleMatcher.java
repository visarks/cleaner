package com.cleaner.matcher;

import com.cleaner.model.DeleteRule;
import com.cleaner.model.KeepRule;
import com.cleaner.model.Rule;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static com.cleaner.util.PathUtils.matches;

public class RuleMatcher {

    private final List<KeepRule> keepRules = new ArrayList<>();
    private final List<DeleteRule> deleteRules = new ArrayList<>();

    public void addRule(Rule rule) {
        if (rule instanceof KeepRule keepRule) {
            keepRules.add(keepRule);
        } else if (rule instanceof DeleteRule deleteRule) {
            deleteRules.add(deleteRule);
        }
    }

    public void removeRule(Rule rule) {
        if (rule instanceof KeepRule) {
            keepRules.remove(rule);
        } else if (rule instanceof DeleteRule) {
            deleteRules.remove(rule);
        }
    }

    public void clearRules() {
        keepRules.clear();
        deleteRules.clear();
    }

    public List<KeepRule> getKeepRules() {
        return keepRules;
    }

    public List<DeleteRule> getDeleteRules() {
        return deleteRules;
    }

    /**
     * Match result containing the matched rule and type.
     * Priority: Keep rules > Delete rules
     */
    public record MatchResult(String rule, String type) {
        public static final MatchResult NO_MATCH = new MatchResult(null, null);
        public static final MatchResult KEEP = new MatchResult(null, "keep");
    }

    /**
     * Matches a file path against all rules.
     * Keep rules have higher priority than delete rules.
     *
     * @param path the file path to match
     * @return MatchResult containing the matched rule pattern and type ("keep" or "delete"),
     *         or NO_MATCH if no rules match
     */
    public MatchResult match(Path path) {
        // First check keep rules (higher priority)
        for (KeepRule rule : keepRules) {
            if (rule.isEnabled() && matches(path, rule.getPattern())) {
                return new MatchResult(rule.getPattern(), "keep");
            }
        }

        // Then check delete rules
        for (DeleteRule rule : deleteRules) {
            if (rule.isEnabled() && matches(path, rule.getPattern())) {
                return new MatchResult(rule.getPattern(), "delete");
            }
        }

        return MatchResult.NO_MATCH;
    }

    /**
     * Check if a path should be deleted (matches delete rule and no keep rule).
     */
    public boolean shouldDelete(Path path) {
        // Check keep rules first
        for (KeepRule rule : keepRules) {
            if (rule.isEnabled() && matches(path, rule.getPattern())) {
                return false;
            }
        }

        // Check delete rules
        for (DeleteRule rule : deleteRules) {
            if (rule.isEnabled() && matches(path, rule.getPattern())) {
                return true;
            }
        }

        return false;
    }
}