package vu.de.urpool.quickdroid.apps;

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
import java.util.List;
import android.app.Service;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.os.IBinder;

public class AppSyncer extends Service implements Runnable {
	public static final String APPS_SETTINGS = "AppsSettings";
	private static final String[] APPS_PROJECTION = new String[] {
	       "_ID", // 0
	        "Package", // 1
	        "Class" // 2
	    };
	private static final int ID_COLUMN_INDEX = 0;
	private static final int PACKAGE_COLUMN_INDEX = 1;
	private static final int CLASS_COLUMN_INDEX = 2;
	
	private ContentResolver mContentResolver;
	private Thread mSyncThread = null;
	
	@Override
    public void onCreate() {
    	super.onCreate();
    	mContentResolver = getContentResolver();
    }
	
	@Override
    public void onStart(Intent intent, int startId) {
		super.onStart(intent, startId);
    	synchronized (this) {
		    if (mSyncThread == null) {
		    	mSyncThread = new Thread(this);
		    	mSyncThread.setPriority(Thread.MIN_PRIORITY + 1);
		    	mSyncThread.start();
		    }
    	}
    }

    @Override
	public void onDestroy() {
    	synchronized (this) {
    		if (mSyncThread != null) {
    			mSyncThread.interrupt();
    			while (mSyncThread != null) {
	    			try {
	    				wait(100);
	    			} catch (InterruptedException e) {
	    			}
    			}
    		}
    	}
    	super.onDestroy();
    }
    
    @Override
	public void run() {
    	SharedPreferences settings = getSharedPreferences(APPS_SETTINGS, 0);
    	int syncState = settings.getInt("syncState", AppProvider.OUT_OF_SYNC);
		do {
			if (syncState != AppProvider.SYNC) {
				try {
					if (synchronize()) {
						SharedPreferences.Editor editor = settings.edit();
						syncState = AppProvider.SYNC;
						editor.putInt("syncState", syncState);
						editor.commit();			
					}
				} catch (Exception e) {
	            }
			}
			
			synchronized (this) {
				syncState = settings.getInt("syncState", AppProvider.OUT_OF_SYNC);
				if (syncState == AppProvider.SYNC) {
					mSyncThread = null;
					stopSelf();
					return;
				}
			}
		} while ((syncState != AppProvider.SYNC) && !mSyncThread.isInterrupted());
		
		synchronized (this) {
			mSyncThread = null;
			stopSelf();
		}
	}

	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
	
	public static final void start(Context context) {
		SharedPreferences settings = context.getSharedPreferences(AppSyncer.APPS_SETTINGS, 0);
		int syncState = settings.getInt("syncState", AppProvider.OUT_OF_SYNC);
		if (syncState == AppProvider.OUT_OF_SYNC) {
			context.startService(new Intent(context, AppSyncer.class));
		}
	}
	
	private boolean synchronize() {
		PackageManager pm = getPackageManager();
		Cursor cursor;
		
		cursor = mContentResolver.query(AppProvider.APPS_URI, APPS_PROJECTION,
			null, 
			null, 
			null);
		if (cursor != null) {
			cursor.moveToFirst();
 			while(!cursor.isAfterLast() && !mSyncThread.isInterrupted()) {
 				Intent intent = new Intent(Intent.ACTION_MAIN);
				intent.addCategory(Intent.CATEGORY_LAUNCHER);
				intent.setClassName(cursor.getString(PACKAGE_COLUMN_INDEX), cursor.getString(CLASS_COLUMN_INDEX));	
				List<ResolveInfo> list = pm.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
				if(list.size() == 0) {
					mContentResolver.delete(AppProvider.APPS_URI, "_ID = ?", new String[] { String.valueOf(cursor.getInt(ID_COLUMN_INDEX)) });
				}
 				cursor.moveToNext();
 			}
 			cursor.close();
		}
		
		if (mSyncThread.isInterrupted()) {
			return false;
		}
		
		Intent intent = new Intent(Intent.ACTION_MAIN);
		intent.addCategory(Intent.CATEGORY_LAUNCHER);
		List<ResolveInfo> launchActivities = pm.queryIntentActivities(intent,
			PackageManager.GET_ACTIVITIES);
		ArrayList<ContentValues> apps = new ArrayList<ContentValues>();
		for (ResolveInfo ri : launchActivities) {
			if (mSyncThread.isInterrupted()) {
				return false;
			}
			if (ri != null && ri.activityInfo != null && ri.activityInfo.targetActivity == null) {
				CharSequence appLabel = ri.activityInfo.loadLabel(pm);
				if (appLabel != null) {
					cursor = mContentResolver.query(AppProvider.APPS_URI, APPS_PROJECTION,
						"Label = ? AND Package = ? AND Class = ? AND Intent = ? AND Category = ?", 
						new String[] { appLabel.toString(), ri.activityInfo.packageName, ri.activityInfo.name, Intent.ACTION_MAIN, Intent.CATEGORY_LAUNCHER }, 
						null);
					if (cursor != null) {
						if (cursor.getCount() == 0) {
							ContentValues values = new ContentValues();
							values.put("label", appLabel.toString());
							values.put("package", ri.activityInfo.packageName);
							values.put("class", ri.activityInfo.name);
							values.put("intent", Intent.ACTION_MAIN);
							values.put("category", Intent.CATEGORY_LAUNCHER);
							apps.add(values);
							if (apps.size() >= 10) {
								ContentValues[] contentValues = new ContentValues[apps.size()];
								mContentResolver.bulkInsert(AppProvider.APPS_URI, apps.toArray(contentValues));
								apps.clear();
							}
						}
						cursor.close();
					}
				}
			}
		}
		
		if (mSyncThread.isInterrupted()) {
			return false;
		}
		
		if (apps.size() > 0) {
			ContentValues[] contentValues = new ContentValues[apps.size()];
			mContentResolver.bulkInsert(AppProvider.APPS_URI, apps.toArray(contentValues));
			apps.clear();
		}
		
		return true;
	}
}