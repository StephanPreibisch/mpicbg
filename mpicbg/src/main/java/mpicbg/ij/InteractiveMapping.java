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

import ij.IJ;
import ij.ImageListener;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.ImageWindow;
import ij.gui.PointRoi;
import ij.gui.Roi;
import ij.gui.Toolbar;
import ij.plugin.PlugIn;
import ij.process.ImageProcessor;

import java.awt.Event;
import java.awt.TextField;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import mpicbg.models.IllDefinedDataPointsException;
import mpicbg.models.NotEnoughDataPointsException;
import mpicbg.models.Point;
import mpicbg.models.PointMatch;

/**
 * An interactive parent class for point based image deformation.
 *
 * @author Stephan Saalfeld &lt;saalfeld@mpi-cbg.de&gt;
 * @version 0.2b
 */
public abstract class InteractiveMapping implements PlugIn, MouseListener, MouseMotionListener, KeyListener, ImageListener
{
	protected ImagePlus imp;
	protected ImageProcessor target;
	protected ImageProcessor source;

	protected Point[] p;
	protected Point[] q;
	final protected ArrayList< PointMatch > m = new ArrayList< PointMatch >();
	protected PointRoi handles;
	final protected ArrayList< Point > hooks = new ArrayList< Point >();

	protected Mapping< ? > mapping;
	protected MappingThread painter;
	final protected AtomicBoolean pleaseRepaint = new AtomicBoolean( false );

	static protected boolean showIllustration = false;
	static protected boolean showPreview = false;

	protected int targetIndex = -1;
	static protected boolean interpolate;

	abstract protected void updateHandles( int x, int y );

	public void init(){}
	abstract protected void createMapping();
	abstract protected void updateMapping() throws NotEnoughDataPointsException, IllDefinedDataPointsException;
	abstract protected void addHandle( int x, int y );
	abstract protected void updateIllustration();

	@Override
	public void run( final String arg )
    {
		/* cleanup */
		m.clear();

		imp = IJ.getImage();
		target = imp.getProcessor();
		source = target.duplicate();
		source.setInterpolationMethod( ImageProcessor.BILINEAR );

		init();

		createMapping();

		painter = new MappingThread(
				imp,
				source,
				target,
				pleaseRepaint,
				mapping,
				interpolate );
		painter.start();

		Toolbar.getInstance().setTool( Toolbar.getInstance().addTool( "Add_and_drag_handles." ) );

		imp.getCanvas().addMouseListener( this );
		imp.getCanvas().addMouseMotionListener( this );
		imp.getCanvas().addKeyListener( this );
		imp.getWindow().addKeyListener( this );
		IJ.getInstance().addKeyListener( this );
    }

	final protected void updateRoi()
	{
		final int[] x = new int[ hooks.size() ];
		final int[] y = new int[ hooks.size() ];

		for ( int i = 0; i < hooks.size(); ++ i )
		{
			final double[] l = hooks.get( i ).getW();
			x[ i ] = ( int )l[ 0 ];
			y[ i ] = ( int )l[ 1 ];
		}
		handles = new PointRoi( x, y, hooks.size() );
		imp.setRoi( handles );
	}

	@Override
	public void imageClosed( final ImagePlus impl )
	{
		if ( impl == this.imp )
			painter.interrupt();
	}
	@Override
	public void imageOpened( final ImagePlus impl ){}
	@Override
	public void imageUpdated( final ImagePlus impl ){}

	@Override
	public void keyPressed( final KeyEvent e)
	{
		if ( e.getKeyCode() == KeyEvent.VK_ESCAPE || e.getKeyCode() == KeyEvent.VK_ENTER )
		{
			painter.interrupt();
			if ( imp != null )
			{
				imp.getCanvas().removeMouseListener( this );
				imp.getCanvas().removeMouseMotionListener( this );
				imp.getCanvas().removeKeyListener( this );
				imp.getWindow().removeKeyListener( this );
				IJ.getInstance().removeKeyListener( this );
				imp.setOverlay( null );
				imp.setRoi( ( Roi )null );
			}
			if ( e.getKeyCode() == KeyEvent.VK_ESCAPE )
			{
				imp.setProcessor( null, source );
			}
			else
			{
				target.reset();
				mapping.mapInterpolated( source, target );
				imp.updateAndDraw();
			}
		}
		else if ( e.getKeyCode() == KeyEvent.VK_U )
		{
			showIllustration = !showIllustration;
			updateIllustration();
			e.consume();
		}
		else if (
				( e.getKeyCode() == KeyEvent.VK_F1 ) &&
				( e.getSource() instanceof TextField ) ){}
	}

	@Override
	public void keyReleased( final KeyEvent e ){ e.consume(); }
	@Override
	public void keyTyped( final KeyEvent e ){ e.consume(); }

	@Override
	public void mousePressed( final MouseEvent e )
	{
		targetIndex = -1;
		final ImageWindow win = WindowManager.getCurrentWindow();
		final int xm = win.getCanvas().offScreenX( e.getX() );
		final int ym = win.getCanvas().offScreenY( e.getY() );

		double target_d = Double.MAX_VALUE;
		for ( int i = 0; i < hooks.size(); ++i )
		{
			final double[] l = hooks.get( i ).getW();
			final double dx = win.getCanvas().getMagnification() * ( l[ 0 ] - xm );
			final double dy = win.getCanvas().getMagnification() * ( l[ 1 ] - ym );
			final double d =  dx * dx + dy * dy;

			if ( d < 64.0 && d < target_d )
			{
				targetIndex = i;
				target_d = d;
			}
		}

		if ( e.getButton() == MouseEvent.BUTTON1 )
		{
			if ( targetIndex == -1 )
			{
				addHandle( xm, ym );
				updateRoi();
			}
		}
	}
	@Override
	public void mouseReleased( final MouseEvent e )
	{
		if ( !showPreview )
			try
			{
				updateMapping();
				synchronized ( painter )
				{
					//if ( !pleaseRepaint.getAndSet( true ) )
					pleaseRepaint.set( true );
					painter.notify();
				}
			}
			catch ( final NotEnoughDataPointsException ex ){}
			catch ( final IllDefinedDataPointsException ex ){}
	}

	@Override
	public void mouseExited( final MouseEvent e ){}
	@Override
	public void mouseClicked( final MouseEvent e ){}
	@Override
	public void mouseEntered( final MouseEvent e ){}

	@Override
	public void mouseDragged( final MouseEvent e )
	{
		if ( targetIndex >= 0 )
		{
			final ImageWindow win = WindowManager.getCurrentWindow();
			final int x = win.getCanvas().offScreenX( e.getX() );
			final int y = win.getCanvas().offScreenY( e.getY() );

			updateHandles( x, y );
			updateIllustration();
			updateRoi();
			try
			{
				updateMapping();
				if ( showPreview )
				{
					synchronized ( painter )
					{
						//if ( !pleaseRepaint.getAndSet( true ) )
						pleaseRepaint.set( true );
						painter.notify();
					}
				}
			}
			catch ( final NotEnoughDataPointsException ex ){}
			catch ( final IllDefinedDataPointsException ex ){}
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
