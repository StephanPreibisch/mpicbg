package mpicbg;

/**
 * In the mpicbg package, the PointMatch would be a Fitable (or just Object). The Model
 * class itself defines that it can work with PointMatch.
 * 
 * @author preibischs
 *
 */
public interface PointMatch2 extends Fitable
{
	/**
	 * apply would be called by Function.getDistance( PointMatch2 )
	 * 
	 * @param m
	 */
	public void apply( Model2 m );
}
