/* 
+ ======================================================================

 NAME:
   ChiSq.java

 PURPOSE:
   This class computes the chi-squared error between the predicted and
	 astronomical images.
   
 COMMENTS:
   This class is incomplete, and has NOT yet been tested.
   
 USAGE:
	 ChiSq.java [options] inputs

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
   List all classes that this class depends on, and which classes
   call this one

 BUGS:
   List all known (or suspected) bugs here
  
 REVISION HISTORY:
   2010-02-23  started Naudus (GMU)

- ======================================================================
*/

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.*;
import javax.imageio.ImageIO;
import java.util.*;

import org.apache.commons.math.*;
import org.apache.commons.math.analysis.*;
import org.apache.commons.math.optimization.*;
import org.apache.commons.math.optimization.direct.*;

import static java.lang.Math.*;

public class ChiSq
{
	private static AstroImageFrame astroImageFrame;
	private static PredictedImageFrame predictedImageFrame;
	private static ArrayList<Source> sourceList;
	private static ArrayList<Lens> lensList;
	private static ArrayList<Mask> maskList;

	private static double[] chiSqChans = new double[3];
	private static double chiSq, omega;
	private static int[] residualPixels = new int[3*SystemParam.getN()*SystemParam.getN()];

	// false - set the flux of the predicted image by matching it with the astronomical image
	// true - set the flux by minimizing the chi squared.
	public static boolean optimizeFlux = false;

	/**
	 * Constructor
	 */
	public ChiSq(AstroImageFrame astroImageFrame, PredictedImageFrame predictedImageFrame, ArrayList<Source> sourceList, ArrayList<Lens> lensList, ArrayList<Mask> maskList)
	{
		ChiSq.astroImageFrame = astroImageFrame;
		ChiSq.predictedImageFrame = predictedImageFrame;
		ChiSq.sourceList = sourceList;
		ChiSq.lensList = lensList;
		ChiSq.maskList = maskList;
	}
	
	public static double getChiSq()
	{
		return ChiSq.chiSq;
	}

	public static double getOmega()
	{
		return ChiSq.omega;
	}

	public static int[] getResidualPixels()
	{
		return ChiSq.residualPixels;
	}

