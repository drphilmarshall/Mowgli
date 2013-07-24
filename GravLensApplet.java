/*
+ ======================================================================

 NAME:
   GravLensApplet.java

 PURPOSE:
   This class functions as the main applet class. Its main purpose is
   to call and update the various display frames and update the history.
     All frame updates are passed through this class before the screen
     is updated for the user to see.

 COMMENTS:
   Add any comments on working of class here

 USAGE:
     GravLensApplet.java [options] inputs

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
       AstroImageFrame.java
         PredictedImageFrame.java
         Source.java
         Lens.java
         History.java
   Called by:
       AstroImageFrame.java
         PredictedImageFrame.java

 BUGS:
   List all known (or suspected) bugs here

 REVISION HISTORY:
   2010-02-23  started Naudus (GMU)

- ======================================================================
*/

import java.awt.*;

import javax.swing.*;
import java.util.*;
import java.applet.*;
import java.awt.event.*;
import javax.swing.event.*;
import javax.imageio.*;
import java.io.*;
import java.text.*;

public class GravLensApplet extends Applet
{
    private static ArrayList<Source> sourceList = new ArrayList<Source>();
    private static ArrayList<Lens> lensList = new ArrayList<Lens>();
    private static ArrayList<Mask> maskList = new ArrayList<Mask>();

