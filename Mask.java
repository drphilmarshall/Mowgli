/* 
+ ======================================================================

 NAME:
   Lens.java

 PURPOSE:
   This class holds the data corresponding to the current lens
   configuration. Its main purpose is to store variables and
	 provide getter and setter functions.
	 In addition, this class has a subclass (EllipModel) which holds
	 information about the lens mask and mass models.
   
 COMMENTS:
   Add any comments on working of class here
   
 USAGE:
	 Lens.java [options] inputs

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

import static java.lang.Math.*;
import java.awt.*;

class Mask extends AbstractEllip implements InterfaceEllip
{
	public static boolean isAdjusting = false;
	public static boolean isVisible = false;

	public Mask(double rot, double ellip, double rad)
	{
		super();

		this.rot = rot;
		this.ellip = ellip;
		this.rad = rad;
	}
	
	public Mask()
	{
		super();
	}

	public void setIsAdjusting(boolean isAdjusting)
	{
		Mask.isAdjusting = isAdjusting;
	}

	public boolean getIsAdjusting()
	{
		return Mask.isAdjusting;
	}

	public ResizeAstroEllip getListener(InterfaceComponent component, int[] whichProperty)
	{
		return new ResizeAstroEllip(component, this, whichProperty);
	}
}
