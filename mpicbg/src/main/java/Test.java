import java.util.ArrayList;
import java.util.Random;

import mpicbg.models.IllDefinedDataPointsException;
import mpicbg.models.NotEnoughDataPointsException;
import mpicbg.models.Point;
import mpicbg.models.PointMatch;
import mpicbg.models.TranslationModel3D;


public class Test
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

	public Test() throws NotEnoughDataPointsException
	{
		final ArrayList< PointMatch2 > candidates = new ArrayList<Test.PointMatch2>();
		final ArrayList< PointMatch2 > inliers = new ArrayList<Test.PointMatch2>();

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
		}
		else
		{
			System.out.println( "failed" );
		}

	}
	public static void main( String[] args ) throws NotEnoughDataPointsException, IllDefinedDataPointsException
	{
		new Test();
	}
}
