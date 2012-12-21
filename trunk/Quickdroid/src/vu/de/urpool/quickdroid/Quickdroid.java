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

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import vu.de.urpool.quickdroid.apps.AppLauncher;
import vu.de.urpool.quickdroid.apps.AppProvider;
import vu.de.urpool.quickdroid.apps.AppSyncer;
import vu.de.urpool.quickdroid.browser.BookmarkLauncher;
import vu.de.urpool.quickdroid.contacts.ContactLauncher;
import vu.de.urpool.quickdroid.contacts.OldContactLauncher;
import vu.de.urpool.quickdroid.favoriteitems.FavoriteItemsLauncher;
import vu.de.urpool.quickdroid.favoriteitems.FavoriteItemsProvider;
import vu.de.urpool.quickdroid.media.audio.AlbumLauncher;
import vu.de.urpool.quickdroid.media.audio.ArtistLauncher;
import vu.de.urpool.quickdroid.media.audio.SongLauncher;
import android.app.*;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.gesture.Gesture;
import android.gesture.GestureLibraries;
import android.gesture.GestureLibrary;
import android.gesture.GestureOverlayView;
import android.gesture.Prediction;
import android.gesture.GestureOverlayView.OnGesturePerformedListener;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.speech.RecognizerIntent;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnFocusChangeListener;
import android.view.View.OnKeyListener;
import android.view.Window;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.view.MotionEvent;
import android.widget.*;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.TextView.OnEditorActionListener;

public class Quickdroid extends ListActivity implements OnGesturePerformedListener {
	public static final String LOG_TAG = "Quickdroid";
	private static final String CLEAR_SEARCH_TEXT_APPROVAL = "clearSearchTextApproval";
	private static final int SETTINGS_MENU = Menu.FIRST;
	private static final int CLEAR_SEARCH_HISTORY = Menu.FIRST + 1;
	private static final int QUICK_LAUNCH_THUMBNAIL_ID = 1;
	private static final int SETTINGS = 17;
	private static final int VOICE_RECOGNIZER = 42;
	private ArrayList<Launcher> mLaunchers;
	private SearchResultComposer mSearchResultComposer;
	private SearchHistoryComposer mSearchHistoryComposer;
	private BaseAdapter mListAdapter;
	private SearchTextView mSearchText;
	private ImageButton mClearSearchText;
	private SharedPreferences mSettings;
	private Launchable mActiveLaunchable;
	private GestureLibrary mGestureLibrary;
	private boolean mClearSearchTextApproval;
	private OnClickListener mOnThumbnailClickListener;
	private ContentResolver mContentResolver;
	private FavoriteItemsLauncher mFavoriteItemsLauncher;
	private Handler mHandler = new Handler();
	private boolean mShowSoftKeyboard = true;
	
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        getWindow().setGravity(Gravity.TOP);
        
