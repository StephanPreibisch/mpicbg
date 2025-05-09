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

/**
 *
 * @author Stephan Saalfeld &lt;saalfelds@janelia.hhmi.org&gt;
 */
public interface Affine3D< T extends Affine3D< T > > extends InvertibleCoordinateTransform
{
	public void preConcatenate( final T affine3d );
	public void concatenate( final T affine3d );

	/**
	 * Write the 12 parameters of the affine into a double array.  The order is
	 * m00, m10, m20, m01, m11, m21, m02, m12, m22, m03, m13, m23
	 */
	public void toArray( final double[] data );

	/**
	 * Write the 12 parameters of the affine into a 4x3 double array.  The order
	 * is
	 * <pre>{@code
	 * [0][0] -> m00; [0][1] -> m01; [0][2] -> m02; [0][3] -> m03
	 * [1][0] -> m10; [1][1] -> m11; [1][2] -> m12; [1][3] -> m13
	 * [2][0] -> m20; [2][1] -> m21; [2][2] -> m22; [2][3] -> m23
	 * }</pre>
	 */
	public void toMatrix( final double[][] data );

	@Override
	public T createInverse();
}
