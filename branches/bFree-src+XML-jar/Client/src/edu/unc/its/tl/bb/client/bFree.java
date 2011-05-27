package edu.unc.its.tl.bb.client;

import java.awt.*;

import javax.swing.*;

public final class bFree {
   //NOTE Now called "bFree".
   //An application for exploring and selectively extracting the contents 
   //of a course "Export" or "Arhive" zip file from Blackboard.
   
   //NOTE: This application uses XML classes from a project called
   //XMLProcessing, and a JTreeTable from a project called TreeTable.
   
   //NOTE: See Unzipper.java for the beginning of the program documentation.
   
   //Once an XML text is parsed, all the tags and attribute names 
   //are in lowercase, while the values of texts and attributes
   //are in their original cases. So, comparisons of tag names should
   //be in lowercase, AND paths (which are sequences of tags) should
   //be in lowercase.
   
   //The platform on which this is running, set in checkOS at startup:
   public static String strOS = "win";

   public static final String kTypeWindows = "win";
   public static final String kTypeMacintosh = "mac";
   public static final String kTypeLinux = "lin";

   public bFree() {
      JFrame frame = new Unzipper();
      Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
      Dimension frameSize = frame.getSize();
      if (frameSize.height > screenSize.height) {
         frameSize.height = screenSize.height;
      }
      if (frameSize.width > screenSize.width) {
         frameSize.width = screenSize.width;
      }
      frame.setLocation((screenSize.width - frameSize.width) / 2,
                        (screenSize.height - frameSize.height) / 2);
      frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
      frame.setVisible(true);
      //Delete the default temp files, if any:
      Unzipper.deleteOldArchive(null);
   } //constructor

   public static void main(String[] args) {
      try {
         //UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
         UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
      } catch (Exception e) {
         e.printStackTrace();
      }
      checkOS();
      new bFree();
   } //main

   private static void checkOS() {
      //Find out which operating system this is running on, set specific properties:
      strOS = System.getProperty("os.name");
      
      if (strOS.startsWith("Win"))
         strOS = kTypeWindows;
      else if (strOS.startsWith("Mac")) {
         strOS = kTypeMacintosh;
         System.setProperty("apple.awt.antialiasing", "true"); //generally
         System.setProperty("apple.awt.textantialiasing", "true");
         System.setProperty("apple.awt.showGrowBox", "false");
      } else
         strOS = kTypeLinux;
   } //checkOS

}//BflatUnzip
