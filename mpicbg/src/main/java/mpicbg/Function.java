package mpicbg;

import java.util.Collection;
import java.util.List;

import mpicbg.models.NotEnoughDataPointsException;

/**
 * A function itself, can compute distances to/within <P> and do ransac if it has a Fitter
 * 
 * @author preibischs
 *
 * @param <P> - any Fitable (rather Object) it can deal with
 */
public interface Function< P extends Fitable >
{
	/**
	 * Get distance/error for a certain fitable and this function
	 * @param fitable
	 * @return
	 */
	public float getDistance( final P fitable );
	
	/**
	 * Call {@link #ransac(List, Collection, int, double, double, int)} with
	 * minNumInliers = {@link #getMinNumMatches()}.
	 */
	public boolean ransac(
			final Fitter< Function< P >, P > fitter,
			final List< P > candidates,
			final Collection< P > inliers,
			final int iterations,
			final double epsilon,
			final double minInlierRatio )
		throws NotEnoughDataPointsException;

}
