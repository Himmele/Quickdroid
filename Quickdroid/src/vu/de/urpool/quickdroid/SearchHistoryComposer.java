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
import java.util.HashMap;
import java.util.Vector;
import vu.de.urpool.quickdroid.apps.AppSyncer;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class SearchHistoryComposer extends BaseAdapter {
	private static final String SEARCH_HISTORY_DB = "SearchHistory";
	
	private static final String[] SEARCH_HISTORY_PROJECTION = new String[] {
       "_ID", // 0
        "LauncherID", // 1
        "LaunchableID" // 2
    };
	private static final int ID_COLUMN_INDEX = 0;
	private static final int LAUNCHER_ID_COLUMN_INDEX = 1;
	private static final int LAUNCHABLE_ID_COLUMN_INDEX = 2;
	
	private static final int EVENT_ARG_INIT_SEARCH_HISTORY = 1;
    private static final int EVENT_ARG_ADD_LAUNCHABLE_TO_SEARCH_HISTORY = 2;
    private static final int EVENT_ARG_CLEAR_SEARCH_HISTORY = 3;
	
	private static HandlerThread sHandlerThread = null;
	
	private final Quickdroid mQuickdroid;
	private final Context mContext;
	private final LayoutInflater mLayoutInflater;
	private final SearchHistoryWorker mSearchHistoryWorker;
	private final ArrayList<Launcher> mLaunchers;
	private final int mNumLaunchers;
	private final HashMap<Integer, Integer> mLauncherIndexes;
	private Vector<Launchable> mSuggestions = new Vector<Launchable>();
	private boolean mEnableSearchHistory = true;
	private int mMaxSearchHistorySize = Integer.parseInt(Preferences.DEFAULT_SEARCH_HISTORY_SIZE);
	private boolean mCancelInitSearchHistory = false;
	
	private static class SearchHistoryDatabase extends SQLiteOpenHelper {
		private static final String DB_NAME = "SearchHistory.db";
		private static final int DB_VERSION = 2;
		
		public SearchHistoryDatabase(Context context) {
			super(context, DB_NAME, null, DB_VERSION);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			updateDatabase(db, 0, DB_VERSION);
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
			updateDatabase(db, oldVersion, newVersion);
		}
		
		private void updateDatabase(SQLiteDatabase db, int oldVersion, int newVersion) {
			if (oldVersion < 2) {
				db.execSQL("DROP TABLE IF EXISTS " + SEARCH_HISTORY_DB);				
			}
			db.execSQL("CREATE TABLE IF NOT EXISTS " + SEARCH_HISTORY_DB + " ( "
				+ "_ID INTEGER PRIMARY KEY AUTOINCREMENT, "
				+ "LauncherID INTEGER, "
				+ "LaunchableID INTEGER "
				+ ")");
		}
	}
	
	private class SearchHistoryWorker extends Handler {
		private final AsyncSearchHistoryWorker mAsyncSearchHistoryWorker;
		private final SearchHistoryDatabase mSearchHistoryDatabase;
		
		public SearchHistoryWorker(Context context) {
			synchronized (SearchHistoryWorker.class) {
	            if (sHandlerThread == null) {
	            	sHandlerThread = new HandlerThread("SearchHistoryWorker");
	            	sHandlerThread.start();
	            }
			}
			mAsyncSearchHistoryWorker = new AsyncSearchHistoryWorker(sHandlerThread.getLooper());
			mSearchHistoryDatabase = new SearchHistoryDatabase(context);
		}
		
		public void initSearchHistory() {
			Message msg = mAsyncSearchHistoryWorker.obtainMessage();
			msg.arg1 = EVENT_ARG_INIT_SEARCH_HISTORY;
			msg.obj = this;
			mAsyncSearchHistoryWorker.sendMessage(msg);
		}
		
		public void addLaunchableToSearchHistory(Launchable launchable) {
			Message msg = mAsyncSearchHistoryWorker.obtainMessage();
	        msg.arg1 = EVENT_ARG_ADD_LAUNCHABLE_TO_SEARCH_HISTORY;
	        msg.obj = launchable;
	        mAsyncSearchHistoryWorker.sendMessage(msg);
		}
		
		public void clearSearchHistory() {
			Message msg = mAsyncSearchHistoryWorker.obtainMessage();
	        msg.arg1 = EVENT_ARG_CLEAR_SEARCH_HISTORY;
	        mAsyncSearchHistoryWorker.sendMessage(msg);
		}
		
		@Override
        public void handleMessage(Message msg) {
			int event = msg.arg1;
			Launchable launchable = (Launchable) msg.obj;
	        switch (event) {
	            case EVENT_ARG_INIT_SEARCH_HISTORY:
	            	addLaunchable(launchable, false, false);
	            	break;
	        }
		}
		
		private class AsyncSearchHistoryWorker extends Handler {
			public AsyncSearchHistoryWorker(Looper looper) {
				super(looper);
	        }
	
			@Override	
	        public void handleMessage(Message msg) {
				int event = msg.arg1;
	            switch (event) {
	                case EVENT_ARG_INIT_SEARCH_HISTORY:
	                {
	                	initSearchHistory(msg);
	                	break;
	                }
	                case EVENT_ARG_ADD_LAUNCHABLE_TO_SEARCH_HISTORY:
	                {
	                	addLaunchableToSearchHistory(msg);
	                	break;
	                }
	                case EVENT_ARG_CLEAR_SEARCH_HISTORY:
	                {
	                	clearSearchHistory();
	                	break;
	                }
	            }
			}

			private void initSearchHistory(Message msg) {
				Handler handler = (Handler) msg.obj;
				SQLiteDatabase db;
				try {
					db = mSearchHistoryDatabase.getWritableDatabase();
				} catch (SQLiteException e) {
					db = null;
				}
				if (db != null) {
					Cursor cursor = db.rawQuery("SELECT _ID, LauncherID, LaunchableID FROM " + SEARCH_HISTORY_DB
						+ " ORDER BY _ID DESC LIMIT " + mMaxSearchHistorySize, null);
					if(cursor != null) {
						if(cursor.getCount() > 0) {
							cursor.moveToFirst();
							while(!cursor.isAfterLast() && !mCancelInitSearchHistory) {
								int launcherId = cursor.getInt(LAUNCHER_ID_COLUMN_INDEX);
								int launchableId = cursor.getInt(LAUNCHABLE_ID_COLUMN_INDEX);
								Integer launcherIndex = mLauncherIndexes.get(launcherId);
								if (launcherIndex != null) {	
									Launchable launchable = mLaunchers.get(launcherIndex).getLaunchable(launchableId);							
									if (launchable != null) {
										Message reply = handler.obtainMessage();
							            reply.arg1 = msg.arg1;
							            reply.obj = launchable;
							            reply.sendToTarget();							
									}
								}
								cursor.moveToNext();
							}
						}
						cursor.close();
					}
					db.close();
				}
				
				SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(mContext);
        		boolean quickLaunch = settings.getBoolean(Preferences.PREF_QUICK_LAUNCH, true);
        		if (quickLaunch) {
        			Quickdroid.activateQuickLaunch(mContext);
        		}
        		AppSyncer.start(mContext);
			}
			
			private void addLaunchableToSearchHistory(Message msg) {
				Launchable launchable = (Launchable) msg.obj;
				SQLiteDatabase db;
				try {
					db = mSearchHistoryDatabase.getWritableDatabase();
				} catch (SQLiteException e) {
					db = null;
				}
				if (db != null) {
					db.execSQL("DELETE FROM " + SEARCH_HISTORY_DB
						+ " WHERE LauncherID = " + launchable.getLauncher().getId() + " AND LaunchableID = " + launchable.getId());
					db.execSQL("INSERT INTO " + SEARCH_HISTORY_DB + " (LauncherID, LaunchableID) VALUES "
						+ "('" + launchable.getLauncher().getId() + "', '" 
						+ launchable.getId() + "');");
					Cursor cursor = db.rawQuery("SELECT _ID FROM " + SEARCH_HISTORY_DB + " ORDER BY _ID DESC LIMIT " + mMaxSearchHistorySize, null);
					if(cursor != null) {
						if(cursor.getCount() >= mMaxSearchHistorySize) {
							cursor.moveToLast();
							if (!cursor.isAfterLast()) {
								int id = cursor.getInt(ID_COLUMN_INDEX);
								db.execSQL("DELETE FROM " + SEARCH_HISTORY_DB + " WHERE _ID < " + id);
							}
						}
						cursor.close();
					}
					db.close();
				}
			}
			
			private void clearSearchHistory() {
				SQLiteDatabase db;
				try {
					db = mSearchHistoryDatabase.getWritableDatabase();
				} catch (SQLiteException e) {
					db = null;
				}
				if (db != null) {
					db.delete(SEARCH_HISTORY_DB, null, null);					
					db.close();
				}
			}
		}
	}
	
	public SearchHistoryComposer(Quickdroid quickdroid) {
		mQuickdroid = quickdroid;
		mContext = quickdroid;
		mLayoutInflater = LayoutInflater.from(mQuickdroid);
		mSearchHistoryWorker = new SearchHistoryWorker(mContext);
		mLaunchers = mQuickdroid.getLaunchers();
		mNumLaunchers = mLaunchers.size();
		mLauncherIndexes = new HashMap<Integer, Integer>(mNumLaunchers);
		
		for (int i = 0; i < mNumLaunchers; i++) {
			mLauncherIndexes.put(mLaunchers.get(i).getId(), i);
		}
		
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(quickdroid);
		mEnableSearchHistory = settings.getBoolean(Preferences.PREF_SEARCH_HISTORY, true);
		String strMaxSearchHistorySize = settings.getString(Preferences.PREF_MAX_SEARCH_HISTORY_SIZE,
			Preferences.DEFAULT_SEARCH_HISTORY_SIZE);
		try {
			mMaxSearchHistorySize = Integer.parseInt(strMaxSearchHistorySize);
    	} catch (NumberFormatException e) {	
    	}
    	
    	mSearchHistoryWorker.initSearchHistory();
	}
	
	public void onDestroy() {
		mCancelInitSearchHistory = true;
	}
	
	public void addLaunchable(Launchable launchable, boolean topOfList, boolean updateSearchHistory) {
		if (mEnableSearchHistory) {
			for (Launchable l : mSuggestions) {
				if (launchable.getId() == l.getId() &&
						launchable.getLauncher().getId() == l.getLauncher().getId()) {
					mSuggestions.remove(l);
					break;
				}
			}
			if (topOfList) {
				mSuggestions.add(0, launchable);		
			} else {
				mSuggestions.add(launchable);
			}
			if (mSuggestions.size() > mMaxSearchHistorySize) {
				mSuggestions.setSize(mMaxSearchHistorySize);
			}
			notifyDataSetChanged();
			if (updateSearchHistory) {
				mSearchHistoryWorker.addLaunchableToSearchHistory(launchable);			
			}
		}
	}
	
	public void clearSearchHistory() {
		mSuggestions.clear();
		notifyDataSetChanged();
		mSearchHistoryWorker.clearSearchHistory();
	}
	
	@Override
	public int getCount() {
		return mSuggestions.size();			
	}

	@Override
	public Object getItem(int position) {
		if (position < mSuggestions.size()) {
			return mSuggestions.get(position);
		} else {
			return null;
		}
	}

	@Override
	public long getItemId(int position) {
		return mSuggestions.get(position).getId();
	}

	private class ViewHolder {
		ImageView mThumbnail;
		TextView mLabel;
		TextView mInfoText;
    }
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		ViewHolder viewHolder;
        if (convertView == null) {
        	convertView = mLayoutInflater.inflate(R.layout.launchable, null);
        	viewHolder = new ViewHolder();
            LinearLayout textContainer = (LinearLayout) convertView.findViewById(R.id.textContainer);
            viewHolder.mThumbnail =  (ImageView) convertView.findViewById(R.id.thumbnail);
            viewHolder.mLabel = (TextView) textContainer.findViewById(R.id.label);
            viewHolder.mInfoText = (TextView) textContainer.findViewById(R.id.infoText);
            convertView.setTag(viewHolder);
        } else {                
        	viewHolder = (ViewHolder) convertView.getTag();
        }
        Launchable launchable = mSuggestions.get(position);
        if(launchable.getThumbnail() != null) {
        	viewHolder.mThumbnail.setImageDrawable(launchable.getThumbnail());
        	viewHolder.mThumbnail.setVisibility(View.VISIBLE);
        } else {
        	viewHolder.mThumbnail.setVisibility(View.GONE);
        }
        viewHolder.mLabel.setText(launchable.getLabel());
        if(launchable.getInfoText() != null) {
        	viewHolder.mInfoText.setText(launchable.getInfoText());
        	viewHolder.mInfoText.setVisibility(View.VISIBLE);	
        } else {
        	viewHolder.mInfoText.setVisibility(View.GONE);
        }
        return convertView;
	}
}