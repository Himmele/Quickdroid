package vu.de.urpool.quickdroid.utils;

/*
 * Copyright (C) 2008 The Android Open Source Project
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

import android.graphics.Bitmap.Config;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.PaintDrawable;
import android.graphics.Bitmap;
import android.graphics.Paint;
import android.graphics.PaintFlagsDrawFilter;
import android.graphics.PixelFormat;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.content.Context;

public final class ThumbnailFactory {
	private static final int THUMBNAIL_SIZE = 36; // DIPs
    private static int sThumbnailWidth = -1;
    private static int sThumbnailHeight = -1;

    private static final Rect sOldBounds = new Rect();
    private static Canvas sCanvas = new Canvas();
    
    static {
        sCanvas.setDrawFilter(new PaintFlagsDrawFilter(Paint.DITHER_FLAG,
        	Paint.FILTER_BITMAP_FLAG));
    }

    public static Drawable createThumbnail(Context context, Drawable drawable) {
    	final float scaleFactor = context.getResources().getDisplayMetrics().density;
    	int width = (int)(THUMBNAIL_SIZE * scaleFactor + 0.5f);
    	int height = (int)(THUMBNAIL_SIZE * scaleFactor + 0.5f);
    	if (sThumbnailWidth == -1) {
            sThumbnailWidth = width;
            sThumbnailHeight = height;
        }

        if (drawable instanceof PaintDrawable) {
            PaintDrawable painter = (PaintDrawable) drawable;
            painter.setIntrinsicWidth(width);
            painter.setIntrinsicHeight(height);
        }
        
        final int drawableWidth = drawable.getIntrinsicWidth();
        final int drawableHeight = drawable.getIntrinsicHeight();

        if (width > 0 && height > 0) {
            if (width < drawableWidth || height < drawableHeight) {
                final float ratio = (float) drawableWidth / drawableHeight;

                if (drawableWidth > drawableHeight) {
                    height = (int) (width / ratio);
                } else if (drawableHeight > drawableWidth) {
                    width = (int) (height * ratio);
                }

                final Bitmap.Config config = drawable.getOpacity() != PixelFormat.OPAQUE ?
                	Bitmap.Config.ARGB_8888 : Bitmap.Config.RGB_565;
                final Bitmap thumbnail = Bitmap.createBitmap(sThumbnailWidth, sThumbnailHeight, config);            
                final Canvas canvas = sCanvas;
                canvas.setBitmap(thumbnail);
                sOldBounds.set(drawable.getBounds());
                final int x = (sThumbnailWidth - width) / 2;
                final int y = (sThumbnailHeight - height) / 2;
                drawable.setBounds(x, y, x + width, y + height);
                drawable.draw(canvas);
                drawable.setBounds(sOldBounds);
                drawable = new BitmapDrawable(thumbnail);
            } else if (drawableWidth < width && drawableHeight < height) {
                final Bitmap.Config config = Bitmap.Config.ARGB_8888;
                final Bitmap thumbnail = Bitmap.createBitmap(sThumbnailWidth, sThumbnailHeight, config);
                final Canvas canvas = sCanvas;
                canvas.setBitmap(thumbnail);
                sOldBounds.set(drawable.getBounds());
                final int x = (width - drawableWidth) / 2;
                final int y = (height - drawableHeight) / 2;
                drawable.setBounds(x, y, x + drawableWidth, y + drawableHeight);
                drawable.draw(canvas);
                drawable.setBounds(sOldBounds);
                drawable = new BitmapDrawable(thumbnail);
            }
        }

        return drawable;
    }
    
    public static Drawable createThumbnail(Context context, Bitmap bitmap) {
    	if (sThumbnailWidth == -1) {
    		final float scaleFactor = context.getResources().getDisplayMetrics().density;
    		sThumbnailWidth = (int)(THUMBNAIL_SIZE * scaleFactor + 0.5f);
    		sThumbnailHeight = (int)(THUMBNAIL_SIZE * scaleFactor + 0.5f);
        }    	
    	return new BitmapDrawable(Bitmap.createScaledBitmap(bitmap, sThumbnailWidth, sThumbnailHeight, false));
    }
    
    public static Bitmap createShortcutIcon(Drawable drawable) {
        if (drawable instanceof BitmapDrawable) {
            return ((BitmapDrawable)drawable).getBitmap();
        }

        Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap); 
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);

        return bitmap;
    }
}