/* 
+ ======================================================================

 NAME:
   PredictedImage.java

 PURPOSE:
   This class retrieves the system configuration from Source.java and
	 Lens.java and generates the resulting predicted image.
   
 COMMENTS:
   Add any comments on working of class here
   
 USAGE:
	 PredictedImage.java [options] inputs

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
	   Source.java
		 Lens.java
	 Called by:
	   PredictedImageFrame.java

 BUGS:
   - magnification not calculated correctly (not simple sum) PJM 2010-04-08
   - Gaussian PSF implemented as a hard-coded "blur radius" - this should
     be data image -dependent
  
 REVISION HISTORY:
   2010-02-23  started Naudus (GMU)

- ======================================================================
*/

import java.awt.*;
import java.awt.image.*;
import java.util.*;
import static java.lang.Math.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;

public class PredictedImage
{
	private BufferedImage image;
	private Vector<Double> caustic;
	private ArrayList<Source> sourceList;
	private ArrayList<Lens> lensList;

	double[] observation;
	
	// Image parameters
	private int[] pixels;

	/**
	 * Constructor
	 */
	public PredictedImage(ArrayList<Source> sourceList, ArrayList<Lens> lensList)
	{
		this.sourceList = sourceList;
		this.lensList = lensList;
	}

	// Given a particular resolution, compute the predicted image
	public BufferedImage getImage(double rho)
	{
		// TODO: PSF is not the "blurRad" (gaussian width), but FWHM.
		float blurRad = (float)(SystemParam.getPSF()/SystemParam.getScaledArcsecsPerPix());
		int pad = (int) (((sqrt(2) - 1) / 2) * SystemParam.getN() + blurRad) + 1;

		image = new BufferedImage(SystemParam.getN(), SystemParam.getN(), BufferedImage.TYPE_INT_RGB);
		int pN = SystemParam.getN() + 2*pad;
		BufferedImage pImage = new BufferedImage(pN, pN, BufferedImage.TYPE_INT_RGB);

		double[] X = new double[2], Y = new double[2],
			alpha = new double[2];
		double gauss_A, gauss_a, gauss_b, gauss_c, sigmaX, sigmaY;

		this.pixels = new int[SystemParam.getN() * SystemParam.getN() * 3];
		int[] pPixels = new int[pN * pN * 3];

		observation = new double[pN*pN];

		/*
		 * Draw the predicted image
		 */
		int i, j, k, z;
		for (j = 0; j <= pN - 1; j += 1)
		{
			for (i = 0; i <= pN - 1; i += 1)
			{
				X[0] = rho * (i - ((double) pN - 1.) / 2.);
				X[1] = rho * (j - ((double) pN - 1.) / 2.);

				// Compute alpha for each lens and sum them together
				alpha[0] = alpha[1] = 0;
				for(k=0; k<lensList.size(); k++)
				{
					computeAlpha(X, alpha, k);
				}

				// The lens equation
				Y[0] = X[0] - alpha[0];
				Y[1] = X[1] - alpha[1];

				// Compute the gravitatially lensed image for each source
				// and sum them together
				z = (j * pN + i);
				observation[z] = 0;
				for(k=0; k<sourceList.size(); k++)
				{
					// Set the profile of the source surface
					// brightness to be Gaussian:
					sigmaX = sourceList.get(k).getRad();
					sigmaY = sourceList.get(k).getRad()
						*(1.-sourceList.get(k).getEllip());

					gauss_a = pow(cos(sourceList.get(k).getRot()),2)/(2*pow(sigmaX,2))
						+ pow(sin(sourceList.get(k).getRot()),2)/(2*pow(sigmaY,2));
					gauss_b = -sin(2*sourceList.get(k).getRot())/(4*pow(sigmaX,2))
						+ sin(2 * sourceList.get(k).getRot())/(4*pow(sigmaY,2));
					gauss_c = pow(sin(sourceList.get(k).getRot()),2)/(2*pow(sigmaX,2))
						+ pow(cos(sourceList.get(k).getRot()),2)/(2*pow(sigmaY,2));

					// Normalize this gaussian function to unity
					gauss_A = sqrt(gauss_c*gauss_a - pow(gauss_b,2))/PI/sourceList.size();

					observation[z] += gauss_A
						*exp(-(gauss_a*pow(Y[0]-sourceList.get(k).getPos()[0],2)
									+ 2*gauss_b*(Y[0]-sourceList.get(k).getPos()[0])
									  *(Y[1]-sourceList.get(k).getPos()[1])
									+ gauss_c*pow(Y[1]-sourceList.get(k).getPos()[1], 2)));
				}
			}
		}

		setPixelFlux(observation, pPixels, pad);

		pImage.getRaster().setPixels(0, 0, pN, pN, pPixels);

		/*
		 * Blur the predicted image - blurRad = FWHM of PSF in pixels
		 */
		pImage = new ConvolveOp(gaussKernel(blurRad, true),
				ConvolveOp.EDGE_NO_OP, null).filter(pImage, null);
		pImage = new ConvolveOp(gaussKernel(blurRad, false),
				ConvolveOp.EDGE_NO_OP, null).filter(pImage, null);

		// Clip the padding off of the predicted image
		pImage.getRaster().getPixels(pad, pad, SystemParam.getN(), SystemParam.getN(), this.pixels);
		image.getRaster().setPixels(0, 0, SystemParam.getN(), SystemParam.getN(), this.pixels);

		return image;
	}

