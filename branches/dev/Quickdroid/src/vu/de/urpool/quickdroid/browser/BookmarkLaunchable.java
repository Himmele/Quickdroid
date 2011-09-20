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

import vu.de.urpool.quickdroid.Launchable;
import vu.de.urpool.quickdroid.Launcher;
import android.graphics.drawable.Drawable;

public class BookmarkLaunchable extends Launchable {
	private final BookmarkLauncher mBookmarkLauncher;
	private final String mUrl;
	
	public BookmarkLaunchable(Launcher launcher, int id, String label, String url) {
		super(launcher, id, label, url);
		mBookmarkLauncher = (BookmarkLauncher) launcher;
		mUrl = url;
	}
	
	@Override
	public Drawable getThumbnail() {
		return mBookmarkLauncher.getThumbnail();
	}
	
	public String getUrl() {
		return mUrl;
	}
	
	public int hashCode() {
		return super.hashCode();
	}
	
	public boolean equals(Object o) {
		return super.equals(o);
	}
}