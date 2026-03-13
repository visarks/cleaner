package com.cleaner.model;

import com.fasterxml.jackson.annotation.JsonTypeName;

@JsonTypeName("keep")
public class KeepRule extends Rule {

    public KeepRule() {
        super();
    }

    public KeepRule(String pattern) {
        super(pattern);
    }

    @Override
    public String getType() {
        return "keep";
    }
}