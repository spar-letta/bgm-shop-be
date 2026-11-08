package com.app.itibari.services;

import com.app.itibari.dataTypes.CounterType;

public interface CounterService {

    Integer getNextCounter(CounterType counterType);
}
