package com.cleaner.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = DeleteRule.class, name = "delete"),
    @JsonSubTypes.Type(value = KeepRule.class, name = "keep")
})
@JsonIgnoreProperties(ignoreUnknown = true)
public abstract class Rule {
    protected String pattern;
    protected boolean enabled = true;

    protected Rule() {}

    protected Rule(String pattern) {
        this.pattern = pattern;
    }

    public String getPattern() { return pattern; }
    public void setPattern(String pattern) { this.pattern = pattern; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public abstract String getType();

    @Override
    public String toString() {
        return pattern;
    }
}