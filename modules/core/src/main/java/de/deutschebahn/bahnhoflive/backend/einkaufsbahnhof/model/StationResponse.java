package de.deutschebahn.bahnhoflive.backend.einkaufsbahnhof.model;

import java.util.List;

public class StationResponse {
    public final int status;

    public final Station station;

    public final List<Store> stores;

    public StationResponse(int status, Station station, List<Store> stores) {
        this.status = status;
        this.station = station;
        this.stores = stores;
    }
}
