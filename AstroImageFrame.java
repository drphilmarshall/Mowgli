/* 
+ ======================================================================

 NAME:
   AstroImageFrame.java

 PURPOSE:
   This class functions solely as an interactive display frame. Its primary
   purpose is to provide a portal between the user and the astronomical
   image. This class will retrieve mouse-action input from the user,
   interpret the results and pass them to Lens.java.
   
 COMMENTS:
   Add any comments on working of class here
   
 USAGE:
	 AstroImageFrame.java [options] inputs

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

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.AffineTransform;
import java.awt.image.*;
import java.util.*;
import java.awt.geom.*;
import java.io.*;
import static java.lang.Math.*;

import org.apache.commons.math.stat.descriptive.moment.*;
import org.apache.commons.lang3.ArrayUtils;

import javax.swing.*;
import javax.imageio.*;

import eap.fits.*;
import eap.filter.*;
import eap.fitsbrowser.*;

public class AstroImageFrame extends JPanel implements InterfaceComponent
{
	private ArrayList<Source> sourceList;
	private ArrayList<Lens> lensList;
	private ArrayList<Mask> maskList;

	private BufferedImage image;
	private Image origImage;
	private BufferedImage smallImage;
	private BufferedImage rebinnedImage;
	private BufferedImage unmaskedRebinnedImage;

	private int[] pixelsN;
	private int[] pixelsFullN;
	private int numUnmasked;	// The number of pixels not under the mask
	
	private JPopupMenu massPopup = new JPopupMenu();
	private JPopupMenu maskPopup = new JPopupMenu();

	private int[] cursor = new int[2];
	private ArrayList<ResizeAstroEllip> massListeners = new ArrayList<ResizeAstroEllip>();
	private ArrayList<ResizeAstroEllip> maskListeners = new ArrayList<ResizeAstroEllip>();
	private int whichListener = 1;
	private int[] whichProperty;

	AstroImageFrame astroImageFrame = this;
	public int[] red, green, blue;
	private boolean sdssRotate = false;
	
	/**
	 * Constructor
	 */
	public AstroImageFrame(ArrayList<Source> sourceList, ArrayList<Lens> lensList, ArrayList<Mask> maskList, int[] whichProperty)
	{
		this.sourceList = sourceList;
		this.lensList = lensList;
		this.maskList = maskList;
		this.whichProperty = whichProperty;

		this.pixelsFullN = new int[SystemParam.getFullN()*SystemParam.getFullN()*3];
		this.pixelsN = new int[SystemParam.getN()*SystemParam.getN()*3];
		
		this.red = new int[SystemParam.getN()*SystemParam.getN()];
		this.green = new int[SystemParam.getN()*SystemParam.getN()];
		this.blue = new int[SystemParam.getN()*SystemParam.getN()];
		
		image = new BufferedImage(SystemParam.getFullN(), SystemParam.getFullN(), BufferedImage.TYPE_INT_RGB);

		setPreferredSize(new Dimension(SystemParam.getFullN()+2,SystemParam.getFullN()+2));
		setBackground(Color.black);

		whichProperty[0] = 900;
		
		// Initalize the popup menus
		ActionListener popupListener = new PopupActionListener();
		JMenuItem addMass = new JMenuItem("Add lens here");
			addMass.addActionListener(popupListener);
			massPopup.add(addMass);
		JMenuItem delMass = new JMenuItem("Delete active lens");
			delMass.addActionListener(popupListener);
			massPopup.add(delMass);
		JMenuItem addMask = new JMenuItem("Add mask here");
			addMask.addActionListener(popupListener);
			maskPopup.add(addMask);
		JMenuItem delMask = new JMenuItem("Delete active mask");
			delMask.addActionListener(popupListener);
			maskPopup.add(delMask);

		addMouseListener(new MouseListener());
		addMouseMotionListener(new MouseMotionListener());
	}

	public void setCandidateImage()
	{
		String filename = GravLensApplet.getCandidateImageName();
		int index = filename.lastIndexOf('.');
		String extension = filename.substring(index+1,filename.length());

		try
		{
			// Import the candidate image
			if(extension.equals("fit") || extension.equals("fits"))
			{
				this.origImage = getFitsImage("data/" + filename);
				SystemParam.ifFits = true;
			}
			else
			{
				this.origImage = ImageIO.read(new FileInputStream("data/" + filename));
				SystemParam.defaultPSF();
				SystemParam.setBackground(0);
				SystemParam.ifFits = false;
			}

			ImageIcon imageIcon = new ImageIcon(this.origImage);
			int size = max(imageIcon.getIconWidth(), imageIcon.getIconHeight());
			BufferedImage fullImage = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
			fullImage.getGraphics().drawImage(this.origImage, 0, 0, null);

			/*
			 * Rebin the image. We're going to rebin it by shrinking it
			 * then rescaling it to its original size.
			 */

			// Scale fullImage so that it is shrunk to size N x N.
			// "smallImage" is the new rescaled image
			smallImage = new BufferedImage(SystemParam.getN(), SystemParam.getN(), fullImage.getType());
			Graphics2D g1 = smallImage.createGraphics();
			AffineTransform at1 = AffineTransform.getTranslateInstance(0, 0);
			at1.scale((double)SystemParam.getN()/size, (double)SystemParam.getN()/size);
			if(sdssRotate)
			{
				// If this is a Sloan image, it needs to be rotated 90 degrees counter-clockwise
				// in order to align the image and data coordinates
				if(filename.equals("14.fits") || filename.equals("16.fits"))
					at1.rotate(-PI, size/2, size/2);
				else
					at1.rotate(-PI/2, size/2, size/2);
			}
			g1.drawRenderedImage(fullImage, at1);
			g1.dispose();

			smallImage.getRaster().getPixels(0, 0, SystemParam.getN(), SystemParam.getN(), this.pixelsN);

			BufferedImage unmaskedSmallImage = new BufferedImage(SystemParam.getN(), SystemParam.getN(), fullImage.getType());
			unmaskedSmallImage.getRaster().setPixels(0, 0, SystemParam.getN(), SystemParam.getN(), this.pixelsN);

			// If a "masking image file" exists, we'll open this image and set the candidate image's pixels
			// to black where the masking image's pixels fall below SystemParam.maskThreshold.
			String maskFile = "data/" + filename + "_mask.jpg";
			ArrayList<Double> noisePixels = new ArrayList<Double>(SystemParam.getN()*SystemParam.getN());
			if((new File(maskFile)).exists())
			{
				Image maskImage = ImageIO.read(new FileInputStream(maskFile));
				ImageIcon maskIcon = new ImageIcon(maskImage);
				size = max(maskIcon.getIconWidth(), maskIcon.getIconHeight());
				BufferedImage fullMask = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
				fullMask.getGraphics().drawImage(maskImage, 0, 0, null);

				// Scale fullMask so that it is shrunk to size N x N.
				BufferedImage smallMask = new BufferedImage(SystemParam.getN(), SystemParam.getN(), fullMask.getType());
				g1 = smallMask.createGraphics();
				at1 = AffineTransform.getTranslateInstance(0, 0);
				at1.scale((double)SystemParam.getN()/size, (double)SystemParam.getN()/size);
				g1.drawRenderedImage(fullMask, at1);
				g1.dispose();
				int[] pixelsMask = new int[SystemParam.getN()*SystemParam.getN()*3];
				smallMask.getRaster().getPixels(0, 0, SystemParam.getN(), SystemParam.getN(), pixelsMask);

				for(int i=0; i<this.pixelsN.length; i+=3)
				{
					// Note that the weights are simply present to convert RGB to grayscale
					if( (0.3*pixelsMask[i] + 0.59*pixelsMask[i+1] + 0.11*pixelsMask[i+2]) <=SystemParam.maskThreshold)
					{
//						if(SystemParam.ifFits)
//						{
							noisePixels.add(new Double(this.pixelsN[i]));
//						}

						this.pixelsN[i] = 20;
						this.pixelsN[i+1] = 20;
						this.pixelsN[i+2] = 50;
					}
				}
				noisePixels.trimToSize();
				smallImage.getRaster().setPixels(0, 0, SystemParam.getN(), SystemParam.getN(), this.pixelsN);
			}

			// Checking if this list is not empty is a quick way to verify that a mask file exists,
			// the mask threshold is greater than zero, and the image being analyzed is a FITS image.
			//if(!noisePixels.isEmpty())
			//{
				Double[] noisePixelsArray = new Double[noisePixels.size()];
				noisePixels.toArray(noisePixelsArray);
				StandardDeviation stdev = new StandardDeviation();
				//System.out.println(stdev.evaluate(ArrayUtils.toPrimitive(noisePixelsArray)));
				SystemParam.setSigmaFITS(stdev.evaluate(ArrayUtils.toPrimitive(noisePixelsArray)));
			//}
			//else
			//{
			//	SystemParam.setSigmaFITS(1);
			//}

			// Scale smallImage so that it is SystemParam.getFullN() x SystemParam.getFullN().
			// "rebinnedImage" is the new rescaled image
			this.rebinnedImage = new BufferedImage(SystemParam.getFullN(), SystemParam.getFullN(), this.smallImage.getType());
			Graphics2D g2 = this.rebinnedImage.createGraphics();
			AffineTransform at2 = AffineTransform.getTranslateInstance(0, 0);
			at2.scale((double)SystemParam.getFullN()/SystemParam.getN(), (double)SystemParam.getFullN()/SystemParam.getN());
			g2.drawRenderedImage(this.smallImage, at2);
			g2.dispose();

			this.unmaskedRebinnedImage = new BufferedImage(SystemParam.getFullN(), SystemParam.getFullN(), unmaskedSmallImage.getType());
			g2 = this.unmaskedRebinnedImage.createGraphics();
			at2 = AffineTransform.getTranslateInstance(0, 0);
			at2.scale((double)SystemParam.getFullN()/SystemParam.getN(), (double)SystemParam.getFullN()/SystemParam.getN());
			g2.drawRenderedImage(unmaskedSmallImage, at2);
			g2.dispose();

			updateChannels();
		} catch(IOException e) { System.out.println("Error: " + e.getMessage()); }
	}

	public void updateImage()
	{
		this.rebinnedImage.getRaster().getPixels(0, 0, SystemParam.getFullN(), SystemParam.getFullN(), this.pixelsFullN);

		// Draw the astronomical image
		image.getRaster().setPixels(0, 0, SystemParam.getFullN(), SystemParam.getFullN(), this.pixelsFullN);
		
		int size;
		if(MassModel.isVisible)
		{
			size = lensList.size();
		} else if(Mask.isVisible)
		{
			size = maskList.size();
		} else
		{
			repaint();
			return;
		}
		
		// Display the elliptical models (mask or mass models) of the lenses
		double rad;
		for(int i=0; i<size; i++)
		{
			Graphics2D g2d = image.createGraphics();
			
			InterfaceEllip ellipModel;
			if(Mask.isVisible)
			{
				ellipModel = maskList.get(i);
				if(maskList.get(i).getIsActive())
				{
					g2d.setColor(Color.black);
					g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float)0.75));
				} else if(maskList.get(i).getIsHover())
				{
					g2d.setColor(Color.yellow);
					g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float)0.25));
				} else
				{
					g2d.setColor(Color.gray);
					g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float)0.2));
				}
			} else if(MassModel.isVisible)
			{
				ellipModel = lensList.get(i).mass;
				if(lensList.get(i).getIsActive())
				{
					g2d.setColor(Color.red);
					g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float)0.25));
				} else if(lensList.get(i).getIsHover())
				{
					g2d.setColor(Color.yellow);
					g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float)0.25));
				} else
				{
					g2d.setColor(Color.gray);
					g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float)0.2));
				}
			} else
			{
				ellipModel = null;
			}
	
			if(ellipModel != null)
			{
				rad = ellipModel.getRad()/SystemParam.getArcsecsPerPix();
		
				g2d.translate(ellipModel.getPosPixels()[0], ellipModel.getPosPixels()[1]);
				g2d.rotate(PI-ellipModel.getRot());
				g2d.translate(-ellipModel.getPosPixels()[0], -ellipModel.getPosPixels()[1]);
				
				// Draw the filled ellipse
				g2d.fill(new Ellipse2D.Double(ellipModel.getPosPixels()[0]-rad,
					ellipModel.getPosPixels()[1]-rad*abs(1-ellipModel.getEllip()),
					2*rad, 2*rad*abs(1-ellipModel.getEllip())));
		
				// Draw the border around the ellipse
				g2d.setColor(Color.white);
				g2d.draw(new Ellipse2D.Double(ellipModel.getPosPixels()[0]-rad,
					ellipModel.getPosPixels()[1]-rad*abs(1-ellipModel.getEllip()),
					2*rad, 2*rad*abs(1-ellipModel.getEllip())));
			}
		}
		
		if(Lens.isAdjusting || Mask.isAdjusting)
		{
			updateChannels();
		}
		
		repaint();
	}

	/*
	 * This function splits the pixels array into three arrays, one for each channel (color)
	 * and updates the properties of the source(s) so that the predicted image can be
	 * normalized to have the same brightness/color as the astronomical image.
	 */
	public void updateChannels()
	{
		this.rebinnedImage.getRaster().getPixels(0, 0, SystemParam.getFullN(), SystemParam.getFullN(), this.pixelsFullN);
		
		this.numUnmasked = 0;
		for(int i=0; i<3*(this.blue.length-2); i+=3)
		{
			// Grab the properties of this pixel if it is outside of the mask
			if(!ChiSq.ifUnderMask(i))
			{
				this.numUnmasked += 1;
				this.red[i/3] = this.pixelsN[i];
				this.green[(i/3)+1] = this.pixelsN[i+1];
				this.blue[(i/3)+2] = this.pixelsN[i+2];
			}
			else
			{
				// Set it to infinity so that it won't appear in the lowest 20%
				this.red[i/3] = this.green[(i/3)+1] = this.blue[(i/3)+2] = (int)Double.POSITIVE_INFINITY;
			}
		}

		// Sort the three arrays
		Arrays.sort(this.red);
		Arrays.sort(this.green);
		Arrays.sort(this.blue);

		double[] avgPixels = astroImageFrame.getAvgPixels(1);
		Source.setAvgRed(avgPixels[0]);
		Source.setAvgGreen(avgPixels[1]);
		Source.setAvgBlue(avgPixels[2]);
	}

	/**
	 * Returns the average over some fraction of the darkest pixels (the fraction is specified as
	 * a decimal number in the input). If 0.2 is passed in, this function returns the average over
	 * the darkest 20% of the pixels which are not under the mask. of 1.0 is passed in, this
	 * function returns the average over all pixels not under the mask
	 */
	public double[] getAvgPixels(double fraction)
	{
		int sumRed = 0, sumGreen = 0, sumBlue = 0;
		// We only need consider the first fraction of the pixels since the arrays are sorted
		for(int i=0; i<fraction*this.numUnmasked; i++)
		{
			sumRed += red[i];
			sumGreen += green[i];
			sumBlue += blue[i];
		}

		double[] returnArray = {(double)sumRed/(fraction*this.numUnmasked),
			(double)sumGreen/(fraction*this.numUnmasked),
			(double)sumBlue/(fraction*this.numUnmasked)};

		return returnArray;
	}

	/**
	 * Returns the pixelsN array
	 */
	public int[] getPixelsN()
	{
		return pixelsN;
	}

	public BufferedImage getRebinnedImage()
	{
		return rebinnedImage;
	}

	public BufferedImage getUnmaskedRebinnedImage()
	{
		return unmaskedRebinnedImage;
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
	 * public void setWhichListener(int whichListener)
	 * Arguments: int whichListener =>	0: set the MouseMotionListener to nothing
	 *                                  1: set the MouseMotionListener to massListeners
	 *                                  2: set the MouseMotionListener to maskListeners
	 * Returns: Nothing
	 */
	public void setWhichListener(int whichListener)
	{
		for(int i=0; i<massListeners.size(); i++)
		{
			removeMouseMotionListener(this.massListeners.get(i));
		}
		for(int i=0; i<maskListeners.size(); i++)
		{
			removeMouseMotionListener(this.maskListeners.get(i));
		}
		massListeners.clear();
		maskListeners.clear();
		
		this.whichListener = whichListener;
		if(whichListener==1)
		{
			for(int i=0; i<lensList.size(); i++)
			{
				ResizeAstroEllip listener = lensList.get(i).mass.getListener(this, whichProperty);
				massListeners.add(listener);
				addMouseMotionListener(listener);
			}
		} else if(whichListener==2)
		{
			for(int i=0; i<maskList.size(); i++)
			{
				ResizeAstroEllip listener = maskList.get(i).getListener(this, whichProperty);
				massListeners.add(listener);
				addMouseMotionListener(listener);
			}
		}
	}
	
	// Set the isHover values of all the mass models to false, except for the
	// one on top (Only one can be true at a time).
	public void setHoverEllip()
	{
		int hoverIndex = -1;
		if(MassModel.isVisible)
		{
			for(int i=lensList.size()-1; i>=0; i--)
			{
				if(hoverIndex>=0)
				{
					lensList.get(i).setIsHover(false);
				} else
				{
					if(lensList.get(i).getIsHover())
					{
						hoverIndex = i;
					}
				}
			}
		} else if(Mask.isVisible)
		{
			for(int i=maskList.size()-1; i>=0; i--)
			{
				if(hoverIndex>=0)
				{
					maskList.get(i).setIsHover(false);
				} else
				{
					if(maskList.get(i).getIsHover())
					{
						hoverIndex = i;
					}
				}
			}
		}
		
		// If the cursor is not hovering over a lens, set the cursor to the
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
		if(MassModel.isVisible)
		{
			for(i=0; i<lensList.size(); i++)
			{
				lensList.get(i).setIsActive(false);
			}
		} else if(Mask.isVisible)
		{
			for(i=0; i<maskList.size(); i++)
			{
				maskList.get(i).setIsActive(false);
			}
		}
	}	

	// Raise the active lens to the top of the stack
	public void raiseActiveEllip()
	{
		int activeIndex = 0;
		if(MassModel.isVisible)
		{
			// First find the index of the active lens
			for(int i=0; i<lensList.size(); i++)
			{
				if(lensList.get(i).getIsActive())
				{
					activeIndex = i;
				}
			}
			
			// Now, raise the lens to the top
			lensList.add(lensList.get(activeIndex));
			lensList.remove(activeIndex);
		} else if(Mask.isVisible)
		{
			// First find the index of the active lens
			for(int i=0; i<maskList.size(); i++)
			{
				if(maskList.get(i).getIsActive())
				{
					activeIndex = i;
				}
			}
			
			// Now, raise the lens to the top
			maskList.add(maskList.get(activeIndex));
			maskList.remove(activeIndex);
		}	
	}
	
	// Add a lens to the system
	public Lens addLens()
	{
		Lens lens = new Lens();
		lensList.add(lens);
		setWhichListener(whichListener);
		
		inactiveAllEllip();
		lens.setIsActive(true);
	
		Lens.newLens = true;
		return lens;
	}
	
	// Add a mask to the system
	public Mask addMask()
	{
		Mask mask = new Mask();

		maskList.add(mask);
		maskListeners.add(mask.getListener(this, whichProperty));
		setWhichListener(whichListener);
		
		inactiveAllEllip();
		mask.setIsActive(true);
		
		return mask;
	}

	/**
	 * Extracts the image from a FITS file (input argument "filename" must be a FITS file)
	 * and returns it as an Image object.
	 * If the FITS file contains the PSF width, this value is set in the SystemParam class.
	 * Otherwise, the PSF is set to the default specified in the SystemParam class.
	 */
	public Image getFitsImage(String filename) throws IOException
	{
		File file = new File(filename);
		FitsFile fits = null;
		FilterBroker filters = new FilterBroker();

		if(filters.hasFilter(filename))
		{
			InputStream in = new FileInputStream(file);
			InputStream filtered =filters.filter(in, file.getName());

			if(filtered != in)
			{
				fits = new InputStreamFitsFile(filtered);
			}
		}

		if(fits==null)
		{
			fits = new RandomAccessFitsFile(new RandomAccessFile(file,"r"));
		}
		FitsHDU hdu = fits.getHDU(0);
		FitsData data = hdu.getData();
		FitsImageData imageData = (FitsImageData) data;
		RealImageProducer view = imageData.createView();

		FitsImageViewer viewer = new FitsImageViewer(imageData);
		ColorModel cm = viewer.makeColorModel(Color.black, Color.white);
		ImageDigitizer digitizer =  new ImageDigitizer(view, cm);

		// Let's set the PSF and background if the data is included in the FITS file
		FitsHeader header = hdu.getHeader();
		SystemParam.defaultPSF();
		SystemParam.setBackground(0);
		for(int i=0; i<header.cardCount(); ++i)
		{
			String str = header.card(i).toString();
			if(str.startsWith("PSFWID ") || str.startsWith("SKY ") || str.startsWith("ORIGIN "))
			{
				String val = new String(str);
				int index = val.indexOf('=');
				if(index != -1)
					val = val.substring(index+1,val.length());

				index = val.indexOf('/');
				if(index != -1)
					val = val.substring(0,index-1);

				if(str.startsWith("PSFWID "))
				{
					SystemParam.setPSF(Double.valueOf(val.trim()).doubleValue());
				} else if(str.startsWith("SKY "))
				{
					SystemParam.setBackground(Double.valueOf(val.trim()).doubleValue());
				} else if(str.startsWith("ORIGIN "))
				{
					if(val.trim().startsWith("'SDSS"))
					{
						// Because this is a Sloan image, we know that the image and
						// data coordinates are offset by 90 degrees. Flag this image
						// for rotation.
						sdssRotate = true;
					}
				}
			}
		}

		return this.getToolkit().createImage(digitizer);
	}

	class MouseListener extends MouseAdapter
	{
		public void mousePressed(MouseEvent e)
		{
			maybeShowPopup(e);
		}

		public void mouseReleased(MouseEvent e)
		{
			maybeShowPopup(e);

			Lens.isAdjusting = false;
			GravLensApplet.update();
			GravLensApplet.updateHistory();

			setWhichListener(whichListener);
		}

		private void maybeShowPopup(MouseEvent e)
		{
			if (e.isPopupTrigger())
			{
				if(MassModel.isVisible)
				{
					massPopup.show(e.getComponent(), e.getX(), e.getY());
				} else if(Mask.isVisible)
				{
					maskPopup.show(e.getComponent(), e.getX(), e.getY());
				}
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
			for(int i=0; i<massListeners.size(); i++)
			{
				temp = massListeners.get(i).getCursor(e);
				if(temp != null)
					cursor = temp;
			}
			if(cursor != null)
			{
				astroImageFrame.setCursor(cursor);
			}
		}
	}
	
	class PopupActionListener implements ActionListener
	{
		public void actionPerformed(ActionEvent e)
		{
			if(e.getActionCommand()=="Add lens here")
			{
				Lens lens = addLens();
				lens.setPosPixels(cursor);
				GravLensApplet.update();
			} else if(e.getActionCommand()=="Delete active lens")
			{
				for(int i=0; i<lensList.size(); i++)
				{
					if(lensList.get(i).mass.getIsActive())
					{
						lensList.remove(i--);
					}
				}
				// Set the lens to adjusting in order to get the caustic updated.
				Lens.isAdjusting = true;
				GravLensApplet.update();
				Lens.isAdjusting = false;
				GravLensApplet.update();
			} else if(e.getActionCommand()=="Add mask here")
			{
				Mask mask = addMask();
				mask.setPosPixels(cursor);
				GravLensApplet.update();
			} else if(e.getActionCommand()=="Delete active mask")
			{
				maskList.remove(maskList.size()-1);
				GravLensApplet.update();
			}
		}
	}
}
