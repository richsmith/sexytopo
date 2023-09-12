package org.hwyl.sexytopo.control.components;

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;

import org.hwyl.sexytopo.R;
import org.hwyl.sexytopo.SexyTopoConstants;

import java.util.ArrayList;
import java.util.List;

// see http://blog.stylingandroid.com/archives/432

public class ScrollingTable extends LinearLayout
{
    public ScrollingTable( Context context )
    {
        super( context );
    }

    public ScrollingTable( Context context, AttributeSet attrs )
    {
        super( context, attrs );
    }

    @Override
    protected void onLayout( boolean changed, int l, int t, int r, int b )
    {


        try {
            onLayout2( changed, l, t, r, b );
        } catch (Exception e) {
            Log.d(SexyTopoConstants.TAG, "Error updating table: " + e);
        }

    }

    @SuppressLint("WrongCall")
    protected void onLayout2( boolean changed, int l, int t, int r, int b ) throws Exception {

        super.onLayout(changed, l, t, r, b);

        List<Integer> colWidths = new ArrayList<>();

        TableLayout header = findViewById( R.id.HeaderTable );
        TableLayout body = findViewById( R.id.BodyTable );



        if (header.getChildCount() == 0) {
            return;
        }

        for ( int rownum = 0; rownum < body.getChildCount(); rownum++ )
        {
            TableRow row = (TableRow) body.getChildAt( rownum );
            for ( int cellnum = 0; cellnum < row.getChildCount(); cellnum++ )
            {
                View cell = row.getChildAt( cellnum );
                Integer cellWidth = cell.getWidth();
                if ( colWidths.size() <= cellnum )
                {
                    colWidths.add( cellWidth );
                }
                else
                {
                    Integer current = colWidths.get( cellnum );
                    if( cellWidth > current )
                    {
                        colWidths.remove( cellnum );
                        colWidths.add( cellnum, cellWidth );
                    }
                }
            }
        }

        for ( int rownum = 0; rownum < header.getChildCount(); rownum++ )
        {
            TableRow row = (TableRow) header.getChildAt( rownum );
            for ( int cellnum = 0; cellnum < row.getChildCount(); cellnum++ )
            {
                View cell = row.getChildAt( cellnum );
                TableRow.LayoutParams params = (TableRow.LayoutParams)cell.getLayoutParams();
                params.width = colWidths.get( cellnum );
            }
        }
    }
}