	/**
	 * Set the three channels of the source flux which minimizes chi-squared.
	 */
	public static void optimizeFlux(BufferedImage image)
	{
		int[] astroPixels = astroImageFrame.getPixelsN();
		int[] predictedPixels = new int[3*SystemParam.getN()*SystemParam.getN()];
		image.getRaster().getPixels(0, 0, SystemParam.getN(), SystemParam.getN(), predictedPixels);

		double[] predictedAvgPixels = new double[3];
		double[] astroAvgPixels = astroImageFrame.getAvgPixels(1);
		int numUnmasked = 0;

		double[] sum1 = {0, 0, 0};
		double[] sum2 = {0, 0, 0};
		double[] rescale = {1, 1, 1};

		for(int i=0; i<3*SystemParam.getN()*SystemParam.getN(); i+=3)
		{
			if(!ifUnderMask(i))
			{
				for(int j=0; j<=2; j++)
				{
					// sum1 and sum2 are used for computing the optimum flux
					sum1[j] += predictedPixels[i+j]*astroPixels[i+j];
					sum2[j] += predictedPixels[i+j]*predictedPixels[i+j];

					predictedAvgPixels[j] += predictedPixels[i+j];
				}
				numUnmasked++;
			} else
			{
				for(int j=0; j<=2; j++)
				{
					sum2[j] += predictedPixels[i+j]*predictedPixels[i+j];
					predictedAvgPixels[j] += predictedPixels[i+j];
				}
				numUnmasked++;
			}
		}
		for(int j=0; j<=2; j++)
		{
			predictedAvgPixels[j] /= numUnmasked;
		}

		if(optimizeFlux)
		{
			/*
			 * The derivative of chi squared is:
			 *   d chisq/dA = 2*sum_i [a_i*p_i] - 2*A*sum_i [p_i^2] 
			 * a_i is the i'th pixel of the astronomical image.
			 * p_i is the i'th pixel of the predicted image.
			 *
			 * In the code, we call the first summation sum1 and the second sum2.
			 * The optimum flux A is found by setting d chisq/dA=0 and solving for A.
			 * That is, A = sum1/sum2.
			 */
			if(sum2[0]>0 && sum2[1]>0 && sum2[2]>0)
			{
				for(int j=0; j<=2; j++)
				{
					rescale[j] = sum1[j]/sum2[j];
				}
			}
		} else
		{
			if(predictedAvgPixels[0]>0 && predictedAvgPixels[1]>0 && predictedAvgPixels[2]>0)
			{
				for(int j=0; j<=2; j++)
				{
					rescale[j] = astroAvgPixels[j]/predictedAvgPixels[j];
				}
			}
		}

		int newValue;
		for(int i=0; i<3*SystemParam.getN()*SystemParam.getN(); i+=3)
		{
			for(int j=0; j<=2; j++)
			{
				newValue = (int)(predictedPixels[i+j]*rescale[j]);
				predictedPixels[i+j] = newValue<255?newValue:255;
			}
		}

		image.getRaster().setPixels(0, 0, SystemParam.getN(), SystemParam.getN(), predictedPixels);

		/*
		 * Compute the residual pixels and chi-squared
		 */
		for(int i=0; i<3*SystemParam.getN()*SystemParam.getN(); i+=3)
		{
			if(ifUnderMask(i))
			{
				// Set this pixel to black since it's under a mask.
				ChiSq.residualPixels[i] = 0;
				ChiSq.residualPixels[i+1] = 0;
				ChiSq.residualPixels[i+2] = 0;
			} else
			{
				// Compute this pixel's residual and add it to sum1 and sum2
				// (sum1 and sum2 are used for computing the optimum flux)

				for(int j=0; j<=2; j++)
				{
					ChiSq.residualPixels[i+j] = abs(predictedPixels[i+j]-astroPixels[i+j]);
				}
			}
		}

		/*
		 * Using the residual pixels we just computed, compute chi-squared
		 */
		// This 3-element array contains chi-squared for the three channels
		double[] chiSq = {0., 0., 0.};
		double[] chiSqDenom = {0., 0., 0.};
//		if(SystemParam.ifFits)
//		{
			chiSqDenom[0] = 1.;
			chiSqDenom[1] = 1.;
			chiSqDenom[2] = 1.;
//		}

		for(int i=0; i<ChiSq.residualPixels.length; i+=3)
		{
			if(!ifUnderMask(i))
			{
				for (int j=0; j<=2; j++)
				{
//					if(SystemParam.ifFits)
//					{
						chiSq[j] += pow(ChiSq.residualPixels[i+j],2)/pow(SystemParam.getSigmaFITS(),2);
//					} else
//					{
//						chiSq[j] += pow(ChiSq.residualPixels[i+j],2);
//						if(!ifUnderMask(i))
//						{
//							chiSqDenom[j] += pow(astroPixels[i+j],2);
//						}
//					}
				}
			}
		}

		ChiSq.chiSqChans[0] = chiSq[0]/chiSqDenom[0];
		ChiSq.chiSqChans[1] = chiSq[1]/chiSqDenom[1];
		ChiSq.chiSqChans[2] = chiSq[2]/chiSqDenom[2];
		ChiSq.chiSq = chiSqChans[0] + chiSqChans[1] + chiSqChans[2];
		System.out.println(ChiSq.chiSq/3);

		/*
		 * Now we compute omega, which is the percent of the flux explained by the model
		 */
		ChiSq.omega = 100.*(1. - (ChiSq.chiSq/3));
	}

