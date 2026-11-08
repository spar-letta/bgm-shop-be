package com.app.itibari.converter;

// spring has a similar interface,hehe  - we will modify it with time
public interface Converter<S, T> {
    T convert(S source);
}
