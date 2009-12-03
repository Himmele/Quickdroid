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
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import android.os.Handler;
import android.os.Message;

public class Searcher extends Handler {
	public static final int MIN_CORE_POOL_SIZE = 1;
    private static final int MAX_POOL_SIZE = 5;
    private static final int MAX_QUEUE_SIZE = 10;
    private static final int KEEP_ALIVE = 10;

    private static final AtomicInteger sNumSearchers = new AtomicInteger(0);
    
    private static final BlockingQueue<Runnable> sWorkQueue =
    	new ArrayBlockingQueue<Runnable>(MAX_QUEUE_SIZE);

    private static final ThreadFactory sThreadFactory = new ThreadFactory() {
        private final AtomicInteger mCount = new AtomicInteger(1);

        public Thread newThread(Runnable runnable) {
        	Thread thread = new Thread(runnable, "Searcher #" + mCount.getAndIncrement());
        	return thread;
        }
    };

    private static final ThreadPoolExecutor sExecutor = new ThreadPoolExecutor(MIN_CORE_POOL_SIZE,
    	MAX_POOL_SIZE, KEEP_ALIVE, TimeUnit.SECONDS, sWorkQueue, sThreadFactory, new ThreadPoolExecutor.DiscardOldestPolicy());

	private static final int MAX_SUGGESTIONS_PER_QUERY = 8;
    private static final int EVENT_ARG_PUBLISH_SUGGESTIONS = 1;

	private final Launcher mLauncher;
	private final SearchResultComposer mSearchResultComposer;
	private AsyncSearcher mAsyncSearcher;
	private String mSearchText;
	private int mNumSuggestions;
	private int mPatternMatchingLevel;
	private int mOffset;
	
	private class AsyncSearcher implements Runnable {
		private SearchResult mSearchResult;
		private String mSearchText;
		private int mPatternMatchingLevel;
		private int mOffset;
		private int mLimit;
		
		public AsyncSearcher(String searchText, int patternMatchingLevel, int offset, int limit) {
			mSearchResult = new SearchResult();
			mSearchText = searchText;
			mPatternMatchingLevel = patternMatchingLevel;
			mOffset = offset;
			mLimit = limit;
			mSearchResult.searchText = mSearchText;
			mSearchResult.patternMatchingLevel = mPatternMatchingLevel;
		}
		
		@Override
		public void run() {
			mSearchResult.suggestions = mLauncher.getSuggestions(mSearchText, mPatternMatchingLevel, mOffset, mLimit);
			Message msg = obtainMessage();
			msg.arg1 = EVENT_ARG_PUBLISH_SUGGESTIONS;
			msg.obj = mSearchResult;
			msg.sendToTarget();
		}
		
		public void cancel() {
			mSearchResult.searchText = null;
		}
	}
	
	private class SearchResult {
		public String searchText;
		public int patternMatchingLevel;
        public ArrayList<Launchable> suggestions;
    }
		
	public Searcher(Launcher launcher, SearchResultComposer searchResultComposer) {
		int numSearchers = sNumSearchers.incrementAndGet();
		setThreadPoolSize(numSearchers + 1);
		mLauncher = launcher;
		mSearchResultComposer = searchResultComposer;
	}
	
	public void destroy() {
		int numSearchers = sNumSearchers.decrementAndGet();
		setThreadPoolSize(numSearchers + 1);
		// post an empty runnable to the thread pool in order to shrink its size immediately
		sExecutor.execute(new Runnable() {
			@Override
			public void run() {
			}
		});
	}
	
	public void search(final String searchText) {
		if (mAsyncSearcher != null) {
			sExecutor.remove(mAsyncSearcher);
			mAsyncSearcher.cancel();
			mAsyncSearcher = null;
		}
		mSearchText = searchText;
		mNumSuggestions = 0;
		mPatternMatchingLevel = PatternMatchingLevel.TOP;
		mOffset = 0;
		doSearch();
	}
	
	public void cancel() {
		mSearchText = null;
		if (mAsyncSearcher != null) {
			sExecutor.remove(mAsyncSearcher);
			mAsyncSearcher.cancel();
			mAsyncSearcher = null;
		}
	}
	
	protected void doSearch() {
		mAsyncSearcher = new AsyncSearcher(mSearchText, mPatternMatchingLevel, mOffset, mLauncher.getMaxSuggestions() - mNumSuggestions);
        sExecutor.execute(mAsyncSearcher);
	}

	@Override
    public void handleMessage(Message msg) {
		SearchResult searchResult = (SearchResult) msg.obj;
        int event = msg.arg1;     
		if (event == EVENT_ARG_PUBLISH_SUGGESTIONS) {
        	if (searchResult.searchText != null) {
        		mSearchResultComposer.addSuggestions(mLauncher, searchResult.searchText, searchResult.patternMatchingLevel, searchResult.suggestions);
        		int numSuggestions = (searchResult.suggestions != null) ? searchResult.suggestions.size() : 0;
        		mNumSuggestions += numSuggestions;
        		if (numSuggestions < MAX_SUGGESTIONS_PER_QUERY) {
        			mPatternMatchingLevel = PatternMatchingLevel.nextLowerLevel(mPatternMatchingLevel);
        			mOffset = 0;
        		} else {
        			mOffset += numSuggestions;
        		}        		
        		boolean done = (mPatternMatchingLevel < mLauncher.getPatternMatchingLevel() || mNumSuggestions >= mLauncher.getMaxSuggestions());
        		if (!done) {
        			doSearch();
        		} else {
        			mSearchResultComposer.onDone(this);
        		}
        	}
        }
	}
	
	private static void setThreadPoolSize(int size) {
		int corePoolSize = (size < MIN_CORE_POOL_SIZE) ? MIN_CORE_POOL_SIZE : size;
		corePoolSize = (corePoolSize > MAX_POOL_SIZE) ? MAX_POOL_SIZE : corePoolSize;
		sExecutor.setCorePoolSize(corePoolSize);
	}
}