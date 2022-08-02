package dev.sbutler.bitflask.common.configuration;

import com.google.common.collect.ImmutableList;

public record Configuration(ImmutableList<String> flags, String propertyKey, Object defaultValue) {

}
