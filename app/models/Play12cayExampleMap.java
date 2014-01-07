package models;

import models.auto._Play12cayExampleMap;

public class Play12cayExampleMap extends _Play12cayExampleMap {

    private static Play12cayExampleMap instance;

    private Play12cayExampleMap() {}

    public static Play12cayExampleMap getInstance() {
        if(instance == null) {
            instance = new Play12cayExampleMap();
        }

        return instance;
    }
}
