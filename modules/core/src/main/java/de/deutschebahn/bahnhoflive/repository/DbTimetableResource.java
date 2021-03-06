package de.deutschebahn.bahnhoflive.repository;

import androidx.annotation.Nullable;
import androidx.lifecycle.Observer;

import com.android.volley.VolleyError;

import java.util.List;

import de.deutschebahn.bahnhoflive.BaseApplication;
import de.deutschebahn.bahnhoflive.backend.db.publictrainstation.model.DetailedStopPlace;
import de.deutschebahn.bahnhoflive.backend.db.publictrainstation.model.StopPlace;
import de.deutschebahn.bahnhoflive.backend.local.model.EvaIds;
import de.deutschebahn.bahnhoflive.repository.timetable.Timetable;
import de.deutschebahn.bahnhoflive.repository.timetable.TimetableRepository;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;

public class DbTimetableResource extends RemoteResource<Timetable> {

    private float distanceInKm = -1;

    private Station station;
    private String stationId;
    private String stationName;

    private final TimetableRepository timetableCollector = BaseApplication.get().getRepositories().getTimetableRepository();

    private final Disposable disposable = timetableCollector
            .getMergedTrainInfosObservable()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(this::setResult, throwable -> {
                setError(new VolleyError(throwable));
            });

    public DbTimetableResource(Station station, StopPlace stopPlace) {
        distanceInKm = stopPlace.getDistanceInKm();
        stationId = stopPlace.getStadaId();
        stationName = stopPlace.getName();

        if (station == null) {
            setEvaIds(stopPlace.getEvaIds());
        } else {
            setEvaIds(station.getEvaIds());
            initialize(station);
        }
    }

    public DbTimetableResource(InternalStation internalStation) {
        station = internalStation;
        stationId = internalStation.getId();
        stationName = internalStation.getTitle();
        setEvaIds(internalStation.getEvaIds());
    }

    public void setEvaIdsMissing() {
        setError(new VolleyError("Eva IDs unavailable"));
    }

    public DbTimetableResource() {
    }

    @Override
    protected void onStartLoading(final boolean force) {
        final List<String> evaIds = getEvaIds();
        if (evaIds == null) {
            final DetailedStopPlaceResource detailedStopPlaceResource = new DetailedStopPlaceResource();
            detailedStopPlaceResource.initialize(getStation());
            final ResourceClient<DetailedStopPlace, VolleyError> resourceClient = new ResourceClient<>(new Observer<DetailedStopPlace>() {
                @Override
                public void onChanged(@Nullable DetailedStopPlace detailedStopPlace) {
                    if (detailedStopPlace != null) {
                        setEvaIds(detailedStopPlace.getEvaIds());

                        DbTimetableResource.this.loadData(force);
                    }
                }
            }, new Observer<LoadingStatus>() {
                @Override
                public void onChanged(@Nullable LoadingStatus loadingStatus) {
                    if (loadingStatus != null && loadingStatus == LoadingStatus.BUSY) {
                        DbTimetableResource.this.loadingStatus.setValue(LoadingStatus.BUSY);
                    }
                }
            }, new Observer<VolleyError>() {
                @Override
                public void onChanged(@Nullable VolleyError volleyError) {
                    setError(volleyError);
                }
            });
            resourceClient.observe(detailedStopPlaceResource);
        } else {
            timetableCollector.getRefreshTrigger().onNext(force);
        }
    }

    @Override
    public boolean isLoadingPreconditionsMet() {
        return true;
    }

    public void initialize(Station station) {
        this.station = station;
    }

    public void setEvaIds(EvaIds evaIds) {
        if (evaIds != null) {
            timetableCollector.getEvaIdsInput().onNext(evaIds.getIds());
            error.setValue(null);
        }
    }

    public float getDistanceInKm() {
        return distanceInKm;
    }

    public InternalStation getInternalStation() {
        if (station != null) {
            return InternalStation.of(station);
        }

        return new InternalStation(stationId, stationName, null);
    }

    public Station getStation() {
        return station;
    }

    public String getStationName() {
        return station == null ? stationName : station.getTitle();
    }

    public List<String> getEvaIds() {
        return timetableCollector.getEvaIdsInput().getValue();
    }

    public String getStationId() {
        return stationId;
    }

    @Override
    protected boolean onRefresh() {
        timetableCollector.getRefreshTrigger().onNext(true);
        return true;
    }

    public void loadMore() {
        timetableCollector.getNextHourTrigger().onNext(true);
    }
}
