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

public class MRUListComposer extends BaseAdapter {
	private static final String MRU_LIST_TABLE = "MRUList";
	
	private static final String[] MRU_LIST_PROJECTION = new String[] {
       "_ID", // 0
        "LauncherID", // 1
        "LaunchableID" // 2
    };
	private static final int ID_COLUMN_INDEX = 0;
	private static final int LAUNCHER_ID_COLUMN_INDEX = 1;
	private static final int LAUNCHABLE_ID_COLUMN_INDEX = 2;
	
	private static final int EVENT_ARG_INIT_MRU_LIST = 1;
    private static final int EVENT_ARG_ADD_LAUNCHABLE_TO_MRU_LIST_DB = 2;
	
	private static HandlerThread sHandlerThread = null;
	
	private final Quickdroid mQuickdroid;
	private final Context mContext;
	private final LayoutInflater mLayoutInflater;
	private final MRUListWorker mMRUListWorker;
	private final ArrayList<Launcher> mLaunchers;
	private final int mNumLaunchers;
	private final HashMap<Integer, Integer> mLauncherIndexes;
	private Vector<Launchable> mSuggestions = new Vector<Launchable>();
	private int mMaxMruListSize = 10;
	private boolean mCancelInitMRUList = false;
	
	private static class MRUListDatabase extends SQLiteOpenHelper {
		private static final String DB_NAME = "MRUList.db";
		private static final int DB_VERSION = 2;
		
		public MRUListDatabase(Context context) {
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
				db.execSQL("DROP TABLE IF EXISTS " + MRU_LIST_TABLE);				
			}
			db.execSQL("CREATE TABLE IF NOT EXISTS " + MRU_LIST_TABLE + " ( "
				+ "_ID INTEGER PRIMARY KEY AUTOINCREMENT, "
				+ "LauncherID INTEGER, "
				+ "LaunchableID INTEGER "
				+ ")");
		}
	}
	
	private class MRUListWorker extends Handler {
		private final AsyncMRUListWorker mAsyncMRUListWorker;
		private final MRUListDatabase mMRUListDatabase;
		
		public MRUListWorker(Context context) {
			synchronized (MRUListWorker.class) {
	            if (sHandlerThread == null) {
	            	sHandlerThread = new HandlerThread("MRUListWorker");
	            	sHandlerThread.start();
	            }
			}
			mAsyncMRUListWorker = new AsyncMRUListWorker(sHandlerThread.getLooper());
			mMRUListDatabase = new MRUListDatabase(context);
		}
		
		public void initMRUList() {
			Message msg = mAsyncMRUListWorker.obtainMessage();
			msg.arg1 = EVENT_ARG_INIT_MRU_LIST;
			msg.obj = mMRUListWorker;
			mAsyncMRUListWorker.sendMessage(msg);
		}
		
		public void addLaunchableToMRUListDB(Launchable launchable) {
			Message msg = mAsyncMRUListWorker.obtainMessage();
	        msg.arg1 = EVENT_ARG_ADD_LAUNCHABLE_TO_MRU_LIST_DB;
	        msg.obj = launchable;
	        mAsyncMRUListWorker.sendMessage(msg);
		}
		
		@Override
        public void handleMessage(Message msg) {
			int event = msg.arg1;
			Launchable launchable = (Launchable) msg.obj;
	        switch (event) {
	            case EVENT_ARG_INIT_MRU_LIST:
	            	addLaunchable(launchable, false, false);
	            	break;
	        }
		}
		
		private class AsyncMRUListWorker extends Handler {
			public AsyncMRUListWorker(Looper looper) {
				super(looper);
	        }
	
			@Override	
	        public void handleMessage(Message msg) {
				int event = msg.arg1;
	            switch (event) {
	                case EVENT_ARG_INIT_MRU_LIST:
	                {
	                	initMRUList(msg);
	                	break;
	                }
	                case EVENT_ARG_ADD_LAUNCHABLE_TO_MRU_LIST_DB:
	                {
	                	addLaunchableToMRUListDB(msg);
	                	break;
	                }
	            }
			}

			private void initMRUList(Message msg) {
				Handler handler = (Handler) msg.obj;
				SQLiteDatabase db;
				try {
					db = mMRUListDatabase.getWritableDatabase();
				} catch (SQLiteException e) {
					db = null;
				}
				if (db != null) {
					Cursor cursor = db.rawQuery("SELECT _ID, LauncherID, LaunchableID FROM " + MRU_LIST_TABLE
						+ " ORDER BY _ID DESC LIMIT " + mMaxMruListSize, null);
					if(cursor != null) {
						if(cursor.getCount() > 0) {
							cursor.moveToFirst();
							while(!cursor.isAfterLast() && !mCancelInitMRUList) {
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
			
			private void addLaunchableToMRUListDB(Message msg) {
				Launchable launchable = (Launchable) msg.obj;
				SQLiteDatabase db;
				try {
					db = mMRUListDatabase.getWritableDatabase();
				} catch (SQLiteException e) {
					db = null;
				}
				if (db != null) {
					db.execSQL("DELETE FROM " + MRU_LIST_TABLE
						+ " WHERE LauncherID = " + launchable.getLauncher().getId() + " AND LaunchableID = " + launchable.getId());
					db.execSQL("INSERT INTO " + MRU_LIST_TABLE + " (LauncherID, LaunchableID) VALUES "
						+ "('" + launchable.getLauncher().getId() + "', '" 
						+ launchable.getId() + "');");
					Cursor cursor = db.rawQuery("SELECT _ID FROM " + MRU_LIST_TABLE + " ORDER BY _ID DESC LIMIT " + mMaxMruListSize, null);
					if(cursor != null) {
						if(cursor.getCount() >= mMaxMruListSize) {
							cursor.moveToLast();
							int id = cursor.getInt(ID_COLUMN_INDEX);
							db.execSQL("DELETE FROM " + MRU_LIST_TABLE + " WHERE _ID < " + id);
						}
						cursor.close();
					}
					db.close();
				}
			}
		}
	}
	
	public MRUListComposer(Quickdroid quickdroid) {
		mQuickdroid = quickdroid;
		mContext = quickdroid;
		mLayoutInflater = LayoutInflater.from(mQuickdroid);
		mMRUListWorker = new MRUListWorker(mContext);
		mLaunchers = mQuickdroid.getLaunchers();
		mNumLaunchers = mLaunchers.size();
		mLauncherIndexes = new HashMap<Integer, Integer>(mNumLaunchers);
		
		for (int i = 0; i < mNumLaunchers; i++) {
			mLauncherIndexes.put(mLaunchers.get(i).getId(), i);
		}
		
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(quickdroid);
		String strMaxMruListSize = settings.getString(Preferences.PREF_MAX_MRU_LIST_SIZE,
			Preferences.DEFAULT_MRU_LIST_SIZE);
		try {
			mMaxMruListSize = Integer.parseInt(strMaxMruListSize);
    	} catch (NumberFormatException e) {	
    	}
    	
    	mMRUListWorker.initMRUList();
	}
	
	public void onDestroy() {
		mCancelInitMRUList = true;
	}
	
	public void addLaunchable(Launchable launchable, boolean topOfList, boolean updateMRUListDB) {
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
		if (mSuggestions.size() > mMaxMruListSize) {
			mSuggestions.setSize(mMaxMruListSize);
		}
		notifyDataSetChanged();
		if (updateMRUListDB) {
			mMRUListWorker.addLaunchableToMRUListDB(launchable);			
		}
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