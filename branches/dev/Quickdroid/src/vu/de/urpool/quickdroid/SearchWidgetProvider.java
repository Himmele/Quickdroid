package vu.de.urpool.quickdroid;

/*
 * Copyright (C) 2009 The Android Open Source Project
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
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

public class SearchWidgetProvider extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {        
        String action = intent.getAction();
        if (AppWidgetManager.ACTION_APPWIDGET_ENABLED.equals(action)) {
        	updateWidget(context);
        } else if (AppWidgetManager.ACTION_APPWIDGET_UPDATE.equals(action)) {
        	updateWidget(context);
        }
    }   
    
    public void updateWidget(Context context) {        
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.search_widget);        
        Intent intent = new Intent(context, Quickdroid.class);        
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_CLEAR_TOP
                | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);       
//        setOnClickActivityIntent(context, views, R.id.search_widget_text, intent);
//        setOnClickActivityIntent(context, views, R.id.search_widget_thumbnail, intent);
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        appWidgetManager.updateAppWidget(0, views);
    }
    
    private void setOnClickActivityIntent(Context context, RemoteViews views, int viewId, Intent intent) {
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);
        views.setOnClickPendingIntent(viewId, pendingIntent);
    }
}