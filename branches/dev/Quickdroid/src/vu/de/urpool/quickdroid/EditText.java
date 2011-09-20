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

public class EditText extends android.widget.EditText {
	private Quickdroid mQuickdroid;
	
	public EditText(Context context) {
		super(context);		
	}

	public EditText(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	public EditText(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}
	
	void setOnEditTextBackPressedListener(Quickdroid quickdroid) {
		mQuickdroid = quickdroid;
	}

	@Override
	public boolean onKeyPreIme(int keyCode, KeyEvent event) {
		if (event.getKeyCode() == KeyEvent.KEYCODE_BACK) {			
			if (mQuickdroid != null) {
				mQuickdroid.onEditTextBackPressed();
				return true;
			}
		}
		return super.dispatchKeyEvent(event);
	}
}