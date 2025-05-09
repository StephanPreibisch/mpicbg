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
/**
 * License: GPL
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License 2
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package mpicbg.models;

import java.util.ArrayList;
import java.util.Set;

/**
 *
 * @author Stephan Saalfeld &lt;saalfelds@janelia.hhmi.org&gt;
 */
public class TransformMeshMap2D extends CoordinateTransformMap2D
{
	private static final long serialVersionUID = 224883558672239029L;

	public TransformMeshMap2D( final TransformMesh t, final int width, final int height )
	{
		super( new float[ height ][ width * 2 ] );
		final double[] min = new double[ 2 ];
		final double[] max = new double[ 2 ];
		final double[] l = new double[ 2 ];

		for ( int y = 0; y < map.length; ++y )
			for ( int x = 0; x < map[ y ].length; ++x )
				map[ y ][ x ] = Float.NaN;

		final Set< AffineModel2D > s = t.getAV().keySet();
		for ( final AffineModel2D ai : s )
		{
			final ArrayList< PointMatch > pm = t.getAV().get( ai );
			calculateBoundingBoxInverse( pm, min, max );

			for ( int y = ( int )min[ 1 ]; y <= max[ 1 ]; ++y )
			{
				if ( y >= 0 && y < map.length )
					for ( int x = ( int )min[ 0 ]; x <= max[ 0 ]; ++x )
					{
						final int xi = 2 * x;
						if ( x >= 0 && xi < map[ y ].length )
						{
							l[ 0 ] = x;
							l[ 1 ] = y;
							if ( TransformMesh.isInSourcePolygon( pm, l ) )
							{
								ai.applyInPlace( l );
								map[ y ][ xi ] = ( float )l[ 0 ];
								map[ y ][ xi + 1 ] = ( float )l[ 1 ];
							}
						}
					}
			}
		}
	}

	/**
	 *
	 * @param pm PointMatches
	 * @param min x = min[0], y = min[1]
	 * @param max x = max[0], y = max[1]
	 */
	final static private void calculateBoundingBoxInverse(
			final ArrayList< PointMatch > pm,
			final double[] min,
			final double[] max )
	{
		final double[] first = pm.get( 0 ).getP1().getL();
		min[ 0 ] = first[ 0 ];
		min[ 1 ] = first[ 1 ];
		max[ 0 ] = first[ 0 ];
		max[ 1 ] = first[ 1 ];

		for ( final PointMatch p : pm )
		{
			final double[] t = p.getP1().getL();
			if ( t[ 0 ] < min[ 0 ] ) min[ 0 ] = t[ 0 ];
			else if ( t[ 0 ] > max[ 0 ] ) max[ 0 ] = t[ 0 ];
			if ( t[ 1 ] < min[ 1 ] ) min[ 1 ] = t[ 1 ];
			else if ( t[ 1 ] > max[ 1 ] ) max[ 1 ] = t[ 1 ];
		}
	}
}
