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
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.database.ContentObserver;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import vu.de.urpool.quickdroid.Launcher;
import vu.de.urpool.quickdroid.Launchable;
import vu.de.urpool.quickdroid.PatternMatchingLevel;

public class AppLauncher extends Launcher {
	private static final String NAME = "AppLauncher";
	private static final String[] APPS_PROJECTION = new String[] {
	       "_ID", // 0
	        "Label", // 1
	        "Package", // 2
	        "Class" // 3
	    };
	private static final int ID_COLUMN_INDEX = 0;
	private static final int LABEL_COLUMN_INDEX = 1;
	private static final int PACKAGE_COLUMN_INDEX = 2;
	private static final int CLASS_COLUMN_INDEX = 3;
	
	private Context mContext;
	private ContentResolver mContentResolver;
	private PackageManager mPackageManager;
	
	public AppLauncher(Context context) {
		mContext = context;
		mContentResolver = context.getContentResolver();
		mPackageManager = mContext.getPackageManager();
	}
	
	@Override
	public String getName() {
		return NAME;
	}
	
	@Override
	public ArrayList<Launchable> getSuggestions(String searchText, int patternMatchingLevel, int offset, int limit) {
		Cursor cursor = null;
		switch(patternMatchingLevel) {
		case PatternMatchingLevel.TOP:
			cursor = mContentResolver.query(AppProvider.APPS_URI, APPS_PROJECTION,
				"LOWER(Label) LIKE ?", 
				new String[] { searchText }, 
				"Label");
			break;
		case PatternMatchingLevel.HIGH:
			cursor = mContentResolver.query(AppProvider.APPS_URI, APPS_PROJECTION,
				"LOWER(Label) LIKE ? AND length(Label) > " + searchText.length(), 
				new String[] { searchText + "%" },
				"Label");
			break;
		case PatternMatchingLevel.MIDDLE:
			cursor = mContentResolver.query(AppProvider.APPS_URI, APPS_PROJECTION,
				"LOWER(Label) LIKE ? AND LOWER(Label) NOT LIKE ?", 
				new String[] { "%" + searchText + "%", searchText + "%" }, 
				"Label");
			break;
		case PatternMatchingLevel.LOW:
			String searchPattern = "";
			for(char c : searchText.toCharArray()) {
				searchPattern += "%" + c;
			}
			searchPattern += "%";
			cursor = mContentResolver.query(AppProvider.APPS_URI, APPS_PROJECTION,
				"LOWER(Label) LIKE ? AND LOWER(Label) NOT LIKE ?", 
				new String[] { searchPattern, "%" + searchText + "%" }, 
				"Label");
			break;
		default:
			break;
		}
		
		ArrayList<Launchable> suggestions = new ArrayList<Launchable>();
		if(cursor != null) {
			if(cursor.getCount() > offset) {
				cursor.moveToFirst();
				cursor.move(offset);
				int i = 0;
				while(!cursor.isAfterLast() && i++ < limit) {
					Intent intent = new Intent(Intent.ACTION_MAIN);
					intent.addCategory(Intent.CATEGORY_LAUNCHER);
					intent.setClassName(cursor.getString(PACKAGE_COLUMN_INDEX), cursor.getString(CLASS_COLUMN_INDEX));
					intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
					Drawable thumbnail = getThumbnail(cursor.getInt(ID_COLUMN_INDEX), intent);
					suggestions.add(new AppLaunchable(this, cursor.getInt(ID_COLUMN_INDEX), cursor.getString(LABEL_COLUMN_INDEX), intent, thumbnail));
					cursor.moveToNext();
				}
			}
			cursor.close(); 			
		}
		return suggestions;
	}
	
	@Override
	public Launchable getLaunchable(int id) {
		Launchable launchable = null;
		Cursor cursor = null;
		cursor = mContentResolver.query(AppProvider.APPS_URI, APPS_PROJECTION,
			"_ID = ?", 
			new String[] { String.valueOf(id) }, 
			null);
		if(cursor != null) {
			if(cursor.getCount() > 0) {
				cursor.moveToFirst();
				Intent intent = new Intent(Intent.ACTION_MAIN);
				intent.addCategory(Intent.CATEGORY_LAUNCHER);
				intent.setClassName(cursor.getString(PACKAGE_COLUMN_INDEX), cursor.getString(CLASS_COLUMN_INDEX));
				intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
				Drawable thumbnail = getThumbnail(cursor.getInt(ID_COLUMN_INDEX), intent);
				launchable = new AppLaunchable(this, cursor.getInt(ID_COLUMN_INDEX), cursor.getString(LABEL_COLUMN_INDEX), intent, thumbnail); 				
			}
			cursor.close();
		}
		return launchable;
	}
	
	@Override
	public boolean activate(Launchable launchable) {
		if(launchable instanceof AppLaunchable) {
			AppLaunchable appLaunchable = (AppLaunchable) launchable;
			List<ResolveInfo> list = mContext.getPackageManager().queryIntentActivities(appLaunchable.getIntent(), 
				PackageManager.MATCH_DEFAULT_ONLY);
			if(list.size() > 0) {
				mContext.startActivity(appLaunchable.getIntent());
				return true;
			}			
		}
		return false;
	}
	
	public boolean registerContentObserver(ContentObserver observer) {
		mContentResolver.registerContentObserver(AppProvider.APPS_URI, false, observer);
		return true;
	}
	
	public boolean unregisterContentObserver(ContentObserver observer) {
		mContentResolver.unregisterContentObserver(observer);
		return true;
	}
	
	private Drawable getThumbnail(int id, Intent intent) {
		Drawable thumbnail = null;
		try {
			// PackageManager already caches app icons
			thumbnail = mPackageManager.getActivityIcon(intent);
		} catch (NameNotFoundException e) {
		}
		return thumbnail;
	}
}