	/**
	 * Optimize the parameters such that the residual image is minimized
	 * (Finds a local minimum in the residual near the model generated by the user)
	 */
	public static void optimizeParams()
	{
		MultivariateRealOptimizer optimizer = new NelderMead();

		// Build an array containing information on the user's model.
		// We will pass this array into the optimizer as the starting point.
		MultivariateRealFunction optimizableFunction = new OptimizableFunction(predictedImageFrame.predictedImage, sourceList, lensList);
		int j=0;
		double[] x = new double[5*sourceList.size() + 5*lensList.size()];
		double[] pos;

		for(int i=0; i<sourceList.size(); i++)
		{
			pos = sourceList.get(i).getPos();
			x[j++] = pos[0];
			x[j++] = pos[1];
			x[j++] = sourceList.get(i).getRad();
			x[j++] = sourceList.get(i).getEllip();
			x[j++] = sourceList.get(i).getRot();
		}

		for(int i=0; i<lensList.size(); i++)
		{
			pos = lensList.get(i).getPos();
			x[j++] = pos[0];
			x[j++] = pos[1];
			x[j++] = lensList.get(i).getEinsteinRad();
			x[j++] = lensList.get(i).getF();
			x[j++] = lensList.get(i).getRot();
		}

		try
		{
			RealPointValuePair pair = optimizer.optimize(optimizableFunction, GoalType.MINIMIZE, x);
			System.out.println(SystemParam.maskThreshold);
		} catch(Exception e)
		{
			System.err.println("Error: " + e.getMessage());
		}
	}

