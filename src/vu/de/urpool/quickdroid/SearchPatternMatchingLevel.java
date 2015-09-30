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

public class SearchPatternMatchingLevel {
	public static final int NONE = 0;
	public static final int CONTAINS_EACH_CHAR_OF_SEARCH_TEXT = 1;
	public static final int CONTAINS_SEARCH_TEXT = 2;
	public static final int CONTAINS_WORD_THAT_STARTS_WITH_SEARCH_TEXT = 3;
	public static final int STARTS_WITH_SEARCH_TEXT = 4;
	public static final int NUM_LEVELS = 4;

	public static int next(int level) {
		switch(level) {
			case STARTS_WITH_SEARCH_TEXT:
				return CONTAINS_WORD_THAT_STARTS_WITH_SEARCH_TEXT;
			case CONTAINS_WORD_THAT_STARTS_WITH_SEARCH_TEXT:
				return CONTAINS_SEARCH_TEXT;
			case CONTAINS_SEARCH_TEXT:
				return CONTAINS_EACH_CHAR_OF_SEARCH_TEXT;
			case CONTAINS_EACH_CHAR_OF_SEARCH_TEXT:
				return NONE;
			default:
				return NONE;
		}
	}
}
