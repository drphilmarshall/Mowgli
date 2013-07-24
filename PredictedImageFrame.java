/* 
+ ======================================================================

 NAME:
   PredictedImageFrame.java

 PURPOSE:
   This class functions solely as an interactive display frame. Its primary
	 purpose is to provide a portal between the user and PredictedImage.java.
	 This class will retrieve mouse-action input from the user, interpret the
	 results and pass them to Source.java and Lens.java. Then, it will
	 retrieve the resulting predicted image from PredictedImage.java and
	 display it to the user.
   
 COMMENTS:
   Add any comments on working of class here
   
 USAGE:
	 PredictedImageFrame.java [options] inputs

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
		 PrefictedImage.java
		 ResizeAstroEllip.java
	   GravLensApplet.java
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

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;

import static java.lang.Math.*;

import java.awt.geom.AffineTransform;
import java.awt.geom.*;
import java.awt.image.BufferedImage;

public class PredictedImageFrame extends JPanel implements InterfaceComponent
{
	public PredictedImage predictedImage;
	private BufferedImage image;

	private ArrayList<Source> sourceList;
	private ArrayList<Lens> lensList;
	private ArrayList<Mask> maskList;
	private AstroImageFrame astroImageFrame;
	private PredictedImageFrame predictedImageFrame = this;

	// Image parameters
	private int blurRad = 8;
	private int pad = (int)(((sqrt(2)-1)/2)*SystemParam.getN()+blurRad)+1;
	private int pN = SystemParam.getN() + 2*pad;
	private int[] caustic;
	private boolean ifHiResCaustic = true;
	private boolean ifShowResidual = false;

	private int[] pixelsN = new int[SystemParam.getN()*SystemParam.getN()*3];
	
	private JPopupMenu sourcePopup = new JPopupMenu();
	
	private int[] cursor = new int[2];
	private ArrayList<ResizeAstroEllip> sourceListeners = new ArrayList<ResizeAstroEllip>();
	private int[] whichProperty;
	
	/**
	 * Constructor
	 */
	public PredictedImageFrame(ArrayList<Source> sourceList, ArrayList<Lens> lensList, ArrayList<Mask> maskList, AstroImageFrame astroImageFrame, int[] whichProperty)
	{
		this.sourceList = sourceList;
		this.lensList = lensList;
		this.maskList = maskList;
		this.astroImageFrame = astroImageFrame;
		this.whichProperty = whichProperty;

		this.predictedImage = new PredictedImage(sourceList, lensList);
		updateCaustic(SystemParam.getFullN());

		// TODO: The actual image doesn't seem to be this exact size. Fix that.
		setMinimumSize(new Dimension(SystemParam.getFullN()+2,SystemParam.getFullN()+2));
		setPreferredSize(new Dimension(SystemParam.getFullN()+2,SystemParam.getFullN()+2));
		setMaximumSize(new Dimension(SystemParam.getFullN()+2,SystemParam.getFullN()+2));
		
		setBackground(Color.black);
		
		whichProperty[0] = 900;
		
		// Add Listeners
		addMouseListener(new MouseListener());
		addMouseMotionListener(new MouseMotionListener());

		// Initalize the popup menus
		ActionListener popupListener = new PopupActionListener();
		JMenuItem addSource = new JMenuItem("Add source here");
			addSource.addActionListener(popupListener);
			sourcePopup.add(addSource);
		JMenuItem delSource= new JMenuItem("Delete active source");
			delSource.addActionListener(popupListener);
			sourcePopup.add(delSource);
	}

	public void updateCaustic(int N)
	{
		Vector<Double> caustic = predictedImage.getCaustic(N, SystemParam.getScaledArcsecsPerPix()*((double)SystemParam.getN()/N));
		this.caustic = new int[caustic.size()];

		double rho = SystemParam.getScaledArcsecsPerPix()*((double)SystemParam.getN()/N);
		double xyMin = -rho*((double)N-1.)/2.;
		double xyMax = rho*((double)N-1.)/2.;
		int x, y;
		for(int i=0; i<caustic.size(); i+=2)
		{
			x = (int)round((double)SystemParam.getFullN()*(caustic.get(i)-xyMin)/(xyMax-xyMin));
			y = (int)round((double)SystemParam.getFullN()*(caustic.get(i+1)-xyMin)/(xyMax-xyMin));

			this.caustic[i] = x;
			this.caustic[i+1] = y;
		}
	}

	public void updateImage()
	{
		this.image = getImage(ifShowResidual, true);
		repaint();
	}

	public BufferedImage getImage(boolean ifShowResidual, boolean drawDisplaySources)
	{
		BufferedImage smallImage = predictedImage.getImage(SystemParam.getScaledArcsecsPerPix());
		smallImage.getRaster().getPixels(0, 0, SystemParam.getN(), SystemParam.getN(), this.pixelsN);

		ChiSq.optimizeFlux(smallImage);
	
		// If we're supposed to show the resudual image, set smallImage to the residual
		// Otherwise, leave it the way it is
		if(ifShowResidual)
		{
			int[] residualPixels = ChiSq.getResidualPixels();
			smallImage.getRaster().setPixels(0, 0, SystemParam.getN(), SystemParam.getN(), residualPixels);
		}

		// Rescale the image
		BufferedImage image = new BufferedImage(SystemParam.getFullN(), SystemParam.getFullN(), smallImage.getType());
		Graphics2D g2 = image.createGraphics();
		AffineTransform at = AffineTransform.getTranslateInstance(0, 0);
		at.scale((double)SystemParam.getFullN()/SystemParam.getN(), (double)SystemParam.getFullN()/SystemParam.getN());
		g2.drawRenderedImage(smallImage, at);
		g2.dispose();

		return(paintCurves(image, drawDisplaySources));
	}

	/**
	 * This method paints the caustics and sources on top of an image.
	 * Arguments:
	 *     BufferedImage image: unpainted image
	 *     boolean drawDisplaySources: true = fill sources with translucent
	 *          center and draw sources with the display size.
	 *                                 false = do not fill sources and draw
	 *          sources with the actual size.
	 *  Returns:
	 *     BufferedImage image: image with representative curves
	 */
	public BufferedImage paintCurves(BufferedImage image, boolean drawDisplaySources)
	{
		/*
		 * Draw representative curves on the image
		 */
		if(Lens.isAdjusting)
		{
			updateCaustic(3*SystemParam.getN());
			ifHiResCaustic = false;
		} else if(!ifHiResCaustic || Lens.newLens)
		{
			ifHiResCaustic = true;
			Lens.newLens = false;
			updateCaustic(SystemParam.getFullN());
		}

		int[] pixelsFullN = new int[3*SystemParam.getFullN()*SystemParam.getFullN()];
		image.getRaster().getPixels(0, 0, SystemParam.getFullN(), SystemParam.getFullN(), pixelsFullN);

		int i, z;
		if(!Lens.isAdjusting)
		{
			// Draw the high-resolution caustic
			for(i=0; i<caustic.length; i+=2)
			{
				z = 3*(caustic[i+1]*SystemParam.getFullN() + caustic[i]);
				if(caustic[i]<SystemParam.getFullN()-1 && caustic[i]>0
					&& caustic[i+1]<SystemParam.getFullN()-1 && caustic[i+1]>0
					&& z+2<pixelsFullN.length)
				{
					pixelsFullN[z] = 0;
					pixelsFullN[z+1] = 255;
					pixelsFullN[z+2] = 0;
				}
			}
		} else
		{
			// Draw the low-resolution caustic
			for(i=0; i<caustic.length; i+=2)
			{
				for(int j=-2; j<=2; j++)
				{
					for(int k=-2; k<=2; k++)
					{
						z = 3*((caustic[i+1]+j)*SystemParam.getFullN() + (caustic[i]+k));
						if(caustic[i]<SystemParam.getFullN()-1 && caustic[i]>1
							&& caustic[i+1]<SystemParam.getFullN()-1 && caustic[i+1]>1
							&& z+2<pixelsFullN.length)
						{
							pixelsFullN[z] = 0;
							pixelsFullN[z+1] = 255;
							pixelsFullN[z+2] = 0;
						}
					}
				}
			}			
		}

		image.getRaster().setPixels(0, 0, SystemParam.getFullN(), SystemParam.getFullN(), pixelsFullN);

		// Draw the sources
		double rad;
		for(i=0; i<sourceList.size(); i++)
		{
			Graphics2D g2d = image.createGraphics();
			Source source = sourceList.get(i);
			if(drawDisplaySources && sourceList.get(i).getIsHover())
			{
				rad = source.getGrowRad()/SystemParam.getArcsecsPerPix();
			} else
			{
				rad = source.getRad()/SystemParam.getArcsecsPerPix();
			}

			g2d.translate(source.getPosPixels()[0], source.getPosPixels()[1]);
			g2d.rotate(PI-source.getRot());
			g2d.translate(-source.getPosPixels()[0], -source.getPosPixels()[1]);
				
			// Draw the filled ellipse
			if(drawDisplaySources)
			{
				if(sourceList.get(i).getIsActive())
				{
					g2d.setColor(Color.white);
					g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float)0.75));
				} else if(sourceList.get(i).getIsHover())
				{
					g2d.setColor(Color.yellow);
					g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float)0.25));
				} else
				{
					g2d.setColor(Color.gray);
					g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float)0.2));
				}
		
				g2d.fill(new Ellipse2D.Double(source.getPosPixels()[0]-rad,
					source.getPosPixels()[1]-rad*abs(1-source.getEllip()),
					2*rad, 2*rad*abs(1-source.getEllip())));
			}
	
			// Draw the border around the ellipse
			g2d.setColor(Color.white);
			g2d.draw(new Ellipse2D.Double(source.getPosPixels()[0]-rad,
				source.getPosPixels()[1]-rad*abs(1-source.getEllip()),
				2*rad, 2*rad*abs(1-source.getEllip())));
		}

		// Draw x's at the centers of the lenses
		rad = 2;
		for(i=0; i<lensList.size(); i++)
		{
			Graphics2D g2d = image.createGraphics();
			Lens lens = lensList.get(i);

			// Draw the border around the ellipse
			g2d.setColor(Color.red);
			g2d.draw(new Line2D.Double(
						lens.getPosPixels()[0]+blurRad/2.5-rad, lens.getPosPixels()[1]+blurRad/2.5-rad,
						lens.getPosPixels()[0]+blurRad/2.5+rad, lens.getPosPixels()[1]+blurRad/2.5+rad));
			g2d.draw(new Line2D.Double(
						lens.getPosPixels()[0]+blurRad/2.5+rad, lens.getPosPixels()[1]+blurRad/2.5-rad,
						lens.getPosPixels()[0]+blurRad/2.5-rad, lens.getPosPixels()[1]+blurRad/2.5+rad));
		}

		return image;
	}

