package vu.de.urpool.quickdroid.media.audio;

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
import vu.de.urpool.quickdroid.Launchable;
import vu.de.urpool.quickdroid.Launcher;
import vu.de.urpool.quickdroid.R;
import vu.de.urpool.quickdroid.SearchPatternMatchingLevel;
import vu.de.urpool.quickdroid.utils.ThumbnailFactory;
import android.app.SearchManager;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Toast;

public class AlbumLauncher extends Launcher {
	public static final String NAME = "AlbumLauncher";
	
	private static final String[] ALBUMS_PROJECTION = new String[] {
		MediaStore.Audio.Albums._ID, // 0
		MediaStore.Audio.Albums.ARTIST, // 1
		MediaStore.Audio.Albums.ALBUM // 2	
    };
	private static final int ID_COLUMN_INDEX = 0;
	private static final int ARTIST_COLUMN_INDEX = 1;
	private static final int ALBUM_COLUMN_INDEX = 2;
	
	private Context mContext;
	private ContentResolver mContentResolver;
	private Drawable mThumbnail;
	
	public AlbumLauncher(Context context) {
		mContext = context;
		mContentResolver = context.getContentResolver();
		mThumbnail = ThumbnailFactory.createThumbnail(context, context.getResources().getDrawable(R.drawable.music_albums));
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
				cursor = mContentResolver.query(MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
					ALBUMS_PROJECTION,
					"LOWER(" + MediaStore.Audio.Albums.ALBUM + ") LIKE ?",
					new String[] { searchText + "%" },
					MediaStore.Audio.Albums.ALBUM + " ASC");
				break;
			case SearchPatternMatchingLevel.CONTAINS_WORD_THAT_STARTS_WITH_SEARCH_TEXT:
				cursor = mContentResolver.query(MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
					ALBUMS_PROJECTION,
					"LOWER(" + MediaStore.Audio.Albums.ALBUM + ") LIKE ?",
					new String[] { "% " + searchText + "%" },
					MediaStore.Audio.Albums.ALBUM + " ASC");
				break;
			case SearchPatternMatchingLevel.CONTAINS_SEARCH_TEXT:
				cursor = mContentResolver.query(MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
					ALBUMS_PROJECTION,
					"LOWER(" + MediaStore.Audio.Albums.ALBUM + ") LIKE ?" +
						" AND LOWER(" + MediaStore.Audio.Albums.ALBUM + ") NOT LIKE ?" +
						" AND LOWER(" + MediaStore.Audio.Albums.ALBUM + ") NOT LIKE ?",
					new String[] { "%" + searchText + "%", searchText + "%", "% " + searchText + "%" }, 
					MediaStore.Audio.Albums.ALBUM + " ASC");
				break;
			case SearchPatternMatchingLevel.CONTAINS_EACH_CHAR_OF_SEARCH_TEXT:
				String searchPattern = "";
				for(char c : searchText.toCharArray()) {
					searchPattern += "%" + c;
				}
				searchPattern += "%";
				cursor = mContentResolver.query(MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
					ALBUMS_PROJECTION,
					"LOWER(" + MediaStore.Audio.Albums.ALBUM + ") LIKE ?" + 
						" AND LOWER(" + MediaStore.Audio.Albums.ALBUM + ") NOT LIKE ?",
					new String[] { searchPattern, "%" + searchText + "%" },
					MediaStore.Audio.Albums.ALBUM + " ASC");
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
 					AlbumLaunchable launchable = new AlbumLaunchable(this,
 						cursor.getInt(ID_COLUMN_INDEX),
 						cursor.getString(ALBUM_COLUMN_INDEX),
 						cursor.getString(ARTIST_COLUMN_INDEX));
 					suggestions.add(launchable);
 					cursor.moveToNext();
 				}
 			}
 			cursor.close();
 		}
		return suggestions;
	}
	
	@Override
	public Launchable getLaunchable(int id) {
		Uri uri = ContentUris.withAppendedId(MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI, id);
		Cursor cursor = mContentResolver.query(uri, ALBUMS_PROJECTION,	null, null, null);
		Launchable launchable = null;
		if(cursor != null) {
 			if(cursor.getCount() > 0) {
 				cursor.moveToFirst();
 				launchable = new AlbumLaunchable(this,
					cursor.getInt(ID_COLUMN_INDEX),
					cursor.getString(ALBUM_COLUMN_INDEX),
					cursor.getString(ARTIST_COLUMN_INDEX));
 			}
 			cursor.close();
		}
		return launchable;
	}
	
    @Override
	public boolean activate(Launchable launchable) {
    	if (launchable instanceof AlbumLaunchable) {
	    	if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.GINGERBREAD) {   	
	    		return activateByMediaSearchIntent(launchable);
	    	} else {
	    		return activateByPickIntent(launchable);
	    	}
    	}
    	return false;
	}

	private boolean activateByMediaSearchIntent(Launchable launchable) {
		Intent intent;		
		String query = "";    		          
		intent = new Intent();
		intent.setAction(MediaStore.INTENT_ACTION_MEDIA_SEARCH);
		intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET); 	
		query = launchable.getLabel();
		intent.putExtra(MediaStore.EXTRA_MEDIA_ALBUM, launchable.getLabel());
//		query = query + " " + launchable.getInfoText();
//		intent.putExtra(MediaStore.EXTRA_MEDIA_ARTIST, launchable.getInfoText());
		intent.putExtra(MediaStore.EXTRA_MEDIA_FOCUS, MediaStore.Audio.Albums.ENTRY_CONTENT_TYPE);
		intent.putExtra(SearchManager.QUERY, query);
		try {
			mContext.startActivity(intent);
		} catch (Exception ex) {
			Toast.makeText(mContext, "Sorry: Cannot launch \"" + launchable.getLabel() + "\"", Toast.LENGTH_SHORT).show();
			Log.e(mContext.getResources().getString(R.string.appName), ex.getMessage());
			return false;
		}	    		
		return true;
	}

	private boolean activateByPickIntent(Launchable launchable) {
		Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setDataAndType(Uri.EMPTY, "vnd.android.cursor.dir/track");
        intent.putExtra("album", String.valueOf(launchable.getId()));
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
        try {
        	mContext.startActivity(intent);
        } catch (Exception e) {
        	return activateByMediaSearchIntent(launchable);
        }
        return true;
	}
    
    public Drawable getThumbnail(Launchable launchable) {
		return mThumbnail;
	}
}
