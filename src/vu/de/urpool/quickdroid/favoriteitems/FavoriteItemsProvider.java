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

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.net.Uri;

public class FavoriteItemsProvider extends ContentProvider {
	private static final String AUTHORITY = "vu.de.urpool.quickdroid.favoriteitems.FavoriteItemsProvider";
	private static final String FAVORITE_ITEMS_TABLE_NAME = "favoriteItems";
	private static final int FAVORITE_ITEMS = 1;
	public static final Uri FAVORITE_ITEMS_URI = Uri.parse("content://" + AUTHORITY + "/" + FAVORITE_ITEMS_TABLE_NAME);
	public static final int OUT_OF_SYNC = 0;
	public static final int SYNC = 1;
	private static final UriMatcher URI_MATCHER = new UriMatcher(UriMatcher.NO_MATCH);
	
	private FavoriteItemsDatabase mFavoriteItemsDatabase;
	
	static {
        URI_MATCHER.addURI(AUTHORITY, FAVORITE_ITEMS_TABLE_NAME, FAVORITE_ITEMS);
    }

	private static class FavoriteItemsDatabase extends SQLiteOpenHelper {
		private static final String DB_NAME = "FavoriteItems.db";
		private static final int DB_VERSION = 1;
		
		public FavoriteItemsDatabase(Context context) {
			super(context, DB_NAME, null, DB_VERSION);
		}

		@Override
		public void onCreate(SQLiteDatabase db) {
			db.execSQL("CREATE TABLE IF NOT EXISTS " + FAVORITE_ITEMS_TABLE_NAME + " ( "
	            + "_ID INTEGER PRIMARY KEY AUTOINCREMENT, "
	            + "SearchText TEXT, "
	            + "LauncherID INTEGER, "
	            + "LaunchableID INTEGER, "
	            + "Counter INTEGER "
	            + ")");
		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		}
	}

	@Override
	public boolean onCreate() {
		mFavoriteItemsDatabase = new FavoriteItemsDatabase(getContext());
		return true;
	}
	
	@Override
	public String getType(Uri uri) {
		return null;
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		if (URI_MATCHER.match(uri) == FAVORITE_ITEMS) {
			SQLiteDatabase db = mFavoriteItemsDatabase.getWritableDatabase();
			insert(db, values);
			getContext().getContentResolver().notifyChange(uri, null);
			return uri;
		} else {
			return null;			
		}
	}
	
	@Override
	public int bulkInsert(Uri uri, ContentValues[] values) {
		if (URI_MATCHER.match(uri) == FAVORITE_ITEMS) {
			SQLiteDatabase db = mFavoriteItemsDatabase.getWritableDatabase();
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
		if (URI_MATCHER.match(uri) == FAVORITE_ITEMS) {
			SQLiteDatabase db = mFavoriteItemsDatabase.getReadableDatabase();
			Cursor cursor = db.query(FAVORITE_ITEMS_TABLE_NAME,
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
		if (URI_MATCHER.match(uri) == FAVORITE_ITEMS) {
			SQLiteDatabase db = mFavoriteItemsDatabase.getWritableDatabase();
			int numRows = db.update(FAVORITE_ITEMS_TABLE_NAME, values, whereClause, whereArgs);
			getContext().getContentResolver().notifyChange(uri, null);
			return numRows;
		} else {
			return 0;
		}
	}
	
	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		if (URI_MATCHER.match(uri) == FAVORITE_ITEMS) {
			SQLiteDatabase db = mFavoriteItemsDatabase.getWritableDatabase();
			int numDeletedItems = db.delete(FAVORITE_ITEMS_TABLE_NAME, selection, selectionArgs);
			getContext().getContentResolver().notifyChange(uri, null);
			return numDeletedItems;
		} else {
			return 0;
		}
	}
	
	private void insert(SQLiteDatabase db, ContentValues values) {	
		String searchText = values.getAsString("SearchText").replace("'", "''"); // escape apostrophes inside of SQL strings		
		values.remove("SearchText");
		int launcherId = values.getAsInteger("LauncherID");		
		values.remove("LauncherID");
		int launchableId = values.getAsInteger("LaunchableID");		
		values.remove("LaunchableID");
				
		int id = -1;
		int counter = -1;
		Cursor cursor = db.rawQuery("SELECT _ID, Counter FROM " + FAVORITE_ITEMS_TABLE_NAME + " WHERE SearchText = ? AND LauncherID = ? AND LaunchableID = ?",
				new String[] { searchText, String.valueOf(launcherId),  String.valueOf(launchableId)});
		if (cursor != null) {			
			cursor.moveToFirst();
			if (!cursor.isAfterLast()) {
				id = cursor.getInt(0);
				counter = cursor.getInt(1);
			}
			cursor.close();
		}
		if (id >= 0) {
			db.execSQL("UPDATE " + FAVORITE_ITEMS_TABLE_NAME + " SET Counter = " + counter * 2 + " WHERE SearchText = '" + searchText + "' AND _ID = " + id);
		}
		db.execSQL("UPDATE " + FAVORITE_ITEMS_TABLE_NAME + " SET Counter = min(Counter - 1, 64) WHERE SearchText = '" + searchText + "'");
		db.execSQL("DELETE FROM " + FAVORITE_ITEMS_TABLE_NAME + " WHERE Counter <= 0");
		if (id < 0) {
			db.execSQL("INSERT INTO " + FAVORITE_ITEMS_TABLE_NAME + " (SearchText, LauncherID, LaunchableID, Counter) VALUES "
					+ "('" + searchText + "', "
					+ launcherId + ", "
					+ launchableId + ", "
					+ "4)");
		}
	}
}