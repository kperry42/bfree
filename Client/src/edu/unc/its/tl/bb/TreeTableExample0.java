/*
 * %W% %E%
 *
 * Copyright 1997, 1998 Sun Microsystems, Inc. All Rights Reserved.
 *
 * Redistribution and use in source and binary forms, with or
 * without modification, are permitted provided that the following
 * conditions are met:
 *
 * - Redistributions of source code must retain the above copyright
 *   notice, this list of conditions and the following disclaimer.
 *
 * - Redistribution in binary form must reproduce the above
 *   copyright notice, this list of conditions and the following
 *   disclaimer in the documentation and/or other materials
 *   provided with the distribution.
 *
 * Neither the name of Sun Microsystems, Inc. or the names of
 * contributors may be used to endorse or promote products derived
 * from this software without specific prior written permission.
 *
 * This software is provided "AS IS," without a warranty of any
 * kind. ALL EXPRESS OR IMPLIED CONDITIONS, REPRESENTATIONS AND
 * WARRANTIES, INCLUDING ANY IMPLIED WARRANTY OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE OR NON-INFRINGEMENT, ARE HEREBY
 * EXCLUDED. SUN AND ITS LICENSORS SHALL NOT BE LIABLE FOR ANY
 * DAMAGES OR LIABILITIES SUFFERED BY LICENSEE AS A RESULT OF OR
 * RELATING TO USE, MODIFICATION OR DISTRIBUTION OF THIS SOFTWARE OR
 * ITS DERIVATIVES. IN NO EVENT WILL SUN OR ITS LICENSORS BE LIABLE
 * FOR ANY LOST REVENUE, PROFIT OR DATA, OR FOR DIRECT, INDIRECT,
 * SPECIAL, CONSEQUENTIAL, INCIDENTAL OR PUNITIVE DAMAGES, HOWEVER
 * CAUSED AND REGARDLESS OF THE THEORY OF LIABILITY, ARISING OUT OF
 * THE USE OF OR INABILITY TO USE THIS SOFTWARE, EVEN IF SUN HAS
 * BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.
 *
 * You acknowledge that this software is not designed, licensed or
 * intended for use in the design, construction, operation or
 * maintenance of any nuclear facility.
 */
package edu.unc.its.tl.bb;


import java.awt.Dimension;
import java.awt.Label;
import java.awt.Toolkit;

import javax.swing.*;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import java.io.BufferedReader;
import java.io.File;

import java.io.FileReader;
import java.io.IOException;

import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

import org.dvm.java.xml.XMLAccessor;
import org.dvm.java.xml.XMLException;
import org.dvm.java.xml.XMLObject;

/**
 * A TreeTable example, showing a JTreeTable, operating on the local file
 * system.
 *
 * @version %I% %G%
 *
 * @author Philip Milne
 */
public class TreeTableExample0 {

   public static XMLObject objTT = null;

   public static void main(String[] args) {
      new TreeTableExample0();
   }

