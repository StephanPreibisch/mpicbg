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
package mpicbg.ij;

import java.awt.Event;
import java.awt.TextField;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import ij.CompositeImage;
import ij.IJ;
import ij.ImageListener;
import ij.ImagePlus;
import ij.ImageStack;
import ij.WindowManager;
import ij.gui.ImageWindow;
import ij.gui.PointRoi;
import ij.gui.Roi;
import ij.gui.Toolbar;
import ij.plugin.PlugIn;
import ij.process.ImageProcessor;
import mpicbg.models.IllDefinedDataPointsException;
import mpicbg.models.InvertibleCoordinateTransform;
import mpicbg.models.Model;
import mpicbg.models.NotEnoughDataPointsException;
import mpicbg.models.Point;
import mpicbg.models.PointMatch;

/**
 *
 * An interactive parent class for point based image deformation.
 *
 * @param <M> the transformation model to be used
 *
 * @author Stephan Saalfeld &lt;saalfeld@mpi-cbg.de&gt;
 * @version 0.2b
 */
public abstract class InteractiveInvertibleCoordinateTransform< M extends Model< M > & InvertibleCoordinateTransform > implements PlugIn, MouseListener, MouseMotionListener, KeyListener, ImageListener
{
	static public class Tuple
	{
		final public ImageProcessor source;
		final public ImageProcessor target;
		final public AtomicBoolean pleaseRepaint = new AtomicBoolean( false );
		public MappingThread painter = null;

		Tuple( final ImageProcessor source, final ImageProcessor target )
		{
			this.source = source;
			this.target = target;
		}
	}

	final static protected void useRoi( final float[] x, final float[] y )
	{

	}

	protected InverseTransformMapping< M > mapping;
	protected ImagePlus imp;
	final protected ArrayList< Tuple > tuples = new ArrayList< Tuple >();

	protected Point[] p;
	protected Point[] q;
	final protected ArrayList< PointMatch > m = new ArrayList< PointMatch >();
	protected PointRoi handles;

	protected int targetIndex = -1;

	abstract protected M myModel();
	abstract protected void setHandles();
	abstract protected void updateHandles( int x, int y );

	protected void onReturn() {}

	@Override
	public void run( final String arg )
    {
		// cleanup
		m.clear();
		tuples.clear();

		imp = IJ.getImage();
		if ( imp.isComposite() && ( ( CompositeImage )imp ).getMode() == CompositeImage.COMPOSITE )
		{
			final int z = imp.getSlice();
			final int t = imp.getFrame();
			for ( int c = 1; c <= imp.getNChannels(); ++c )
			{
				final int i = imp.getStackIndex( c, z, t );
				final ImageProcessor target = imp.getStack().getProcessor( i );
				final ImageProcessor source = target.duplicate();
				source.setInterpolationMethod( ImageProcessor.BILINEAR );
				tuples.add( new Tuple( source, target ) );
			}
		}
		else
		{
			final ImageProcessor target = imp.getProcessor();
			final ImageProcessor source = target.duplicate();
			source.setInterpolationMethod( ImageProcessor.BILINEAR );
			tuples.add( new Tuple( source, target ) );
		}

		mapping = new InverseTransformMapping< M >( myModel() );
		for ( final Tuple tuple : tuples )
		{
			tuple.painter = new MappingThread(
					imp,
					tuple.source,
					tuple.target,
					tuple.pleaseRepaint,
					mapping,
					false,
					imp.getStackIndex( imp.getChannel(), imp.getSlice(), imp.getFrame() ) );
			tuple.painter.start();
		}

		setHandles();

		Toolbar.getInstance().setTool( Toolbar.getInstance().addTool( "Drag_the_handles." ) );

		imp.getCanvas().addMouseListener( this );
		imp.getCanvas().addMouseMotionListener( this );
		imp.getCanvas().addKeyListener( this );
    }

	@Override
	public void imageClosed( final ImagePlus imp2 )
	{
		if ( imp2 == this.imp )
			for ( final Tuple tuple : tuples )
				tuple.painter.interrupt();
	}
	@Override
	public void imageOpened( final ImagePlus imp2 ){}
	@Override
	public void imageUpdated( final ImagePlus imp2 ){}

