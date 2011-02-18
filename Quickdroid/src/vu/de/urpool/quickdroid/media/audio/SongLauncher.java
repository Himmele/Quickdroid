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
import java.util.List;
import vu.de.urpool.quickdroid.Launchable;
import vu.de.urpool.quickdroid.Launcher;
import vu.de.urpool.quickdroid.R;
import vu.de.urpool.quickdroid.PatternMatchingLevel;
import vu.de.urpool.quickdroid.utils.ThumbnailFactory;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.provider.MediaStore;

public class SongLauncher extends Launcher {
	public static final String NAME = "SongLauncher";
	
	private static final String[] SONG_PROJECTION = new String[] {
		MediaStore.Audio.Media._ID, // 0
		MediaStore.Audio.Media.ARTIST, // 1
		MediaStore.Audio.Media.ALBUM, // 2
		MediaStore.Audio.Media.TITLE // 3
	};
	private static final int ID_COLUMN_INDEX = 0;
	private static final int ARTIST_COLUMN_INDEX = 1;
	private static final int ALBUM_COLUMN_INDEX = 2;
	private static final int TITLE_COLUMN_INDEX = 3;
	
	private Context mContext;
	private ContentResolver mContentResolver;
	private Drawable mThumbnail;
	
	public SongLauncher(Context context) {
		mContext = context;
		mContentResolver = context.getContentResolver();
		mThumbnail = ThumbnailFactory.createThumbnail(context, context.getResources().getDrawable(R.drawable.music_tracks));
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
				cursor = mContentResolver.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
					SONG_PROJECTION,
					MediaStore.Audio.Media.IS_MUSIC + " = 1" +
					 " AND LOWER(" + MediaStore.Audio.Media.TITLE + ") LIKE ?",
					new String[] { searchText + "%" },
					MediaStore.Audio.Media.TITLE + " ASC");
				break;
			case PatternMatchingLevel.HIGH:
				cursor = mContentResolver.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
					SONG_PROJECTION,
					MediaStore.Audio.Media.IS_MUSIC + " = 1" +
					 " AND LOWER(" + MediaStore.Audio.Media.TITLE + ") LIKE ?",
					new String[] { "% " + searchText + "%" },
					MediaStore.Audio.Media.TITLE + " ASC");
				break;
			case PatternMatchingLevel.MIDDLE:
				cursor = mContentResolver.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
					SONG_PROJECTION,
					MediaStore.Audio.Media.IS_MUSIC + " = 1" +
						" AND LOWER(" + MediaStore.Audio.Media.TITLE + ") LIKE ?" +
						" AND LOWER(" + MediaStore.Audio.Media.TITLE + ") NOT LIKE ?" +
						" AND LOWER(" + MediaStore.Audio.Media.TITLE + ") NOT LIKE ?", 
					new String[] { "%" + searchText + "%", searchText + "%", "% " + searchText + "%" },
					MediaStore.Audio.Media.TITLE + " ASC");
				break;
			case PatternMatchingLevel.LOW:
				String searchPattern = "";
				for(char c : searchText.toCharArray()) {
					searchPattern += "%" + c;
				}
				searchPattern += "%";
				cursor = mContentResolver.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
					SONG_PROJECTION,
					MediaStore.Audio.Media.IS_MUSIC + " = 1" +
						" AND LOWER(" + MediaStore.Audio.Media.TITLE + ") LIKE ?" +
						" AND LOWER(" + MediaStore.Audio.Media.TITLE + ") NOT LIKE ?",
					new String[] { searchPattern, "%" + searchText + "%" }, 
					MediaStore.Audio.Media.TITLE + " ASC");
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
 					SongLaunchable launchable = new SongLaunchable(this,
						cursor.getInt(ID_COLUMN_INDEX),
						cursor.getString(TITLE_COLUMN_INDEX), cursor.getString(ARTIST_COLUMN_INDEX) 
							+ " - " + cursor.getString(ALBUM_COLUMN_INDEX));
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
		Uri uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id);
		Cursor cursor = mContentResolver.query(uri, SONG_PROJECTION,	null, null, null);
		Launchable launchable = null;
		if(cursor != null) {
 			if(cursor.getCount() > 0) {
 				cursor.moveToFirst();
 				launchable = new SongLaunchable(this,
					cursor.getInt(ID_COLUMN_INDEX),
					cursor.getString(TITLE_COLUMN_INDEX), cursor.getString(ARTIST_COLUMN_INDEX) + " - " + cursor.getString(ALBUM_COLUMN_INDEX));
 			}
 			cursor.close();
		}
		return launchable;
	}
	
    @Override
	public boolean activate(Launchable launchable) {
    	if (launchable instanceof SongLaunchable) {
    		Intent intent = new Intent(Intent.ACTION_VIEW,
				ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, launchable.getId()));
			intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
			List<ResolveInfo> list = mContext.getPackageManager().queryIntentActivities(intent, 
				PackageManager.MATCH_DEFAULT_ONLY);
			if(list.size() > 0) {
				mContext.startActivity(intent);
			}
			return true;
    	}
    	return false;
	}
    
    public Drawable getThumbnail(Launchable launchable) {
		return mThumbnail;
	}
}
