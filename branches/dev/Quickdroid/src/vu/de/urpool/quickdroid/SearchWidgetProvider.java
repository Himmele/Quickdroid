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

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

public class SearchWidgetProvider extends AppWidgetProvider {
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
    	for (int i = 0; i < appWidgetIds.length; i++) {
    		int appWidgetId = appWidgetIds[i];
	        Intent intent = new Intent(context, Quickdroid.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                    | Intent.FLAG_ACTIVITY_CLEAR_TOP
                    | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);       
	        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);
	        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.search_widget);
	        views.setOnClickPendingIntent(R.id.search_widget_thumbnail, pendingIntent);
	        views.setOnClickPendingIntent(R.id.search_widget_text, pendingIntent);
	        appWidgetManager.updateAppWidget(appWidgetId, views);
    	}
    }
}