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
import vu.de.urpool.quickdroid.PatternMatchingLevel;
import vu.de.urpool.quickdroid.utils.ThumbnailFactory;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.provider.MediaStore;

public class ArtistLauncher extends Launcher {
	public static final String NAME = "ArtistLauncher";
	
	private static final String[] ARTISTS_PROJECTION = new String[] {
		MediaStore.Audio.Artists._ID, // 0
		MediaStore.Audio.Artists.ARTIST // 1
    };
	private static final int ID_COLUMN_INDEX = 0;
	private static final int ARTIST_COLUMN_INDEX = 1;
	
	private Context mContext;
	private ContentResolver mContentResolver;
	private Drawable mThumbnail;
	
	public ArtistLauncher(Context context) {
		mContext = context;
		mContentResolver = context.getContentResolver();
		mThumbnail = ThumbnailFactory.createThumbnail(context, context.getResources().getDrawable(R.drawable.music_artists));
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
				cursor = mContentResolver.query(MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI,
					ARTISTS_PROJECTION,
					"LOWER(" + MediaStore.Audio.Artists.ARTIST + ") LIKE ?",
					new String[] { searchText },
					MediaStore.Audio.Artists.ARTIST + " ASC");
				break;
			case PatternMatchingLevel.HIGH:
				cursor = mContentResolver.query(MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI,
					ARTISTS_PROJECTION,
					"LOWER(" + MediaStore.Audio.Artists.ARTIST + ") LIKE ? AND length("
						+ MediaStore.Audio.Artists.ARTIST + ") > " + searchText.length(),							
					new String[] { searchText + "%" },
					MediaStore.Audio.Artists.ARTIST + " ASC");
				break;
			case PatternMatchingLevel.MIDDLE:
				cursor = mContentResolver.query(MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI,
					ARTISTS_PROJECTION,
					"LOWER(" + MediaStore.Audio.Artists.ARTIST + ") LIKE ? AND LOWER(" + MediaStore.Audio.Artists.ARTIST + ") NOT LIKE ?",
					new String[] { "%" + searchText + "%", searchText + "%" },
					MediaStore.Audio.Artists.ARTIST + " ASC");
				break;
			case PatternMatchingLevel.LOW:
				String searchPattern = "";
				for(char c : searchText.toCharArray()) {
					searchPattern += "%" + c;
				}
				searchPattern += "%";
				cursor = mContentResolver.query(MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI,
					ARTISTS_PROJECTION,
					"LOWER(" + MediaStore.Audio.Artists.ARTIST + ") LIKE ? AND LOWER("
						+ MediaStore.Audio.Artists.ARTIST + ") NOT LIKE ?", 
					new String[] { searchPattern, "%" + searchText + "%" },
					MediaStore.Audio.Artists.ARTIST + " ASC");
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
 					ArtistLaunchable launchable = new ArtistLaunchable(this,
 						cursor.getInt(ID_COLUMN_INDEX),
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
		Uri uri = ContentUris.withAppendedId(MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI, id);
		Cursor cursor = mContentResolver.query(uri, ARTISTS_PROJECTION,	null, null, null);
		Launchable launchable = null;
		if(cursor != null) {
 			if(cursor.getCount() > 0) {
 				cursor.moveToFirst();
 				launchable = new ArtistLaunchable(this,
					cursor.getInt(ID_COLUMN_INDEX),
					cursor.getString(ARTIST_COLUMN_INDEX));
 			}
 			cursor.close();
		}
		return launchable;
	}
	
    @Override
	public boolean activate(Launchable launchable) {
    	if (launchable instanceof ArtistLaunchable) {
    		Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setDataAndType(Uri.EMPTY, "vnd.android.cursor.dir/album");
            intent.putExtra("artist", String.valueOf(launchable.getId()));
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
            mContext.startActivity(intent);
			return true;
    	}
    	return false;
	}
    
    public Drawable getThumbnail(Launchable launchable) {
		return mThumbnail;
	}
}