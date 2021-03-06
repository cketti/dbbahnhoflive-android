package de.deutschebahn.bahnhoflive.ui.station.info

import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations
import com.google.android.gms.maps.model.LatLng
import de.deutschebahn.bahnhoflive.backend.db.publictrainstation.model.Availability
import de.deutschebahn.bahnhoflive.backend.db.publictrainstation.model.AvailabilityEntry
import de.deutschebahn.bahnhoflive.backend.db.publictrainstation.model.DetailedStopPlace
import de.deutschebahn.bahnhoflive.backend.db.publictrainstation.model.TravelCenter
import de.deutschebahn.bahnhoflive.backend.local.model.ServiceContent
import de.deutschebahn.bahnhoflive.repository.DetailedStopPlaceResource
import de.deutschebahn.bahnhoflive.repository.ShopsResource
import de.deutschebahn.bahnhoflive.stream.livedata.MergedLiveData
import de.deutschebahn.bahnhoflive.ui.station.StaticInfoCollection
import de.deutschebahn.bahnhoflive.ui.station.shop.Shop
import de.deutschebahn.bahnhoflive.util.then

class InfoAndServices(
        detailedStopPlaceResource: DetailedStopPlaceResource,
        val staticInfoCollectionSource: LiveData<StaticInfoCollection>,
        val travelCenter: LiveData<TravelCenter>,
        shopsResource: ShopsResource
) : MergedLiveData<List<ServiceContent>?>(null) {
    val detailedStopPlaceLiveData = detailedStopPlaceResource.data
    val travelCenterOpenHours = Transformations.map(shopsResource.data) {
        getTravelCenterOpenHours(it?.travelCenter)
    }

    init {
        addSource(detailedStopPlaceLiveData)
        addSource(travelCenterOpenHours)
        addSource(staticInfoCollectionSource)
        addSource(travelCenter)
    }

    override fun onSourceChanged(source: LiveData<*>) {
        detailedStopPlaceLiveData.value?.also { detailedStopPlace ->
            staticInfoCollectionSource.value?.also { staticInfoCollection ->
                update(detailedStopPlace, staticInfoCollection, travelCenterOpenHours.value, travelCenter.value)
            }
        }
    }

    fun update(detailedStopPlace: DetailedStopPlace, staticInfoCollection: StaticInfoCollection, travelCenterOpenHours: String?, travelCenter: TravelCenter?) {
        value = listOfNotNull(
            composeServiceContent(
                detailedStopPlace,
                staticInfoCollection,
                ServiceContent.Type.DB_INFORMATION,
                renderSchedule(detailedStopPlace.details?.dbInformation)
            ),
            composeServiceContent(
                detailedStopPlace,
                staticInfoCollection,
                ServiceContent.Type.MOBILE_SERVICE,
                renderSchedule(detailedStopPlace.details?.localServiceStaff)
            ),
                composeServiceContent(detailedStopPlace, staticInfoCollection, ServiceContent.Type.BAHNHOFSMISSION),
                staticInfoCollection.typedStationInfos[ServiceContent.Type.Local.TRAVEL_CENTER]?.let { staticInfo ->
                    (PublicTrainStationService.predicates[ServiceContent.Type.Local.TRAVEL_CENTER]?.invoke(detailedStopPlace) == true).then {
                        ServiceContent(staticInfo, travelCenter?.openingTimes?.rendered
                                ?: travelCenterOpenHours)
                    } ?: travelCenter?.let { travelCenter ->
                        ServiceContent(
                            staticInfo,
                            travelCenter.openingTimes?.rendered,
                            travelCenter.composedAddress,
                            LatLng(travelCenter.lat, travelCenter.lon)
                        )
                    }
                },
                composeServiceContent(detailedStopPlace, staticInfoCollection, ServiceContent.Type.Local.DB_LOUNGE)
        )
    }

    fun composeServiceContent(detailedStopPlace: DetailedStopPlace, staticInfoCollection: StaticInfoCollection, type: String, additionalInfo: String? = null) =
            PublicTrainStationService.predicates[type]?.invoke(detailedStopPlace)?.then {
                staticInfoCollection.typedStationInfos[type]?.let {
                    ServiceContent(it, additionalInfo)
            }}

    companion object {
        val dayLabels = mapOf(
                "monday" to "Montag",
                "tuesday" to "Dienstag",
                "wednesday" to "Mittwoch",
                "thursday" to "Donnerstag",
                "friday" to "Freitag",
                "saturday" to "Samstag",
                "sunday" to "Sonntag",
                "holiday" to "Feiertag"
        )
    }

    private fun renderSchedule(schedule: Availability?): String? {
        if (schedule != null) {
            val stringBuilder = StringBuilder()

            schedule.availability?.asSequence()?.filterNotNull()
                ?.forEach { availabilityEntry: AvailabilityEntry ->
                stringBuilder.append(String.format("<br/>%s: %s-%s", dayLabels[availabilityEntry.day]
                        ?: availabilityEntry.day,
                        availabilityEntry.openTime, availabilityEntry.closeTime))
            }

            if (stringBuilder.isNotEmpty()) {
                return "<b>Öffnungszeiten</b>$stringBuilder"
            }

        }

        return null
    }

    private fun getTravelCenterOpenHours(travelCenter: Shop?): String? {
        if (travelCenter == null) {
            return null
        }

        val openHoursInfo = travelCenter.openHoursInfo ?: return null

        return openHoursInfo.replace("\n".toRegex(), "<br/>")
    }

}