	/**
	 * Varies each parameter in order to compute the uncertainty associated with that parameter
	 */
	public static void computeUncertainties()
	{
		double initVal;
		double newVal;
		double initChiSq = ChiSq.getChiSq();
		double chiSqTol = 1.;
		double incrPos = 0.0001;
		double incrRad = 0.0001;
		double incrEinsteinRad = 0.0001;
		double incrEllip=0.0001;
		double incrRot = 0.0001;
		int timeout = 100; // If the uncertainty hasn't been find after this number iterations,
		                     // it is assumed to be "undefined"
		int k;
		double[] err = new double[2];

		if(!SystemParam.ifFits)
		{
			chiSqTol = 1.34;
			//chiSqTol = 5.;
		}

		for(int i=0; i<sourceList.size(); i++)
		{
			// Position
			double[] initPos = sourceList.get(i).getPos();
			double[] posErr = new double[2];
			for(int j=0; j<=1; j++)
			{
				err[0]=0; err[1]=0;
				for(int sign=-1; sign<=1; sign+=2)
				{
					double[] newPos = {initPos[0], initPos[1]};
					k=1;
					do
					{
						newPos[j] += sign*incrPos;
						sourceList.get(i).setPos(newPos);
						BufferedImage image = predictedImageFrame.predictedImage.getImage(SystemParam.getScaledArcsecsPerPix());
						ChiSq.optimizeFlux(image);
						k++;
						if(k>=timeout) break;
					} while(abs(ChiSq.getChiSq() - initChiSq) < chiSqTol);
					if(k<timeout)
					{
						err[(sign==-1)?0:1] = abs(newPos[j] - initPos[j]);
					}
					else
					{
						err[0] = Double.POSITIVE_INFINITY;
						err[1] = Double.POSITIVE_INFINITY;
						break;
					}
				}
				sourceList.get(i).setPos(initPos); // Put the source position back
				posErr[j] = (err[0]+err[1])/2;
			}
			sourceList.get(i).setPosErr(posErr);

			// Radius
			initVal = sourceList.get(i).getRad();
			err[0]=0; err[1]=0;
			for(int sign=-1; sign<=1; sign+=2)
			{
				newVal = initVal;
				k=1;
				do
				{
					newVal += sign*incrRad;
					if(newVal<0)
					{
						newVal = Double.NEGATIVE_INFINITY;
						break;
					}
					sourceList.get(i).setRad(newVal);
					BufferedImage image = predictedImageFrame.predictedImage.getImage(SystemParam.getScaledArcsecsPerPix());
					ChiSq.optimizeFlux(image);
					k++;
					if(k>=timeout)
					{
						System.out.println(abs(ChiSq.getChiSq() - initChiSq));
						break;
					}
				} while(abs(ChiSq.getChiSq() - initChiSq) < chiSqTol);
				if(newVal==Double.NEGATIVE_INFINITY)
					err[(sign==-1)?0:1] = Double.NEGATIVE_INFINITY;
				else if(k<timeout)
					err[(sign==-1)?0:1] = abs(newVal - initVal);
				else
				{
					err[0] = Double.POSITIVE_INFINITY;
					err[1] = Double.POSITIVE_INFINITY;
					break;
				}
			}
			sourceList.get(i).setRad(initVal);
			if(err[0]==Double.NEGATIVE_INFINITY)
				sourceList.get(i).setRadErr(err[1]);
			else if(err[1]==Double.NEGATIVE_INFINITY)
				sourceList.get(i).setRadErr(err[0]);
			else
				sourceList.get(i).setRadErr((err[0]+err[1])/2);

			// Ellipticity
			initVal = sourceList.get(i).getEllip();
			double initRot = sourceList.get(i).getRot();
			err[0]=0; err[1]=0;
			for(int sign=-1; sign<=1; sign+=2)
			{
				newVal = initVal;
				k=1;
				do
				{
					newVal += sign*incrEllip;
					if(newVal<0)
					{
						newVal = Double.NEGATIVE_INFINITY;
						break;
					}
					sourceList.get(i).setEllip(newVal);
					BufferedImage image = predictedImageFrame.predictedImage.getImage(SystemParam.getScaledArcsecsPerPix());
					ChiSq.optimizeFlux(image);
					k++;
					if(k>=timeout) break;
				} while(abs(ChiSq.getChiSq() - initChiSq) < chiSqTol);
				if(newVal==Double.NEGATIVE_INFINITY)
					err[(sign==-1)?0:1] = Double.NEGATIVE_INFINITY;
				else if(k<timeout)
					err[(sign==-1)?0:1] = abs(newVal - initVal);
				else
				{
					err[0] = Double.POSITIVE_INFINITY;
					err[1] = Double.POSITIVE_INFINITY;
					break;
				}
			}
			sourceList.get(i).setEllip(initVal);
			sourceList.get(i).setRot(initRot); // We need to reset the rotation because sometimes messing
			                                   // with the axis ratio messes with the rotation as well
			if(err[0]==Double.NEGATIVE_INFINITY)
				sourceList.get(i).setEllipErr(err[1]);
			else if(err[1]==Double.NEGATIVE_INFINITY)
				sourceList.get(i).setEllipErr(err[0]);
			else
				sourceList.get(i).setEllipErr((err[0]+err[1])/2);

			// Rotation
			initVal = sourceList.get(i).getRot();
			err[0]=0; err[1]=0;
			for(int sign=-1; sign<=1; sign+=2)
			{
				newVal = initVal;
				k=1;
				do
				{
					newVal += sign*incrRot;
					sourceList.get(i).setRot(newVal);
					BufferedImage image = predictedImageFrame.predictedImage.getImage(SystemParam.getScaledArcsecsPerPix());
					ChiSq.optimizeFlux(image);
					k++;
					if(k>=timeout) break;
				} while(abs(ChiSq.getChiSq() - initChiSq) < chiSqTol);
				if(k<timeout)
				{
					err[(sign==-1)?0:1] = abs(newVal - initVal);
				}
				else
				{
					err[0] = Double.POSITIVE_INFINITY;
					err[1] = Double.POSITIVE_INFINITY;
					break;
				}
			}
			sourceList.get(i).setRot(initVal);
			sourceList.get(i).setRotErr((err[0]+err[1])/2);
		}

		for(int i=0; i<lensList.size(); i++)
		{
			// Position
			double[] initPos = lensList.get(i).getPos();
			double[] posErr = new double[2];
			err[0]=0; err[1]=0;
			for(int j=0; j<=1; j++)
			{
				for(int sign=-1; sign<=1; sign+=2)
				{
					double[] newPos = {initPos[0], initPos[1]};
					k=1;
					do
					{
						newPos[j] += sign*incrPos;
						lensList.get(i).setPos(newPos);
						BufferedImage image = predictedImageFrame.predictedImage.getImage(SystemParam.getScaledArcsecsPerPix());
						ChiSq.optimizeFlux(image);
						k++;
						if(k>=timeout) break;
					} while(abs(ChiSq.getChiSq() - initChiSq) < chiSqTol);
					if(k<timeout)
					{
						err[(sign==-1)?0:1] = abs(newPos[j] - initPos[j]);
					}
					else
					{
						err[0] = Double.POSITIVE_INFINITY;
						err[1] = Double.POSITIVE_INFINITY;
						break;
					}
				}
				lensList.get(i).setPos(initPos);
				posErr[j] = (err[0]+err[1])/2;
			}
			lensList.get(i).setPosErr(posErr);

			// Einstein Radius
			initVal = lensList.get(i).getEinsteinRad();
			err[0]=0; err[1]=0;
			for(int sign=-1; sign<=1; sign+=2)
			{
				newVal = initVal;
				k=1;
				do
				{
					newVal += sign*incrEinsteinRad;
					if(newVal<0)
					{
						newVal = Double.NEGATIVE_INFINITY;
						break;
					}
					lensList.get(i).setEinsteinRad(newVal);
					BufferedImage image = predictedImageFrame.predictedImage.getImage(SystemParam.getScaledArcsecsPerPix());
					ChiSq.optimizeFlux(image);
					k++;
					if(k>=timeout) break;
				} while(abs(ChiSq.getChiSq() - initChiSq) < chiSqTol);
				if(newVal==Double.NEGATIVE_INFINITY)
					err[(sign==-1)?0:1] = Double.NEGATIVE_INFINITY;
				else if(k<timeout)
					err[(sign==-1)?0:1] = abs(newVal - initVal);
				else
				{
					err[0] = Double.POSITIVE_INFINITY;
					err[1] = Double.POSITIVE_INFINITY;
					break;
				}
			}
			lensList.get(i).setEinsteinRad(initVal);
			if(err[0]==Double.NEGATIVE_INFINITY)
				lensList.get(i).setEinsteinRadErr(err[1]);
			else if(err[1]==Double.NEGATIVE_INFINITY)
				lensList.get(i).setEinsteinRadErr(err[0]);
			else
				lensList.get(i).setEinsteinRadErr((err[0]+err[1])/2);

			// Axis Ratio (F)
			initVal = lensList.get(i).getF();
			double initRot = lensList.get(i).getRot();
			err[0]=0; err[1]=0;
			for(int sign=-1; sign<=1; sign+=2)
			{
				newVal = initVal;
				k=1;
				do
				{
					newVal += sign*incrEllip;
					if(newVal<0 || newVal>1)
					{
						newVal = Double.NEGATIVE_INFINITY;
						break;
					}
					lensList.get(i).setF(newVal);
					BufferedImage image = predictedImageFrame.predictedImage.getImage(SystemParam.getScaledArcsecsPerPix());
					ChiSq.optimizeFlux(image);
					k++;
					if(k>=timeout) break;
				} while(abs(ChiSq.getChiSq() - initChiSq) < chiSqTol);
				
				if(newVal==Double.NEGATIVE_INFINITY)
					err[(sign==-1)?0:1] = Double.NEGATIVE_INFINITY;
				else if(k<timeout)
					err[(sign==-1)?0:1] = abs(newVal - initVal);
				else
				{
					err[0] = Double.POSITIVE_INFINITY;
					err[1] = Double.POSITIVE_INFINITY;
					break;
				}
			}
			lensList.get(i).setF(initVal);
			lensList.get(i).setRot(initRot); // We need to reset the rotation because sometimes messing
			                                 // with the axis ratio messes with the rotation as well
			if(err[0]==Double.NEGATIVE_INFINITY)
				lensList.get(i).setFErr(err[1]);
			else if(err[1]==Double.NEGATIVE_INFINITY)
				lensList.get(i).setFErr(err[0]);
			else
				lensList.get(i).setFErr((err[0]+err[1])/2);

			// Rotation
			initVal = lensList.get(i).getRot();
			err[0]=0; err[1]=0;
			for(int sign=-1; sign<=1; sign+=2)
			{
				newVal = initVal;
				k=1;
				do
				{
					newVal += sign*incrRot;
					lensList.get(i).setRot(newVal);
					BufferedImage image = predictedImageFrame.predictedImage.getImage(SystemParam.getScaledArcsecsPerPix());
					ChiSq.optimizeFlux(image);
					k++;
					if(k>=timeout) break;
				} while(abs(ChiSq.getChiSq() - initChiSq) < chiSqTol);
				if(k<timeout)
				{
					err[(sign==-1)?0:1] = abs(newVal - initVal);
				}
				else
				{
					err[0] = Double.POSITIVE_INFINITY;
					err[1] = Double.POSITIVE_INFINITY;
					break;
				}
			}
			lensList.get(i).setRot(initVal);
			lensList.get(i).setRotErr((err[0]+err[1])/2);
		}

		// Recompute the ChiSq after we've put everything back where it belongs
		BufferedImage image = predictedImageFrame.predictedImage.getImage(SystemParam.getScaledArcsecsPerPix());
		ChiSq.optimizeFlux(image);
		GravLensApplet.updateParams();
	}

