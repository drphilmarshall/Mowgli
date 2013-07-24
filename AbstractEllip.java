/* 
+ ======================================================================

 NAME:
   AbstractEllip.java

 PURPOSE:
   This is an abstract class which is implemented by classes which produce
	 resizable ellipses. Currently, is implemented by Source and Lens.Mask
   
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

import java.awt.*;
import static java.lang.Math.*;

public abstract class AbstractEllip
{
	protected double rot = 0;
	protected double rotErr = 0;
	protected double ellip = 0;
	protected double ellipErr = 0;
	protected double rad = 0.5;
	protected double radErr = 0;
	protected double[] pos = {0, 0};
	protected double[] posErr = {0, 0};

	protected int[] posPixels = {SystemParam.getFullN()/2, SystemParam.getFullN()/2};
	protected boolean isHover = false;
	protected boolean isActive = true;
	protected ResizeAstroEllip listener;

	public AbstractEllip()
	{
	}

	/**
	 * Setter Functions
	 */
	public void setRot(double rot)
	{
		while(rot>PI)
			rot-=PI;
		while(rot<0)
			rot+=PI;

		this.rot = rot;
	}

	// uncertainty in the rotation
	public void setRotErr(double rotErr)
	{
		this.rotErr = rotErr;
	}

	public void setEllip(double ellip)
	{
		if(ellip<=0)
		{
			ellip = 0;
			setRot(getRot() + PI/2);
		}

		this.ellip = ellip;
	}

	// uncertainty in the ellipticity
	public void setEllipErr(double ellipErr)
	{
		this.ellipErr = ellipErr;
	}

	public void setRad(double rad)
	{
		if(rad<=1e-5)
		{
			rad = 1e-5;
			setRot(getRot() + PI/2);
		}

		this.rad = rad;
	}

	public void setRadErr(double radErr)
	{
		this.radErr = radErr;
	}
	
	public void setGrowRad(double growRad)
	{
		setRad(growRad);
	}
	
	public void setDispRad(double rad)
	{
		setRad(rad);
	}
	
	public void setPos(double[] pos)
	{
		this.pos[0] = pos[0];
		this.pos[1] = pos[1];

		// Update the actual position of the ellipModel
		this.posPixels[0] = (int)(SystemParam.getFullN()/2
				+Double.valueOf(pos[0])/SystemParam.getArcsecsPerPix());
		this.posPixels[1] = (int)(SystemParam.getFullN()/2
				+Double.valueOf(pos[1])/SystemParam.getArcsecsPerPix());
	}

	public void setPosErr(double[] posErr)
	{
		this.posErr[0] = posErr[0];
		this.posErr[1] = posErr[1];
	}
	
	public void setPosPixels(int[] posPixels)
	{
		this.posPixels[0] = posPixels[0];
		this.posPixels[1] = posPixels[1];

		// Update the actual position of the ellipse
		this.pos[0] = SystemParam.getScaledArcsecsPerPix()*
			((posPixels[0]*(double)SystemParam.getN()/SystemParam.getFullN())
			 - ((double)SystemParam.getN()-1.)/2.);
		this.pos[1] = SystemParam.getScaledArcsecsPerPix()*
			((posPixels[1]*(double)SystemParam.getN()/SystemParam.getFullN())
			 - ((double)SystemParam.getN()-1.)/2.);
	}

  public void setIsActive(boolean isActive)
  {
    this.isActive = isActive;
  }

	public void setIsHover(boolean isHover)
	{
		this.isHover = isHover;
	}
	
	
	/**
	 * Getter Functions
	 */
	public double getRot()
	{
		return this.rot;
	}

	public double getRotErr()
	{
		return this.rotErr;
	}

	public double getEllip()
	{
		return this.ellip;
	}

	public double getEllipErr()
	{
		return this.ellipErr;
	}

	public double getRad()
	{
		return this.rad;
	}

	public double getRadErr()
	{
		return this.radErr;
	}
	
	public double getDispRad()
	{
		return getRad();
	}
	
	public double getGrowRad()
	{
		return getRad();
	}
	
	public double[] getPos()
	{
		// Do a deep copy of the array and return the copy
		// to prevent accidental pointer errors
		double[] pos = {this.pos[0], this.pos[1]};
		return pos;
	}

	public double[] getPosErr()
	{
		return this.posErr;
	}

	public int[] getPosPixels()
	{
		return this.posPixels;
	}
	
	public boolean getIsActive()
	{
		return this.isActive;
	}
	
	public boolean getIsHover()
	{
		return this.isHover;
	}
}
