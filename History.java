/* 
+ ======================================================================

 NAME:
   History.java

 PURPOSE:
   This class stores previous states of source and lens configurations
	 and allows for easy recall of a a previous state.
   
 COMMENTS:
   Add any comments on working of class here
   
 USAGE:
	 History.java [options] inputs

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
	   GravLensApplet.java

 BUGS:
   List all known (or suspected) bugs here
  
 REVISION HISTORY:
   2010-02-23  started Naudus (GMU)

- ======================================================================
*/

import java.util.*;

public class History
{
	private ArrayList<Source> sourceList;
	private ArrayList<Lens> lensList;

	// Source Parameters
	private Vector<Double> sourceRot = new Vector<Double>();
	private Vector<Double> sourceEllip = new Vector<Double>();
	private Vector<Double> sourceRad = new Vector<Double>();
	private Vector<Double[]> sourcePos = new Vector<Double[]>();

	//Lens Parameters
	private Vector<Double> lensF = new Vector<Double>();
	private Vector<Double> lensEinsteinRad = new Vector<Double>();
	private Vector<Double> lensRot = new Vector<Double>();

	/**
	 * Constructor
	 */
	public History(ArrayList<Source> sourceList, ArrayList<Lens> lensList)
	{
		this.sourceList = sourceList;
		this.lensList = lensList;
	}

	/**
	 * Append the current values of source and lens configurations to the respective vectors
	 */
	public void update()
	{
		/*
		this.sourceRot.add(new Double(sourceList.get(0).getRot()));
		this.sourceEllip.add(new Double(sourceList.get(0).getEllip()));
		this.sourceRad.add(new Double(sourceList.get(0).getRad()));

		double[] primitiveSourcePos = sourceList.get(0).getPos();
		Double[] objSourcePos = {new Double(primitiveSourcePos[0]), new Double(primitiveSourcePos[1])};
		this.sourcePos.add(objSourcePos);

		this.lensF.add(new Double(lensList.get(0).getF()));
		this.lensEinsteinRad.add(new Double(lensList.get(0).getEinsteinRad()));
		this.lensRot.add(new Double(lensList.get(0).getRot()));
		*/
	}

	/**
	 * Rolls back the Source and Lens classes to an earlier time (specified by an index).
	 * Use indices 0, 1, 2, ... to grab elements the elements oldest in history.
	 * Use indices -1, -2, -3, ... to grab elements most recent in history.
	 * (0 is oldest, -1 is most recent)
	 */
	public void rollBack(int index)
	{
		if(index < 0)
		{
			index = sourceRot.size()+index;
		}

		sourceList.get(0).setRot(sourceRot.get(index).doubleValue());
		sourceList.get(0).setEllip(sourceEllip.get(index).doubleValue());
		sourceList.get(0).setRad(sourceRad.get(index).doubleValue());

		Double[] objSourcePos = sourcePos.get(index);
		double[] primitiveSourcePos = {objSourcePos[0].doubleValue(), objSourcePos[1].doubleValue()};
		sourceList.get(0).setPos(primitiveSourcePos);

		lensList.get(0).setF(lensF.get(index).doubleValue());
		lensList.get(0).setEinsteinRad(lensEinsteinRad.get(index).doubleValue());
		lensList.get(0).setRot(lensRot.get(index).doubleValue());
	}
}
