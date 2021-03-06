package de.deutschebahn.bahnhoflive.ui.search;

import android.content.Context;

import androidx.annotation.DrawableRes;

public interface SearchResult {
    CharSequence getTitle();

    boolean isFavorite();

    void setFavorite(boolean favorite);

    void onClick(Context context, boolean details);

    @DrawableRes
    int getIcon();

    boolean isLocal();
}
