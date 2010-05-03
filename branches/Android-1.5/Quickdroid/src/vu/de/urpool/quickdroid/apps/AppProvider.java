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

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;

public class AppProvider extends ContentProvider {
	public static final Uri APPS_URI = Uri.parse("content://quickdroid/apps");
	public static final int OUT_OF_SYNC = 0;
	public static final int SYNC = 1;
	private static final UriMatcher URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);
	private static final String APPS_TABLE = "Apps";
	private static final int APPS = 1;
	
	private AppDatabase mAppDatabase;
	
	static
    {
        URI_MATCHER.addURI("quickdroid", "apps", APPS);
    }

	private static class AppDatabase extends SQLiteOpenHelper {
		private static final String DB_NAME = "Applications.db";
		private static final int DB_VERSION = 1;
		
		public AppDatabase(Context context) {
			super(context, DB_NAME, null, DB_VERSION);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL("CREATE TABLE IF NOT EXISTS " + APPS_TABLE + " ( "
	            + "_ID INTEGER PRIMARY KEY AUTOINCREMENT, "
	            + "Label TEXT, "
	            + "Package TEXT, "
	            + "Class TEXT, "
	            + "Intent TEXT, "
	            + "Category TEXT "
	            + ")");
			db.execSQL("CREATE INDEX IF NOT EXISTS AppsIndex ON Apps (Label)");
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		}
	}

	@Override
	public boolean onCreate() {
		mAppDatabase = new AppDatabase(getContext());
		return true;
	}
	
	@Override
	public String getType(Uri uri) {
		return null;
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		if (URI_MATCHER.match(uri) == APPS) {
			SQLiteDatabase db = mAppDatabase.getWritableDatabase();
			insert(db, values);
			getContext().getContentResolver().notifyChange(uri, null);
			return uri;
		} else {
			return null;			
		}
	}
	
	@Override
	public int bulkInsert(Uri uri, ContentValues[] values) {
		if (URI_MATCHER.match(uri) == APPS) {
			SQLiteDatabase db = mAppDatabase.getWritableDatabase();
			int i = 0;
			for (; i < values.length; i++) {
				insert(db, values[i]);
			}
			getContext().getContentResolver().notifyChange(uri, null);
			return i;
		} else {
			return 0;
		}
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
		if (URI_MATCHER.match(uri) == APPS) {
			SQLiteDatabase db = mAppDatabase.getReadableDatabase();
			Cursor cursor = db.query(APPS_TABLE,
				projection,
				selection, 
				selectionArgs,
				null,
				null,
				sortOrder,
				null);
			return cursor;
		} else {
			return null;			
		}
	}

	@Override
	public int update(Uri uri, ContentValues values, String whereClause, String[] whereArgs) {
		if (URI_MATCHER.match(uri) == APPS) {
			SQLiteDatabase db = mAppDatabase.getWritableDatabase();
			int numRows = db.update(APPS_TABLE, values, whereClause, whereArgs);
			getContext().getContentResolver().notifyChange(uri, null);
			return numRows;
		} else {
			return 0;
		}
	}
	
	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		if (URI_MATCHER.match(uri) == APPS) {
			SQLiteDatabase db = mAppDatabase.getWritableDatabase();
			int numDeletedItems = db.delete(APPS_TABLE, selection, selectionArgs);
			getContext().getContentResolver().notifyChange(uri, null);
			return numDeletedItems;
		} else {
			return 0;
		}
	}
	
	private void insert(SQLiteDatabase db, ContentValues values) {
		Object object = values.get("Label");
		String appLabel = (object == null ? "" : object.toString().replace("'", "''")); // escape apostrophes inside of SQL strings
		values.remove("Label");
		
		object = values.get("Package");
		String appPackage = (object == null ? "" : object.toString());
		values.remove("Package");
		
		object = values.get("Class");
		String appClass = (object == null ? "" : object.toString());
		values.remove("Class");
		
		object = values.get("Intent");
		String appIntent = (object == null ? "" : object.toString());
		values.remove("Intent");
		
		object = values.get("Category");
		String appCategory = (object == null ? "" : object.toString());
		values.remove("Category");
		
		db.execSQL("INSERT INTO " + APPS_TABLE + " (Label, Package, Class, Intent, Category) VALUES "
			+ "('" + appLabel + "', '"
			+ appPackage + "', '" 
			+ appClass + "', '"
			+ appIntent + "', '"
			+ appCategory + "');");
	}
}
