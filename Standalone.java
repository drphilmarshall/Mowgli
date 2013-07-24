/*
+ ======================================================================

 NAME:
   Standalone.java

 PURPOSE:
   This class embeds the main applet class (GravLensApplet.java)
   inside a standalone Java application.

 COMMENTS:

 INPUTS:

 OPTIONAL INPUTS:
   image              A FITS or JPG image, located in the data directory.

 OUTPUTS:
   models/image.tex   Model parameters, in a latex table.
   
 EXAMPLE:
    
    java Standalone [image]

    "image" is an image file (JPG or FITS) located in the data directory.
    
    Upon loading the candidate image, MOWGLI will check to see if the file
    models/image.tex exists. If it does, the parameters from this
    file will be loaded into the current model.
    
    If no command line arguments are provided, the candidate image defaults 
    to CSWA5_15x15arcsec.jpg


 DEPENDENCIES:
   Depends on:
       GravLensApplet.java
     Called by:
       None.

 BUGS:

 REVISION HISTORY:
   2010-02-23  started Naudus (GMU)
   2013-07-23  cleaned up for GitHub release Marshall (KIPAC)

- ======================================================================
*/

import javax.swing.*;

public class Standalone
{
    public static void main(String[] args)
    {
        // Set up the gui...
        JFrame frame = new JFrame();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Make a new GravLensApplet
        GravLensApplet spanel;
        if(args.length==0)
        {
            spanel = new GravLensApplet(true);
        } else
        {
            spanel = new GravLensApplet(true, args[0]);
        }

        // Throw everything to the screen
        frame.getContentPane().add(spanel);
        frame.pack();
        frame.setVisible(true);
    }
}
