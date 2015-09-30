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

import vu.de.urpool.quickdroid.Launchable;
import vu.de.urpool.quickdroid.Launcher;
import android.graphics.drawable.Drawable;

public class AlbumLaunchable extends Launchable {
	private final AlbumLauncher mMusicLauncher;
	
	public AlbumLaunchable(Launcher launcher, int id, String label, String infoText) {
		super(launcher, id, label, infoText);
		mMusicLauncher = (AlbumLauncher) launcher;
	}
	
	@Override
	public Drawable getThumbnail() {
		return mMusicLauncher.getThumbnail(this);
	}
	
	public int hashCode() {
		return super.hashCode();
	}
	
	public boolean equals(Object o) {
		return super.equals(o);
	}
}
