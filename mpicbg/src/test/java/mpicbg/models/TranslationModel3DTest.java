package mpicbg.models;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Random;

import org.junit.Test;

public class TranslationModel3DTest
{
	public class PointMatch2 extends PointMatch
	{
		private static final long serialVersionUID = 9208477766162765648L;
		public PointMatch2(Point p1, Point p2) { super(p1, p2); }
		
		public String toString()
		{ 
			String l1 = "";
			String l2 = "";
			String w1 = "";
			String w2 = "";
			
			for ( int d = 0; d < p1.getL().length; ++d )
			{
				l1 += p1.getL()[ d ];
				l2 += p2.getL()[ d ];
				w1 += p1.getW()[ d ];
				w2 += p2.getW()[ d ];
				
				if ( d != p1.getL().length - 1 )
				{
					l1 += ", ";
					l2 += ", ";
					w1 += ", ";
					w2 += ", ";
				}
			}
			
			return l1 + " [" + w1 + "] >>> " + l2 + " [" + w2 + "]";
		}
	}

	@Test
	public void test()
	{
		final ArrayList< PointMatch2 > candidates = new ArrayList<PointMatch2>();
		final ArrayList< PointMatch2 > inliers = new ArrayList<PointMatch2>();

		final TranslationModel3D m = new TranslationModel3D();

		// 100 pointmatches shifted by v=(1,2,3)
		for ( int i = 0; i < 10; ++i )
			candidates.add( new PointMatch2( new Point( new float[]{ i, i, i } ), new Point( new float[]{ i+1, i+2, i+3 } ) ) );

		final Random rnd = new Random( 23455 );

		// 100 random pointmatches
		for ( int i = 0; i < 10; ++i )
			candidates.add( new PointMatch2(
					new Point( new float[]{ rnd.nextFloat(), rnd.nextFloat(), rnd.nextFloat() } ),
					new Point( new float[]{ rnd.nextFloat() + 10, rnd.nextFloat() - 3, rnd.nextFloat() + 1 } ) ) );

		try
		{
			if ( m.ransac( candidates, inliers, 10, 0.001, 0.3 ) )
			{
				m.fit( inliers );
				System.out.println( "Model: " + m );
				System.out.println( "|inliers|: " + inliers.size() );
	
				for ( final PointMatch2 pm : inliers )
				{
					pm.apply( m );
					System.out.println( pm );
				}
				
				assertTrue( inliers.size() == 10 ); // 10 inliers
				assertArrayEquals( new float[]{ 1.0f, 2.0f, 3.0f }, m.translation, 0.00001f ); // the model must be correct
				assertTrue( m.getCost() == 0.5 ); // 50% inliers
			}
			else
			{
				fail( "Could not fit TranslationModel3D with RANSAC" );
			}
		}
		catch ( NotEnoughDataPointsException e )
		{
			fail( "NotEnoughDataPointsException thrown while fitting TranslationModel3D with RANSAC" );
		}
	}

}
