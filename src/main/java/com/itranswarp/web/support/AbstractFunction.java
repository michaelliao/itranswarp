package com.itranswarp.web.support;

import java.util.List;

import com.mitchellbosecke.pebble.extension.Function;

public abstract class AbstractFunction implements Function {

	public abstract String getName();

	@Override
	public List<String> getArgumentNames() {
		return null;
	}

}
