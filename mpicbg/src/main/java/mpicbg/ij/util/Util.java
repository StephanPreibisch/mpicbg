/*-
 * #%L
 * MPICBG Core Library.
 * %%
 * Copyright (C) 2008 - 2025 Stephan Saalfeld et. al.
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 2 of the
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-2.0.html>.
 * #L%
 */
package mpicbg.ij.util;

import ij.gui.PointRoi;
import ij.process.ByteProcessor;
import ij.process.ColorProcessor;
import ij.process.FloatPolygon;
import ij.process.FloatProcessor;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Random;

import mpicbg.models.Point;
import mpicbg.models.PointMatch;

public class Util
{
	private Util(){}


	/**
	 * Create an ImageJ {@link PointRoi} from a {@link Collection} of
	 * {@link Point Points}.
	 *
	 * Since ImageJ 1.46a, PointRois have sub-pixel coordinates.
	 *
	 * @param points
	 * @return
	 */
	final static public PointRoi pointsToPointRoi( final Collection< ? extends Point > points )
	{
		final float[] x = new float[ points.size() ];
		final float[] y = new float[ points.size() ];

		int i = 0;
		for ( final Point p : points )
		{
			final double[] l = p.getL();
			x[ i ] = ( float )l[ 0 ];
			y[ i ] = ( float )l[ 1 ];
			++i;
		}

		return new PointRoi( x, y, x.length );
	}


	final static public List< Point > pointRoiToPoints( final PointRoi roi )
	{
		final FloatPolygon fp = roi.getFloatPolygon();
		final float[] x = fp.xpoints;
		final float[] y = fp.ypoints;

		final ArrayList< Point > points = new ArrayList< Point >();
		for ( int i = 0; i < x.length; ++i )
			points.add( new Point( new double[]{ x[ i ], y[ i ] } ) );

		return points;
	}


	final static public List< PointMatch > pointRoisToPointMatches( final PointRoi sourceRoi, final PointRoi targetRoi )
	{
		final ArrayList< PointMatch > matches = new ArrayList< PointMatch >();

		final List< Point > sourcePoints = Util.pointRoiToPoints( sourceRoi );
		final List< Point > targetPoints = Util.pointRoiToPoints( targetRoi );

		final int numMatches = Math.min( sourcePoints.size(), targetPoints.size() );

		for ( int i = 0; i < numMatches; ++i )
			matches.add( new PointMatch( sourcePoints.get( i ), targetPoints.get( i ) ) );

		return matches;
	}


	final static public void fillWithNoise( final ByteProcessor bp )
    {
    	final byte[] data = ( byte[] )bp.getPixels();
    	final Random random = new Random( System.nanoTime() );
    	for ( int i = 0; i < data.length; ++i )
    		data[ i ] = ( byte )random.nextInt( 256 );
    }

	final static public void fillWithNoise( final FloatProcessor fp )
    {
    	final float[] data = ( float[] )fp.getPixels();
    	final Random random = new Random( System.nanoTime() );
    	for ( int i = 0; i < data.length; ++i )
    		data[ i ] = random.nextFloat();
    }

    final static public void fillWithNaN( final FloatProcessor fp )
    {
    	final float[] data = ( float[] )fp.getPixels();
    	for ( int i = 0; i < data.length; ++i )
    		data[ i ] = Float.NaN;
    }

    /**
	 * Normalize the dynamic range of a {@link FloatProcessor} to the interval [0,1].
	 *
	 * @param fp
	 */
    final static public void normalizeContrast( final FloatProcessor fp )
    {
    	final float[] data = ( float[] )fp.getPixels();
    	float min = data[ 0 ];
    	float max = min;
    	for ( final float f : data )
    	{
    		if ( f < min ) min = f;
    		else if ( f > max ) max = f;
    	}
    	final float s = 1 / ( max - min );
    	for ( int i = 0; i < data.length; ++i )
    		data[ i ] = s * ( data[ i ] - min );
    }


    /**
     * Create a saturated color in a periodic interval
     *
     * @param i
     * @param interval
     *
     * @return
     */
    final static public Color createSaturatedColor( final float i, final float interval )
	{
		float o = i / interval * 6;

		final float r, g, b;

		final float a = 1;

		if ( o < 3 )
			r = Math.min( 1.0f, Math.max( 0.0f, 2.0f - o ) ) * a;
		else
			r = Math.min( 1.0f, Math.max( 0.0f, o - 4.0f ) ) * a;

		o += 2;
		if ( o >= 6 ) o -= 6;

		if ( o < 3 )
			g = Math.min( 1.0f, Math.max( 0.0f, 2.0f - o ) ) * a;
		else
			g = Math.min( 1.0f, Math.max( 0.0f, o - 4.0f ) ) * a;

		o += 2;
		if ( o >= 6 ) o -= 6;

		if ( o < 3 )
			b = Math.min( 1.0f, Math.max( 0.0f, 2.0f - o ) ) * a;
		else
			b = Math.min( 1.0f, Math.max( 0.0f, o - 4.0f ) ) * a;

		return new Color( r, g, b );
	}

    /**
     * Generate an integer encoded 24bit RGB color that encodes a 2d vector
     * with amplitude being intensity and color being orientation.
     *
     * Only amplitudes in [0,1] will render into useful colors, so the vector
     * should be normalized to an expected max amplitude.
     * @param xs
     * @param ys
     * @return
     */
    final static public int colorVector( final double xs, final double ys )
	{
		final double a = Math.sqrt( xs * xs + ys * ys );
		if ( a == 0.0 ) return 0;

		double o = ( Math.atan2( xs / a, ys / a ) + Math.PI ) / Math.PI * 3;

		final double r, g, b;

		if ( o < 3 )
			r = Math.min( 1.0, Math.max( 0.0, 2.0 - o ) ) * a;
		else
			r = Math.min( 1.0, Math.max( 0.0, o - 4.0 ) ) * a;

		o += 2;
		if ( o >= 6 ) o -= 6;

		if ( o < 3 )
			g = Math.min( 1.0, Math.max( 0.0, 2.0 - o ) ) * a;
		else
			g = Math.min( 1.0, Math.max( 0.0, o - 4.0 ) ) * a;

		o += 2;
		if ( o >= 6 ) o -= 6;

		if ( o < 3 )
			b = Math.min( 1.0, Math.max( 0.0, 2.0 - o ) ) * a;
		else
			b = Math.min( 1.0, Math.max( 0.0, o - 4.0 ) ) * a;

		return ( ( ( ( int )( r * 255 ) << 8 ) | ( int )( g * 255 ) ) << 8 ) | ( int )( b * 255 );
	}


    /**
     * Draw a color circle into a {@link ColorProcessor}.
     *
     * @param ip
     */
    final static public void colorCircle( final ColorProcessor ip )
	{
		final int r1 = Math.min( ip.getWidth(), ip.getHeight() ) / 2;

		for ( int y = 0; y < ip.getHeight(); ++y )
		{
			final float dy = y - ip.getHeight() / 2;
			for ( int x = 0; x < ip.getWidth(); ++x )
			{
				final float dx = x - ip.getWidth() / 2;
				final float l = ( float )Math.sqrt( dx * dx + dy * dy );

				if ( l <= r1 )
					ip.putPixel( x, y, colorVector( dx / r1, dy / r1 ) );
			}
		}
	}
}
