package vu.de.urpool.quickdroid.contacts;

/*
 * Copyright (C) 2010 Daniel Himmelein
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

import java.io.InputStream;
import java.util.ArrayList;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.provider.ContactsContract.QuickContact;
import android.util.Log;
import android.view.View;
import android.widget.Toast;
import vu.de.urpool.quickdroid.Launchable;
import vu.de.urpool.quickdroid.Launcher;
import vu.de.urpool.quickdroid.Preferences;
import vu.de.urpool.quickdroid.R;
import vu.de.urpool.quickdroid.PatternMatchingLevel;
import vu.de.urpool.quickdroid.utils.ThumbnailFactory;

public class ContactLauncher extends Launcher {
	public static final String NAME = "ContactLauncher";
	private static final String NAME_COLUMN = ContactsContract.Contacts.DISPLAY_NAME;
	private static final String PRESENCE_STATUS = ContactsContract.Contacts.CONTACT_PRESENCE;
	private static final String LOOKUP_KEY = ContactsContract.Contacts.LOOKUP_KEY;
	private static final String VISIBILITY = ContactsContract.Contacts.IN_VISIBLE_GROUP;
	private static final String[] CONTACTS_PROJECTION = new String[] {
		ContactsContract.Contacts._ID, // 0
        NAME_COLUMN, // 1
        PRESENCE_STATUS, // 2
        LOOKUP_KEY, // 3
        VISIBILITY // 4
    };
	private static final int ID_COLUMN_INDEX = 0;
	private static final int NAME_COLUMN_INDEX = 1;
	private static final int PRESENCE_STATUS_COLUMN_INDEX = 2;
	private static final int LOOKUP_KEY_COLUMN_INDEX = 3;
	private static final int VISIBILITY_COLUMN_INDEX = 4;
	
	private static final class PresenceStatus {
		private static final int OFFLINE = 0;
		private static final int INVISIBLE = 1;
		private static final int AWAY = 2;
		private static final int IDLE = 3;
		private static final int BUSY = 4;
		private static final int ONLINE = 5;
	}
    
	private static final Uri MY_CONTACTS = ContactsContract.Contacts.CONTENT_URI;
	
    private Context mContext;
    private ContentResolver mContentResolver;
    private boolean mUseContactPhotos;
	private Drawable mContactDefaultThumbnail;
	private Drawable mContactInvisibleThumbnail;
	private Drawable mContactAwayThumbnail;
	private Drawable mContactBusyThumbnail;
	private Drawable mContactOnlineThumbnail;
	
	public ContactLauncher(Context context) {
		mContext = context;
		mContentResolver = context.getContentResolver();
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
		if (settings.getBoolean(Preferences.PREF_CONTACT_PHOTOS, false)) {
			mUseContactPhotos = true;
		} else {
			mUseContactPhotos = false;
		}
		mContactDefaultThumbnail = ThumbnailFactory.createThumbnail(context, context.getResources().getDrawable(R.drawable.contact_launcher));
		mContactInvisibleThumbnail = ThumbnailFactory.createThumbnail(context, context.getResources().getDrawable(R.drawable.contact_invisible));
		mContactAwayThumbnail = ThumbnailFactory.createThumbnail(context, context.getResources().getDrawable(R.drawable.contact_away));
		mContactBusyThumbnail = ThumbnailFactory.createThumbnail(context, context.getResources().getDrawable(R.drawable.contact_busy));
		mContactOnlineThumbnail = ThumbnailFactory.createThumbnail(context, context.getResources().getDrawable(R.drawable.contact_online));
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
				cursor = mContentResolver.query(MY_CONTACTS,
					CONTACTS_PROJECTION, 
					"LOWER(" + ContactsContract.Contacts.DISPLAY_NAME + ") LIKE ?", 
					new String[] { searchText + "%" },
					ContactsContract.Contacts.DISPLAY_NAME + " ASC");
				break;
			case PatternMatchingLevel.HIGH:
				cursor = mContentResolver.query(MY_CONTACTS,
					CONTACTS_PROJECTION, 
					"LOWER(" + ContactsContract.Contacts.DISPLAY_NAME + ") LIKE ?", 
					new String[] { "% " + searchText + "%" },
					ContactsContract.Contacts.DISPLAY_NAME + " ASC");
				break;
			case PatternMatchingLevel.MIDDLE:
				cursor = mContentResolver.query(MY_CONTACTS,
					CONTACTS_PROJECTION,
					"LOWER(" + ContactsContract.Contacts.DISPLAY_NAME + ") LIKE ?" +
						" AND LOWER(" + ContactsContract.Contacts.DISPLAY_NAME + ") NOT LIKE ?" +
						" AND LOWER(" + ContactsContract.Contacts.DISPLAY_NAME + ") NOT LIKE ?",
					new String[] { "%" + searchText + "%", searchText + "%", "% " + searchText + "%" },
					ContactsContract.Contacts.DISPLAY_NAME + " ASC");
				break;
			case PatternMatchingLevel.LOW:
				String searchPattern = "";
				for(char c : searchText.toCharArray()) {
					searchPattern += "%" + c;
				}
				searchPattern += "%";
				cursor = mContentResolver.query(MY_CONTACTS, 
					CONTACTS_PROJECTION,
					"LOWER(" + ContactsContract.Contacts.DISPLAY_NAME + ") LIKE ?" +
						" AND LOWER(" + ContactsContract.Contacts.DISPLAY_NAME + ") NOT LIKE ?",
					new String[] { searchPattern, "%" + searchText + "%" },  
					ContactsContract.Contacts.DISPLAY_NAME + " ASC");
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
 				while (!cursor.isAfterLast() && i < limit) {
 					if (cursor.getInt(VISIBILITY_COLUMN_INDEX) != 0) {
	 					ContactLaunchable contactLaunchable = new ContactLaunchable(this,
	 						cursor.getInt(ID_COLUMN_INDEX),
	 						cursor.getString(NAME_COLUMN_INDEX),
	 						cursor.getInt(PRESENCE_STATUS_COLUMN_INDEX),
	 						ContactsContract.Contacts.getLookupUri(cursor.getInt(ID_COLUMN_INDEX),
	 							cursor.getString(LOOKUP_KEY_COLUMN_INDEX)));
	 					suggestions.add(contactLaunchable);
	 					i++;
 					}
 					cursor.moveToNext();
 				}
 			}
 			cursor.close();
 		}
		return suggestions;
	}
	
	@Override
	public Launchable getLaunchable(int id) {		
		Uri uri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, id);
		Cursor cursor = mContentResolver.query(uri, CONTACTS_PROJECTION, null, null, null);
		Launchable launchable = null;
		if(cursor != null) {
 			if(cursor.getCount() > 0) {
 				cursor.moveToFirst();
 				launchable = new ContactLaunchable(this,
					cursor.getInt(ID_COLUMN_INDEX),
					cursor.getString(NAME_COLUMN_INDEX),
					cursor.getInt(PRESENCE_STATUS_COLUMN_INDEX),
					ContactsContract.Contacts.getLookupUri(cursor.getInt(ID_COLUMN_INDEX),
 						cursor.getString(LOOKUP_KEY_COLUMN_INDEX)));
 			}
 			cursor.close();
		}
		return launchable;
	}
    
    @Override
	public boolean activate(Launchable launchable) {
    	if(launchable instanceof ContactLaunchable) {
    		Intent intent = new Intent(Intent.ACTION_VIEW, ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, launchable.getId()));
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
    	return false;
	}
    
    @Override
	public boolean activateBadge(Launchable launchable, View badgeParent) {
    	if((launchable instanceof ContactLaunchable) && (badgeParent != null)) {
    		ContactsContract.QuickContact.showQuickContact(mContext,
    			badgeParent,
    			((ContactLaunchable)launchable).getLookupUri(),
				QuickContact.MODE_MEDIUM,
				null);
			return true;
    	}
    	return false;
	}
    
    public Drawable getThumbnail(ContactLaunchable launchable) {
    	if (mUseContactPhotos) {
		    Uri uri = ContentUris.withAppendedId(ContactsContract.Contacts.CONTENT_URI, launchable.getId());
		    InputStream contactPhoto = ContactsContract.Contacts.openContactPhotoInputStream(mContentResolver, uri);
		    if (contactPhoto == null) {
		         return mContactDefaultThumbnail;
		    }
		    return ThumbnailFactory.createThumbnail(mContext, BitmapFactory.decodeStream(contactPhoto));
    	} else {
    		switch (launchable.getPresenceStatus()) {
			case PresenceStatus.INVISIBLE:
				return mContactInvisibleThumbnail;
			case PresenceStatus.AWAY:
			case PresenceStatus.IDLE:
				return mContactAwayThumbnail;
			case PresenceStatus.BUSY:
				return mContactBusyThumbnail;
			case PresenceStatus.ONLINE:
				return mContactOnlineThumbnail;
			case PresenceStatus.OFFLINE:
			default:
				return mContactDefaultThumbnail;
			}
    	}
	}
}