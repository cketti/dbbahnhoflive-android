package de.deutschebahn.bahnhoflive.repository.parking

import de.deutschebahn.bahnhoflive.backend.VolleyRestListener
import de.deutschebahn.bahnhoflive.backend.db.parkinginformation.model.ParkingFacility
import de.deutschebahn.bahnhoflive.repository.fail

open class ParkingRepository {

    open fun queryFacilities(
        stationId: String,
        listener: VolleyRestListener<List<ParkingFacility>>
    ) {
        listener.fail()
    }
}