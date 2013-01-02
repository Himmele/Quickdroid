package vu.de.urpool.quickdroid.favoriteitems;

/*
 * Copyright (C) 2011 Daniel Himmelein
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

import java.util.ArrayList;
import java.util.HashMap;
import android.content.ContentResolver;
import android.database.ContentObserver;
import android.database.Cursor;
import vu.de.urpool.quickdroid.Launcher;
import vu.de.urpool.quickdroid.Launchable;
import vu.de.urpool.quickdroid.SearchPatternMatchingLevel;
import vu.de.urpool.quickdroid.Quickdroid;

public class FavoriteItemsLauncher extends Launcher {
	private static final String NAME = "FavoriteItemsLauncher";
	private static final String[] FAVORITE_ITEMS_PROJECTION = new String[] {
	       "_ID", // 0
	        "SearchText", // 1
	        "LauncherID", // 2
	        "LaunchableID", // 3
	        "Counter" // 4
	    };
	private static final int ID_COLUMN_INDEX = 0;
	private static final int SEARCH_TEXT_COLUMN_INDEX = 1;
	private static final int LAUNCHER_ID_COLUMN_INDEX = 2;
	private static final int LAUNCHABLE_ID_COLUMN_INDEX = 3;
	private static final int COUNTER_COLUMN_INDEX = 4;
		
	private ContentResolver mContentResolver;
	private final Quickdroid mQuickdroid;
	private ArrayList<Launcher> mLaunchers;
	private int mNumLaunchers;
	private HashMap<Integer, Integer> mLauncherIndexes;
	
	public FavoriteItemsLauncher(Quickdroid quickdroid) {	
		mQuickdroid = quickdroid;		
		mContentResolver = quickdroid.getContentResolver();
	}
	
	public void init() {
		mLaunchers = mQuickdroid.getLaunchers();
		mNumLaunchers = mLaunchers.size();
		mLauncherIndexes = new HashMap<Integer, Integer>(mNumLaunchers);
		for (int i = 0; i < mNumLaunchers; i++) {
			mLauncherIndexes.put(mLaunchers.get(i).getId(), i);			
		}
	}
	
	@Override
	public String getName() {
		return NAME;
	}
	
	@Override
	public ArrayList<Launchable> getSuggestions(String searchText, int patternMatchingLevel, int offset, int limit) {
		Cursor cursor = null;
		switch(patternMatchingLevel) {
		case SearchPatternMatchingLevel.STARTS_WITH_SEARCH_TEXT:
			cursor = mContentResolver.query(FavoriteItemsProvider.FAVORITE_ITEMS_URI, FAVORITE_ITEMS_PROJECTION,
				"LOWER(SearchText) LIKE ?", 
				new String[] { searchText + "%" }, 
				"SearchText ASC, Counter DESC");
			break;
		case SearchPatternMatchingLevel.CONTAINS_WORD_THAT_STARTS_WITH_SEARCH_TEXT:
			cursor = mContentResolver.query(FavoriteItemsProvider.FAVORITE_ITEMS_URI, FAVORITE_ITEMS_PROJECTION,
				"LOWER(SearchText) LIKE ?", 
				new String[] { "% " + searchText + "%" },
				"SearchText ASC, Counter DESC");
			break;
		case SearchPatternMatchingLevel.CONTAINS_SEARCH_TEXT:
			cursor = mContentResolver.query(FavoriteItemsProvider.FAVORITE_ITEMS_URI, FAVORITE_ITEMS_PROJECTION,
				"LOWER(SearchText) LIKE ?" +
					" AND LOWER(SearchText) NOT LIKE ?" +
					" AND LOWER(SearchText) NOT LIKE ?",
				new String[] { "%" + searchText + "%", searchText + "%", "% " + searchText + "%" },
				"SearchText ASC, Counter DESC");
			break;
		case SearchPatternMatchingLevel.CONTAINS_EACH_CHAR_OF_SEARCH_TEXT:
			String searchPattern = "";
			for(char c : searchText.toCharArray()) {
				searchPattern += "%" + c;
			}
			searchPattern += "%";
			cursor = mContentResolver.query(FavoriteItemsProvider.FAVORITE_ITEMS_URI, FAVORITE_ITEMS_PROJECTION,
				"LOWER(SearchText) LIKE ? AND LOWER(SearchText) NOT LIKE ?", 
				new String[] { searchPattern, "%" + searchText + "%" }, 
				"SearchText ASC, Counter DESC");
			break;
		default:
			break;
		}		
		
		ArrayList<Launchable> suggestions = new ArrayList<Launchable>();
		if (cursor != null) {
			if (cursor.getCount() > offset) {
				cursor.moveToFirst();
				cursor.move(offset);
				int i = 0;
				while (!cursor.isAfterLast() && i++ < limit) {
					Integer launcherID = cursor.getInt(LAUNCHER_ID_COLUMN_INDEX);
					Integer launcherIndex = mLauncherIndexes.get(launcherID);
					if (launcherIndex != null) {
						Launchable launchable = mLaunchers.get(launcherIndex).getLaunchable(cursor.getInt(LAUNCHABLE_ID_COLUMN_INDEX));
						if ((launchable != null) && !suggestions.contains(launchable)) {
							suggestions.add(launchable);
						}
					}
					cursor.moveToNext();
				}
			}
			cursor.close();
		}
		return suggestions;
	}
	
	@Override
	public Launchable getLaunchable(int id) {
		return null;
	}
	
	@Override
	public boolean activate(Launchable launchable) {		
		return false;
	}
	
	public boolean registerContentObserver(ContentObserver observer) {		
		return true;
	}
	
	public boolean unregisterContentObserver(ContentObserver observer) {		
		return true;
	}	
}