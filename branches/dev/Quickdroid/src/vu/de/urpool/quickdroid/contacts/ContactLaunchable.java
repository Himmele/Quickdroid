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

import android.graphics.drawable.Drawable;
import android.net.Uri;
import vu.de.urpool.quickdroid.Launchable;
import vu.de.urpool.quickdroid.Launcher;

public class ContactLaunchable extends Launchable {
	private final ContactLauncher mContactLauncher;
	private int mPresenceStatus;
	private Uri mLookupUri;
	
	public ContactLaunchable(Launcher launcher, int id, String label, int presenceStatus, Uri lookupUri) {
		super(launcher, id, label);
		mPresenceStatus = presenceStatus;
		mLookupUri = lookupUri;
		mContactLauncher = (ContactLauncher) launcher;
	}
	
	@Override
	public Drawable getThumbnail() {
		return mContactLauncher.getThumbnail(this);
	}
	
	public int getPresenceStatus() {
		return mPresenceStatus;
	}
	
	public Uri getLookupUri() {
		return mLookupUri;
	}
	
	public boolean equals(Object o) {
		return super.equals(o);
	}
}
