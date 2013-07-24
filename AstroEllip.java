/* 
+ ======================================================================

 NAME:
   AstroEllip.java

 PURPOSE:
   This is an interface which is implemented by classes which produce resizable ellipses.
	 Currently, is implemented by Source and Lens.Mask
   
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

 BUGS:
   List all known (or suspected) bugs here
  
 REVISION HISTORY:
   2010-02-23  started Naudus (GMU)

- ======================================================================
*/

public interface AstroEllip {
	/**
	 * Setter Functions
	 */
	public void setRot(double rot);

	public void setEllip(double ellip);

	public void setRad(double rad);
	
	public void setPos(double[] pos);
	
	public void setPosPixels(int[] pos);
	
	public void setIsAdjusting(boolean isAdjusting);
	
	
	/**
	 * Getter Function
	 */
	public double getRot();

	public double getEllip();

	public double getRad();
	
	public double[] getPos();
	
	public int[] getPosPixels();
	
	public boolean getIsAdjusting();
}
