/* 
+ ======================================================================

 NAME:
   InterfaceEllip.java

 PURPOSE:
   This is an interface which is implemented by classes which produce resizable ellipses.
   
 COMMENTS:
   Add any comments on working of class here
   
 EXAMPLES:
   Optional: provide some examples of class in action here
   (This is more common for high level scripts) 

 DEPENDENCIES:
   Depends on:
		 None.
	 Called by:
	   Source
	 	 Lens.Mask
	 	 Lens.Mass

 BUGS:
   List all known (or suspected) bugs here
  
 REVISION HISTORY:
   2010-02-23  started Naudus (GMU)

- ======================================================================
*/

import java.awt.*;

public interface InterfaceEllip
{
	public void setRot(double rot);
	public void setRotErr(double rotErr);
	public void setEllip(double ellip);
	public void setEllipErr(double ellipErr);
	public void setRad(double rad);
	public void setRadErr(double radErr);
	public void setGrowRad(double growRad);
	public void setPos(double[] pos);
	public void setPosErr(double[] posErr);
	public void setPosPixels(int[] posPixels);
	public void setIsActive(boolean isActive);
	public void setIsHover(boolean isHover);
	public void setIsAdjusting(boolean isAdjusting);

	public double getRot();
	public double getRotErr();
	public double getEllip();
	public double getEllipErr();
	public double getRad();
	public double getRadErr();
	public double getGrowRad();
	public double[] getPos();
	public double[] getPosErr();
	public int[] getPosPixels();
	public boolean getIsActive();
	public boolean getIsHover();
	public boolean getIsAdjusting();
}
