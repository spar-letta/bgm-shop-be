package com.app.itibari.services;

import com.app.itibari.models.entities.TraSwitch;

public interface TraSwitchService {

    TraSwitch switchTraOn(Boolean switchOn, String name);

    boolean traSwitchOn();

    TraSwitch getTraSwitchStatus(String name);
}
