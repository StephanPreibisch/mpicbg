package mpicbg;

import java.util.Collection;
import java.util.List;

import mpicbg.models.NotEnoughDataPointsException;

public interface Function< P >
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
			final List< P > candidates,
			final Collection< P > inliers,
			final int iterations,
			final double epsilon,
			final double minInlierRatio )
		throws NotEnoughDataPointsException;

}
