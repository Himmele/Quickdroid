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
	private static final int THUMBNAIL_SIZE = 36;
    private static int sThumbnailWidth = -1;
    private static int sThumbnailHeight = -1;

    private static final Rect sOldBounds = new Rect();
    private static Canvas sCanvas = new Canvas();
    
    static {
        sCanvas.setDrawFilter(new PaintFlagsDrawFilter(Paint.DITHER_FLAG,
        	Paint.FILTER_BITMAP_FLAG));
    }

    public static Drawable createThumbnail(Context context, Drawable drawable) {
        int width = THUMBNAIL_SIZE;
        int height = THUMBNAIL_SIZE;
    	if (sThumbnailWidth == -1) {
            sThumbnailWidth = width;
            sThumbnailHeight = height;
        }

        final int thumbnailWidth = drawable.getIntrinsicWidth();
        final int thumbnailHeight = drawable.getIntrinsicHeight();

        if (drawable instanceof PaintDrawable) {
            PaintDrawable painter = (PaintDrawable) drawable;
            painter.setIntrinsicWidth(width);
            painter.setIntrinsicHeight(height);
        }

        if (width > 0 && height > 0) {
            if (width < thumbnailWidth || height < thumbnailHeight) {
                final float ratio = (float) thumbnailWidth / thumbnailHeight;

                if (thumbnailWidth > thumbnailHeight) {
                    height = (int) (width / ratio);
                } else if (thumbnailHeight > thumbnailWidth) {
                    width = (int) (height * ratio);
                }

                final Bitmap.Config config = drawable.getOpacity() != PixelFormat.OPAQUE ?
                	Bitmap.Config.ARGB_8888 : Bitmap.Config.RGB_565;
                final Bitmap thumb = Bitmap.createBitmap(sThumbnailWidth, sThumbnailHeight, config);
                final Canvas canvas = sCanvas;
                canvas.setBitmap(thumb);
                sOldBounds.set(drawable.getBounds());
                final int x = (sThumbnailWidth - width) / 2;
                final int y = (sThumbnailHeight - height) / 2;
                drawable.setBounds(x, y, x + width, y + height);
                drawable.draw(canvas);
                drawable.setBounds(sOldBounds);
                drawable = new BitmapDrawable(thumb);
            } else if (thumbnailWidth < width && thumbnailHeight < height) {
                final Bitmap.Config config = Bitmap.Config.ARGB_8888;
                final Bitmap thumb = Bitmap.createBitmap(sThumbnailWidth, sThumbnailHeight, config);
                final Canvas canvas = sCanvas;
                canvas.setBitmap(thumb);
                sOldBounds.set(drawable.getBounds());
                final int x = (width - thumbnailWidth) / 2;
                final int y = (height - thumbnailHeight) / 2;
                drawable.setBounds(x, y, x + thumbnailWidth, y + thumbnailHeight);
                drawable.draw(canvas);
                drawable.setBounds(sOldBounds);
                drawable = new BitmapDrawable(thumb);
            }
        }

        return drawable;
    }
    
    public static Drawable createThumbnail(Context context, Bitmap bitmap) {
    	return new BitmapDrawable(Bitmap.createScaledBitmap(bitmap, THUMBNAIL_SIZE, THUMBNAIL_SIZE, false));
    }
}