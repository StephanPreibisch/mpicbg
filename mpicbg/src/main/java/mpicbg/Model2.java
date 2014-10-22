package mpicbg;

/**
 * In the mpicbg package, the Model is a combination of Function & Fitter, a model
 * does both. But this is not necessarily like that, but can be.
 * 
 * 
 * @author preibischs
 *
 */
public interface Model2 extends Function< PointMatch2 >, Fitter< Model2, PointMatch2 >
{

}
