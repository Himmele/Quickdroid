package vu.de.urpool.quickdroid;

/*
 * Copyright (C) 20011 Daniel Himmelein
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

import android.content.Context;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.widget.ListView;

public class BouncingListView extends ListView {
	private static final int MAX_Y_OVERSCROLL_DISTANCE = 200;

	private Context mContext;
	private int mMaxOverscrollYDistance;

	public BouncingListView(Context context) {
		super(context);
		mContext = context;
		initBouncingListView();
	}

	public BouncingListView(Context context, AttributeSet attrs) {
		super(context, attrs);
		mContext = context;
		initBouncingListView();
	}

	public BouncingListView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		mContext = context;
		initBouncingListView();
	}

	private void initBouncingListView() {
		DisplayMetrics metrics = mContext.getResources().getDisplayMetrics();
		float density = metrics.density;
		mMaxOverscrollYDistance = (int) (density * MAX_Y_OVERSCROLL_DISTANCE);
	}

	@Override
	protected boolean overScrollBy(int deltaX, int deltaY, int scrollX,
			int scrollY, int scrollRangeX, int scrollRangeY,
			int maxOverScrollX, int maxOverScrollY, boolean isTouchEvent) {		
		return super.overScrollBy(deltaX, deltaY, scrollX, scrollY,
				scrollRangeX, scrollRangeY, maxOverScrollX,
				mMaxOverscrollYDistance, isTouchEvent);
	}
}