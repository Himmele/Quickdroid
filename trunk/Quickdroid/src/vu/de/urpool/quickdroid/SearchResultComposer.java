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

import java.util.Arrays;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Vector;
import android.database.ContentObserver;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

public class SearchResultComposer extends BaseAdapter {
	private final Quickdroid mQuickdroid;
	private final LayoutInflater mLayoutInflater;
	private final LauncherObserver mLauncherObserver;
	private final ArrayList<Searcher> mSearchers;
	private final ArrayList<Launcher> mLaunchers;
	private final int mNumLaunchers;
	private final HashMap<Integer, Integer> mLauncherIndexes;
	private Vector<Launchable> mSuggestions;
	private String mSearchText = null;
	private boolean mClearSuggestions = false;
	private int mNumDoneSearchers = 0;
	private int[] mSearchResultPos;
	
	public SearchResultComposer(Quickdroid quickdroid) {
		mQuickdroid = quickdroid;
		mLayoutInflater = LayoutInflater.from(mQuickdroid);
		mLauncherObserver = new LauncherObserver(new Handler());
		mLaunchers = mQuickdroid.getLaunchers();
		mNumLaunchers = mLaunchers.size();
		mLauncherIndexes = new HashMap<Integer, Integer>(mNumLaunchers);
		mSearchers = new ArrayList<Searcher>();
		for (int i = 0; i < mNumLaunchers; i++) {
			mLauncherIndexes.put(mLaunchers.get(i).getId(), i);
			mLaunchers.get(i).registerContentObserver(mLauncherObserver);
			mSearchers.add(new Searcher(mLaunchers.get(i), this));
		}
		mSuggestions = new Vector<Launchable>();
		mSearchResultPos = new int[mNumLaunchers * PatternMatchingLevel.NUM_LEVELS];
	}
	
	public void onDestroy() {
		for (int i = 0; i < mNumLaunchers; i++) {
			mLaunchers.get(i).unregisterContentObserver(mLauncherObserver);
			mSearchers.get(i).cancel();
			mSearchers.get(i).destroy();
		}
	}
	
	public int getCount() {
		return mSuggestions.size();
	}
	
	public Object getItem(int position) {
		if (position < mSuggestions.size()) {
			return mSuggestions.get(position);
		} else {
			return null;
		}
	}
	
	public long getItemId(int position) {
		return mSuggestions.get(position).getId();
	}
	
	private class ViewHolder {
		ImageView mThumbnail;
		TextView mLabel;
		TextView mInfoText;
    }
	
	public View getView(int position, View convertView, ViewGroup parent) {
		ViewHolder viewHolder;
        if (convertView == null) {
        	convertView = mLayoutInflater.inflate(R.layout.launchable, null);
        	viewHolder = new ViewHolder();
            LinearLayout textContainer = (LinearLayout) convertView.findViewById(R.id.textContainer);
            viewHolder.mThumbnail =  (ImageView) convertView.findViewById(R.id.thumbnail);
            viewHolder.mLabel = (TextView) textContainer.findViewById(R.id.label);
            viewHolder.mInfoText = (TextView) textContainer.findViewById(R.id.infoText);
            convertView.setTag(viewHolder);
        } else {                
        	viewHolder = (ViewHolder) convertView.getTag();
        }     
        Launchable launchable = mSuggestions.get(position);
        viewHolder.mThumbnail.setImageDrawable(mQuickdroid.getResources().getDrawable(R.drawable.app_thumbnail));
        if(launchable.getThumbnail() != null) {
        	viewHolder.mThumbnail.setImageDrawable(launchable.getThumbnail().getCurrent());
        	viewHolder.mThumbnail.setVisibility(View.VISIBLE);
        } else {
        	viewHolder.mThumbnail.setVisibility(View.GONE);
        }
        viewHolder.mLabel.setText(launchable.getLabel());
        if(launchable.getInfoText() != null) {
        	viewHolder.mInfoText.setText(launchable.getInfoText());
        	viewHolder.mInfoText.setVisibility(View.VISIBLE);	
        } else {
        	viewHolder.mInfoText.setVisibility(View.GONE);
        }
        return convertView;
	}

	public final void search(final String searchText) {		
		resetSearchResultPositions();
		mSearchText = searchText;
		if (searchText != null && searchText.length() > 0) {
			mQuickdroid.setProgressBarIndeterminateVisibility(true);
			mClearSuggestions = true;
			mNumDoneSearchers = 0;
			for (int i = 0; i < mNumLaunchers; i++) {
				mSearchers.get(i).search(searchText.toLowerCase());
			}			
		} else {
			mSuggestions.clear();
			notifyDataSetChanged();
			mQuickdroid.setProgressBarIndeterminateVisibility(false);
		}
	}
	
	public void addSuggestions(Launcher launcher, String searchText, int patternMatchingLevel, ArrayList<Launchable> suggestions) {
		if (suggestions != null && suggestions.size() > 0 && patternMatchingLevel >= PatternMatchingLevel.LOW && patternMatchingLevel <= PatternMatchingLevel.TOP) {
			if (mClearSuggestions) {				
				mClearSuggestions = false;
				mSuggestions.clear();
			}
			 
			// flattened 2 dimensional array that holds the positions to insert new suggestions for each Launcher depending on its pattern matching level
			int insertPos = 0;
			Integer launcherIndex = mLauncherIndexes.get(launcher.getId());
			for (int i = 1; i <= (PatternMatchingLevel.NUM_LEVELS - patternMatchingLevel); i++) {
				insertPos += mSearchResultPos[mNumLaunchers * i - 1];
			}
			insertPos += mSearchResultPos[(PatternMatchingLevel.NUM_LEVELS - patternMatchingLevel) * mNumLaunchers + launcherIndex];
			mSuggestions.addAll(insertPos, suggestions);
			int pos = (PatternMatchingLevel.NUM_LEVELS - patternMatchingLevel) * mNumLaunchers + launcherIndex; 
			for (int i = pos; i < pos + mNumLaunchers - launcherIndex; i++) {
				mSearchResultPos[i] += suggestions.size();
			}
			
			if (mQuickdroid.getListView().getItemAtPosition(0) != null) {
				mQuickdroid.getListView().setSelection(0);
			}	
			notifyDataSetChanged();
		}			
   	}
	
	public void onDone(Searcher searcher) {
		mNumDoneSearchers++;
		if (mNumDoneSearchers == mSearchers.size()) {
			if (mClearSuggestions) {
				mClearSuggestions = false;
				mSuggestions.clear();
				notifyDataSetChanged();
			}
			mQuickdroid.setProgressBarIndeterminateVisibility(false);
			mQuickdroid.updateSearchTextColor();
		}
	}
	
	public boolean hasSuggestions() {
		return (!mSuggestions.isEmpty());
	}
	
	private final void resetSearchResultPositions() {
		Arrays.fill(mSearchResultPos, 0);
	}
	
	private class LauncherObserver extends ContentObserver {
		public LauncherObserver(Handler handler) {
			super(handler);
		}
		
		@Override
		public void onChange (boolean selfChange) {
			if (mSearchText != null && mSearchText.length() > 0) {
				search(mSearchText);				
			}
		}
	}
}