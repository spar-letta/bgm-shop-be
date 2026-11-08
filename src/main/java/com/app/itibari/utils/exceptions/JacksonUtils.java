package com.app.itibari.utils.exceptions;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.IOException;
import java.text.DateFormat;

public class JacksonUtils {

    private static final ObjectMapper mapper;

    static {
        mapper = new ObjectMapper();
        mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
        DateFormat df = DateFormat.getDateInstance(DateFormat.LONG);
        mapper.setDateFormat(df);
    }

    public static <T> T fromJSON(String json, Class<T> type) {

        try {
            return mapper.readValue(json, type);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static String toJSON(Object o) {

        try {
            String json = mapper.writeValueAsString(o);
            return json;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
