package de.deutschebahn.bahnhoflive.backend.db.publictrainstation

import android.location.Location
import com.android.volley.NetworkResponse
import com.android.volley.Response
import com.android.volley.VolleyError
import de.deutschebahn.bahnhoflive.backend.ForcedCacheEntryFactory
import de.deutschebahn.bahnhoflive.backend.GsonResponseParser
import de.deutschebahn.bahnhoflive.backend.VolleyRestListener
import de.deutschebahn.bahnhoflive.backend.db.DbAuthorizationTool
import de.deutschebahn.bahnhoflive.backend.db.publictrainstation.model.StopPlace
import de.deutschebahn.bahnhoflive.backend.db.publictrainstation.model.StopPlacesPage
import java.net.URLEncoder

class StopPlacesRequest(
    listener: VolleyRestListener<List<StopPlace>>,
    dbAuthorizationTool: DbAuthorizationTool,
    query: String? = null,
    private val location: Location? = null,
    force: Boolean = false,
    private val limit: Int = 25,
    radius: Int = 2000
) : PublicTrainStationRequest<List<StopPlace>>(
    Method.GET,
    "stop-places?" + sequenceOf(
        "size" to (50 + limit * 8).toString()
    ).let { sequence ->
        query?.trim()?.takeUnless { it.isEmpty() }?.let { "name" to URLEncoder.encode(it, "UTF-8") }
            ?.let { sequence.plus(it) }
            ?: location?.let { location ->
                sequence.plus(
                    sequenceOf(
                        "latitude" to location.latitude.obfuscate().toString(),
                        "longitude" to location.longitude.obfuscate().toString(),
                        "radius" to radius.toString()
                    )
                )
            }
            ?: sequence
    }.filterNotNull().joinToString("&") {
        "${it.first}=${it.second}"
    },
    dbAuthorizationTool,
    listener
) {

    init {
        setShouldCache(!force)
    }

    override fun getCountKey() = "PTS/stop-places"

    override fun parseNetworkResponse(response: NetworkResponse?): Response<List<StopPlace>> = try {
        val stationQueryResponseParser = GsonResponseParser(StopPlacesPage::class.java)
        val stopPlacesPage = stationQueryResponseParser.parseResponse(response)
        val stopPlaces = stopPlacesPage.stopPlaces ?: emptyList()
        val filteredStopPlaces = stopPlaces.asSequence()
            .filterNotNull()
            .filter { it.isDbStation }
            .run {
                location?.let { location ->
                    val distanceCalulator =
                            DistanceCalulator(
                                location.latitude,
                                location.longitude
                            )
                    onEach { stopPlace ->
                        stopPlace.calculateDistance(distanceCalulator)
                    }.sortedBy { it.distanceInKm }
                } ?: this
            }
            .take(limit)
            .toList()

        val forcedCacheEntryFactory = ForcedCacheEntryFactory(ForcedCacheEntryFactory.DAY_IN_MILLISECONDS)

        Response.success(filteredStopPlaces, forcedCacheEntryFactory.createCacheEntry(response))
    } catch (e: Exception) {
        Response.error(VolleyError(e))
    }

    companion object {
        fun Double.obfuscate() = Math.round(this * 1000) / 1000.0
    }
}