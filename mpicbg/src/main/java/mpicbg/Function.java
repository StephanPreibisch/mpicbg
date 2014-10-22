package mpicbg;

import java.util.Collection;
import java.util.List;

import mpicbg.models.Model;
import mpicbg.models.NotEnoughDataPointsException;
import mpicbg.models.PointMatch;

/**
 * A function itself, can compute distances to/within <P> and do ransac if it has a Fitter
 * 
 * @author preibischs
 *
 * @param <P> - any Fitable (rather Object) it can deal with
 */
public interface Function< F extends Function< F, P >, P >
{
	public double getCost();
	public void setCost( final double c );

	public int getMinNumFitables();
	
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
	public < E extends P > boolean ransac(
			final Fitter< F, P > fitter,
			final List< E > candidates,
			final Collection< E > inliers,
			final int iterations,
			final double epsilon,
			final double minInlierRatio,
			final int minNumInliers )
		throws NotEnoughDataPointsException;

	/**
	 * "Less than" operater to make {@link Model Models} comparable.
	 * 
	 * @param m
	 * @return false for {@link #cost} < 0.0, otherwise true if
	 *   {@link #cost this.cost} is smaller than {@link #cost m.cost}
	 */
	public boolean betterThan( final F m );

	/**
	 * Test the {@link Model} for a set of {@link PointMatch} candidates.
	 * Return true if the number of inliers / number of candidates is larger
	 * than or equal to min_inlier_ratio, otherwise false.
	 * 
	 * Clears inliers and fills it with the fitting subset of candidates.
	 * 
	 * Sets {@link #getCost() cost} = 1.0 - |inliers| / |candidates|.
	 * 
	 * @param candidates set of point correspondence candidates
	 * @param inliers set of point correspondences that fit the model
	 * @param epsilon maximal allowed transfer error
	 * @param minInlierRatio minimal ratio |inliers| / |candidates| (0.0 => 0%, 1.0 => 100%)
	 * @param minNumInliers minimally required absolute number of inliers
	 */
	public < E extends P > boolean test(
			final Collection< E > candidates,
			final Collection< E > inliers,
			final double epsilon,
			final double minInlierRatio,
			final int minNumInliers );

	/**
	 * Call {@link #test(Collection, Collection, double, double, int)} with
	 * minNumInliers = {@link #getMinNumMatches()}.
	 */
	public < E extends P > boolean test(
			final Collection< E > candidates,
			final Collection< E > inliers,
			final double epsilon,
			final double minInlierRatio );

	/**
	 * Set the function to m
	 * @param m
	 */
	public void set( final F m );

	
	/**
	 * Clone the function.
	 */
	public F copy();
}
