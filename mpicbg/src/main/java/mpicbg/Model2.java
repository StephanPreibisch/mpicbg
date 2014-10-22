package mpicbg;

/**
 * In the mpicbg package, the Model is a combination of Function & Fitter, a model
 * does both. But this is not necessarily like that, but can be.
 * 
 * 
 * @author preibischs
 *
 */
public interface Model2< M extends Model2< M > > extends Function< M, PointMatch2 >, Fitter< M, PointMatch2 >
{

}
