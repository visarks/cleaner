package com.cleaner.model;

import com.fasterxml.jackson.annotation.JsonTypeName;

@JsonTypeName("delete")
public class DeleteRule extends Rule {

    public DeleteRule() {
        super();
    }

    public DeleteRule(String pattern) {
        super(pattern);
    }

    @Override
    public String getType() {
        return "delete";
    }
}