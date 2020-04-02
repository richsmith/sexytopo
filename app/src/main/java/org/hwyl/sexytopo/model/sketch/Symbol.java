package org.hwyl.sexytopo.model.sketch;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import org.hwyl.sexytopo.R;


public enum Symbol {

    STALAGMITE(R.drawable.stalagmite),
    STALACTITE(R.drawable.stalactite);

    private static Resources resources;

    private int bitmapId;
    private Bitmap bitmap;

    Symbol(int bitmapId) {
        this.bitmapId = bitmapId;
    }

    public static void setResources(Resources resources) {
        Symbol.resources = resources;
    }

    public int getBitmapId() {
        return bitmapId;
    }

    public Bitmap getBitmap() {
        if (resources == null) {
            return null;
        }
        if (bitmap == null) {
            bitmap = BitmapFactory.decodeResource(resources, bitmapId);
        }
        return bitmap;
    }

    public static Symbol getDefault() {
        return STALACTITE;
    }

}
