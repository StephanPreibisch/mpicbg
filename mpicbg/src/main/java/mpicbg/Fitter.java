package mpicbg;

import java.util.Collection;

/**
 * Something that fits a function to fitables
 * 
 * @author preibischs
 *
 * @param <F> - any Function that can deal with P
 * @param <P> - any Fitable, actually Object (see Fitable interface)
 */
public interface Fitter< F extends Function< P >, P extends Fitable >
{
	public void fit(
			final F function,
			final Collection< P > fitables );
}
