Mowgli
======

Manually-operated widget for gravitational lens identification.
Can be run as a standalone java app, or as a web applet. 

### Web version

Online at **http://ephysics.org/mowgli**

### Standalone app

![Mowgli in action](https://github.com/drphilmarshall/Mowgli/edit/master/doc/figs/applet-schematic.png)

Compile and run with:

    javac *.java
    java Standalone

### Authors

* Philip Naudus (ephysics.org)
* Phil Marshall (KIPAC)
* John Wallin (MTSU)

If you make use of Mowgli in your research, please cite Naudus, Marshall and
Wallin, in prep. You can read this paper: it has been submitted to MNRAS, and
lives in the doc directory.

### License

GPLv2. Please keep in touch if you do fork this repository - send Phil an
email at dr.phil.marshall+mowgli@gmail.com!

### Dependencies

To enable Mowgli to carry out automatic parameter optimisation, and to work on
FITS images, it calls functions from the eap and Apache commons libraries. For
convenience, these are provided as binary class files with the Mowgli
repository, a method which is known to work under Mac OS X Snow Leopard. A
better approach would be to install the libraries independently on your
machine.

### Bugs

Standalone app fails with:

    Uncaught error fetching image:
    java.lang.NullPointerException
          at sun.awt.image.URLImageSource.getConnection(URLImageSource.java:99)
          at sun.awt.image.URLImageSource.getDecoder(URLImageSource.java:113)
          at sun.awt.image.InputStreamImageSource.doFetch(InputStreamImageSource.java:240)
          at sun.awt.image.ImageFetcher.fetchloop(ImageFetcher.java:172)
          at sun.awt.image.ImageFetcher.run(ImageFetcher.java:136)


