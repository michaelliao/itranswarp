package com.itranswarp.web.support;

import java.util.List;

import io.pebbletemplates.pebble.extension.Function;

public abstract class AbstractFunction implements Function {

    public abstract String getName();

    @Override
    public List<String> getArgumentNames() {
        return null;
    }

}
