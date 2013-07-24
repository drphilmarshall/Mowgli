/* 
+ ======================================================================

 NAME:
   OptimizableFunction.java

 PURPOSE:
   This class provides an interface between MOWGLI and MultivariateRealOptimizer
	 from Apache Commons.
   
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
   2011-06-28  started Naudus (GMU)

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

public class OptimizableFunction implements MultivariateRealFunction
{
	private PredictedImage predictedImage;
	private ArrayList<Source> sourceList;
	private ArrayList<Lens> lensList;
//	private ArrayList<Source> origSourceList = new ArrayList<Source>();
//	private ArrayList<Lens> origLensList = new Arraylist<Lens>();

	/**
	 * Constructor
	 */
	public OptimizableFunction(PredictedImage predictedImage, ArrayList<Source> sourceList, ArrayList<Lens> lensList)
	{
		this.predictedImage = predictedImage;
		this.sourceList = sourceList;
		this.lensList = lensList;

/*
		double[] pos = new double[2];
		for(int i=0; i<sourceList.size(); i++)
		{
			origSourceList.add(new Source());
			pos[0] = sourceList.get(i).getPos()[0];
			pos[1] = sourceList.get(i).getPos()[1];
			origSourceList.get(i).setPos(pos);
			origSourceList.get(i).setRad(sourceList.get(i).getRad());
			origSourceList.get(i).setEllip(sourceList.get(i).getEllip());
			origSourceList.get(i).setRot(sourceList.get(i).getRot());
		}

		double[] pos = new double[2];
		for(int i=0; i<sourceList.size(); i++)
		{
			origLensList.add(new Lens());
			pos[0] = lensList.get(i).getPos()[0];
			pos[1] = lensList.get(i).getPos()[1];
			origLensList.get(i).setPos(pos);
			origLensList.get(i).setEinsteinRad(lensList.get(i).getEinsteinRad());
			origLensList.get(i).setF(lensList.get(i).getF());
			origLensList.get(i).setRot(lensList.get(i).getRot());
		}
*/
	}
	
	public double value(double[] x)
	{
		int j = 0;
		double[] pos = new double[2];

		for(int i=0; i<sourceList.size(); i++)
		{
			pos[0] = x[j++];
			pos[1] = x[j++];
			sourceList.get(i).setPos(pos);
			sourceList.get(i).setRad(x[j++]);
			sourceList.get(i).setEllip(x[j++]);
			sourceList.get(i).setRot(x[j++]);
		}

		for(int i=0; i<lensList.size(); i++)
		{
			pos[0] = x[j++];
			pos[1] = x[j++];
			lensList.get(i).setPos(pos);
			lensList.get(i).setEinsteinRad(x[j++]);
			lensList.get(i).setF(x[j++]);
			lensList.get(i).setRot(x[j++]);
		}

		BufferedImage image = predictedImage.getImage(SystemParam.getScaledArcsecsPerPix());
		ChiSq.optimizeFlux(image);

		return ChiSq.getChiSq();
	}
}
