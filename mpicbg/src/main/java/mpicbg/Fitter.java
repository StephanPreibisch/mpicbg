package mpicbg;

import java.util.Collection;

import mpicbg.models.IllDefinedDataPointsException;
import mpicbg.models.NotEnoughDataPointsException;

/**
 * Something that fits a function to fitables
 * 
 * @author preibischs
 *
 * @param <F> - any Function that can deal with P
 * @param <P> - any Fitable, actually Object (see Fitable interface)
 */
public interface Fitter< F extends Function< F, P >, P >
{
	public void fit(
			final F function,
			final Collection< ? extends P > fitables )
				throws NotEnoughDataPointsException, IllDefinedDataPointsException;
}
