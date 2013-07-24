/* 
+ ======================================================================

 NAME:
   Source.java

 PURPOSE:
   This class holds the data corresponding to the current source 
   configutation. Its main purpose is to store variables and
	 provide getter and setter functions.
   
 COMMENTS:
   Add any comments on working of class here
   
 USAGE:
	 Source.java [options] inputs

 INPUTS:
   List all required inputs of class here

 OPTIONAL INPUTS:
   List all optional inputs of class here

 OUTPUTS:
   List all outputs of class here 
 
 EXAMPLES:
   Optional: provide some examples of class in action here
   (This is more common for high level scripts) 

 DEPENDENCIES:
   Depends on:
	   None.
   Called by:
	   GravLensApplet.java
		 AstroImageFrame.java
		 PredictedImageFrame.java
		 History.java

 BUGS:
   List all known (or suspected) bugs here
  
 REVISION HISTORY:
   2010-02-23  started Naudus (GMU)

- ======================================================================
*/

import java.awt.*;

public class Source extends AbstractEllip implements InterfaceEllip
{
	private static double avgRed = 1;
	private static double avgGreen = 1;
	private static double avgBlue = 1;
	private static double brightness = 1;
	public static boolean isAdjusting = false;

	/**
	 * Constructor
	 */
	public Source(double[] pos, double rad, double ellip, double rot)
	{
		setPos(pos);
		this.rad = rad;
		this.ellip = ellip;
		this.rot = rot;
	}
	
	public Source()
	{
		this.rot = 0;
		this.ellip = 0;
		this.rad = 0.5;
	}

	/**
	 * Setter Functions
	 */
	public void setF(double F)
	{
		setEllip(1. - F);
	}

	public void setFErr(double FErr)
	{
		setEllipErr(FErr);
	}

	public void setGrowRad(double growRad)
	{
		setRad(growRad - 0.3);
	}

	public static void setAvgRed(double avgRed)
	{
		Source.avgRed = avgRed;
	}

	public static void setAvgGreen(double avgGreen)
	{
		Source.avgGreen = avgGreen;
	}

	public static void setAvgBlue(double avgBlue)
	{
		Source.avgBlue = avgBlue;
	}
	
	public static void setBrightness(double brightness)
	{
		Source.brightness = brightness;
	}

	public void setIsAdjusting(boolean isAdjusting)
	{
		Source.isAdjusting = isAdjusting;
	}


	/**
	 * Getter Functions
	 */
	// Get the axis ratio of the source
	public double getF()
	{
		return 1. - getEllip();
	}

	public double getFErr()
	{
		return getEllipErr();
	}

	public double getGrowRad()
	{
		return getRad() + 0.3;
	}

	public static double getAvgRed()
	{
		return Source.avgRed;
	}
	
	public static double getAvgGreen()
	{
		return Source.avgGreen;
	}
	
	public static double getAvgBlue()
	{
		return Source.avgBlue;
	}
	
	public static double getBrightness()
	{
		return Source.brightness;
	}
	
	public boolean getIsAdjusting()
	{
		return Source.isAdjusting;
	}

	public ResizeAstroEllip getListener(InterfaceComponent component, int[] whichProperty)
	{
		return new ResizeAstroEllip(component, (InterfaceEllip)this, whichProperty);
	}
}
