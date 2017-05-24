package edu.uw.cyclife.cyclifeapp;

import java.util.Observable;

/**
 * Created by emaye on 5/23/2017.
 */

public class KSWrapper extends Observable {
    private boolean alarm = false;

    public boolean getAlarm() {
        return alarm;
    }

    public void alarmOn() {
        alarm = true;
        setChanged();
        notifyObservers();
    }

    public void alarmOff() {
        alarm = false;
    }
}
