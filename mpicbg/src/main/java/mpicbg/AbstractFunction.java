package mpicbg;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

import mpicbg.models.IllDefinedDataPointsException;
import mpicbg.models.NotEnoughDataPointsException;

public abstract class AbstractFunction< F extends Function< F, P >, P extends Fitable > implements Function< F, P >
{
	// real random
	//final Random random = new Random( System.currentTimeMillis() );
	// repeatable results
	final static protected Random rnd = new Random( 69997 );

	/**
	 * The cost depends on what kind of algorithm is running.  It is always
	 * true that a smaller cost is better than large cost
	 */
	protected double cost = Double.MAX_VALUE;

	@Override
	public boolean ransac(
			final Fitter< F, P > fitter,
			final List< P > candidates,
			final Collection< P > inliers,
			final int iterations,
			final double epsilon,
			final double minInlierRatio,
			final int minNumInliers )
					throws NotEnoughDataPointsException
	{
		if ( candidates.size() < getMinNumFitables() )
			throw new NotEnoughDataPointsException( candidates.size() + " data points are not enough to solve the Function, at least " + getMinNumFitables() + " data points required." );
		
		cost = Double.MAX_VALUE;
		
		final F copy = copy();
		final F m = copy();
		
		inliers.clear();
		
		int i = 0;
		final HashSet< P > minMatches = new HashSet< P >();
		
A:		while ( i < iterations )
		{
			// choose model.MIN_SET_SIZE disjunctive matches randomly
			minMatches.clear();
			for ( int j = 0; j < getMinNumFitables(); ++j )
			{
				P p;
				do
				{
					p = candidates.get( ( int )( rnd.nextDouble() * candidates.size() ) );
				}
				while ( minMatches.contains( p ) );
				minMatches.add( p );
			}
			try { fitter.fit( m, minMatches ); }
			catch ( final IllDefinedDataPointsException e )
			{
				++i;
				continue;
			}
			
			final ArrayList< P > tempInliers = new ArrayList< P >();
			
			int numInliers = 0;
			boolean isGood = m.test( candidates, tempInliers, epsilon, minInlierRatio );
			while ( isGood && numInliers < tempInliers.size() )
			{
				numInliers = tempInliers.size();
				try { fitter.fit( m, tempInliers ); }
				catch ( final IllDefinedDataPointsException e )
				{
					++i;
					continue A;
				}
				isGood = m.test( candidates, tempInliers, epsilon, minInlierRatio, minNumInliers );
			}
			if (
					isGood &&
					m.betterThan( copy ) &&
					tempInliers.size() >= minNumInliers )
			{
				copy.set( m );
				inliers.clear();
				inliers.addAll( tempInliers );
			}
			++i;
		}
		if ( inliers.size() == 0 )
			return false;
		
		set( copy );
		return true;
	}

	/**
	 * Test the {@link AbstractModel} for a set of {@link PointMatch} candidates.
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
	@Override
	final public boolean test(
			final Collection< P > candidates,
			final Collection< P > inliers,
			final double epsilon,
			final double minInlierRatio,
			final int minNumInliers )
	{
		inliers.clear();
		
		for ( final P m : candidates )
		{
			//m.apply( this );
			if ( this.getDistance( m ) < epsilon ) inliers.add( m );
		}
		
		final float ir = ( float )inliers.size() / ( float )candidates.size();
		setCost( Math.max( 0.0, Math.min( 1.0, 1.0 - ir ) ) );
		
		return ( inliers.size() >= minNumInliers && ir > minInlierRatio );
	}

	@Override
	final public boolean test(
			final Collection< P > candidates,
			final Collection< P > inliers,
			final double epsilon,
			final double minInlierRatio )
	{
		return test( candidates, inliers, epsilon, minInlierRatio, getMinNumFitables() );
	}

	@Override
	final public boolean betterThan( final F m )
	{
		if ( cost < 0 ) return false;
		return cost < m.getCost();
	}
}
