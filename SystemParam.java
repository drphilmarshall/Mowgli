/* 
+ ======================================================================

 NAME:
   SystemParam.java

 PURPOSE:
   This class holds the data corresponding to the system in
   general which is not specific to the source or lens.

 COMMENTS:
   Add any comments on working of class here

 USAGE:
	 SystemParam.java [options] inputs

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

public class SystemParam
{
	private static double zs = 2.39;
	private static double zd = 0.444;

	private static double fov=15;
	private static double arcsecsPerPix = 0.0247579375;
	private static double scaledArcsecsPerPix;
	private static double defaultPSF = 1.4;
	private static double psf = defaultPSF;
	private static double background = 0;
	private static int origN = 512;
	private static int fullN = 300;
	private static int N = 50;

	public static final double c = 2.99792458e8;
	public static boolean ifFits = false;
	public static int maskThreshold = 25;
	public static double sigmaFITS = 1;

	public SystemParam(double arcsecsPerPix, double zs, double zd, int fullN, int N)
	{
		SystemParam.arcsecsPerPix = arcsecsPerPix;
		SystemParam.zs = zs;
		SystemParam.zd = zd;
		SystemParam.fullN = fullN;
		SystemParam.N = N;
		
		init();
	}
	
	public SystemParam()
	{
		init();
	}
	
	public static void init()
	{
		//arcsecsPerPix = arcsecsPerPix*origN/fullN;
		//scaledArcsecsPerPix = arcsecsPerPix*fullN/N;
		//fov = arcsecsPerPix*fullN;
		setFOV(fov);
	}

	/**
	 * Setter Functions
	 */
	public static void setArcsecsPerPix(double arcsecsPerPix)
	{
		SystemParam.arcsecsPerPix = arcsecsPerPix;
		SystemParam.scaledArcsecsPerPix = SystemParam.arcsecsPerPix*SystemParam.fullN/SystemParam.N;
	}
	
	public static void setFOV(double fov)
	{
		SystemParam.fov = fov;

		setArcsecsPerPix(SystemParam.fov/SystemParam.fullN);
	}

	public static void setPSF(double psf)
	{
		SystemParam.psf = psf;
	}

	public static void setBackground(double background)
	{
		SystemParam.background = background;
	}

	public static void defaultPSF()
	{
		SystemParam.psf = SystemParam.defaultPSF;
	}
	public static void setZs(double zs)
	{
		SystemParam.zs = zs;
	}

	public static void setZd(double zd)
	{
		SystemParam.zd = zd;
	}

	public static void setOrigN(int origN)
	{
		SystemParam.origN = origN;
	}

	public static void setFullN(int fullN)
	{
		SystemParam.fullN = fullN;
	}

	public static void setN(int N)
	{
		SystemParam.N = N;
	}

	public static void setSigmaFITS(double sigmaFITS)
	{
		SystemParam.sigmaFITS = sigmaFITS;
	}


	/**
	 * Getter Functions
	 */
	public static double getArcsecsPerPix()
	{
		return SystemParam.arcsecsPerPix;
	}

	public static double getScaledArcsecsPerPix()
	{
		return SystemParam.scaledArcsecsPerPix;
	}

	public static double getFOV()
	{
		return SystemParam.fov;
	}

	public static double getPSF()
	{
		return SystemParam.psf;
	}

	public static double getBackground()
	{
		return SystemParam.background;
	}

	public static double getZs()
	{
		return SystemParam.zs;
	}

	public static double getZd()
	{
		return SystemParam.zd;
	}

	public static int getOrigN()
	{
		return SystemParam.origN;
	}

	public static int getFullN()
	{
		return SystemParam.fullN;
	}

	public static int getN()
	{
		return SystemParam.N;
	}

	public static double getSigmaFITS()
	{
		return SystemParam.sigmaFITS;
	}

	public static double getC()
	{
		return SystemParam.c;
	}
}
