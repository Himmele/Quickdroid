package vu.de.urpool.quickdroid;

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

import android.graphics.drawable.Drawable;
import android.view.View;

public class Launchable {
	private Launcher mLauncher;
	private int mId;
	private String mLabel;
	private String mInfoText;
	private View mBadgeParent;
	
	public Launchable(Launcher launcher, int id, String label) {
		mLauncher = launcher;
		mId = id;
		mLabel = label;
	}
	
	public Launchable(Launcher launcher, int id, String label, String infoText) {
		mLauncher = launcher;
		mId = id;
		mLabel = label;
		mInfoText = infoText;
	}
	
	public Launcher getLauncher() {
		return mLauncher;
	}
	
	public int getId() {
		return mId;
	}
	
	public String getLabel() {
		return mLabel;
	}
	
	public String getInfoText() {
		return mInfoText;
	}
	
	public Drawable getThumbnail() {
		return null;
	}
	
	public boolean activate() {
		return mLauncher.activate(this);
	}
	
	public boolean activateBadge() {
		return mLauncher.activateBadge(this, mBadgeParent);
	}
	
	public void deactivate() {
		mLauncher.deactivate(this);
	}
	
	public void deactivateBadge() {
		mLauncher.deactivateBadge(this, mBadgeParent);
	}

	public View getBadgeParent() {
		return mBadgeParent;
	}

	public void setBadgeParent(View badgeParent) {
		mBadgeParent = badgeParent;
	}
	
	public int hashCode() {
		int hashCode = 17;
		hashCode = hashCode * 31 + mId;
		hashCode = hashCode * 31 + (mLauncher != null ? mLauncher.getId() : 0);
		return hashCode;
	}
	
	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		Launchable launchable = (Launchable) o;
		if (mLauncher != null ? mLauncher.getId() != launchable.getLauncher().getId() : launchable.getLauncher() != null) return false;
		if (mId != launchable.mId) return false;
		return true;
	}
}