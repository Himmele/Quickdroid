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
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;

public class Preferences extends PreferenceActivity implements OnSharedPreferenceChangeListener {
	public static final String PREF_SEARCH_HISTORY = "searchHistory";
	public static final String PREF_MAX_SEARCH_HISTORY_SIZE = "maxSearchHistorySize";
	public static final String DEFAULT_SEARCH_HISTORY_SIZE = "10";
	public static final String PREF_QUICK_LAUNCH = "quickLaunch";
	public static final String PREF_CLEAR_SEARCH_TEXT = "clearSearchTextOnResume";
	public static final String PREF_TASK_STACK_BEHAVIOR = "taskStackBehavior";
	public static final String PREF_SOFT_KEYBOARD = "softKeyboard";
	public static final String PREF_BACK_KEY_HANDLING = "backKeyHandling";
	public static final String PREF_GESTURE_RECOGNIZER = "gestureRecognizer";
	public static final String PREF_SPEECH_RECOGNIZER = "speechRecognizer";
	public static final String PREF_CONTACT_PHOTOS = "contactPhotos";
	public static boolean SEARCH_LAUNCHER = true;
	public static boolean DO_NOT_SEARCH_LAUNCHER = false;
	public static final String PREF_SEARCH_FAVORITE_ITEMS = "searchFavoriteItems";
	public static final String PREF_SEARCH_APPS = "searchApps";
	public static final String PREF_SEARCH_CONTACTS = "searchContacts";
	public static final String PREF_SEARCH_BOOKMARKS = "searchBookmarks";
	public static final String PREF_SEARCH_ARTISTS = "searchArtists";
	public static final String PREF_SEARCH_ALBUMS = "searchAlbums";
	public static final String PREF_SEARCH_SONGS = "searchSongs";
	public static final String DEFAULT_NUM_SUGGESTIONS_2 = "2";
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
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.layout.preferences);
        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    }

	@Override
	public void onSharedPreferenceChanged(SharedPreferences preferences, String key) {
		Intent data = new Intent();
		if (key.equals(PREF_QUICK_LAUNCH)) {
            if (preferences.getBoolean(key, true)) {
            	Quickdroid.activateQuickLaunch(this);
            } else {
            	Quickdroid.deactivateQuickLaunch(this);
            }
		} else if (key.equals(PREF_SEARCH_HISTORY)) {
			if (preferences.getBoolean(key, false)) {
				data.putExtra(PREF_SEARCH_HISTORY, false);
			}
		}
		data.putExtra(PREFS_CHANGED, true);
		setResult(RESULT_OK, data);
	}
}