        View rootView = getLayoutInflater().inflate(R.layout.quickdroid, null);
        LinearLayout.LayoutParams rootLayout = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.FILL_PARENT,
    		LinearLayout.LayoutParams.FILL_PARENT);
        setContentView(rootView, rootLayout);
        
        mContentResolver = getContentResolver();
        
        mSettings = PreferenceManager.getDefaultSharedPreferences(this);
        checkSettings();
        
        mLaunchers = new ArrayList<Launcher>();
		createLaunchers();
		if (mFavoriteItemsLauncher != null) {
			mFavoriteItemsLauncher.init();
		}
		
		mSearchText = (SearchTextView) findViewById(R.id.searchText);
        mSearchText.setHint(R.string.searchHint);
		
		mSearchResultComposer = new SearchResultComposer(this);
		mSearchHistoryComposer = new SearchHistoryComposer(this);
		setListAdapter(mSearchHistoryComposer);
		
		mSearchText.setOnBackKeyInterceptor(this);
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
					mClearSearchText.setVisibility(View.VISIBLE);
				} else {
					setListAdapter(mSearchHistoryComposer);
					mSearchResultComposer.search(null);
					mClearSearchText.setVisibility(View.GONE);
				}
			}
        });
        mSearchText.setOnKeyListener(new OnKeyListener() {
			@Override
			public boolean onKey(View v, int keyCode, KeyEvent event) {
				if (keyCode == KeyEvent.KEYCODE_ENTER || 
						keyCode == KeyEvent.KEYCODE_DPAD_CENTER) {
					Launchable launchable = (Launchable) mListAdapter.getItem(0);
					if(launchable != null) {
						InputMethodManager imm = Quickdroid.this.getInputMethodManager();
						if (imm != null) {
							imm.hideSoftInputFromWindow(mSearchText.getWindowToken(), 0);
						}
						activateLaunchable(launchable);
					}
					return true;
				}
				return false;
			}
        });
        mSearchText.setOnEditorActionListener(new OnEditorActionListener() {
			@Override
			public boolean onEditorAction(TextView view, int actionId, KeyEvent event) {
				if (actionId == EditorInfo.IME_ACTION_GO ||
						(event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
					Launchable launchable = (Launchable) mListAdapter.getItem(0);
					if(launchable != null) {
						InputMethodManager imm = Quickdroid.this.getInputMethodManager();
						if (imm != null) {
							imm.hideSoftInputFromWindow(mSearchText.getWindowToken(), 0);
						}
						activateLaunchable(launchable);
					}
					return true;
				}
				return false;
			}
        });
        mSearchText.setOnFocusChangeListener(new OnFocusChangeListener() {			
			@Override
			public void onFocusChange(View view, boolean hasFocus) {
				if (hasFocus && mShowSoftKeyboard) {					
					mHandler.postDelayed(mShowInputMethodTask, 0);
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
        getListView().setOnItemLongClickListener(new OnItemLongClickListener() {
			@Override
			public boolean onItemLongClick(AdapterView<?> adapterView, View view, int position, long id) {
				if (mListAdapter == mSearchHistoryComposer) {
					final Launchable launchable = (Launchable) mListAdapter.getItem(position);
					AlertDialog.Builder builder = new AlertDialog.Builder(Quickdroid.this);
					builder.setTitle(launchable.getLabel());					
					builder.setItems(new CharSequence[] { getResources().getString(R.string.removeFromList) }, new DialogInterface.OnClickListener() {
					    public void onClick(DialogInterface dialog, int pos) {					    	
					    	mSearchHistoryComposer.removeLaunchable(launchable);
					    }
					});
					builder.setNegativeButton(R.string.cancel, null);
					AlertDialog dialog = builder.create();
					dialog.show();					
					return true;
				}
				return false;
			}
		});
        getListView().setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View view, MotionEvent event) {
				InputMethodManager imm = getInputMethodManager();
				if (imm != null) {
					imm.hideSoftInputFromWindow(mSearchText.getWindowToken(), 0);
				}
				return false;
			}
        });
        getListView().setOnFocusChangeListener(new OnFocusChangeListener() {			
			@Override
			public void onFocusChange(View view, boolean hasFocus) {
				InputMethodManager imm = getInputMethodManager();
				if (imm != null) {
					imm.hideSoftInputFromWindow(mSearchText.getWindowToken(), 0);
				}
			}
		});
        
        mOnThumbnailClickListener = new OnClickListener() {
			@Override
			public void onClick(View view) {
				int position = (Integer) view.getTag();
				Launchable launchable = (Launchable) mListAdapter.getItem(position);
				if (launchable.activateBadge()) {
					mSearchHistoryComposer.addLaunchable(launchable, true, false, true);					
				}
			}
        };
        
        if (mSettings.getBoolean(Preferences.PREF_SPEECH_RECOGNIZER, false)) {
	        ImageButton speechRecognizer = (ImageButton) findViewById(R.id.speechRecognizer);
	        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.HONEYCOMB) {
	            speechRecognizer.setImageResource(R.drawable.speech_recognizer2);
	        }
	        speechRecognizer.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View view) {
					Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH); 
					intent.putExtra(RecognizerIntent.EXTRA_PROMPT, getResources().getString(R.string.speakHint)); 
					intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_WEB_SEARCH);
					intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1);
					List<ResolveInfo> list = getPackageManager().queryIntentActivities(intent, 
						PackageManager.MATCH_DEFAULT_ONLY);
					if(list.size() > 0) {
						startActivityForResult(intent, VOICE_RECOGNIZER);
					} else {
						Toast.makeText(Quickdroid.this, R.string.speechRecognizerError, Toast.LENGTH_SHORT).show();
					}
				}
	        });
	        speechRecognizer.setClickable(true);
	        speechRecognizer.setVisibility(View.VISIBLE);
	        mSearchText.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.placeholder2, 0);
        } else {
        	mSearchText.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.placeholder1, 0);
        }
        
        mClearSearchText = (ImageButton) findViewById(R.id.clearSearchText);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.HONEYCOMB) {
            mClearSearchText.setImageResource(R.drawable.clear_search_text2);
        }
        mClearSearchText.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view) {
				mSearchText.setText("");
			}
        });
        
        SearchTextGestureDetector searchTextViewGestureDetector = 
        	new SearchTextGestureDetector(mSearchText);
        mSearchText.setOnTouchListener(searchTextViewGestureDetector);
        
        GestureOverlayView gestures = (GestureOverlayView) findViewById(R.id.gestures);
        if (mSettings.getBoolean(Preferences.PREF_GESTURE_RECOGNIZER, false)) {
	        File gesturesFile = new File(Environment.getExternalStorageDirectory() + File.separator + "gestures");
	        if (Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED) && gesturesFile.exists()) {
	        	mGestureLibrary = GestureLibraries.fromFile(gesturesFile);
	        } else {
	        	mGestureLibrary = GestureLibraries.fromRawResource(this, R.raw.gestures);
	        }
	        if (mGestureLibrary.load()) {
	        	gestures.addOnGesturePerformedListener(this);
	        	gestures.setGestureVisible(true);
	        } else {
	        	gestures.setGestureVisible(false);
	        }
        } else {
        	gestures.setGestureVisible(false);
        }
        
        mShowSoftKeyboard = mSettings.getBoolean(Preferences.PREF_SHOW_SOFT_KEYBOARD, true);
        if (!mShowSoftKeyboard) {
	        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN |
	        	WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        }
        
        if (savedInstanceState != null) {
        	if (savedInstanceState.containsKey(CLEAR_SEARCH_TEXT_APPROVAL)) {
        		mClearSearchTextApproval = savedInstanceState.getBoolean(CLEAR_SEARCH_TEXT_APPROVAL);        		
        	} else {
        		mClearSearchTextApproval = true;
        	}
		} else {
			mClearSearchTextApproval = false;
		}
    }
	
	@Override
    public void onResume() {
		super.onResume();
		if (mClearSearchTextApproval && mSettings.getBoolean(Preferences.PREF_CLEAR_SEARCH_TEXT, true)) {
			mSearchText.getEditableText().clear();
		}
		mClearSearchTextApproval = true;
		
		if (mSearchHistoryComposer.isListUpdatePending()) {			
			mSearchHistoryComposer.notifyDataSetChanged();
		}
	}
	
	@Override
    public void onSaveInstanceState(Bundle instanceState) {
		super.onSaveInstanceState(instanceState);
		if (getChangingConfigurations() != 0) {
			mClearSearchTextApproval = false;
			instanceState.putBoolean(CLEAR_SEARCH_TEXT_APPROVAL, mClearSearchTextApproval);
		}
	}
	
	@Override
	public void onDestroy() {
		mSearchHistoryComposer.onDestroy();
		mSearchResultComposer.onDestroy();
		super.onDestroy();
	}
	
	@Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus && mShowSoftKeyboard) {        	
            mHandler.postDelayed(mShowInputMethodTask, 0);
        }
    }

	private void createLaunchers() {
		int launcherIndex = 0;
		
	    if (mSettings.getBoolean(Preferences.PREF_SEARCH_FAVORITE_ITEMS, Preferences.SEARCH_LAUNCHER)) {
	    	mFavoriteItemsLauncher = new FavoriteItemsLauncher(this);
	    	String strNumSuggestions = Preferences.DEFAULT_NUM_SUGGESTIONS_4;
	    	try {
	    		int numSuggestions = Integer.parseInt(strNumSuggestions);
	    		mFavoriteItemsLauncher.setMaxSuggestions(numSuggestions);
	    	} catch (NumberFormatException e) {	
	    	}
	    	String strPatternMatchingLevel = Preferences.DEFAULT_PATTERN_MATCHING_LEVEL;
	    	try {
	    		int patternMatchingLevel = Integer.parseInt(strPatternMatchingLevel);
	    		mFavoriteItemsLauncher.setPatternMatchingLevel(patternMatchingLevel);
	    	} catch (NumberFormatException e) {	
	    	}
	    	mLaunchers.add(launcherIndex++, mFavoriteItemsLauncher);
	    }
		
		if (mSettings.getBoolean(Preferences.PREF_SEARCH_APPS, Preferences.SEARCH_LAUNCHER)) {
	    	AppLauncher appLauncher = new AppLauncher(this);
	    	String strNumSuggestions = mSettings.getString(Preferences.PREF_APPS_NUM_SUGGESTIONS,
		    	Preferences.DEFAULT_NUM_SUGGESTIONS_4);
	    	try {
	    		int numSuggestions = Integer.parseInt(strNumSuggestions);
	    		appLauncher.setMaxSuggestions(numSuggestions);
	    	} catch (NumberFormatException e) {	
	    	}
	    	String strPatternMatchingLevel = mSettings.getString(Preferences.PREF_APPS_PATTERN_MATCHING_LEVEL,
		    	Preferences.DEFAULT_PATTERN_MATCHING_LEVEL);
	    	try {
	    		int patternMatchingLevel = Integer.parseInt(strPatternMatchingLevel);
	    		appLauncher.setPatternMatchingLevel(patternMatchingLevel);
	    	} catch (NumberFormatException e) {	
	    	}
	    	mLaunchers.add(launcherIndex++, appLauncher);
		}
		
		if (mSettings.getBoolean(Preferences.PREF_SEARCH_CONTACTS, Preferences.SEARCH_LAUNCHER)) {
			Launcher contactLauncher;
			if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.ECLAIR) {
				contactLauncher = new OldContactLauncher(this);
			} else {
				contactLauncher = new ContactLauncher(this);
			}
			String strNumSuggestions = mSettings.getString(Preferences.PREF_CONTACTS_NUM_SUGGESTIONS,
				Preferences.DEFAULT_NUM_SUGGESTIONS_4);
			try {
	    		int numSuggestions = Integer.parseInt(strNumSuggestions);
	    		contactLauncher.setMaxSuggestions(numSuggestions);
	    	} catch (NumberFormatException e) {	
	    	}
	    	String strPatternMatchingLevel = mSettings.getString(Preferences.PREF_CONTACTS_PATTERN_MATCHING_LEVEL,
		    	Preferences.DEFAULT_PATTERN_MATCHING_LEVEL);
	    	try {
	    		int patternMatchingLevel = Integer.parseInt(strPatternMatchingLevel);
	    		contactLauncher.setPatternMatchingLevel(patternMatchingLevel);
	    	} catch (NumberFormatException e) {	
	    	}
			mLaunchers.add(launcherIndex++, contactLauncher);
		}
		
		if (mSettings.getBoolean(Preferences.PREF_SEARCH_BOOKMARKS, Preferences.SEARCH_LAUNCHER)) {
			BookmarkLauncher bookmarkLauncher = new BookmarkLauncher(this);
			String strNumSuggestions = mSettings.getString(Preferences.PREF_BOOKMARKS_NUM_SUGGESTIONS,
				Preferences.DEFAULT_NUM_SUGGESTIONS_4);
			try {
	    		int numSuggestions = Integer.parseInt(strNumSuggestions);
	    		bookmarkLauncher.setMaxSuggestions(numSuggestions);
	    	} catch (NumberFormatException e) {	
	    	}
	    	String strPatternMatchingLevel = mSettings.getString(Preferences.PREF_BOOKMARKS_PATTERN_MATCHING_LEVEL,
		    	Preferences.DEFAULT_PATTERN_MATCHING_LEVEL);
	    	try {
	    		int patternMatchingLevel = Integer.parseInt(strPatternMatchingLevel);
	    		bookmarkLauncher.setPatternMatchingLevel(patternMatchingLevel);
	    	} catch (NumberFormatException e) {	
	    	}
			mLaunchers.add(launcherIndex++, bookmarkLauncher);
		}
		
		boolean defEnableSearchCategory = (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.ECLAIR) ? false : true;
		
		if (mSettings.getBoolean(Preferences.PREF_SEARCH_ARTISTS, defEnableSearchCategory)) {
			if (!mSettings.contains(Preferences.PREF_SEARCH_ARTISTS)) {
				SharedPreferences.Editor editor = mSettings.edit();
				editor.putBoolean(Preferences.PREF_SEARCH_ARTISTS, defEnableSearchCategory);
				editor.commit();
			}
			ArtistLauncher artistLauncher = new ArtistLauncher(this);
			String strNumSuggestions = mSettings.getString(Preferences.PREF_ARTISTS_NUM_SUGGESTIONS,
				Preferences.DEFAULT_NUM_SUGGESTIONS_2);
			try {
	    		int numSuggestions = Integer.parseInt(strNumSuggestions);
	    		artistLauncher.setMaxSuggestions(numSuggestions);
	    	} catch (NumberFormatException e) {	
	    	}
	    	String strPatternMatchingLevel = mSettings.getString(Preferences.PREF_ARTISTS_PATTERN_MATCHING_LEVEL,
		    	Preferences.DEFAULT_PATTERN_MATCHING_LEVEL);
	    	try {
	    		int patternMatchingLevel = Integer.parseInt(strPatternMatchingLevel);
	    		artistLauncher.setPatternMatchingLevel(patternMatchingLevel);
	    	} catch (NumberFormatException e) {	
	    	}
			mLaunchers.add(launcherIndex++, artistLauncher);
		}
		
		if (mSettings.getBoolean(Preferences.PREF_SEARCH_ALBUMS, defEnableSearchCategory)) {
			if (!mSettings.contains(Preferences.PREF_SEARCH_ALBUMS)) {
				SharedPreferences.Editor editor = mSettings.edit();
				editor.putBoolean(Preferences.PREF_SEARCH_ALBUMS, defEnableSearchCategory);
				editor.commit();
			}
			AlbumLauncher albumLauncher = new AlbumLauncher(this);
			String strNumSuggestions = mSettings.getString(Preferences.PREF_ALBUMS_NUM_SUGGESTIONS,
				Preferences.DEFAULT_NUM_SUGGESTIONS_2);
			try {
	    		int numSuggestions = Integer.parseInt(strNumSuggestions);
	    		albumLauncher.setMaxSuggestions(numSuggestions);
	    	} catch (NumberFormatException e) {	
	    	}
	    	String strPatternMatchingLevel = mSettings.getString(Preferences.PREF_ALBUMS_PATTERN_MATCHING_LEVEL,
		    	Preferences.DEFAULT_PATTERN_MATCHING_LEVEL);
	    	try {
	    		int patternMatchingLevel = Integer.parseInt(strPatternMatchingLevel);
	    		albumLauncher.setPatternMatchingLevel(patternMatchingLevel);
	    	} catch (NumberFormatException e) {	
	    	}
			mLaunchers.add(launcherIndex++, albumLauncher);
		}
		
		if (mSettings.getBoolean(Preferences.PREF_SEARCH_SONGS, Preferences.DO_NOT_SEARCH_LAUNCHER)) {
			SongLauncher songLauncher = new SongLauncher(this);
			String strNumSuggestions = mSettings.getString(Preferences.PREF_SONGS_NUM_SUGGESTIONS,
				Preferences.DEFAULT_NUM_SUGGESTIONS_2);
			try {
	    		int numSuggestions = Integer.parseInt(strNumSuggestions);
	    		songLauncher.setMaxSuggestions(numSuggestions);
	    	} catch (NumberFormatException e) {	
	    	}
	    	String strPatternMatchingLevel = mSettings.getString(Preferences.PREF_SONGS_PATTERN_MATCHING_LEVEL,
		    	Preferences.DEFAULT_PATTERN_MATCHING_LEVEL);
	    	try {
	    		int patternMatchingLevel = Integer.parseInt(strPatternMatchingLevel);
	    		songLauncher.setPatternMatchingLevel(patternMatchingLevel);
	    	} catch (NumberFormatException e) {	
	    	}
			mLaunchers.add(launcherIndex++, songLauncher);
		}
	}
	
	public ArrayList<Launcher> getLaunchers() {
		return mLaunchers;
	}
	
	public void updateSearchTextColor() {
		if (mSearchText.getText().length() == 0 || mSearchResultComposer.hasSuggestions()) {
			mSearchText.setTextColor(mSearchText.getDefaultTextColor());
		} else {
			mSearchText.setTextColor(mSearchText.getCurrentHintTextColor());
		}
	}
	
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		menu.clear();
		if (getListAdapter() == mSearchHistoryComposer) {
			MenuItem clearSearchHistory = menu.add(0, CLEAR_SEARCH_HISTORY, 0, R.string.clearSearchHistory);
			clearSearchHistory.setIcon(android.R.drawable.ic_menu_delete);
		}
		MenuItem settings = menu.add(0, SETTINGS_MENU, 0, R.string.appSettings);
		settings.setIcon(android.R.drawable.ic_menu_preferences);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case SETTINGS_MENU:
				Intent intent = new Intent(this, Preferences.class);
				startActivityForResult(intent, SETTINGS);
				return true;
			case CLEAR_SEARCH_HISTORY:
				mSearchHistoryComposer.clearSearchHistory(true);
				return true;
		}
		return super.onOptionsItemSelected(item);
	}
	
	public void activateLaunchable(Launchable launchable) {
		mActiveLaunchable = launchable;
		if (mActiveLaunchable.activate()) {
			mSearchHistoryComposer.addLaunchable(launchable, true, true, true);
			
			if ((mFavoriteItemsLauncher != null) && (mSearchText.getText().length() > 0)) {
				// TODO: AsyncTask
				ContentValues values = new ContentValues();
				values.put("SearchText", mSearchText.getText().toString());
				values.put("LauncherID", launchable.getLauncher().getId());
				values.put("LaunchableID", launchable.getId());		
				mContentResolver.insert(FavoriteItemsProvider.FAVORITE_ITEMS_URI, values);
			}
			
			if (!mSettings.getBoolean(Preferences.PREF_BACK_STACK_BEHAVIOR, true)) {
				finish();
			}
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
	
	public boolean onInterceptBackKey() {
		if (mSettings.getBoolean(Preferences.PREF_BACK_KEY_HANDLING, true)) {
			InputMethodManager imm = Quickdroid.this.getInputMethodManager();
			if (imm != null) {
				if (!imm.isFullscreenMode()) {
					finish();
					return true;
				}
			}
		}
		return false;
	}
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == VOICE_RECOGNIZER) {
			if (resultCode == RESULT_OK) {
				ArrayList<String> suggestions = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS); 
				if (suggestions != null && suggestions.size() > 0) {
					Editable editableText = mSearchText.getEditableText();
					editableText.clear();
					editableText.append(suggestions.get(0));
					mClearSearchTextApproval = false;
				}
			}
		} else if (requestCode == SETTINGS) {
			if (data != null) {
				if (!data.getBooleanExtra(Preferences.PREF_SEARCH_HISTORY, true)) {
					mSearchHistoryComposer.clearSearchHistory(true);
				}
				if (data.getBooleanExtra(Preferences.PREFS_CHANGED, false)) {			
					restart();
				}
			}
		}
	}
	
	private void restart() {
		finish();
		startActivity(getIntent());
	}
	
	private void checkSettings() {
		int versionCode = mSettings.getInt("versionCode", 7);
		if (versionCode < 46) {
			SharedPreferences.Editor editor = mSettings.edit();
			if (versionCode < 8) {
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
			if (versionCode < 22) {
				editor.putInt("versionCode", 22);
				int searchHistorySize = Integer.parseInt(Preferences.DEFAULT_SEARCH_HISTORY_SIZE);
				String strMaxSearchHistorySize = mSettings.getString(Preferences.PREF_MAX_SEARCH_HISTORY_SIZE,
					Preferences.DEFAULT_SEARCH_HISTORY_SIZE);
				try {
					searchHistorySize = Integer.parseInt(strMaxSearchHistorySize);
		    	} catch (NumberFormatException e) {	
		    	}
		    	if (searchHistorySize == 0) {
		    		editor.putBoolean(Preferences.PREF_SEARCH_HISTORY, false);
		    		editor.putString(Preferences.PREF_MAX_SEARCH_HISTORY_SIZE, Preferences.DEFAULT_SEARCH_HISTORY_SIZE);
		    		editor.commit();
		    	}
		    	
		    	SharedPreferences appsSettings = getSharedPreferences(AppSyncer.APPS_SETTINGS, 0);
				SharedPreferences.Editor appsEditor = appsSettings.edit();
				appsEditor.putInt("syncState", AppProvider.OUT_OF_SYNC);
				appsEditor.commit();
			}
			editor.putInt("versionCode", 46);
			editor.commit();
		}
	}
	
	public static final void activateQuickLaunch(Context context) {
		NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);		
		Notification notification = new Notification(R.drawable.app_thumbnail, null, 0);
		Intent quickdroidIntent = new Intent(context, Quickdroid.class);
		quickdroidIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
		PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, quickdroidIntent, 0);
		notification.flags |= (Notification.FLAG_NO_CLEAR | Notification.FLAG_ONGOING_EVENT);
		notification.setLatestEventInfo(context, context.getText(R.string.appName), context.getText(R.string.appDesc), pendingIntent);
		notificationManager.notify(QUICK_LAUNCH_THUMBNAIL_ID, notification);
	}
	
	public static final void deactivateQuickLaunch(Context context) {
		NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
		notificationManager.cancel(QUICK_LAUNCH_THUMBNAIL_ID);
	}
	
	public void onGesturePerformed(GestureOverlayView overlay, Gesture gesture) {
		ArrayList<Prediction> predictions = mGestureLibrary.recognize(gesture);
		if (predictions.size() > 0) {
			Prediction prediction = predictions.get(0);
			if (prediction.score > 1.0) {
				Editable searchText = mSearchText.getText();				
				if ("del_one_char".equals(prediction.name)) {
					if (searchText.length() > 0) {
						searchText.delete(searchText.length() - 1, searchText.length());
					}
				} else if ("clear_search_text".equals(prediction.name)) {
					searchText.clear();
				} else {
					if (prediction.name.startsWith("=")) {
						searchText.clear();
						searchText.append(prediction.name.substring(1));
					} else {
						searchText.append(prediction.name);
					}
				}
			}
		}
	}
	
	public OnClickListener getOnThumbnailClickListener() {
		return mOnThumbnailClickListener;
	}
	
	public FavoriteItemsLauncher getFavoriteItemsLauncher() {
		return mFavoriteItemsLauncher;
	}
	
	private InputMethodManager getInputMethodManager() {
        return (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
    }
	
	private final Runnable mShowInputMethodTask = new Runnable() {
        public void run() {
        	InputMethodManager imm = getInputMethodManager();
			if (imm != null) {
				imm.showSoftInput(mSearchText, 0);
			}
        }
    };
}