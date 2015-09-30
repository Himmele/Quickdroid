package vu.de.urpool.quickdroid.utils;

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

import vu.de.urpool.quickdroid.R;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.TypedArray;
import android.preference.EditTextPreference;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.AttributeSet;

public class NumberPickerPreference extends EditTextPreference implements TextWatcher {
	private final int mMinValue;
	private final int mMaxValue;
	
	public NumberPickerPreference(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		if (attrs != null) {
			TypedArray array = context.obtainStyledAttributes(attrs, R.styleable.quickdroid);
			mMinValue = array.getInt(R.styleable.quickdroid_minValue, Integer.MIN_VALUE);
			mMaxValue = array.getInt(R.styleable.quickdroid_maxValue, Integer.MAX_VALUE);
			array.recycle();
		} else {
			mMinValue = Integer.MIN_VALUE;
			mMaxValue = Integer.MAX_VALUE;
		}
		getEditText().addTextChangedListener(this);
	}
	
	public NumberPickerPreference(Context context, AttributeSet attrs) {
		super(context, attrs);
		if (attrs != null) {
			TypedArray array = context.obtainStyledAttributes(attrs, R.styleable.quickdroid);
			mMinValue = array.getInt(R.styleable.quickdroid_minValue, Integer.MIN_VALUE);
			mMaxValue = array.getInt(R.styleable.quickdroid_maxValue, Integer.MAX_VALUE);
			array.recycle();
		} else {
			mMinValue = Integer.MIN_VALUE;
			mMaxValue = Integer.MAX_VALUE;
		}
		getEditText().addTextChangedListener(this);
	}
	
	public NumberPickerPreference(Context context) {
		super(context);
		mMinValue = Integer.MIN_VALUE;
		mMaxValue = Integer.MAX_VALUE;
		getEditText().addTextChangedListener(this);
	}
	
	public void afterTextChanged(Editable s) {
		AlertDialog dialog = (AlertDialog) getDialog();
		// This callback is called before the dialog has been fully constructed
		if (dialog != null) {
			int maxSuggestions;
			try {
				maxSuggestions = Integer.parseInt(getEditText().getText().toString());
			} catch (NumberFormatException e) {
				maxSuggestions = -1;
			}
		    dialog.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(maxSuggestions >= mMinValue && maxSuggestions <= mMaxValue);
		}
	}
	
	public void beforeTextChanged(CharSequence s, int start, int count, int after) {
	}
	
	public void onTextChanged(CharSequence s, int start, int before, int count) {
	}
}