	@Override
	public void keyPressed( final KeyEvent e)
	{
		if ( e.getKeyCode() == KeyEvent.VK_ESCAPE || e.getKeyCode() == KeyEvent.VK_ENTER )
		{
			for ( final Tuple tuple : tuples )
				tuple.painter.interrupt();
			if ( imp != null )
			{
				imp.getCanvas().removeMouseListener( this );
				imp.getCanvas().removeMouseMotionListener( this );
				imp.getCanvas().removeKeyListener( this );
				imp.getCanvas().setDisplayList( null );
				imp.setRoi( ( Roi )null );
			}

			/* reset pixels */
			final int z = imp.getSlice();
			final int t = imp.getFrame();
			if ( imp.isComposite() && ( ( CompositeImage )imp ).getMode() == CompositeImage.COMPOSITE )
			{
				for ( int c = 1; c <= imp.getNChannels(); ++c )
				{
					final int i = imp.getStackIndex( c, z, t );
					final ImageProcessor ip = tuples.get( c - 1 ).source;
					imp.getStack().setPixels( ip.getPixels(), i );
					if ( c == imp.getChannel() )
						imp.setProcessor( ip );
				}
			}
			else
			{
				final ImageProcessor ip = tuples.get( 0 ).source;
				imp.setProcessor( ip );
				imp.getStack().setPixels( ip.getPixels(), imp.getStackIndex( imp.getChannel(), z, t ) );
			}
			if ( e.getKeyCode() == KeyEvent.VK_ENTER )
			{
				final Thread thread = new Thread(
						new Runnable()
						{
							@Override
							public void run()
							{
								final int si = imp.getStackIndex( imp.getChannel(), imp.getSlice(), imp.getFrame() );
								final ImageStack stack = imp.getStack();
								for ( int i = 1; i <= stack.getSize(); ++i )
								{
									final ImageProcessor source = stack.getProcessor( i ).duplicate();
									final ImageProcessor target = source.createProcessor( source.getWidth(), source.getHeight() );
									source.setInterpolationMethod( ImageProcessor.BILINEAR );
									mapping.mapInterpolated( source, target );
									if ( i == si )
										imp.getProcessor().setPixels( target.getPixels() );
									stack.setPixels( target.getPixels(), i );
									IJ.showProgress( i, stack.getSize() );
								}
								if ( imp.isComposite() )
									( ( CompositeImage )imp ).setChannelsUpdated();
								imp.updateAndDraw();
							}
						} );
				thread.start();
				this.onReturn();
			}
		}
		else if (
				( e.getKeyCode() == KeyEvent.VK_F1 ) &&
				( e.getSource() instanceof TextField ) ){}
	}

	@Override
	public void keyReleased( final KeyEvent e ){}
	@Override
	public void keyTyped( final KeyEvent e ){}

	@Override
	public void mousePressed( final MouseEvent e )
	{
		targetIndex = -1;
		if ( e.getButton() == MouseEvent.BUTTON1 )
		{
			final ImageWindow win = WindowManager.getCurrentWindow();
			final int x = win.getCanvas().offScreenX( e.getX() );
			final int y = win.getCanvas().offScreenY( e.getY() );

			double target_d = Double.MAX_VALUE;
			for ( int i = 0; i < q.length; ++i )
			{
				final double dx = win.getCanvas().getMagnification() * ( q[ i ].getW()[ 0 ] - x );
				final double dy = win.getCanvas().getMagnification() * ( q[ i ].getW()[ 1 ] - y );
				final double d =  dx * dx + dy * dy;
				if ( d < 64.0 && d < target_d )
				{
					targetIndex = i;
					target_d = d;
				}
			}
		}
	}

	@Override
	public void mouseReleased( final MouseEvent e ){}
	@Override
	public void mouseExited( final MouseEvent e ) {}
	@Override
	public void mouseClicked( final MouseEvent e ) {}
	@Override
	public void mouseEntered( final MouseEvent e ) {}

	@Override
	public void mouseDragged( final MouseEvent e )
	{
		if ( targetIndex >= 0 )
		{
			final ImageWindow win = WindowManager.getCurrentWindow();
			final int x = win.getCanvas().offScreenX( e.getX() );
			final int y = win.getCanvas().offScreenY( e.getY() );

			updateHandles( x, y );

			try
			{
				myModel().fit( m );
				for ( final Tuple tuple : tuples )
				{
					synchronized ( tuple.painter )
					{
						tuple.pleaseRepaint.set( true );
						tuple.painter.notify();
					}
				}
			}
			catch ( final NotEnoughDataPointsException ex ) { ex.printStackTrace(); }
			catch ( final IllDefinedDataPointsException ex ) { ex.printStackTrace(); }
		}
	}

	@Override
	public void mouseMoved( final MouseEvent e ){}


	public static String modifiers( final int flags )
	{
		String s = " [ ";
		if ( flags == 0 )
			return "";
		if ( ( flags & Event.SHIFT_MASK ) != 0 )
			s += "Shift ";
		if ( ( flags & Event.CTRL_MASK ) != 0 )
			s += "Control ";
		if ( ( flags & Event.META_MASK ) != 0 )
			s += "Meta (right button) ";
		if ( ( flags & Event.ALT_MASK ) != 0 )
			s += "Alt ";
		s += "]";
		if ( s.equals( " [ ]" ) )
			s = " [no modifiers]";
		return s;
	}
}