    private static JToggleButton massToggle = new JToggleButton("Mass");
    private static JToggleButton maskToggle = new JToggleButton("Mask");
    private static JToggleButton predToggle = new JToggleButton("Predicted");
    private static JToggleButton residToggle = new JToggleButton("Residual");
    private static JToggleButton optimizeFlux = new JToggleButton("Optimize Flux");
    private static JButton optimizeParams = new JButton("Optimize Params");
    private static JButton computeUncertainties = new JButton("Update Uncertainties");
    private static JButton exportParams = new JButton("Export Parameters");
    private static JTextArea paramOutput = new JTextArea();
    private static JScrollPane paramScroll = new JScrollPane(paramOutput, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
    private static JButton updateParams = new JButton("Update");

    // BUG: paramaeter values are given as eg 00.601 - need to lose a zero at the front.
    private static String headerStrSource = "|   xs   |  err   |   ys   |  err   |  Reff  |  err   |  eps   |  err   |   PA   |  err   |";
    private static String headerStrLens   = "|   xd   |  err   |   yd   |  err   |  Rein  |  err   |   q    |  err   |   PA   |  err   |";
    private static String headerStrMask =   "|   xm   |   ym   |  rad   |  eps   |   PA   |";

    // Standard example input image. Image is obtained from "data"
    // folder:
    private static String candidateImageName = "CSWA5_15x15arcsec.jpg";


    // whichProperty is which property we're varying.
    // Note that this is a 1-element array simply to allow
    // us to pass it by reference.
    // 0: sourcePos
    // 1: sourceRad
    // 2: sourceEllip
    // 3: sourceRot
    // 4: lensPos
    // 5: einsteinRad
    // 6: lensF
    // 7: lensRot
    // 900: Nothing
    private static int[] whichProperty = new int[1];

    private static SystemParam systemParam = new SystemParam();
    private static History history = new History(sourceList, lensList);
    private static AstroImageFrame astroImageFrame = new AstroImageFrame(sourceList, lensList, maskList, whichProperty);
    private static PredictedImageFrame predictedImageFrame = new PredictedImageFrame(sourceList, lensList, maskList, astroImageFrame, whichProperty);
    private static ChiSq chiSq = new ChiSq(astroImageFrame, predictedImageFrame, sourceList, lensList, maskList);
    private static JSlider threshold = new JSlider(JSlider.VERTICAL, 0, 40, SystemParam.maskThreshold);

    // Define some custom cursors
    private Toolkit toolkit = Toolkit.getDefaultToolkit();
    private Point hotSpotH = new Point(10,5);
    private Point hotSpotV = new Point(5,10);
    private Point hotSpotRot = new Point(5,10);
    private Cursor resizeH = toolkit.createCustomCursor(toolkit.getImage(getClass().getResource("/images/resizeH.png")), hotSpotH, "Ellipticity");
    private Cursor resizeV = toolkit.createCustomCursor(toolkit.getImage(getClass().getResource("/images/resizeV.png")), hotSpotV, "Ellipticity");
    private Cursor ellipH = toolkit.createCustomCursor(toolkit.getImage(getClass().getResource("/images/ellipH.png")), hotSpotH, "Ellipticity");
    private Cursor ellipV = toolkit.createCustomCursor(toolkit.getImage(getClass().getResource("/images/ellipV.png")), hotSpotV, "Ellipticity");
    private Cursor rotate = toolkit.createCustomCursor(toolkit.getImage(getClass().getResource("/images/rotate.png")), hotSpotRot, "Rotation");

    private GravLensApplet applet;
    private boolean standaloneMode = false;

    public GravLensApplet()
    {
//      init();
//      start();
    }

    // This constructor will only be called if the applet is being
    // run as a standalone application.
    public GravLensApplet(boolean standaloneMode)
    {
        this.standaloneMode = standaloneMode;
        init();
        start();
    }

    // This constructor will only be called if a candidate image
    // is provided as a command line argument
    public GravLensApplet(boolean standaloneMode, String candidateImageName)
    {
        this(standaloneMode);

        String oldCandidateImageName = GravLensApplet.candidateImageName;
        GravLensApplet.candidateImageName = candidateImageName;
        updateParams();
        GravLensApplet.candidateImageName = oldCandidateImageName;
        parseParams();
    }

    public void init()
    {
        applet = this;

        this.add(predictedImageFrame);
        this.add(astroImageFrame);
        this.add(threshold);

        this.add(massToggle);
            massToggle.setSelected(true);
            massToggle.addActionListener(new ButtonListener());
        this.add(maskToggle);
            maskToggle.setSelected(false);
            maskToggle.addActionListener(new ButtonListener());

        predToggle.setSelected(true);
            this.add(predToggle);
            predToggle.addActionListener(new ButtonListener());
        residToggle.setSelected(false);
            this.add(residToggle);
            residToggle.addActionListener(new ButtonListener());

        this.add(paramScroll);
        this.add(updateParams);
            updateParams.addActionListener(new ButtonListener());
        this.add(optimizeFlux);
            optimizeFlux.addActionListener(new ButtonListener());
        this.add(optimizeParams);
            optimizeParams.addActionListener(new ButtonListener());
        this.add(computeUncertainties);
            computeUncertainties.addActionListener(new ButtonListener());

        if(standaloneMode)
        {
            this.add(exportParams);
            exportParams.addActionListener(new ButtonListener());
        }

        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventPostProcessor(new KeyHandler());

        FocusListener focusListener = new FocusListener() {
            public void focusGained(FocusEvent e) {
                paramOutput.requestFocus();
            }

            public void focusLost(FocusEvent e) {
                paramOutput.requestFocus();
            }
        };
        this.addFocusListener(focusListener);
    }

    public void start()
    {
        updateParams();

        SpringLayout layout = new SpringLayout();
        this.setLayout(layout);

        layout.putConstraint(SpringLayout.WEST, predictedImageFrame, 303, SpringLayout.WEST, this);
        layout.putConstraint(SpringLayout.NORTH, predictedImageFrame, 0, SpringLayout.NORTH, this);
        astroImageFrame.setBorder(BorderFactory.createEtchedBorder());

        layout.putConstraint(SpringLayout.WEST, astroImageFrame, 0, SpringLayout.WEST, this);
        layout.putConstraint(SpringLayout.NORTH, astroImageFrame, 0, SpringLayout.NORTH, this);

        /*
         * Brightness slider
         */

        //Create the slider
        threshold.setBackground(Color.black);
        threshold.setForeground(Color.white);

        threshold.setMajorTickSpacing(10);
        threshold.setMinorTickSpacing(1);
        threshold.setPaintTicks(true);
        threshold.setPaintLabels(true);
        Dimension thresholdSize = new Dimension(60,300);
        threshold.setMaximumSize(thresholdSize);
        threshold.setPreferredSize(thresholdSize);
        threshold.setMinimumSize(thresholdSize);
        layout.putConstraint(SpringLayout.WEST, threshold, 603, SpringLayout.WEST, this);
        layout.putConstraint(SpringLayout.NORTH, threshold, 0, SpringLayout.NORTH, this);
        ChangeListener sliderListener = new ChangeListener()
        {
            public void stateChanged(ChangeEvent e)
            {
                JSlider source = (JSlider)e.getSource();
                SystemParam.maskThreshold = source.getValue();
                astroImageFrame.setCandidateImage();
                update();
            }
        };
        threshold.addChangeListener(sliderListener);

        /*
         * Buttons to toggle between mass and mask
         */
        // Mass
        Dimension massToggleSize = new Dimension(90,20);
        massToggle.setMaximumSize(massToggleSize);
        massToggle.setPreferredSize(massToggleSize);
        massToggle.setMinimumSize(massToggleSize);
        layout.putConstraint(SpringLayout.WEST, massToggle, 0, SpringLayout.WEST, this);
        layout.putConstraint(SpringLayout.NORTH, massToggle, 303, SpringLayout.NORTH, this);

        // Mask
        Dimension maskToggleSize = new Dimension(90,20);
        maskToggle.setMaximumSize(maskToggleSize);
        maskToggle.setPreferredSize(maskToggleSize);
        maskToggle.setMinimumSize(maskToggleSize);
        layout.putConstraint(SpringLayout.WEST, maskToggle, 90, SpringLayout.WEST, this);
        layout.putConstraint(SpringLayout.NORTH, maskToggle, 303, SpringLayout.NORTH, this);

        /*
         * Buttons to toggle between predicted and residual images
         */
        // Predicted Image
        Dimension predToggleSize = new Dimension(110,20);
        predToggle.setMaximumSize(predToggleSize);
        predToggle.setPreferredSize(predToggleSize);
        predToggle.setMinimumSize(predToggleSize);
        layout.putConstraint(SpringLayout.WEST, predToggle, 303, SpringLayout.WEST, this);
        layout.putConstraint(SpringLayout.NORTH, predToggle, 303, SpringLayout.NORTH, this);

        // Residual Image
        Dimension residToggleSize = new Dimension(110,20);
        residToggle.setMaximumSize(residToggleSize);
        residToggle.setPreferredSize(residToggleSize);
        residToggle.setMinimumSize(residToggleSize);
        layout.putConstraint(SpringLayout.WEST, residToggle, 413, SpringLayout.WEST, this);
        layout.putConstraint(SpringLayout.NORTH, residToggle, 303, SpringLayout.NORTH, this);

        /*
         * Import/Export Text Area
         */
        paramOutput.setFont(new Font("Monospaced", Font.PLAIN, 12));
        paramOutput.setBackground(Color.black);
        paramOutput.setForeground(Color.white);
        paramOutput.setCaretColor(Color.white);

        Dimension paramScrollSize = new Dimension(605,150);
        paramScroll.setMaximumSize(paramScrollSize);
        paramScroll.setPreferredSize(paramScrollSize);
        paramScroll.setMinimumSize(paramScrollSize);
        layout.putConstraint(SpringLayout.WEST, paramScroll, 0, SpringLayout.WEST, this);
        layout.putConstraint(SpringLayout.NORTH, paramScroll, 325, SpringLayout.NORTH, this);

        // Button to update the parameters based on the values in
        // the paramOutput text field
        Dimension updateParamsSize = new Dimension(90,20);
        updateParams.setMaximumSize(updateParamsSize);
        updateParams.setPreferredSize(updateParamsSize);
        updateParams.setMinimumSize(updateParamsSize);
        layout.putConstraint(SpringLayout.WEST, updateParams, 0, SpringLayout.WEST, this);
        layout.putConstraint(SpringLayout.NORTH, updateParams, 478, SpringLayout.NORTH, this);

        // Button to optimize the flux
        Dimension optimizeFluxSize = new Dimension(170,20);
        optimizeFlux.setMaximumSize(optimizeFluxSize);
        optimizeFlux.setPreferredSize(optimizeFluxSize);
        optimizeFlux.setMinimumSize(optimizeFluxSize);
        layout.putConstraint(SpringLayout.WEST, optimizeFlux, 435, SpringLayout.WEST, this);
        layout.putConstraint(SpringLayout.NORTH, optimizeFlux, 478, SpringLayout.NORTH, this);

        // Button to optimize the parameters
        Dimension optimizeParamsSize = new Dimension(170,20);
        optimizeParams.setMaximumSize(optimizeParamsSize);
        optimizeParams.setPreferredSize(optimizeParamsSize);
        optimizeParams.setMinimumSize(optimizeParamsSize);
        layout.putConstraint(SpringLayout.WEST, optimizeParams, 435, SpringLayout.WEST, this);
        layout.putConstraint(SpringLayout.NORTH, optimizeParams, 498, SpringLayout.NORTH, this);

        // Button to compute the uncertainties
        Dimension computeUncertaintiesSize = new Dimension(170,20);
        computeUncertainties.setMaximumSize(computeUncertaintiesSize);
        computeUncertainties.setPreferredSize(computeUncertaintiesSize);
        computeUncertainties.setMinimumSize(computeUncertaintiesSize);
        layout.putConstraint(SpringLayout.WEST, computeUncertainties, 435, SpringLayout.WEST, this);
        layout.putConstraint(SpringLayout.NORTH, computeUncertainties, 518, SpringLayout.NORTH, this);

        // Button to export the image
        if(standaloneMode)
        {
            Dimension exportParamsSize = new Dimension(180,20);
            exportParams.setMaximumSize(exportParamsSize);
            exportParams.setPreferredSize(exportParamsSize);
            exportParams.setMinimumSize(exportParamsSize);
            layout.putConstraint(SpringLayout.WEST, exportParams, 250, SpringLayout.WEST, this);
            layout.putConstraint(SpringLayout.NORTH, exportParams, 478, SpringLayout.NORTH, this);
        }

        this.setBackground(Color.black);
        this.setVisible(true);

        astroImageFrame.setCandidateImage();
        requestFocus();

        if(lensList.size()>0 || maskList.size()>0 || sourceList.size()>0)
        {
            Object[] options = {"Yes", "No"};
            int n = JOptionPane.showOptionDialog(this,
                    "This application has detected that you were in the middle of"
                    + " modelling when it exited. Would you like to resume from where"
                    + " you left off?",
                    "Crash Recovery",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    options,
                    options[0]);

            if(n==1)
            {
                /*
                 * Delete any residual data wich may be lingering around from the previous run
                 */
                for(int i=0; i<lensList.size(); i++)
                {
                    lensList.remove(i);
                }
                for(int i=0; i<maskList.size(); i++)
                {
                    maskList.remove(i);
                }
                for(int i=0; i<sourceList.size(); i++)
                {
                    sourceList.remove(i);
                }
                // Set the lens to adjusting in order to get the caustic updated.
                Lens.isAdjusting = true;
                update();
                Lens.isAdjusting = false;
                GravLensApplet.candidateImageName = "CSWA5_15x15arcsec.jpg";
                astroImageFrame.setCandidateImage();
                update();
            }
        }
        update();
    }

    public static void update()
    {
        // Update the display panels
        astroImageFrame.updateImage();
        predictedImageFrame.updateImage();

        updateParams();
        updateHistory();
    }

    public static void updateParams()
    {
        paramOutput.setText(getParams(""));
    }

    public static void parseParams()
    {
        String[] lines = GravLensApplet.paramOutput.getText().split("\n");
        String[] words = lines[lines.length-5].split(": ");
        String filename = words[1].trim();

        boolean updateCandidateImage = false;
        if(!filename.equals(GravLensApplet.candidateImageName))
        {
            updateCandidateImage = true;
            GravLensApplet.candidateImageName = filename.trim();
            // If an appropriately named .tex file exists in the "models/" directory,
            // load the parameters detailed in the comments at the beginning
            // of this file.
            try
            {
                FileReader fstream = new FileReader("models/"+candidateImageName+".tex");
                BufferedReader texFile = new BufferedReader(fstream);
                if(texFile.readLine().trim().equals("%@@PARAMS@@"))
                {
                    String line = texFile.readLine().trim()+"\n";
                    String paramText = "";
                    do
                    {
                        paramText += line.substring(1);

                        line = texFile.readLine().trim()+"\n";
                    } while(line.length()>0 && line.substring(0,1).equals("%"));

                    // Set the text of the parameter textarea, which will be parsed
                    // after this if statement has been complated.
                    paramOutput.setText(paramText);
                }
            } catch(Exception exception)
            {
                System.out.println(exception.getMessage());
            }
        }

        String[] line=paramOutput.getText().split("\n");

        // Update the Field of View (FoV) and Point Source Function (PSF)
        lines = GravLensApplet.paramOutput.getText().split("\n");

        words = lines[lines.length-4].split(": ");
        SystemParam.setFOV(Double.valueOf(words[1].trim()));

        words = lines[lines.length-3].split(": ");
        SystemParam.setPSF(Double.valueOf(words[1].trim()));

        words = lines[lines.length-2].split(": ");
        SystemParam.setBackground(Double.valueOf(words[1].trim()));

        words = lines[lines.length-1].split(": ");
        SystemParam.maskThreshold = Integer.valueOf(words[1].trim());
        threshold.setValue(SystemParam.maskThreshold);

        int i=-1;
        while(line[++i].compareTo("Sources:")!=0);

        // Replace the sources with the sources specified by the user
        sourceList.clear();
        while(line[++i].compareTo("Lenses:")!=0)
        {
            if(line[i].compareTo("")==0 || line[i].compareTo(headerStrSource)==0)
            {
                continue;
            }
            String[] strArr=line[i].split("\\|");
            double[] sourcePos = {Double.valueOf(strArr[1]), -Double.valueOf(strArr[3])};

            Source source = predictedImageFrame.addSource();
            source.setPos(sourcePos);
            source.setRad(Double.valueOf(strArr[5]));
            source.setF(Double.valueOf(strArr[7]));
            source.setRot(Double.valueOf(strArr[9]));
        }

        // Replace the lenses with the sources specified by the user
        lensList.clear();
        while(line[++i].startsWith("Masks:")==false)
        {
            if(line[i].compareTo("")==0 || line[i].compareTo(headerStrLens)==0)
            {
                continue;
            }
            String[] strArr=line[i].split("\\|");
            double[] lensPos = {Double.valueOf(strArr[1]), -Double.valueOf(strArr[3])};

            Lens lens = astroImageFrame.addLens();
            lens.setPos(lensPos);
            lens.setEinsteinRad(Double.valueOf(strArr[5]));
            lens.setF(Double.valueOf(strArr[7]));
            lens.setRot((Math.PI/2)-Double.valueOf(strArr[9]));
        }

        // Replace the masks with the sources specified by the user
        maskList.clear();
        while(line[++i].startsWith("Omega:")==false)
        {
            if(line[i].compareTo("")==0 || line[i].compareTo(headerStrMask)==0)
            {
                continue;
            }
            String[] strArr=line[i].split("\\|");
            double[] maskPos = {Double.valueOf(strArr[1]), Double.valueOf(strArr[2])};

            Mask mask = astroImageFrame.addMask();
            mask.setPos(maskPos);
            mask.setRad(Double.valueOf(strArr[3]));
            mask.setEllip(Double.valueOf(strArr[4]));
            mask.setRot(Double.valueOf(strArr[5]));
        }

        if(updateCandidateImage)
        {
            astroImageFrame.setCandidateImage();
            update();
        }
    }

    public static String getParams(String linePrefix)
    {
        // Display the numerical values of the parameters
        DecimalFormat threeDec = new DecimalFormat(" 00.000;-00.000");
        DecimalFormat oneDec = new DecimalFormat(" 0.0;-0.0");
        DecimalFormat sciNot = new DecimalFormat("0.000E0");
        DecimalFormat hundredFormat = new DecimalFormat(" 000.000");

        String text = linePrefix+"Sources:\n"
            +linePrefix+headerStrSource+"\n";

        int i;
        for(i=0; i<sourceList.size(); i++)
        {
            text += linePrefix+"|"
                +threeDec.format(sourceList.get(i).getPos()[0])+" |"
                +threeDec.format(sourceList.get(i).getPosErr()[0])+" |"
                +threeDec.format(-sourceList.get(i).getPos()[1])+" |"
                +threeDec.format(sourceList.get(i).getPosErr()[1])+" |"
                +threeDec.format(sourceList.get(i).getRad())+" |"
                +threeDec.format(sourceList.get(i).getRadErr())+" |"
                +threeDec.format(sourceList.get(i).getF())+" |"
                +threeDec.format(sourceList.get(i).getFErr())+" |"
                +threeDec.format(sourceList.get(i).getRot())+" |"
                +threeDec.format(sourceList.get(i).getRotErr())+" |\n";
        }

        text += linePrefix+"\n"
            +linePrefix+"Lenses:\n"
            +linePrefix+headerStrLens+"\n";
        for(i=0; i<lensList.size(); i++)
        {
            text += linePrefix+"|"
                +threeDec.format(lensList.get(i).getPos()[0])+" |"
                +threeDec.format(lensList.get(i).getPosErr()[0])+" |"
                +threeDec.format(-lensList.get(i).getPos()[1])+" |"
                +threeDec.format(lensList.get(i).getPosErr()[1])+" |"
                +threeDec.format(lensList.get(i).getEinsteinRad())+" |"
                +threeDec.format(lensList.get(i).getEinsteinRadErr())+" |"
                +threeDec.format(lensList.get(i).getF())+" |"
                +threeDec.format(lensList.get(i).getFErr())+" |"
                +threeDec.format(lensList.get(i).getNormRot())+" |"
                +threeDec.format(lensList.get(i).getRotErr())+" |\n";
        }

        text += linePrefix+"\n"
            +linePrefix+"Masks:\n"
            +linePrefix+headerStrMask+"\n";
        for(i=0; i<maskList.size(); i++)
        {
            text += linePrefix+"|"
                +threeDec.format(maskList.get(i).getPos()[0])+" |"
                +threeDec.format(maskList.get(i).getPos()[1])+" |"
                +threeDec.format(maskList.get(i).getRad())+" |"
                +threeDec.format(maskList.get(i).getEllip())+" |"
                +threeDec.format(maskList.get(i).getRot())+" |\n";
        }

        text += linePrefix+"\n"
            +linePrefix+"Omega: " + oneDec.format(ChiSq.getOmega())+"%\n"
            +linePrefix+"Magnification: " + oneDec.format(Lens.magnification)+"\n"
            +linePrefix+"\n"
            +linePrefix+"candidateImage: " + candidateImageName+"\n"
            +linePrefix+"FoV (arcsecs): " + oneDec.format(SystemParam.getFOV())+"\n"
            +linePrefix+"PSF (arcsecs): " + oneDec.format(SystemParam.getPSF())+"\n"
            +linePrefix+"Background (DN/pixel): " + oneDec.format(SystemParam.getBackground())+"\n"
            +linePrefix+"Mask Threshold: " + SystemParam.maskThreshold;

        return text;
    }

    public static void updateHistory()
    {
        history.update();
    }

    public static String getCandidateImageName()
    {
        return candidateImageName;
    }

    class ButtonListener implements ActionListener
    {
        public void actionPerformed(ActionEvent e) {
            if (e.getActionCommand().equals("Mask"))
            {
                // Deselect the mass toggle button
                // and set the mass to invisible
                massToggle.setSelected(false);
                MassModel.isVisible = false;

                Mask.isVisible = maskToggle.isSelected();
                astroImageFrame.setWhichListener(maskToggle.isSelected()?2:0);
                update();
            } else if (e.getActionCommand().equals("Mass"))
            {
                // Deselect the mask toggle button
                // and set the mask to invisible
                maskToggle.setSelected(false);

                Mask.isVisible = false;
                MassModel.isVisible = massToggle.isSelected();
                astroImageFrame.setWhichListener(massToggle.isSelected()?1:0);

                update();
            } else if (e.getActionCommand().equals("Predicted"))
            {
                // Deselect the residual toggle button
                residToggle.setSelected(false);
                predictedImageFrame.setIfShowResidual(false);
                update();
            } else if (e.getActionCommand().equals("Residual"))
            {
                // Deselect the predicted toggle button
                predToggle.setSelected(false);
                predictedImageFrame.setIfShowResidual(true);
                update();
            } else if (e.getActionCommand().equals("Update"))
            {
                parseParams();
                update();
            } else if (e.getActionCommand().equals("Export Parameters"))
            {
                String folder = "models/";

                // Display the numerical values of the parameters
                DecimalFormat oneDec = new DecimalFormat(" 0.0;-0.0");
                DecimalFormat threeDec = new DecimalFormat("0.000");
                DecimalFormat fourDec = new DecimalFormat("0.0000");
                DecimalFormat sciNot = new DecimalFormat("0.000E0");

                try
                {
                    // Create file
                    FileWriter fstream = new FileWriter(folder+candidateImageName+".tex");
                    BufferedWriter out = new BufferedWriter(fstream);

                    String text = "%@@PARAMS@@\n"
                        +getParams("%")+"\n\n\n           ";

                    // Now generate out the LaTeX code
                    int rows = (sourceList.size()>lensList.size()) ? sourceList.size() : lensList.size();
                    // Generate the LaTeX table for the sources
                    for(int i=0; i<rows; i++)
                    {
                        if(lensList.size()>i)
                        {
                            text += "& & $"+fourDec.format(lensList.get(i).getPos()[0])+"\\pm"+fourDec.format(lensList.get(i).getPosErr()[0])+"$"
                                +"  & $"+fourDec.format(-lensList.get(i).getPos()[1])+"\\pm"+fourDec.format(lensList.get(i).getPosErr()[1])+"$"
                                +"  & $"+fourDec.format(lensList.get(i).getEinsteinRad())+"\\pm"+fourDec.format(lensList.get(i).getEinsteinRadErr())+"$"
                                +"  & $"+fourDec.format(lensList.get(i).getF())+"\\pm"+fourDec.format(lensList.get(i).getFErr())+"$"
                                +"  & $"+fourDec.format(lensList.get(i).getNormRot())+"\\pm"+fourDec.format(lensList.get(i).getRotErr())+"$";
                        } else
                        {
                            text += "& &               &               &              &               &            ";
                        }

                        if(sourceList.size()>i)
                        {
                            text += "   & & $"+fourDec.format(sourceList.get(i).getPos()[0])+"\\pm"+fourDec.format(sourceList.get(i).getPosErr()[0])+"$"
                                +"  & $"+fourDec.format(-sourceList.get(i).getPos()[1])+"\\pm"+fourDec.format(sourceList.get(i).getPosErr()[1])+"$"
                                +"  & $"+fourDec.format(sourceList.get(i).getRad())+"\\pm"+fourDec.format(sourceList.get(i).getRadErr())+"$"
                                +"  & $"+fourDec.format(sourceList.get(i).getF())+"\\pm"+fourDec.format(sourceList.get(i).getFErr())+"$"
                                +"  & $"+fourDec.format(sourceList.get(i).getRot())+"\\pm"+fourDec.format(sourceList.get(i).getRotErr())+"$";
                        } else
                        {
                            text += "   & &               &               &              &               &            ";
                        }

                        if(i==0)
                        {
                            text += "& & "+oneDec.format(ChiSq.getOmega())+"\\% & X & $"+oneDec.format(Lens.magnification)+" \\pm X$  \\\\";
                        } else
                        {
                            text += "& &      &   &            \\\\";
                        }
                        if(i==rows-1)
                        {
                             text += "\\cline{1-1}\\cline{3-7}\\cline{9-13}\\cline{15-17}";
                        }
                        text += "\n";
                    }

                    out.write(text);
                    out.close();

                    // Save the images
                    // TODO: Make sure the flux is optimized according to how the user chose in the display.
                    ImageIO.write(predictedImageFrame.getImage(false, false), "png", new File(folder+candidateImageName+"_pred.png"));
                    ImageIO.write(predictedImageFrame.getImage(true, false), "png", new File(folder+candidateImageName+"_resid.png"));
                    ImageIO.write(astroImageFrame.getUnmaskedRebinnedImage(), "png", new File(folder+candidateImageName+".png"));

                    JOptionPane.showMessageDialog(null, "The parameters have been successfully saved to "+folder+candidateImageName+".tex");
                } catch (IOException exception){
                    JOptionPane.showMessageDialog(null, "An error occured: " + exception.getMessage());
                }
            } else if (e.getActionCommand().equals("Optimize Flux"))
            {
                ChiSq.optimizeFlux = optimizeFlux.isSelected();
                update();
            } else if (e.getActionCommand().equals("Optimize Params"))
            {
                optimizeFlux.setSelected(true);
                ChiSq.optimizeFlux = true;
                ChiSq.optimizeParams();
                predictedImageFrame.updateCaustic(SystemParam.getFullN());
                update();
            } else if (e.getActionCommand().equals("Update Uncertainties"))
            {
                ChiSq.computeUncertainties();
                updateParams();
            }
        }
    }

    class KeyHandler implements KeyEventPostProcessor
    {
        public boolean postProcessKeyEvent(KeyEvent e)
        {
            //TODO: Put diffVec back in  later...
            //double[] diffVec = {cursor[0]-source.getPosPixels()[0], cursor[1]-source.getPosPixels()[1]};

            if (e.getID() == KeyEvent.KEY_PRESSED)
            {
                // Test to see if any modifier keys have been used to indicate that
                // we want to modify properties other than just resizing the source
                if(whichProperty[0]==1 || whichProperty[0]==4)
                {
                    if(e.isControlDown() || e.isAltDown())
                    {
                        // TODO: Need to figure out *which* source we're adjusting before we figure out what
                        // direction to place the cursor.
                        astroImageFrame.setCursor(ellipH);
                        predictedImageFrame.setCursor(ellipH);

                        /*
                        if(abs(diffVec[0]) > abs(diffVec[1]))
                        {
                            setCursor(ellipH);
                        } else
                        {
                            setCursor(ellipV);
                        }
                        */
                        whichProperty[0]+=1;
                    } else if(e.isShiftDown())
                    {
                        astroImageFrame.setCursor(rotate);
                        predictedImageFrame.setCursor(rotate);
                        whichProperty[0]+=2;
                    }
                }
            } else if (e.getID() == KeyEvent.KEY_RELEASED)
            {
                if(whichProperty[0]==2 || whichProperty[0]==3 || whichProperty[0]==5 || whichProperty[0]==6)
                {
                    // TODO: Fix this...
                    astroImageFrame.setCursor(resizeH);
                    predictedImageFrame.setCursor(resizeH);
                    /*
                    if(abs(diffVec[0]) > abs(diffVec[1]))
                    {
                        setCursor(resizeH);
                    } else
                    {
                        setCursor(resizeV);
                    }
                    */
                    if(whichProperty[0]==2 || whichProperty[0]==3)
                    {
                        whichProperty[0] = 1;
                    } else if(whichProperty[0]==5 || whichProperty[0]==6)
                    {
                        whichProperty[0] = 1;
                    }
                }
            }
        return true;
        }
    }
}
