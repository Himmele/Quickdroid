package vu.de.urpool.quickdroid;

/*
 * Copyright (C) 2009 Daniel Himmelein
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;

public class Preferences extends PreferenceActivity implements OnSharedPreferenceChangeListener {
	public static final String PREF_QUICK_LAUNCH = "quickLaunch";
	public static final String PREF_MAX_MRU_LIST_SIZE = "maxMRUListSize";
	public static final String DEFAULT_MRU_LIST_SIZE = "10";
	public static boolean SEARCH_LAUNCHER = true;
	public static boolean DO_NOT_SEARCH_LAUNCHER = false;
	public static final String PREF_SEARCH_APPS = "searchApps";
	public static final String PREF_SEARCH_CONTACTS = "searchContacts";
	public static final String PREF_SEARCH_BOOKMARKS = "searchBookmarks";
	public static final String PREF_SEARCH_ARTISTS = "searchArtists";
	public static final String PREF_SEARCH_ALBUMS = "searchAlbums";
	public static final String PREF_SEARCH_SONGS = "searchSongs";
	public static final String DEFAULT_NUM_SUGGESTIONS_4 = "4";
	public static final String DEFAULT_NUM_SUGGESTIONS_10 = "10";
	public static final String PREF_APPS_NUM_SUGGESTIONS = "appsNumSuggestions";
	public static final String PREF_CONTACTS_NUM_SUGGESTIONS = "contactsNumSuggestions";
	public static final String PREF_BOOKMARKS_NUM_SUGGESTIONS = "bookmarksNumSuggestions";
	public static final String PREF_ARTISTS_NUM_SUGGESTIONS = "artistsNumSuggestions";
	public static final String PREF_ALBUMS_NUM_SUGGESTIONS = "albumsNumSuggestions";
	public static final String PREF_SONGS_NUM_SUGGESTIONS = "songsNumSuggestions";
	public static final String DEFAULT_PATTERN_MATCHING_LEVEL = "2";
	public static final String PREF_APPS_PATTERN_MATCHING_LEVEL = "appsPatternMatchingLevel";
	public static final String PREF_CONTACTS_PATTERN_MATCHING_LEVEL = "contactsPatternMatchingLevel";
	public static final String PREF_BOOKMARKS_PATTERN_MATCHING_LEVEL = "bookmarksPatternMatchingLevel";
	public static final String PREF_ARTISTS_PATTERN_MATCHING_LEVEL = "artistsPatternMatchingLevel";
	public static final String PREF_ALBUMS_PATTERN_MATCHING_LEVEL = "albumsPatternMatchingLevel";
	public static final String PREF_SONGS_PATTERN_MATCHING_LEVEL = "songsPatternMatchingLevel";
	public static final String PREFS_CHANGED = "prefsChanged";
	public static final int QUICK_LAUNCH_THUMBNAIL_ID = 1;
	
	private NotificationManager mNM;
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.layout.preferences);
        
        mNM = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        
        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    }

	@Override
	public void onSharedPreferenceChanged(SharedPreferences preferences, String key) {
		if (key.equals(PREF_QUICK_LAUNCH)) {
            if (preferences.getBoolean(key, true)) {
            	activateQuickLaunch();
            } else {
            	deactivateQuickLaunch();
            }
		}
		Intent data = new Intent();
		data.putExtra(PREFS_CHANGED, true);
		setResult(RESULT_OK, data);
	}
	
	private void activateQuickLaunch() {
		Notification notification = new Notification(R.drawable.mini_app_thumbnail, null, 0);
		Intent intent = new Intent(this, Quickdroid.class);
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0, intent, 0);
		notification.flags |= (Notification.FLAG_NO_CLEAR | Notification.FLAG_ONGOING_EVENT);
		notification.setLatestEventInfo(this, getText(R.string.appName), null, contentIntent);
		mNM.notify(QUICK_LAUNCH_THUMBNAIL_ID, notification);
	}

	private void deactivateQuickLaunch() {
		mNM.cancel(QUICK_LAUNCH_THUMBNAIL_ID);
	}
}