	// Compute the caustic, given a particular size and resolution.
	public Vector<Double> getCaustic(int N, double rho)
	{
		Vector<Double> caustic = new Vector<Double>();
		double[] X = new double[2], Xrot = new double[2], Y = new double[2],
			alpha = new double[2];
		double b, mu, gamma1, gamma2, kappa;
		double thisgamma1, thisgamma2, thiskappa, phi;

		int i, j, k;
		for (j = 0; j <= N - 1; j += 1)
		{
			for (i = 0; i <= N - 1; i += 1)
			{
				X[0] = rho * (i - ((double) N - 1.) / 2.);
				X[1] = rho * (j - ((double) N - 1.) / 2.);

				// Compute alpha for each lens and sum them together
				// Compute the magnification for each lens and sum them together
				alpha[0] = alpha[1] = 0;
				kappa = gamma1 = gamma2 = 0;
				for(k=0; k<lensList.size(); k++)
				{
					// Counter-rotate X so that we can calculate the rotated magnification
					Xrot[0] = (X[0]-lensList.get(k).getPos()[0])
						*cos(-lensList.get(k).getRot())
						- (X[1]-lensList.get(k).getPos()[1])
						*sin(-lensList.get(k).getRot())
						+ lensList.get(k).getPos()[0];
					Xrot[1] = (X[0]-lensList.get(k).getPos()[0])
						*sin(-lensList.get(k).getRot())
						+ (X[1]-lensList.get(k).getPos()[1])
						*cos(-lensList.get(k).getRot())
						+ lensList.get(k).getPos()[1];
					
					computeAlpha(X, alpha, k);
					
					b = sqrt(pow(Xrot[0]-lensList.get(k).getPos()[0],2)
						+ pow(lensList.get(k).getF()
							*(Xrot[1]-lensList.get(k).getPos()[1]),2));
					phi = atan2(X[1] - lensList.get(k).getPos()[1],
						X[0] - lensList.get(k).getPos()[0]);

					// Compute the second derivatives of the lens 
					// potential for this lens component:
					thiskappa = lensList.get(k).getEinsteinRad()
						*sqrt(lensList.get(k).getF())/(2*b);
					thisgamma1 = -thiskappa*cos(2*phi);
					thisgamma2 = -thiskappa*sin(2*phi);
					
					// Add the potential derivatives for this lens
					// component to the total:
					kappa += thiskappa;
					// BUG: gamma += sqrt(pow(gamma1,2) + pow(gamma2,2));
					gamma1 += thisgamma1;
					gamma2 += thisgamma2;
				}

				// Find the magnification, from accumulated 
				// derivatives:
				mu = abs(1/(pow((1-kappa),2) - pow(gamma1,2) - pow(gamma2,2)));

				Y[0] = X[0] - alpha[0];
				Y[1] = X[1] - alpha[1];
				
				// Consider the current point to be part of the critical curve if the magnification exceeds 100.
				if (mu > 100)
				{
					caustic.add(X[0]);
					caustic.add(X[1]);
					caustic.add(Y[0]);
					caustic.add(Y[1]);
				}
			}
		}

		return caustic;
	}
	
