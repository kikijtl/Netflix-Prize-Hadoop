///////////////////////////////////////////////////////////////////////////////
//
//  Copyright (c) 2009, Arizona State University
//  All rights reserved.
//  BSD License: http://www.opensource.org/licenses/bsd-license.html
//  Author: Jeff Conner
//
///////////////////////////////////////////////////////////////////////////////

package SobelEdgeDetectorMR;

import java.awt.image.BufferedImage;

public class SobelEdgeDetector 
{
	///////////////////////////////////////////////////////////////////////////////
	//
	// Member Variables
	//
	///////////////////////////////////////////////////////////////////////////////
	
	private BufferedImage 	_image;
	private BufferedImage 	_resultsImage;
	private double		  	_maxSobelValue;
	private double			_minSobelValue;
	private double[][] 		_sobelValues;
	private boolean			_firstRun;
	
	///////////////////////////////////////////////////////////////////////////////
	//
	// Constructor
	//
	///////////////////////////////////////////////////////////////////////////////
	
	public SobelEdgeDetector( BufferedImage image )
	{
		// set the image
		_image = image;
		
		// set the results image
		_resultsImage = image;
		
		// resize the sobel values array
		_sobelValues = new double[image.getWidth()][image.getHeight()];
		
		// this is the first run through
		_firstRun = true;
		
		
	}
	
	///////////////////////////////////////////////////////////////////////////////
	//
	// Get the image
	//
	///////////////////////////////////////////////////////////////////////////////
	
	public BufferedImage image()
	{
		return _image;
	}
	
	
	///////////////////////////////////////////////////////////////////////////////
	//
	// Get the results image
	//
	///////////////////////////////////////////////////////////////////////////////
	
	public BufferedImage results()
	{
		return _resultsImage;
	}
	
	
	
	///////////////////////////////////////////////////////////////////////////////
	//
	// Process the image
	//
	///////////////////////////////////////////////////////////////////////////////
	
	public void process()
	{
		// Get the height and width of the image
		int w = _image.getWidth();
		int h = _image.getHeight();
		
		for( int i = 1; i < w - 1; ++i )
		{
			for( int j = 1; j < h - 1; ++j )
			{	
				// get the sobel value at ij
				double value = _getSobelColor( i, j );
				
				// set the sobel value for this position
				_sobelValues[i][j] = value;
				
				/*
				if( false == _firstRun )
				{
					if( _maxSobelValue < value )
					{
						_maxSobelValue = value;
					}
					
					if( _minSobelValue > value )
					{
						_minSobelValue = value;
					}
				}
				else
				{
					_minSobelValue = _maxSobelValue = value;
					_firstRun = false;
				}
				*/		
			}
		}
		
		// Debugging
		//System.out.println( "Min/Max: " + _minSobelValue + " | " + _maxSobelValue );
		
		// manually seeding the min and max.  This should be done outside proecss in future runs
		// probably through distributed cache or some other means
		_minSobelValue = 0.0;
		_maxSobelValue = 700.0;
		
		// set the colors in the image
		for( int i = 1; i < w - 1; ++i )
		{
			for( int j = 1; j < h - 1; ++j )
			{
				// get the sobel value at ij
				double value = _sobelValues[i][j];
				
				// get the normalized value
				double normal = _getNormalizedValue( value, _minSobelValue, _maxSobelValue, 0.0, 1.0 );
				
				// normalize between 0 and 254
				normal *= 254.0;
				
				// set the color at ij
				_setColor( i, j, (int)normal );
				
				
			}
		}
		
	}
	
	
	///////////////////////////////////////////////////////////////////////////////
	//
	// Get the theta value for the given pixel at xy
	//
	///////////////////////////////////////////////////////////////////////////////

	private double _getSobelColor( int x, int y )
	{		
		// get the colors of the matrix for Gy
		double a00 = _getAverageAt( x - 1, y - 1 );
		double a10 = _getAverageAt( x	, y - 1 );
		double a20 = _getAverageAt( x + 1, y - 1 );
		double a01 = _getAverageAt( x - 1, y 	);
		double a21 = _getAverageAt( x + 1, y 	);
		double a02 = _getAverageAt( x - 1, y + 1 );
		double a12 = _getAverageAt( x	, y + 1 );
		double a22 = _getAverageAt( x + 1, y + 1 );
		
		// Gy value
		double Gy = ( a00 + ( 2.0f * a10 ) + a20 + ( -1.0f * a02 ) + ( -2.0f * a12 ) + ( -1.0f * a22 ) ) / 9.0f;
		
		// Gx value
		double Gx = ( a00 + ( -1 * a20 ) + ( 2.0f * a01 ) + ( -2.0f * a21 ) + a02 + ( -1.0f * a22 ) );
		
		// Theta
		double theta = Math.atan2( Gy , Gx );
		
		// magnitude
		double magnitude = Math.sqrt( ( Gx * Gx ) + ( Gy * Gy ) );
		
		// get the result
		double result = (float)theta * (float) magnitude;
		
		// return
		//return result;
		return (float)magnitude;
	}
	
	
	///////////////////////////////////////////////////////////////////////////////
	//
	// Get the average value at pixel xy
	//
	///////////////////////////////////////////////////////////////////////////////

	private int _getAverageAt( int x, int y )
	{
		// get the color
		int color = _image.getRGB( x, y );
		
		// separate the channels
		int r = color & 0x00FF0000;
		r = r >> 16;
		int g = color & 0x0000FF00;
		g = g >> 8;
		int b = color & 0x000000FF;
		
		int average = ( r + g + b ) / 3;
		
		return average;
	}
	///////////////////////////////////////////////////////////////////////////////
	//
	// Get the normalized value given a theta
	//
	///////////////////////////////////////////////////////////////////////////////

	private double _getNormalizedValue( double theta, double fromMin, double fromMax, double toMin, double toMax )
	{
		// normalize in the positive direction starting at 0
		double fmax = fromMax - fromMin;
		
		// adjust the theta value
		theta = theta - fromMin;
		
		// get the percentage that theta is of fmax
		double percent = theta / fmax;
		
		// normalize in the positive direction starting at 0
		double tmax = toMax - toMin;		
		
		// get the normal
		double normal = percent * tmax;
		
		if( normal > toMax )
		{
			normal = toMax;
		}
		if( (int)normal < toMin )
		{
			normal = toMin;
		}
		// return
		return normal;
	}

	
	///////////////////////////////////////////////////////////////////////////////
	//
	// Set the pixel at xy to color
	//
	///////////////////////////////////////////////////////////////////////////////

	private void _setColor( int x, int y, int color )
	{	
		
		//System.out.println( "_setColor -- color: " + color );
		
		// set up the alpha channel
		int finalColor = 0x000000ff;
		finalColor = finalColor << 8;
		
		// set the red color
		finalColor = finalColor + color;
		finalColor = finalColor << 8;
		
		// set the green color
		finalColor = finalColor + color;
		finalColor = finalColor << 8;
		
		// set the blue color
		finalColor = finalColor + color;		
	
		// set the pixel in the image
		_resultsImage.setRGB(x, y, finalColor );
	}

}