package com.itranswarp.web.view;

import java.util.List;

import io.pebbletemplates.pebble.extension.Filter;

public abstract class AbstractFilter implements Filter {

    public abstract String getName();

    @Override
    public List<String> getArgumentNames() {
        return null;
    }

}
