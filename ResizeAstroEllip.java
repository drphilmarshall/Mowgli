/* 
+ ======================================================================

 NAME:
   ResizeAstroEllip.java

 PURPOSE:
   This class provides general functions for resizing ellipses
   corresponding to astronomical objects, such as sources and
   lens masks.
   
 COMMENTS:
   Add any comments on working of class here
   
 USAGE:
   This class is to be called as a mouse listener like so:
   addMouseMotionListener(new ResizeAstroEllip(this, ellipModel, whichProperty, SystemParam.getRho(), N, SystemParam.getFullN()));

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
     AstroEllip.java
     GravLensApplet.java
   Called by:
     PredictedImageFrame.java
     AstroImageFrame.java

 BUGS:
   > Does not change the cursor back to normal when the cursor is not
     hovering over an element of interest.
   > Does not update the caustic to the full resolution on mouse up.
   > need to fix mouseDragged class
  
 REVISION HISTORY:
   2010-02-23  started Naudus (GMU)

- ======================================================================
 */

import static java.lang.Math.*;
import java.awt.*;
import java.awt.event.*;

public class ResizeAstroEllip extends MouseMotionAdapter {
	private InterfaceComponent component;
	private InterfaceEllip ellipModel;

	private int[] cursorPos = new int[2];
	private int[] whichProperty;

	private double theta;

	// Define some custom cursors
	private Toolkit toolkit = Toolkit.getDefaultToolkit();
	private Point hotSpotH = new Point(10, 5);
	private Point hotSpotV = new Point(5, 10);
	private Point hotSpotRot = new Point(5, 10);
	private Cursor resizeH = toolkit.createCustomCursor(toolkit.getImage(getClass()
			.getResource("/images/resizeH.png")), hotSpotH, "Ellipticity");
	private Cursor resizeV = toolkit.createCustomCursor(toolkit.getImage(getClass()
			.getResource("/images/resizeV.png")), hotSpotV, "Ellipticity");
	private Cursor ellipH = toolkit.createCustomCursor(toolkit.getImage(getClass()
			.getResource("/images/ellipH.png")), hotSpotH, "Ellipticity");
	private Cursor ellipV = toolkit.createCustomCursor(toolkit.getImage(getClass()
			.getResource("/images/ellipV.png")), hotSpotV, "Ellipticity");
	private Cursor rotate = toolkit.createCustomCursor(toolkit.getImage(getClass()
			.getResource("/images/rotate.png")), hotSpotRot, "Rotation");

	public ResizeAstroEllip(InterfaceComponent component, InterfaceEllip ellipModel, int[] whichProperty)
	{
		this.component = component;
		this.ellipModel = ellipModel;
		this.whichProperty = whichProperty;
	}

	public void mouseDragged(MouseEvent e)
	{
		// Only activate dragging features if the mouse is hovering over this ellipModel
		if(!ellipModel.getIsHover())
		{
			return;
		}
		
		// If this lens isn't the active lens, make it active
		if(!ellipModel.getIsActive())
		{
			component.inactiveAllEllip();
			ellipModel.setIsActive(true);
			component.raiseActiveEllip();
		}

		int[] oldCursor = {cursorPos[0], cursorPos[1]};
		cursorPos[0] = e.getX();
		cursorPos[1] = e.getY();

		// The difference vector between the previous and current cursor positions
		int[] diffVec = {cursorPos[0]-oldCursor[0], cursorPos[1]-oldCursor[1]};
		
		// The distance vector between the cursor and the ellipModel center and the cursor
		double[] distVec = {cursorPos[0] - ellipModel.getPosPixels()[0], cursorPos[1] - ellipModel.getPosPixels()[1]};

		if (whichProperty[0] == 0)
		{
			// Move the ellipModel the same number of pixels by which the cursor has moved
			int[] ellipPosPixels = { ellipModel.getPosPixels()[0] + diffVec[0],
					ellipModel.getPosPixels()[1] + diffVec[1] };
			ellipModel.setPosPixels(ellipPosPixels);
			ellipModel.setIsAdjusting(true);
		} else if (whichProperty[0] == 1)
		{
			// We want to set ellipModel's radius so that the edge of the ellipModel
			// at angle theta is at the position of the cursor.
			
			// theta is the angular position of the mouse in the ellipse's
			// unrotated reference frame.
			double theta = PI/2 + ellipModel.getRot() - atan2(distVec[0], distVec[1]);
			
			// Find the edge of the ellipModel for this value of theta
			double x = cos(theta);
			double y = (1-ellipModel.getEllip())*sin(theta);
		
			double rad = sqrt((pow(distVec[0],2) + pow(distVec[1],2))/(pow(x,2) + pow(y,2)));
			
			ellipModel.setGrowRad(SystemParam.getScaledArcsecsPerPix()
					*((double)SystemParam.getN()/SystemParam.getFullN())*rad);
			
			ellipModel.setIsAdjusting(true);
		} else if (whichProperty[0] == 2)
		{
			double theta = PI/2 + ellipModel.getRot() - atan2(distVec[0], distVec[1]);
			if(theta<0)
				theta+=2*PI;
			else if(theta>=2*PI)
				theta-=2*PI;

			double distAbs = sqrt(pow(distVec[0], 2) + pow(distVec[1], 2));

			// The "radius" (semi-major axis)
			double rad = ellipModel.getGrowRad()
				*(SystemParam.getFullN()/SystemParam.getN())/SystemParam.getScaledArcsecsPerPix();
			// The semi-minor axis
			double minor = rad*(1-ellipModel.getEllip());

			if ((theta>PI/4 && theta<3*PI/4) || (theta>5*PI/4 && theta<7*PI/4))
			{
				if(distAbs>rad)
				{
					ellipModel.setRot(ellipModel.getRot() + PI/2);
				}
				else
				{
					double ellip = 1 - distAbs / rad;
					ellipModel.setEllip(1 - distAbs / rad);
				}
			}
			else
			{
				if(distAbs < minor)
				{
					ellipModel.setRot(ellipModel.getRot() + PI/2);
				}
				else
				{
					// We want to stretch the ellipse in the *other* direction by changing
					// the radius, but keeping the product of the radius and (1-e) constant.

					// This is the product we'll be keeping constant.
					double prod = ellipModel.getRad()*(1-ellipModel.getEllip());

					// Set the radius
					double x = cos(theta);
					double y = (1-ellipModel.getEllip())*sin(theta);
					ellipModel.setGrowRad(SystemParam.getScaledArcsecsPerPix()
							*((double)SystemParam.getN()/SystemParam.getFullN())
							*distAbs/(pow(x,2) + pow(y,2)));

					// Now set the ellipticity to keep prod constant
					ellipModel.setEllip(1 - (prod/ellipModel.getRad()));
				}
			}
			ellipModel.setIsAdjusting(true);
		} else if (whichProperty[0] == 3)
		{
			double[] oldDistVec = {
					oldCursor[0] - ellipModel.getPosPixels()[0],
					oldCursor[1] - ellipModel.getPosPixels()[1] };

			double absDistVec = sqrt(pow(distVec[0], 2) + pow(distVec[1], 2));
			double absOldDistVec = sqrt(pow(oldDistVec[0], 2)
					+ pow(oldDistVec[1], 2));

			// Find the angle between the old cursor position and the new cursor
			// position using the cross product
			double crossProd = distVec[0] * oldDistVec[1] - distVec[1]
					* oldDistVec[0];
			double theta = asin(crossProd / (absDistVec * absOldDistVec));

			ellipModel.setRot(ellipModel.getRot() + theta);

			ellipModel.setIsAdjusting(true);
		}

		if (whichProperty[0] != 900)
		{
			GravLensApplet.update();
		}
	}

