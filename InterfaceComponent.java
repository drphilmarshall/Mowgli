/* 
+ ======================================================================

 NAME:
   InterfaceComponent.java

 PURPOSE:
   This is an interface which is implemented by classes which serve as frames.
   
 COMMENTS:
   Add any comments on working of class here
   
 EXAMPLES:
   Optional: provide some examples of class in action here
   (This is more common for high level scripts) 

 DEPENDENCIES:
   Depends on:
		 None.
	 Called by:
	   AstroImageFrame.java
		 PredictedImageFrame.java

 BUGS:
   List all known (or suspected) bugs here
  
 REVISION HISTORY:
   2010-02-23  started Naudus (GMU)

- ======================================================================
*/

import java.awt.*;

public interface InterfaceComponent
{
	public void setHoverEllip();
	public void setCursor(Cursor cursor);
	public void inactiveAllEllip();
	public void raiseActiveEllip();

	public void updateImage();
}
