package com.learning;

import java.util.Observable;

public class ObserveRecord extends Observable {

    String action;

    public void UpdateNotification(String action) {
        this.action = action;
        setChanged();
        notifyObservers();
    }

    public String getAction(){
        return action;
    }

}