	public Cursor getCursor(MouseEvent e)
	{
		Cursor cursor = null;
		cursorPos[0] = e.getX();
		cursorPos[1] = e.getY();
		
		// Find if the cursor is near enough to the ellipse to be
		// moving/resizing it.
		int[] distVec = {cursorPos[0] - ellipModel.getPosPixels()[0], cursorPos[1] - ellipModel.getPosPixels()[1]};
		double rad = ellipModel.getGrowRad()
			*(((double) SystemParam.getFullN()/SystemParam.getN()))
			/SystemParam.getScaledArcsecsPerPix();

		// Find the angular position of the mouse in the ellipse's unrotated
		// coordinate system.
		double theta = atan2(distVec[1], distVec[0]) + ellipModel.getRot();

		// Find the edge of the ellipModel for this value of theta
		double realE = (rad+10)*cos(theta);
		double imagE = (rad+10)*(1-ellipModel.getEllip())*sin(theta);

		int x = (int) round(realE * cos(-ellipModel.getRot())
				- imagE * sin(-ellipModel.getRot()));
		int y = (int) round(imagE * cos(-ellipModel.getRot())
				+ realE * sin(-ellipModel.getRot()));

		// Determine if the cursor is hovering over the ellipModel
		if((pow(distVec[0], 2) + pow(distVec[1], 2)) <= (pow(x, 2) + pow(y, 2)))
		{
			ellipModel.setIsHover(true);
			component.setHoverEllip();
		} else
		{
			ellipModel.setIsHover(false);
			component.setHoverEllip();
			return null;
		}
	
		realE = rad*cos(theta);
		imagE = rad*(1-ellipModel.getEllip())*sin(theta);

		x = (int) round(realE * cos(-ellipModel.getRot())
				- imagE * sin(-ellipModel.getRot()));
		y = (int) round(imagE * cos(-ellipModel.getRot())
				+ realE * sin(-ellipModel.getRot()));
		
		// Determine if the cursor is in position to move or resize the ellipModel
		if ((pow(distVec[0], 2) + pow(distVec[1], 2)) <= (pow(x, 2) + pow(y, 2)))
		{
			cursor = Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR);
			whichProperty[0] = 0; // sourcePos
			ellipModel.setIsHover(true); component.setHoverEllip();
		} else
		{
			if (e.isControlDown() || e.isAltDown())
			{
				if (abs(distVec[0]) > abs(distVec[1]))
				{
					cursor = ellipH;
				} else {
					component.setCursor(ellipV);
				}
				whichProperty[0] = 2; // sourceEllip
			} else if (e.isShiftDown()) {
				cursor = rotate;
				whichProperty[0] = 3; // sourceRot
			} else
			{
				if (abs(distVec[0]) > abs(distVec[1]))
				{
					cursor = resizeH;
				} else {
					cursor = resizeV;
				}
				whichProperty[0] = 1; // sourceRad
			}
		}
		return cursor;
	}
}