//	public BufferedImage getPredictedImage()
//	{
//		return paintCurves(predictedImage.getImage(SystemParam.getScaledArcsecsPerPix()), false);
//	}
//
//	public BufferedImage getResidualImage()
//	{
//		int[] residualPixels = ChiSq.getResidualPixels();
//		BufferedImage image = new BufferedImage(SystemParam.getN(), SystemParam.getN(), BufferedImage.TYPE_INT_RGB);
//		image.getRaster().setPixels(0, 0, SystemParam.getN(), SystemParam.getN(), residualPixels);
//		image = paintCurves(image, false);
//
//		return image;
//	}
	
	public int[] getPixelsN()
	{
		return this.pixelsN;
	}

	// TODO: Get rid of this function eventially.	
	public int[] getUnscaledPixelsN()
	{
		BufferedImage unscaledImage = predictedImage.getImage(SystemParam.getScaledArcsecsPerPix());
		int[] pixels = new int[3*SystemParam.getN()*SystemParam.getN()];
		unscaledImage.getRaster().getPixels(0, 0, SystemParam.getN(), SystemParam.getN(), pixels);
		return pixels;
	}
	
	public void updateListeners()
	{
		for(int i=0; i<sourceListeners.size(); i++)
		{
			removeMouseMotionListener(this.sourceListeners.get(i));
		}

		sourceListeners.clear();
		
		for(int i=0; i<sourceList.size(); i++)
		{
			ResizeAstroEllip listener = sourceList.get(i).getListener(this, whichProperty);
			sourceListeners.add(listener);
			addMouseMotionListener(listener);
		}
	}
	
	public void setIfShowResidual(boolean ifShowResidual)
	{
		this.ifShowResidual = ifShowResidual;
	}
	
	// Set the isHover values of all the sources to false, except for the
	// one on top (Only one can be true at a time).
	public void setHoverEllip()
	{
		int hoverIndex = -1;
		for(int i=sourceList.size()-1; i>=0; i--)
		{
			if(hoverIndex>=0)
			{
				sourceList.get(i).setIsHover(false);
			} else
			{
				if(sourceList.get(i).getIsHover())
				{
					hoverIndex = i;
				}
			}
		}
		
		// If the cursor is not hovering over a source, set the cursor to the
		// default cursor
		if(hoverIndex==-1)
		{
			this.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
		}
		updateImage();
	}
	
	public void inactiveAllEllip()
	{
		int i;
		for(i=0; i<sourceList.size(); i++)
		{
			sourceList.get(i).setIsActive(false);
		}
	}	

	// Raise the active source to the top of the stack
	public void raiseActiveEllip()
	{
		int activeIndex = 0;
		// First find the index of the active source
		for(int i=0; i<sourceList.size(); i++)
		{
			if(sourceList.get(i).getIsActive())
			{
				activeIndex = i;
			}
		}
		
		// Now, raise the source to the top
		sourceList.add(sourceList.get(activeIndex));
		sourceList.remove(activeIndex);
	}
	
	// Add a source to the system
	public Source addSource()
	{
		Source source = new Source();
		sourceList.add(source);
		
		inactiveAllEllip();
		source.setIsActive(true);
		updateListeners();
		
		return source;
	}
	
	/**
	 * public void paintComponent
	 * Arguments: Graphics g
	 * Returns: Nothing
	 */
	public void paint(Graphics g)
	{
		g.drawImage(image, 1, 1, Color.black, this);
		g.setColor(Color.white);
		g.drawLine(0,0,SystemParam.getFullN(),0);
		g.drawLine(0,0,0,SystemParam.getFullN());
		g.drawLine(SystemParam.getFullN()+1,0,SystemParam.getFullN()+1,SystemParam.getFullN()+1);
		g.drawLine(SystemParam.getFullN()+1,SystemParam.getFullN()+1,0,SystemParam.getFullN()+1);
	}
	
	/**
	 * Mouse action handlers
	 */
	class MouseListener extends MouseAdapter
	{
		public void mousePressed(MouseEvent e)
		{
			maybeShowPopup(e);
		}

		public void mouseReleased(MouseEvent e)
		{
			maybeShowPopup(e);

			updateListeners();
		}

		private void maybeShowPopup(MouseEvent e)
		{
			if (e.isPopupTrigger())
			{
				sourcePopup.show(e.getComponent(), e.getX(), e.getY());
				cursor[0] = e.getX();
				cursor[1] = e.getY();
			}
		}
	}
	
	class MouseMotionListener extends MouseMotionAdapter
	{
		public void mouseMoved(MouseEvent e)
		{
			Cursor cursor = null;
			Cursor temp;
			for(int i=0; i<sourceListeners.size(); i++)
			{
				temp = sourceListeners.get(i).getCursor(e);
				if(temp != null)
					cursor = temp;
			}
			if(cursor != null)
			{
				predictedImageFrame.setCursor(cursor);
			}
		}
	}
	
	class PopupActionListener implements ActionListener
	{
		public void actionPerformed(ActionEvent e)
		{
			if(e.getActionCommand()=="Add source here")
			{
				Source source = addSource();
				source.setPosPixels(cursor);
				GravLensApplet.update();
			} else if(e.getActionCommand()=="Delete active source")
			{
				for(int i=0; i<sourceList.size(); i++)
				{
					if(sourceList.get(i).getIsActive())
					{
						sourceList.remove(i--);
					}
				}
				updateListeners();
				GravLensApplet.update();
			}
		}
	}	
}
