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
import android.view.KeyEvent;
import android.widget.EditText;

public class SearchTextView extends EditText {
	private Quickdroid mQuickdroid;
	private PreImeKeyInterceptor mPreImeKeyInterceptor;

	public SearchTextView(Context context) {
		super(context);		
	}

	public SearchTextView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public SearchTextView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

	void setOnBackKeyInterceptor(Quickdroid quickdroid) {
		mQuickdroid = quickdroid;
	}

	@Override
	public boolean onKeyPreIme(int keyCode, KeyEvent event) {
		if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.ECLAIR) {
			if (mPreImeKeyInterceptor == null) {
				mPreImeKeyInterceptor = new PreImeKeyInterceptor();
			}
			if (mPreImeKeyInterceptor.onKeyPreIme(keyCode, event)) {
				return true;
			}
		}	
		return super.dispatchKeyEvent(event);
	}
	
	// PreImeKeyInterceptor contains features that are only available since Android 2.0
	class PreImeKeyInterceptor {
		public boolean onKeyPreIme(int keyCode, KeyEvent event) {
			if (event.getKeyCode() == KeyEvent.KEYCODE_BACK) {			
				KeyEvent.DispatcherState state = getKeyDispatcherState();
				if (state != null) {
					if (event.getAction() == KeyEvent.ACTION_DOWN
							&& event.getRepeatCount() == 0) {
						state.startTracking(event, this);
						return true;
					} else if (event.getAction() == KeyEvent.ACTION_UP
							&& !event.isCanceled() && state.isTracking(event)) {
						if (mQuickdroid != null) {
							mQuickdroid.onInterceptBackKey();
							return true;
						}
					}
				}			
			}
			return false;
		}
	}
}