   public TreeTableExample0() {
      JFrame frame = null;
      JTreeTable treeTable = null;
      JScrollPane scr = null;

      XMLAccessor acc = null;
      XMLTableModel model = null;
      String strXML = null;

      File fil = null;

      frame = new JFrame("TreeTable");
      frame.setSize(904, 400);

      fil = chooseFile(JFileChooser.OPEN_DIALOG, false, frame);

      if (fil == null)
         System.exit(0);

      strXML = getXMLFile(fil);
      acc = getAccessor(strXML);
      objTT = (XMLObject)acc.getXMLObject();
      model = new XMLTableModel(objTT);

      treeTable = new JTreeTable(model);

      treeTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

      int[] wid = { 300, 100, 100, 100, 150, 150 };
      boolean[] lbl = { false, true, false, false, true, true }; //are labels
      String strName;
      TableColumn col;
      TableCellRenderer rend = null;
      for (int i = 0; i < treeTable.getColumnCount(); i++) {
         strName = treeTable.getColumnName(i);
         col = treeTable.getColumn(strName);
         col.setPreferredWidth(wid[i]);
         if (lbl[i]) {
            rend = new DefaultTableCellRenderer();
            col.setCellRenderer(rend);
            ((JLabel)rend).setHorizontalAlignment(SwingConstants.CENTER);
         }
      }
      //Ask to be notified of selection changes.
      ListSelectionModel rowSM = treeTable.getSelectionModel();
      rowSM.addListSelectionListener(new ListSelectionListener() {
               public void valueChanged(ListSelectionEvent e) {
                  //Ignore extra messages.
                  if (e.getValueIsAdjusting())
                     return;

                  ListSelectionModel lsm = (ListSelectionModel)e.getSource();
                  if (lsm.isSelectionEmpty()) {
                     //...//no rows are selected
                     int selectedRow = lsm.getMinSelectionIndex();
                     //System.out.println("deselection="+selectedRow);
                  } else {
                     int selectedRow = lsm.getMinSelectionIndex();
                     //System.out.println("selection="+selectedRow);
                     if (selectedRow >= 0) {
                        //Toolkit.getDefaultToolkit().beep();
                     }
                  }
               }
            });

      frame.addWindowListener(new WindowAdapter() {
               public void windowClosing(WindowEvent we) {
                  System.exit(0);
               }
            });

      scr = new JScrollPane(treeTable);
      scr.setPreferredSize(new Dimension(900, 400));
      frame.getContentPane().add(scr);
      frame.pack();
      frame.setVisible(true);
   } //constructor

   public File chooseFile(int mode, boolean bSave, JFrame frame) {
      //Return the File object for the file or directory that the user chooses,
      //or null if none.
      //"mode" allows files or directories (files for Open, directories
      //for Save.
      //"bSave" is true for a Save dialog; else Open.
      JFileChooser chooser = null;
      String[] ext = { "zip" }; //only zips for Open!
      //ExtensionFileFilter filter = new ExtensionFileFilter(ext);
      String strFile = null;
      File filFile = null;
      int returnVal = 0;

      String strMe =
         System.getProperty("user.dir") + System.getProperty("file.separator");
      File filIn = new File(strMe);
      int iNameLoc = -1;

      //Show the user the chooser:
      chooser = new JFileChooser(filIn);
      chooser.setSelectedFile(filFile);
      chooser.setFileSelectionMode(mode);
      if (bSave) {
         chooser.setDialogTitle("Select a location for the course directory");
         chooser.setApproveButtonText("Select");
         returnVal = chooser.showSaveDialog(frame);
      } else {
         //chooser.setFileFilter(filter);
         returnVal = chooser.showOpenDialog(frame);
      }
      //Get the user's response:
      if (returnVal == JFileChooser.APPROVE_OPTION) {
         //User chose a file:
         filFile = chooser.getSelectedFile();
         strFile = filFile.getPath();
      }
      chooser = null;
      return filFile;
   } //chooseFile

   private String getXMLFile(File fil) {
      //Read an XML descriptor file for the local document tree.
      String strLine;
      StringBuffer buf = new StringBuffer(1024);

      try {
         BufferedReader brData = new BufferedReader(new FileReader(fil));
         strLine = brData.readLine();
         while (strLine != null) {
            buf.append(strLine);
            strLine = brData.readLine();
         }
         brData.close();
      } catch (IOException e) {
         System.out.println("getXMLFile: Cannot read from " + fil.getName());
      }
      return buf.toString();
   } //getXMLFile

   public static XMLAccessor getAccessor(String strXML) {
      //Return an XMLAccessor to handle the XMLObject parsed
      //from the given XML text.
      XMLAccessor accObj = null;
      try {
         accObj = new XMLAccessor();
         accObj.setXMLText(strXML);
         //Toolkit.getDefaultToolkit().beep(); //TEMP
      } catch (XMLException e) {
         accObj = null;
         System.out.println(e.error); //TEMP
      }
      return accObj;
   } //getAccessor

}//TreeTableExample0

