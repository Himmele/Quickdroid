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

import java.util.ArrayList;
import vu.de.urpool.quickdroid.apps.AppLauncher;
import vu.de.urpool.quickdroid.apps.AppProvider;
import vu.de.urpool.quickdroid.apps.AppSyncer;
import vu.de.urpool.quickdroid.browser.BookmarkLauncher;
import vu.de.urpool.quickdroid.contacts.ContactLauncher;
import vu.de.urpool.quickdroid.media.audio.AlbumLauncher;
import vu.de.urpool.quickdroid.media.audio.ArtistLauncher;
import vu.de.urpool.quickdroid.media.audio.SongLauncher;
import android.app.*;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.GestureDetector;
import android.view.GestureDetector.OnGestureListener;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.MotionEvent;
import android.widget.*;
import android.widget.AdapterView.OnItemClickListener;

public class Quickdroid extends ListActivity {
	private static final int SETTINGS = Menu.FIRST;
	private static final int QUICK_LAUNCH_THUMBNAIL_ID = 1;
	private ArrayList<Launcher> mLaunchers;
	private SearchResultComposer mSearchResultComposer;
	private MRUListComposer mMRUListComposer;
	private BaseAdapter mListAdapter;
	private EditText mSearchText;
	private Launchable mActiveLaunchable;
	private int mLauncherIndex = 0;
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);        
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        getWindow().setGravity(Gravity.TOP);
        
        View rootView = getLayoutInflater().inflate(R.layout.quickdroid, null);
        LinearLayout.LayoutParams rootLayout = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT,
    		LinearLayout.LayoutParams.FILL_PARENT);
        setContentView(rootView, rootLayout);
        
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        checkSettings(settings);
        
        mLaunchers = new ArrayList<Launcher>();
		createLaunchers(settings);
		
		mSearchText = (EditText) findViewById(R.id.searchText);
        mSearchText.setHint(R.string.searchHint);
        mSearchText.setCompoundDrawablePadding(4);
        mSearchText.setCompoundDrawablesWithIntrinsicBounds(null, null, this.getResources().getDrawable(R.drawable.search), null);
		
		mSearchResultComposer = new SearchResultComposer(this);
		mMRUListComposer = new MRUListComposer(this);
		setListAdapter(mMRUListComposer);
        
        mSearchText.addTextChangedListener(new TextWatcher() {
			@Override
			public void afterTextChanged(Editable searchText) {
			}

			@Override
			public void beforeTextChanged(CharSequence searchText, int start, int count, int after) {
			}

			@Override
			public void onTextChanged(CharSequence searchText, int start, int before, int count) {
				deactivateLaunchable();
				if (searchText.length() > 0) {
					setListAdapter(mSearchResultComposer);
					mSearchResultComposer.search(mSearchText.getText().toString());	
				} else {
					setListAdapter(mMRUListComposer);
					mSearchResultComposer.search(null);
				}
			}
        });
        
        getListView().setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
				Launchable launchable = (Launchable) mListAdapter.getItem(position);
				if(launchable != null) {
					activateLaunchable(launchable);		
				}
			}
        });
        
        SearchTextGestureDetector searchTextViewGestureDetector = 
        	new SearchTextGestureDetector(mSearchText);
        mSearchText.setOnTouchListener(searchTextViewGestureDetector);
        
        ImageButton clearSearchText = (ImageButton) findViewById(R.id.clearSearchText);
        clearSearchText.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view) {
				mSearchText.setText("");
			}
        });
    }

	private void createLaunchers(SharedPreferences settings) {
		if (settings.getBoolean(Preferences.PREF_SEARCH_APPS, Preferences.SEARCH_LAUNCHER)) {
	    	AppLauncher appLauncher = new AppLauncher(this);
	    	String strNumSuggestions = settings.getString(Preferences.PREF_APPS_NUM_SUGGESTIONS,
		    	Preferences.DEFAULT_NUM_SUGGESTIONS_10);
	    	try {
	    		int numSuggestions = Integer.parseInt(strNumSuggestions);
	    		appLauncher.setMaxSuggestions(numSuggestions);
	    	} catch (NumberFormatException e) {	
	    	}
	    	String strPatternMatchingLevel = settings.getString(Preferences.PREF_APPS_PATTERN_MATCHING_LEVEL,
		    	Preferences.DEFAULT_PATTERN_MATCHING_LEVEL);
	    	try {
	    		int patternMatchingLevel = Integer.parseInt(strPatternMatchingLevel);
	    		appLauncher.setPatternMatchingLevel(patternMatchingLevel);
	    	} catch (NumberFormatException e) {	
	    	}
	    	mLaunchers.add(mLauncherIndex++, appLauncher);
		}
		
		if (settings.getBoolean(Preferences.PREF_SEARCH_CONTACTS, Preferences.SEARCH_LAUNCHER)) {
			ContactLauncher contactLauncher = new ContactLauncher(this);
			String strNumSuggestions = settings.getString(Preferences.PREF_CONTACTS_NUM_SUGGESTIONS,
				Preferences.DEFAULT_NUM_SUGGESTIONS_10);
			try {
	    		int numSuggestions = Integer.parseInt(strNumSuggestions);
	    		contactLauncher.setMaxSuggestions(numSuggestions);
	    	} catch (NumberFormatException e) {	
	    	}
	    	String strPatternMatchingLevel = settings.getString(Preferences.PREF_CONTACTS_PATTERN_MATCHING_LEVEL,
		    	Preferences.DEFAULT_PATTERN_MATCHING_LEVEL);
	    	try {
	    		int patternMatchingLevel = Integer.parseInt(strPatternMatchingLevel);
	    		contactLauncher.setPatternMatchingLevel(patternMatchingLevel);
	    	} catch (NumberFormatException e) {	
	    	}
			mLaunchers.add(mLauncherIndex++, contactLauncher);
		}
		
		if (settings.getBoolean(Preferences.PREF_SEARCH_BOOKMARKS, Preferences.SEARCH_LAUNCHER)) {
			BookmarkLauncher bookmarkLauncher = new BookmarkLauncher(this);
			String strNumSuggestions = settings.getString(Preferences.PREF_BOOKMARKS_NUM_SUGGESTIONS,
				Preferences.DEFAULT_NUM_SUGGESTIONS_10);
			try {
	    		int numSuggestions = Integer.parseInt(strNumSuggestions);
	    		bookmarkLauncher.setMaxSuggestions(numSuggestions);
	    	} catch (NumberFormatException e) {	
	    	}
	    	String strPatternMatchingLevel = settings.getString(Preferences.PREF_BOOKMARKS_PATTERN_MATCHING_LEVEL,
		    	Preferences.DEFAULT_PATTERN_MATCHING_LEVEL);
	    	try {
	    		int patternMatchingLevel = Integer.parseInt(strPatternMatchingLevel);
	    		bookmarkLauncher.setPatternMatchingLevel(patternMatchingLevel);
	    	} catch (NumberFormatException e) {	
	    	}
			mLaunchers.add(mLauncherIndex++, bookmarkLauncher);
		}
		
		if (settings.getBoolean(Preferences.PREF_SEARCH_ARTISTS, Preferences.SEARCH_LAUNCHER)) {
			ArtistLauncher artistLauncher = new ArtistLauncher(this);
			String strNumSuggestions = settings.getString(Preferences.PREF_ARTISTS_NUM_SUGGESTIONS,
				Preferences.DEFAULT_NUM_SUGGESTIONS_4);
			try {
	    		int numSuggestions = Integer.parseInt(strNumSuggestions);
	    		artistLauncher.setMaxSuggestions(numSuggestions);
	    	} catch (NumberFormatException e) {	
	    	}
	    	String strPatternMatchingLevel = settings.getString(Preferences.PREF_ARTISTS_PATTERN_MATCHING_LEVEL,
		    	Preferences.DEFAULT_PATTERN_MATCHING_LEVEL);
	    	try {
	    		int patternMatchingLevel = Integer.parseInt(strPatternMatchingLevel);
	    		artistLauncher.setPatternMatchingLevel(patternMatchingLevel);
	    	} catch (NumberFormatException e) {	
	    	}
			mLaunchers.add(mLauncherIndex++, artistLauncher);
		}
		
		if (settings.getBoolean(Preferences.PREF_SEARCH_ALBUMS, Preferences.SEARCH_LAUNCHER)) {
			AlbumLauncher albumLauncher = new AlbumLauncher(this);
			String strNumSuggestions = settings.getString(Preferences.PREF_ALBUMS_NUM_SUGGESTIONS,
				Preferences.DEFAULT_NUM_SUGGESTIONS_4);
			try {
	    		int numSuggestions = Integer.parseInt(strNumSuggestions);
	    		albumLauncher.setMaxSuggestions(numSuggestions);
	    	} catch (NumberFormatException e) {	
	    	}
	    	String strPatternMatchingLevel = settings.getString(Preferences.PREF_ALBUMS_PATTERN_MATCHING_LEVEL,
		    	Preferences.DEFAULT_PATTERN_MATCHING_LEVEL);
	    	try {
	    		int patternMatchingLevel = Integer.parseInt(strPatternMatchingLevel);
	    		albumLauncher.setPatternMatchingLevel(patternMatchingLevel);
	    	} catch (NumberFormatException e) {	
	    	}
			mLaunchers.add(mLauncherIndex++, albumLauncher);
		}
		
		if (settings.getBoolean(Preferences.PREF_SEARCH_SONGS, Preferences.SEARCH_LAUNCHER)) {
			SongLauncher songLauncher = new SongLauncher(this);
			String strNumSuggestions = settings.getString(Preferences.PREF_SONGS_NUM_SUGGESTIONS,
				Preferences.DEFAULT_NUM_SUGGESTIONS_4);
			try {
	    		int numSuggestions = Integer.parseInt(strNumSuggestions);
	    		songLauncher.setMaxSuggestions(numSuggestions);
	    	} catch (NumberFormatException e) {	
	    	}
	    	String strPatternMatchingLevel = settings.getString(Preferences.PREF_SONGS_PATTERN_MATCHING_LEVEL,
		    	Preferences.DEFAULT_PATTERN_MATCHING_LEVEL);
	    	try {
	    		int patternMatchingLevel = Integer.parseInt(strPatternMatchingLevel);
	    		songLauncher.setPatternMatchingLevel(patternMatchingLevel);
	    	} catch (NumberFormatException e) {	
	    	}
			mLaunchers.add(mLauncherIndex++, songLauncher);
		}
	}
	
	@Override
	public void onDestroy() {
		mMRUListComposer.onDestroy();
		mSearchResultComposer.onDestroy();
		super.onDestroy();
	}
	
	public ArrayList<Launcher> getLaunchers() {
		return mLaunchers;
	}
	
	public void updateSearchTextColor() {
		if (mSearchText.getText().length() == 0 || mSearchResultComposer.hasSuggestions()) {
			mSearchText.setTextColor(Color.BLACK);
		} else {
			mSearchText.setTextColor(Color.GRAY);
		}
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuItem settings = menu.add(0, SETTINGS, 0, R.string.appSettings);
		settings.setIcon(R.drawable.settings);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case SETTINGS:
				Intent intent = new Intent(this, Preferences.class);
				startActivityForResult(intent, 42);
				return true;
		}
		return super.onOptionsItemSelected(item);
	}
	
	public void activateLaunchable(Launchable launchable) {
		mActiveLaunchable = launchable;
		if (mActiveLaunchable.activate()) {
			mMRUListComposer.addLaunchable(launchable, true, true);
		}
	}
	
	public void deactivateLaunchable() {
		if(mActiveLaunchable != null) {
			mActiveLaunchable.deactivate();
			mActiveLaunchable = null;
		}
	}
	
	private void setListAdapter(BaseAdapter listAdapter) {
		if (getListAdapter() != listAdapter) {
			mListAdapter = listAdapter;
			super.setListAdapter(mListAdapter);
	        mSearchText.requestFocus();
		}
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == 42) {
			if (data != null && data.getBooleanExtra(Preferences.PREFS_CHANGED, false)) {
				restart();
			}
		}
	}
	
	private void restart() {
		finish();
		startActivity(getIntent());
	}
	
	private void checkSettings(SharedPreferences settings) {
		int versionCode = settings.getInt("versionCode", 7);
		if (versionCode < 12) {
			if (versionCode < 8) {
				SharedPreferences.Editor editor = settings.edit();
				editor.putInt("versionCode", 8);
				editor.remove(Preferences.PREF_APPS_PATTERN_MATCHING_LEVEL);
				editor.remove(Preferences.PREF_CONTACTS_PATTERN_MATCHING_LEVEL);
				editor.remove(Preferences.PREF_BOOKMARKS_PATTERN_MATCHING_LEVEL);
				editor.remove(Preferences.PREF_ARTISTS_PATTERN_MATCHING_LEVEL);
				editor.remove(Preferences.PREF_ALBUMS_PATTERN_MATCHING_LEVEL);
				editor.remove(Preferences.PREF_SONGS_PATTERN_MATCHING_LEVEL);
				editor.commit();
				
				SharedPreferences appsSettings = getSharedPreferences(AppSyncer.APPS_SETTINGS, 0);
				SharedPreferences.Editor appsEditor = appsSettings.edit();
				appsEditor.putInt("syncState", AppProvider.OUT_OF_SYNC);
				appsEditor.commit();
			}
			
			SharedPreferences.Editor editor = settings.edit();
			editor.putInt("versionCode", 12);
			editor.commit();
		}
	}
	
	public static final void activateQuickLaunch(Context context) {
		NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);		
		Notification notification = new Notification(R.drawable.mini_app_thumbnail, null, 0);
		Intent quickdroidIntent = new Intent(context, Quickdroid.class);
		quickdroidIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		PendingIntent contentIntent = PendingIntent.getActivity(context, 0, quickdroidIntent, 0);
		notification.flags |= (Notification.FLAG_NO_CLEAR | Notification.FLAG_ONGOING_EVENT);
		notification.setLatestEventInfo(context, context.getText(R.string.appName), null, contentIntent);
		notificationManager.notify(QUICK_LAUNCH_THUMBNAIL_ID, notification);
	}
	
	public static final void deactivateQuickLaunch(Context context) {
		NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
		notificationManager.cancel(QUICK_LAUNCH_THUMBNAIL_ID);
	}
}

class SearchTextGestureDetector implements OnTouchListener, OnGestureListener {
	private final EditText mSearchText;
	private GestureDetector mGestureDetector;

	SearchTextGestureDetector(EditText searchText) {
		mSearchText = searchText;
		mGestureDetector = new GestureDetector(this);
	}
	
	@Override
	public boolean onDown(MotionEvent e) {
		return false;
	}

	@Override
	public boolean onFling(MotionEvent event1, MotionEvent event2, float velocityX, float velocityY) {
		float distX = event2.getX() - event1.getX();
		if(Math.abs(distX) >= mSearchText.getWidth() / 4) {
			mSearchText.clearFocus();
			mSearchText.setText("");
			return true;
		}
		return false;
	}

	@Override
	public void onLongPress(MotionEvent event) {
	}

	@Override
	public boolean onScroll(MotionEvent event1, MotionEvent event2, float distanceX, float distanceY) {
		return false;
	}

	@Override
	public void onShowPress(MotionEvent event) {
	}

	@Override
	public boolean onSingleTapUp(MotionEvent event) {
		return false;
	}

	@Override
	public boolean onTouch(View view, MotionEvent event) {
		return mGestureDetector.onTouchEvent(event);
	}
}