	// Compute the deflection angle of a particular lens (lens number i) based
	// on the position in the image plane (X). Add this to alphaTotal.
	private void computeAlpha(double[] X, double[] alphaTotal, int i)
	{
		// Compute the angle (phi) from the of the lens to this particular point (X[0], X[1]).
		// If the lens is at the center of the image and hasn't been rotated, phi is simply
		// atan2(X[1], X[0]).
		// If the lens has been translated, phi must be adjusted so that phi remains the
		// angle from the center of the lens (not necessarily from the center of the image).
		// If the lens has been rotated by some angle, phi must also be decreased by that angle.
		double phi = atan2(X[1] - lensList.get(i).getPos()[1],
			X[0] - lensList.get(i).getPos()[0]) - lensList.get(i).getRot();

		double[] alpha = {lensList.get(i).getAlphaCoef()
				*asinh((lensList.get(i).getFP()/lensList.get(i).getF())*cos(phi)),
			lensList.get(i).getAlphaCoef()
				*asin(lensList.get(i).getFP()*sin(phi))};

		// Rotate alpha
		alphaTotal[0] += (alpha[0])*cos(lensList.get(i).getRot())
			- (alpha[1])*sin(lensList.get(i).getRot());
		alphaTotal[1] += (alpha[0])*sin(lensList.get(i).getRot())
			+ (alpha[1])*cos(lensList.get(i).getRot());
	}

	/**
	 * Make a Gaussian blur kernel. I referenced
	 * http://www.jhlabs.com/ip/blurring.html
	 */
	private static Kernel gaussKernel(float FWHM, boolean horizOrVert)
	{
		int r = (int) Math.ceil(FWHM);
		int rows = r * 2 + 1;
		float[] matrix = new float[rows];
		float sigma = FWHM / (float) 2.35;
		float sigma22 = 2 * sigma * sigma;
		float sigmaPi2 = 2 * (float) PI * sigma;
		float sqrtSigmaPi2 = (float) sqrt(sigmaPi2);
		float radius2 = FWHM * FWHM;
		float total = 0;
		int index = 0;
		for (int row = -r; row <= r; row++) {
			float distance = row * row;
			if (distance > radius2)
				matrix[index] = 0;
			else
				matrix[index] = (float) Math.exp(-(distance) / sigma22)
						/ sqrtSigmaPi2;
			total += matrix[index];
			index++;
		}
		for (int i = 0; i < rows; i++)
			matrix[i] /= total;

		return new Kernel((horizOrVert ? rows : 1), (horizOrVert ? 1 : rows),
				matrix);
	}

	/**
	 * This method will renormalize the predicted image in order to set the flux of the three
	 * channels to the values which are passed in (each channel is renormalized separately)
	 * Input: double[] pixels : Array of pixels corresponding to the predicted image
	 */
	public void setPixelFlux(double[] observation, int[] pixels, int pad)
	{
		int pN = SystemParam.getN() + 2*pad;
		
		double sum = 0;
		for(int i=0; i<pN*pN; i++)
		{
			sum += observation[i];
		}
		Lens.magnification = sum*(SystemParam.getScaledArcsecsPerPix()*SystemParam.getScaledArcsecsPerPix());
		double avg = sum/(SystemParam.getN()*SystemParam.getN());
		
		for(int i=0; i<pixels.length; i+=3)
		{
			pixels[i] = (int)(observation[i/3]*Source.getBrightness()*Source.getAvgRed()/avg);
			pixels[i+1] = (int)(observation[i/3]*Source.getBrightness()*Source.getAvgGreen()/avg);
			pixels[i+2] = (int)(observation[i/3]*Source.getBrightness()*Source.getAvgBlue()/avg);
		
			for(int j=0; j<=2; j++)
			{
				if(pixels[i+j]>255)
				{
					pixels[i+j]=255;
				}
			}
		}
	}
	
	/**
	 * private static double asinh Arguments: double z Returns: double =>
	 * hyperbolic sin of z.
	 */
	private static double asinh(double z) {
		return log(z + sqrt(pow(z, 2) + 1.));
	}
}
