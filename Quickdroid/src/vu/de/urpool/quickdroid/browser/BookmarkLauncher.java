package vu.de.urpool.quickdroid.browser;

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
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.provider.Browser;
import android.util.Log;
import android.widget.Toast;
import vu.de.urpool.quickdroid.Launchable;
import vu.de.urpool.quickdroid.Launcher;
import vu.de.urpool.quickdroid.PatternMatchingLevel;
import vu.de.urpool.quickdroid.R;
import vu.de.urpool.quickdroid.utils.ThumbnailFactory;

public class BookmarkLauncher extends Launcher {
	public static final String NAME = "BookmarkLauncher";
	private static final String[] BOOKMARKS_PROJECTION = new String[] {
        Browser.BookmarkColumns._ID, // 0
        Browser.BookmarkColumns.TITLE, // 1
        Browser.BookmarkColumns.URL // 2
    };
	private static final int ID_COLUMN_INDEX = 0;
	private static final int TITLE_COLUMN_INDEX = 1;
	private static final int URL_COLUMN_INDEX = 2;
	
    private Context mContext;
    private ContentResolver mContentResolver;
    private Drawable mBookmarkThumbnail;
	
	public BookmarkLauncher(Context context) {
		mContext = context;
		mContentResolver = context.getContentResolver();
		mBookmarkThumbnail = ThumbnailFactory.createThumbnail(context, context.getResources().getDrawable(R.drawable.browser_bookmark));
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
				cursor = mContentResolver.query(Browser.BOOKMARKS_URI, 
					BOOKMARKS_PROJECTION,
					Browser.BookmarkColumns.BOOKMARK + " == 1" + 
						" AND LOWER(" + Browser.BookmarkColumns.TITLE + ") LIKE ?", 
					new String[] { searchText + "%" },
					Browser.BookmarkColumns.TITLE + " ASC");
				break;
			case PatternMatchingLevel.HIGH:
				cursor = mContentResolver.query(Browser.BOOKMARKS_URI,
					BOOKMARKS_PROJECTION, 
					Browser.BookmarkColumns.BOOKMARK + " == 1" +
						" AND LOWER(" + Browser.BookmarkColumns.TITLE + ") LIKE ?", 
					new String[] { "% " + searchText + "%" },
					Browser.BookmarkColumns.TITLE + " ASC");
				break;
			case PatternMatchingLevel.MIDDLE:
				cursor = mContentResolver.query(Browser.BOOKMARKS_URI,
					BOOKMARKS_PROJECTION,
					Browser.BookmarkColumns.BOOKMARK + " == 1" +
						" AND LOWER(" + Browser.BookmarkColumns.TITLE + ") LIKE ?" +
						" AND LOWER(" + Browser.BookmarkColumns.TITLE + ") NOT LIKE ?" +
						" AND LOWER(" + Browser.BookmarkColumns.TITLE + ") NOT LIKE ?",
					new String[] { "%" + searchText + "%", searchText + "%", "% " + searchText + "%" }, 
					Browser.BookmarkColumns.TITLE + " ASC");
				break;
			case PatternMatchingLevel.LOW:
				String searchPattern = "";
				for(char c : searchText.toCharArray()) {
					searchPattern += "%" + c;
				}
				searchPattern += "%";
				cursor = mContentResolver.query(Browser.BOOKMARKS_URI, 
					BOOKMARKS_PROJECTION,
					Browser.BookmarkColumns.BOOKMARK + " == 1" +
						" AND LOWER(" + Browser.BookmarkColumns.TITLE + ") LIKE ?" +
						" AND LOWER(" + Browser.BookmarkColumns.TITLE + ") NOT LIKE ?",
					new String[] { searchPattern, "%" + searchText + "%" }, 
					Browser.BookmarkColumns.TITLE + " ASC");
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
 					BookmarkLaunchable bookmarkLaunchable = new BookmarkLaunchable(this,
 						cursor.getInt(ID_COLUMN_INDEX),
 						cursor.getString(TITLE_COLUMN_INDEX),
 						cursor.getString(URL_COLUMN_INDEX));
 					suggestions.add(bookmarkLaunchable);
 					cursor.moveToNext();
 				}
 			}
 			cursor.close(); 			
 		}
		return suggestions;
	}
	
	@Override
	public Launchable getLaunchable(int id) {		
		Uri uri = ContentUris.withAppendedId(Browser.BOOKMARKS_URI, id);
		Cursor cursor = mContentResolver.query(uri, BOOKMARKS_PROJECTION, null, null, null);
		Launchable launchable = null;
		if(cursor != null) {
 			if(cursor.getCount() > 0) {
 				cursor.moveToFirst();
 				launchable = new BookmarkLaunchable(this,
					cursor.getInt(ID_COLUMN_INDEX),
					cursor.getString(TITLE_COLUMN_INDEX),
					cursor.getString(URL_COLUMN_INDEX));
 			}
 			cursor.close();
		}
		return launchable;
	}
    
    @Override
	public boolean activate(Launchable launchable) {
    	if(launchable instanceof BookmarkLaunchable) {
    		Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(((BookmarkLaunchable) launchable).getUrl()));
    		intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
    		try {
            	mContext.startActivity(intent);
            } catch (Exception e) {
            	Toast.makeText(mContext, "Sorry: Cannot launch \"" + launchable.getLabel() + "\"", Toast.LENGTH_SHORT).show();
            	Log.e(mContext.getResources().getString(R.string.appName), e.getMessage());
            	return false;
            }
			return true;
    	}
    	return true;
	}
    
    public Drawable getThumbnail() {
		return mBookmarkThumbnail;
	}
}