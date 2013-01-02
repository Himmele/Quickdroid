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

import java.util.ArrayList;
import android.database.ContentObserver;
import android.view.View;

public abstract class Launcher {
	private int mId;
	private int mMaxSuggestions;
	private int mPatternMatchingLevel;
	
	public Launcher() {
		mId = getName().hashCode();
		mMaxSuggestions = 10;
		mPatternMatchingLevel = SearchPatternMatchingLevel.CONTAINS_EACH_CHAR_OF_SEARCH_TEXT;
	}
	
	public abstract String getName();

	public abstract ArrayList<Launchable> getSuggestions(String intentFilter, int patternMatchingLevel, int offset, int limit);
	
	public abstract Launchable getLaunchable(int id);
	
	public boolean activate(Launchable launchable) {
		return false;
	}
	
	public boolean activateBadge(Launchable launchable, View badgeParent) {
		return activate(launchable);
	}
	
	public void deactivate(Launchable launchable) {
	}
	
	public void deactivateBadge(Launchable launchable, View badgeParent) {
		deactivate(launchable);
	}
	
	public int getId() {
		return mId;
	}
	
	public boolean registerContentObserver(ContentObserver observer) {
		return false;
	}
	
	public boolean unregisterContentObserver(ContentObserver observer) {
		return false;
	}
	
	public void setMaxSuggestions(int numSuggestions) {
		mMaxSuggestions = numSuggestions;
	}
	
	public int getMaxSuggestions() {
		return mMaxSuggestions;
	}
	
	public void setPatternMatchingLevel(int level) {
		mPatternMatchingLevel = level;
	}
	
	public int getPatternMatchingLevel() {
		return mPatternMatchingLevel;
	}
}