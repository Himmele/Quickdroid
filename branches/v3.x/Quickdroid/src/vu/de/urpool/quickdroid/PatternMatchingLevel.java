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

public class PatternMatchingLevel {
	public static final int NONE = 0;
	public static final int LOW = 1; // contain each char of the search text
	public static final int MIDDLE = 2; // contain the search text
	public static final int HIGH = 3; // start at some place with the search text
	public static final int TOP = 4; // start with the search text
	public static final int NUM_LEVELS = 4;

	public static int nextLowerLevel(int level) {
		switch(level) {
			case TOP:
				return HIGH;
			case HIGH:
				return MIDDLE;
			case MIDDLE:
				return LOW;
			case LOW:
				return NONE;
			default:
				return NONE;
		}
	}
}
