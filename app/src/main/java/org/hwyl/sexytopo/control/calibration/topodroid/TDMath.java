package org.hwyl.sexytopo.control.calibration.topodroid;

/* @file TDMath.java
 *
 * @author marco corvi
 * @date jan 2014
 *
 * @grief math utilities
 * --------------------------------------------------------
 *  Copyright This software is distributed under GPL-3.0 or later
 *  See the file COPYING.
 * --------------------------------------------------------
 */

import java.lang.Math;

// import android.util.Log;

public class TDMath
{
    public static final float M_PI  = (float)Math.PI;     // 3.1415926536f;
    public static final float M_2PI = (2*M_PI); // 6.283185307f;
    public static final float M_PI2 = M_PI/2;        // Math.PI/2
    public static final float M_PI4 = M_PI/4;        // Math.PI/4
    public static final float M_PI8 = M_PI/8;        // Math.PI/8
    public static final float RAD2DEG = (180.0f/M_PI);
    public static final float DEG2RAD = (M_PI/180.0f);

    public static float abs( float x )   { return (float)( Math.abs(x) ); }
    public static float cos( float x )   { return (float)Math.cos( x ); }
    public static float cosd( float xd ) { return (float)Math.cos( xd * DEG2RAD ); }
    public static float sin( float x )   { return (float)Math.sin( x ); }
    public static float sind( float xd ) { return (float)Math.sin( xd * DEG2RAD ); }
    public static float atan2( float y, float x ) { return (float)( Math.atan2( y, x ) ); }
    public static float atan2d( float y, float x ) { return (float)( RAD2DEG * Math.atan2( y, x ) ); }
    public static float acos( float x )   { return (float)( Math.acos( x ) ); }
    public static float acosd( float x )  { return (float)( RAD2DEG * Math.acos( x ) ); }
    public static float asind( float x )  { return (float)( RAD2DEG * Math.asin( x ) ); }
    public static float sqrt( float x )   { return (float)Math.sqrt( x ); }

    public static float in360( float f )
    {
        while ( f >= 360 ) f -= 360;
        while ( f < 0 )    f += 360;
        return f;
    }

    public static float around( float f, float f0 )
    {
        if ( f - f0 > 180 ) return f - 360;
        if ( f0 - f > 180 ) return f + 360;
        return f;
    }

    public static float degree2slope( float deg ) { return (float)(100 * Math.tan( deg * DEG2RAD ) ); }
    public static float slope2degree( float slp ) { return (float)( Math.atan( slp/100 ) * RAD2DEG ); }

}