	/**
	 * Returns true/false depending on whether the i'th pixel is underneath a mask.
	 */
	public static boolean ifUnderMask(int i)
	{
		// If this is a FITS file and the pixel has been turned the purple "masking color,"
		// it's treated as masked.
		int[] astroPixels = astroImageFrame.getPixelsN();
		if(astroPixels[i]==20 && astroPixels[i+1]==20 && astroPixels[i+2]==50)
		{
			return true;
		}

		// Loop through the masks and determine if this pixel is under any of them
		for(int j=0; j<maskList.size(); j++)
		{
			int xPixel = (i%(SystemParam.getN()*3))/3;
			int yPixel = (i-xPixel)/(SystemParam.getN()*3);
			int[] distVec = {xPixel - maskList.get(j).getPosPixels()[0]*SystemParam.getN()/SystemParam.getFullN(),
				yPixel - maskList.get(j).getPosPixels()[1]*SystemParam.getN()/SystemParam.getFullN()};
			double rad = maskList.get(j).getRad()/SystemParam.getScaledArcsecsPerPix();

			// Find the angular position of the pixel in the ellipse's unrotated
			// coordinate system.
			double theta = atan2(distVec[1], distVec[0]) + maskList.get(j).getRot();

			// Find the edge of the maskList.get(j) for this value of theta
			double realE = rad*cos(theta);
			double imagE = rad*(1-maskList.get(j).getEllip())*sin(theta);

			int x = (int) round(realE * cos(-maskList.get(j).getRot())
					- imagE * sin(-maskList.get(j).getRot()));
			int y = (int) round(imagE * cos(-maskList.get(j).getRot())
					+ realE * sin(-maskList.get(j).getRot()));

			// We return true if this pixel is under any of the masks
			if ((pow(distVec[0], 2) + pow(distVec[1], 2)) <= (pow(x, 2) + pow(y, 2)))
			{
				return true;
			}
		}

		// If we've made it to this point in the code, this pixel must not have been under any mask
		return false;
	}
}
