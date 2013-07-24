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

public class Lens extends AbstractEllip implements InterfaceEllip
{
	private double F = 1;
	private double FErr = 0;
	private double FP;
	private double alphaCoef;
	private double einsteinRad = 5;
	private double einsteinRadErr = 0;
	private boolean isActive;
	public static boolean isAdjusting;
	public static boolean newLens;
	public static double magnification = 1;

	public MassModel mass;

	/**
	 * Constructor
	 */
	public Lens(double[] pos, double einsteinRad, double F, double rot)
	{
		super();

		this.einsteinRad = einsteinRad;
		this.rot = rot;
		
		setPos(pos);
		setF(F);
		updateAlphaCoef();
	
		//TODO: Need a way of setting the ellipticity of the mass model
		//from F.
		mass = new MassModel(this, 0);
	}
	
	public Lens()
	{
		super();
		mass = new MassModel(this, 0);

		setF(F);
		updateAlphaCoef();
	}

	/**
	 * Setter Functions
	 */
	// Set the axis ratio of the lens
	public void setF(double F)
	{
		if(F > 0.999999)
		{
			F = 0.999999;
		} else if(F < 0)
		{
			F = 0;
		}
		this.F = F;
		this.FP = sqrt(1. - pow(F, 2));
		updateAlphaCoef();

		this.mass.ellip = abs(1-this.F);
	}

	public void setFErr(double FErr)
	{
		this.FErr = FErr;
	}

	public void setEinsteinRad(double einsteinRad)
	{
		this.einsteinRad = einsteinRad;
		this.alphaCoef = einsteinRad*(sqrt(F)/FP);
	}

	public void setEinsteinRadErr(double einsteinRadErr)
{
		this.einsteinRadErr = einsteinRadErr;
	}

	public void setRot(double rot)
	{
		while(rot>PI)
			rot-=PI;
		while(rot<0)
			rot+=PI;

		this.rot = rot;
		this.mass.rot = PI/2-rot;
	}

	public void updateAlphaCoef()
	{
		this.alphaCoef = einsteinRad*(sqrt(F)/FP);
	}

	public void setIsAdjusting(boolean isAdjusting)
	{
		Lens.isAdjusting = isAdjusting;
	}
	
	
	/**
	 * Getter Functions
	 */
	// Get the axis ratio of the lens
	public double getF()
	{
		return this.F;
	}

	public double getFErr()
	{
		return this.FErr;
	}

	public double getFP()
	{
		return this.FP;
	}

	public double getNormRot()
	{
		double rot = (PI/2)-this.rot;
		if(rot<0)
			rot+=PI;
		return rot;
	}

	public double getAlphaCoef()
	{
		return this.alphaCoef;
	}

	public double getEinsteinRad()
	{
		return this.einsteinRad;
	}

	public double getEinsteinRadErr()
	{
		return this.einsteinRadErr;
	}

	public boolean getIsAdjusting()
	{
		return Lens.isAdjusting;
	}
}

class MassModel extends AbstractEllip implements InterfaceEllip
{
	Lens lens;
	public static boolean isVisible = true;

	public MassModel(Lens lens, double ellip)
	{
		super();

		this.lens = lens;
		this.ellip = ellip;
		this.rad = lens.getEinsteinRad();
	}

	/**
	 * Setter Functions
	 */

	public void setPos(double[] pos)
	{
		lens.setPos(pos);
	}

	public void setPosErr(double[] posErr)
	{
		lens.setPosErr(posErr);
	}
	
	public void setPosPixels(int[] posPixels)
	{
		lens.setPosPixels(posPixels);
	}	
	
	public void setRot(double rot)
	{
		while(rot>PI)
			rot-=PI;
		while(rot<0)
			rot+=PI;

		this.rot = rot-PI;
		lens.setRot(PI/2-rot);
	}

	public void setRotErr(double rotErr)
	{
		lens.setRotErr(rotErr);
	}

	public void setEllip(double ellip)
	{
		this.ellip = ellip;
		lens.setF(abs(1-this.ellip));
	}

	public void setEllipErr(double ellipErr)
	{
		lens.setFErr(ellipErr);
	}

	public void setRad(double rad)
	{
		if(rad<=1e-5)
		{
			rad = 1e-5;
			setRot(getRot() + PI/2);
		}

		this.rad = rad;
		lens.setEinsteinRad(rad);
	}

	public void setRadErr(double radErr)
	{
		lens.setEinsteinRadErr(radErr);
	}
	
	public ResizeAstroEllip getListener(InterfaceComponent component, int[] whichProperty)
	{
		return new ResizeAstroEllip(component, (InterfaceEllip)this, whichProperty);
	}

	public void setIsHover(boolean isHover)
	{
		lens.setIsHover(isHover);
	}
	
	public void setIsActive(boolean isActive)
	{
		lens.setIsActive(isActive);
	}

	public void setIsAdjusting(boolean isAdjusting)
	{
		lens.setIsAdjusting(isAdjusting);
	}

	/**
	 * Getter Functions
	 */
	public double[] getPos()
	{
		return lens.getPos();
	}

	public double[] getPosErr()
	{
		return lens.getPosErr();
	}

	public int[] getPosPixels()
	{
		return lens.getPosPixels();
	}

	public boolean getIsHover()
	{
		return lens.getIsHover();
	}

	public boolean getIsActive()
	{
		return lens.getIsActive();
	}

	public boolean getIsAdjusting()
	{
		return lens.getIsAdjusting();
	}
}

abstract class LensEllip extends AbstractEllip
{
	Lens lens;
	
	public LensEllip(Lens lens)
	{
		this.lens = lens;
	}
	
	/**
	 * Setter Functions
	 */
	public void setIsHover(boolean isHover)
	{
		lens.setIsHover(isHover);
	}
	
	public void setIsActive(boolean isActive)
	{
		lens.setIsActive(isActive);
	}

	public void setIsAdjusting(boolean isAdjusting)
	{
		lens.setIsAdjusting(isAdjusting);
	}

	/**
	 * Getter Functions
	 */
	public boolean getIsHover()
	{
		return lens.getIsHover();
	}

	public boolean getIsActive()
	{
		return lens.getIsActive();
	}

	public boolean getIsAdjusting()
	{
		return lens.getIsAdjusting();
	}
}
