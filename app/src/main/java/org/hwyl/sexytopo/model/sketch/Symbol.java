package org.hwyl.sexytopo.model.sketch;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.DisplayMetrics;

import org.hwyl.sexytopo.R;


public enum Symbol {

    STALACTITE(R.drawable.ic_stalactite),
    STALAGMITE(R.drawable.ic_stalagmite),
    COLUMN(R.drawable.ic_column),
    STRAWS(R.drawable.ic_straws),
    HELICTITE(R.drawable.ic_helictite),
    CRYSTAL(R.drawable.ic_crystal);

    private static Resources resources;

    private final int bitmapId;
    private Bitmap bitmap;
    private Bitmap buttonBitmap;

    private static final Symbol DEFAULT = STALACTITE;

    Symbol(int bitmapId) {
        this.bitmapId = bitmapId;
    }

    public static void setResources(Resources resources) {
        Symbol.resources = resources;
    }

    public static Symbol fromString(String name) {
        return name == null? DEFAULT : Symbol.valueOf(name);
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

    public Bitmap getButtonBitmap() {
        if (resources == null) {
            return null;
        }

        if (buttonBitmap == null) {
            Bitmap bitmap = getBitmap();
            if (bitmap == null) {
                return null;
            }

            int dimen = (int)resources.getDimension(R.dimen.toolbar_button_height);
            buttonBitmap = Bitmap.createScaledBitmap(bitmap, dimen, dimen, true);
        }

        return buttonBitmap;
    }

    public Bitmap getBitmapWithHeightDp(float heightDp) {
        int heightInPixels = (int)convertDpToPixel(heightDp);
        return Bitmap.createScaledBitmap(getBitmap(), heightInPixels, heightInPixels, true);
    }

    private static float convertDpToPixel(float dp){
        return dp * ((float)
            resources.getDisplayMetrics().densityDpi / DisplayMetrics.DENSITY_DEFAULT);
    }


    public static Symbol getDefault() {
        return STALACTITE;
    }

}
