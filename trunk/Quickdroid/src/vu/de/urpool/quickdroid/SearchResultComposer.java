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
import java.util.LinkedList;
import java.util.ListIterator;
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
	private final ArrayList<Searcher> mSearchers;
	private final ArrayList<Launcher> mLaunchers;
	private final int mNumLaunchers;
	private final HashMap<Integer, Integer> mLauncherIndexes;
	private final LauncherObserver mLauncherObserver;
	private LinkedList<Launchable> mSuggestions;
	private ArrayList<Launchable> mViewableSuggestions;
	private int[] mSearchResultInsertPositions;
	private String mSearchText = null;
	private boolean mClearSuggestions = false;
	private int mNumDoneSearchers = 0;
	private Launcher mFavoriteItemsLauncher;
	
	public SearchResultComposer(Quickdroid quickdroid) {
		mQuickdroid = quickdroid;
		mLayoutInflater = LayoutInflater.from(mQuickdroid);
		mLaunchers = mQuickdroid.getLaunchers();
		mNumLaunchers = mLaunchers.size();
		mLauncherIndexes = new HashMap<Integer, Integer>(mNumLaunchers);
		mLauncherObserver = new LauncherObserver(new Handler());
		mSearchers = new ArrayList<Searcher>();
		for (int i = 0; i < mNumLaunchers; i++) {
			mLauncherIndexes.put(mLaunchers.get(i).getId(), i);
			mLaunchers.get(i).registerContentObserver(mLauncherObserver);
			mSearchers.add(new Searcher(mLaunchers.get(i), this));
		}
		mFavoriteItemsLauncher = mQuickdroid.getFavoriteItemsLauncher();
		mSuggestions = new LinkedList<Launchable>();
		mViewableSuggestions = new ArrayList<Launchable>();
		mSearchResultInsertPositions = new int[mNumLaunchers * SearchPatternMatchingLevel.NUM_LEVELS];
	}
	
	public void onDestroy() {
		for (int i = 0; i < mNumLaunchers; i++) {
			mLaunchers.get(i).unregisterContentObserver(mLauncherObserver);
			mSearchers.get(i).cancel();
			mSearchers.get(i).destroy();
		}
	}
	
	public int getCount() {
		return mViewableSuggestions.size();
	}
	
	public Object getItem(int position) {
		if (position < mViewableSuggestions.size()) {
			return mViewableSuggestions.get(position);
		} else {
			return null;
		}
	}
	
	public long getItemId(int position) {
		if (position < mViewableSuggestions.size()) {
			return mViewableSuggestions.get(position).getId();
		} else {
			return -1;
		}
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
        Launchable launchable = mViewableSuggestions.get(position);
        viewHolder.mThumbnail.setImageDrawable(mQuickdroid.getResources().getDrawable(R.drawable.app_thumbnail));
        if(launchable.getThumbnail() != null) {
        	viewHolder.mThumbnail.setImageDrawable(launchable.getThumbnail().getCurrent());
        	launchable.setBadgeParent(viewHolder.mThumbnail);
        	viewHolder.mThumbnail.setTag(position);
        	viewHolder.mThumbnail.setOnClickListener(mQuickdroid.getOnThumbnailClickListener());
        	viewHolder.mThumbnail.setVisibility(View.VISIBLE);
        } else {
        	launchable.setBadgeParent(null);
        	viewHolder.mThumbnail.setTag(null);
        	viewHolder.mThumbnail.setOnClickListener(null);
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
		if (mSearchText != null && mSearchText.trim().length() > 0) {
			mSearchText = mSearchText.trim();
			mQuickdroid.setProgressBarIndeterminateVisibility(true);
			mClearSuggestions = true;
			mNumDoneSearchers = 0;
			for (int i = 0; i < mNumLaunchers; i++) {
				mSearchers.get(i).search(mSearchText.toLowerCase());
			}			
		} else {
			mSuggestions.clear();
			mViewableSuggestions.clear();
			notifyDataSetChanged();
			mQuickdroid.setProgressBarIndeterminateVisibility(false);
		}
	}
	
	public void addSuggestions(Launcher launcher, String searchText, int searchPatternMatchingLevel, ArrayList<Launchable> suggestions) {
		if (suggestions != null && suggestions.size() > 0 && searchPatternMatchingLevel >= SearchPatternMatchingLevel.CONTAINS_EACH_CHAR_OF_SEARCH_TEXT && searchPatternMatchingLevel <= SearchPatternMatchingLevel.STARTS_WITH_SEARCH_TEXT) {
			if (mClearSuggestions) {				
				mClearSuggestions = false;
				mSuggestions.clear();
			}
			
			Integer launcherIndex = mLauncherIndexes.get(launcher.getId());
			
			if (mFavoriteItemsLauncher != null) {
				if (launcher != mFavoriteItemsLauncher) {
					// Do not add new suggestions if they are already available as favorite items
					for (Launchable launchable : mSuggestions) {
						int i = 0;
						for (i = 0; i < suggestions.size(); i++) {						
							if ((launchable != null) && (launchable.equals(suggestions.get(i)))) {
								break;
							}					
						}
						if (i < suggestions.size()) {
							suggestions.remove(i);
						}
					}
				} else {
					// Do not add duplicate favorite items
				    int nextFavoriteItemPos = 0;
		            for (int i = 1; i <= (SearchPatternMatchingLevel.NUM_LEVELS - searchPatternMatchingLevel); i++) {
		                nextFavoriteItemPos += mSearchResultInsertPositions[i * mNumLaunchers - 1];
		            }
		            nextFavoriteItemPos += mSearchResultInsertPositions[(SearchPatternMatchingLevel.NUM_LEVELS - searchPatternMatchingLevel) * mNumLaunchers + launcherIndex];
				    
					for (int i = 0; i < nextFavoriteItemPos; i++) {
						Launchable launchable = mSuggestions.get(i);
						int j = 0;
						for (j = 0; j < suggestions.size(); j++) {						
							if ((launchable != null) && (launchable.equals(suggestions.get(j)))) {
								break;
							}					
						}
						if (j < suggestions.size()) {
							suggestions.remove(j);
						}
					}
					
					// Remove suggestions in favor of equipollent highly ranked favorite items
					for (Launchable launchable : suggestions) {
						int i = 0;
						for (i = 0; i < mSuggestions.size(); i++) {						
							if ((mSuggestions.get(i) != null) && (launchable.equals(mSuggestions.get(i)))) {
								break;
							}					
						}
						if (i < mSuggestions.size()) {
							mSuggestions.set(i, null);
						}
					}					
				}
			}
			
			// The mSearchResultInsertPositions array holds the insert positions for new suggestions -> sort order: pattern matching level, launcher ID
			int insertPosition = 0;
			for (int i = 1; i <= (SearchPatternMatchingLevel.NUM_LEVELS - searchPatternMatchingLevel); i++) {
				insertPosition += mSearchResultInsertPositions[i * mNumLaunchers - 1];
			}
			insertPosition += mSearchResultInsertPositions[(SearchPatternMatchingLevel.NUM_LEVELS - searchPatternMatchingLevel) * mNumLaunchers + launcherIndex];
			
			mSuggestions.addAll(insertPosition, suggestions);
			int pos = (SearchPatternMatchingLevel.NUM_LEVELS - searchPatternMatchingLevel) * mNumLaunchers + launcherIndex; 
			for (int i = pos; i < pos + mNumLaunchers - launcherIndex; i++) {
				mSearchResultInsertPositions[i] += suggestions.size();
			}
						
			mViewableSuggestions.clear();
			ListIterator<Launchable> itr = mSuggestions.listIterator();
			while (itr.hasNext()) {
				Launchable launchable = itr.next();
				if (launchable != null) {
					mViewableSuggestions.add(launchable);
				}
			}			
			if (mQuickdroid.getListView().getItemAtPosition(0) != null) {
				mQuickdroid.getListView().setSelection(0);
			}
			notifyDataSetChanged();
		}			
   	}
	
	public void onDone(Searcher searcher) {
		mNumDoneSearchers++;
		if (mNumDoneSearchers >= mSearchers.size()) {
			if (mClearSuggestions) {
				mClearSuggestions = false;
				mSuggestions.clear();
				mViewableSuggestions.clear();
				notifyDataSetChanged();
			}
			mQuickdroid.setProgressBarIndeterminateVisibility(false);
			mQuickdroid.updateSearchTextColor();
		}
	}
	
	public boolean hasSuggestions() {
		return (!mViewableSuggestions.isEmpty());
	}
	
	private final void resetSearchResultPositions() {
		Arrays.fill(mSearchResultInsertPositions, 0);
	}
	
	private class LauncherObserver extends ContentObserver {
		public LauncherObserver(Handler handler) {
			super(handler);
		}
		
		@Override
		public void onChange(boolean selfChange) {
			if (mSearchText != null && mSearchText.length() > 0) {
				search(mSearchText);				
			}
		}
	}
}