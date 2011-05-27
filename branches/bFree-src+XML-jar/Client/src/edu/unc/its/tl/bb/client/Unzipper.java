package edu.unc.its.tl.bb.client;

import edu.unc.its.tl.bb.*;

import org.dvm.java.xml.*;

import java.awt.*;
import java.awt.event.*;

import java.io.*;

import java.net.URL;

import java.util.*;
import java.util.zip.*;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.*;

public final class Unzipper extends JFrame implements ActionListener,
                                                      BbConstants,
                                                      ChangeListener,
                                                      ListSelectionListener {
   //A simple interface and the basic routines to do Blackboard
   //"export" or "archive" zip file extraction.
   //
   //Usage
   //
   //Click the "Open ..." button to find an exported zip file to process.
   //If that is successful, the contents are displayed in a tree table.
   //Click the "Hide..." button to remove empty folders from the display.
   //Check the items to be extracted.
   //Then press the "Extract as files/folders ..." button
   //to make a directory structure containing the content in a hierarchy
   //that parallels the course structure.
   //Or click the "Extract as a web site ..." button
   //to create a web site having the same hierarchy.

   //The Blackboard export zip file format
   //
   //The zip contains a manifest (imsmanifest.xml) consisting of two parts:
   // 1. A hierarchical representation of the course structure, in which
   //    the higher-level items are the Assignments, Course Documents,
   //    Course Information, and other major sections of a course. This
   //    hierarchy goes down to each item within the sections.
   //    Each item has an ID linking it to a "resource" entry and an XML
   //    descriptor. The ID is of the form "res00000".
   // 2. A list of "resource" entries, each of which is a brief entry with
   //    some (generally redundant) information about a content item
   //    or section. It can include the name of an item's linked file.
   //
   //The zip also contains a separate XML descriptor file for each item
   //and section. The name of the XML for an item is the item's ID
   //suffixed with ".dat". This file contains most of the detailed
   //information about the item.
   //
   //Finally, the zip may contain an extra folder, whose name is the item's
   //ID (without a suffix), to hold files attached to an item. Not all
   //items have attached files or these folders. So far, it looks like
   //the resource list entries name any attached files, so these extra
   //folders don't have to be searched; their existance is implied by
   //the FILE attribute (if any) of the resource entry.
   //
   //Processing
   //
   //We first unzip the archive into the user's directory (system property
   //"user.home"), so that we don't have to use linear searches to find the
   //files it contains. (This temp directory is deleted when the user opens
   //a new zip, at program's end, and attempted at startup.)
   //
   //We parse the entire imsmanifest.xml, then step through the structure
   //section, and create a (nearly parallel) tree of items for our use.
   //
   //For output, we step through our tree to determine the names and types
   //of the various items, and we create (or copy) the directories and files
   //onto the user's disk.
   //
   //For the folder/file extraction, each section and subsection is represented
   //by a folder, with the actual content as files within the folders. For the web
   //site extraction, each section and subsection is represented by a web page
   //containing links to subsections and to content files. The files are stored
   //in folders that echo the page names from which they are linked.


   //Interface components are declared at the end of this file,
   //along with methods for initializing them.

   //The frame's glass pane, used for blocking mouse clicks while processing:
   private Component pnlGlass;

   //Where local resources (texts, images) comes from:
   private ClassLoader clResources = null;

   //So that others can call this class's setMessage:
   public static Unzipper ref = null; //set at startup

   //This flag can be set to false at various places in the processing,
   //to make processing stop when an error is encountered.
   public boolean bOKSoFar = true;

   //Several objects are used to get the work done. Singletons:
   private Globals glbl = null;
   private BbInput bbIn = null;
   private BbOut bbOut = null;
   private BbSite bbSite = null;
   private SakaiInput sakaiIn = null;

   private Searcher searcher = null; //new for each archive

   public Unzipper() {
      glbl = Globals.getInstance();

      bbIn = new BbInput();
      bbOut = new BbOut();
      bbSite = new BbSite();
      sakaiIn = new SakaiInput();
      try {
         jbInit();
      } catch (Exception e) {
         e.printStackTrace();
      }
      ref = this;
   } //constructor

   private void jbInit() throws Exception {
      //Build the interface.
      this.getContentPane().setLayout(new BorderLayout());
      this.getContentPane().setBackground(BG_COLOR);
      this.setSize(new Dimension(980, 738)); //768
      this.setTitle("bFree - Blackboard\u2122 Export Extractor - Version " +
                    VERSION);
      this.setResizable(true);
      this.setVisible(false);

      Insets margin = new Insets(0, 0, 0, 0);
      setupTitlePanel();

      setupFindPanel(margin);

      setupTabbedPanel();

      //Set up the panels in the window:
      //A couple of spacers:
      JPanel pnlWest = new JPanel();
      JPanel pnlEast = new JPanel();

      pnlWest.setPreferredSize(new Dimension(10, 10));
      pnlEast.setPreferredSize(new Dimension(10, 10));
      pnlWest.setBackground(BG_COLOR);
      pnlEast.setBackground(BG_COLOR);

      this.getContentPane().add(pnlTitle, BorderLayout.NORTH);
      this.getContentPane().add(pnlTabs, BorderLayout.CENTER);
      //this.getContentPane().add(pnlNoFind, BorderLayout.SOUTH);
      this.getContentPane().add(pnlWarn, BorderLayout.SOUTH);
      bIsShowing = false;
      this.getContentPane().add(pnlWest, BorderLayout.WEST);
      this.getContentPane().add(pnlEast, BorderLayout.EAST);

      this.getContentPane().repaint();

      pnlGlass = this.getGlassPane(); //for blocking clicks

      setupWindowMenus();
      setupPopupMenus();
      showNews();
   } //jbInit

   private void showNews() {
      //Show a list of new features iff it has not yet been shown.
      //This is based on whether the current version differs
      //from the one stored in the user's preferences.
      File filUser =
         new File(System.getProperty("user.home") + "/bFree/prefs.txt");
      PrintStream ps = null;
      BufferedReader br = null;
      boolean bShowIt = false;

      try {
         if (!filUser.exists()) {
            //First-timer; create the file and then show the news:
            filUser.getParentFile().mkdirs();
            if (filUser.createNewFile()) {
               ps = new PrintStream(filUser);
               ps.println(VERSION);
               ps.close();
               bShowIt = true;
            }
         } else {
            br = new BufferedReader(new FileReader(filUser));
            if (!br.readLine().equals(VERSION)) {
               //Update the file, and show the news:
               ps = new PrintStream(filUser);
               ps.println(VERSION);
               ps.close();
               bShowIt = true;
            }
         }
      } catch (IOException e) {

      }
      if (bShowIt) {
         JOptionPane pane =
            new JOptionPane(NEWS, JOptionPane.PLAIN_MESSAGE, JOptionPane.DEFAULT_OPTION,
                            null);
         pane.setFont(new Font("Serif", Font.BOLD, 12));
         JDialog dlg =
            pane.createDialog(this, "New in bFree version " + VERSION);

         dlg.setVisible(true);
      }
   } //showNews

   private void setTableFeatures(JTreeTable tabExport) {
      //Set the selection mode, column widths and column head centering,
      //and the selection listener. This needs to be done every time
      //the contents of the table are replaced.
      tabExport.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

      int[] wid = { 400, 100, 100, 150, 150 };
      boolean[] lbl =
      { false, true, false, true, true }; //contents are JLabels
      String strName;
      TableColumn col;
      TableCellRenderer rend = null;
      for (int i = 0; i < tabExport.getColumnCount(); i++) {
         strName = tabExport.getColumnName(i);
         col = tabExport.getColumn(strName);
         col.setPreferredWidth(wid[i]);
         if (lbl[i]) {
            rend = new DefaultTableCellRenderer();
            col.setCellRenderer(rend);
            ((JLabel)rend).setHorizontalAlignment(SwingConstants.CENTER);
         }
      }
      //Ask to be notified of selection changes.
      ListSelectionModel rowSM = tabExport.getSelectionModel();
      rowSM.addListSelectionListener(this);
   } //setTableFeatures

   private XMLObject makeDummyObject() {
      //Return a dummy display tree so that we can show the tree table
      //at startup.
      XMLObject obj = new XMLObject("course", XMLObject.ELEMENT);

      obj.setAttr("display",
                  "Ready (Choose \"Open an archive \'zip\' ...\" from the File menu)");
      obj.setAttr("dir", "true");
      obj.setAttr("type", "(Ready)");
      obj.setAttr("name", "");//OK
      obj.setAttr("extract", "false");
      obj.setAttr("created", "---");
      obj.setAttr("modified", "---");
      obj.setAttr("id", "res00001");

      return obj;
   } //newmakeDummyObject

   public void setMessage(String msg, int level, boolean bStop) {
      //Display a processing message in the log.
      pnlMessages.addMessage(msg, level);
      if (bStop) {
         bOKSoFar = false; //no further processing on this archive!
         Toolkit.getDefaultToolkit().beep();
         pnlMessages.addMessage("\nNO FURTHER PROCESSING of this archive is possible!",
                                TEXT_MSG);
      }
   } //setMessage

   public void startProgress() {
      //Show the Processing message on the interface.
      lblProcessing.setText("Processing ...");
      update(this.getGraphics());
   } //startProgress

   public void stopProgress() {
      //Hide the Processing message.
      lblProcessing.setText("");
      update(this.getGraphics());
   } //stopProgresss

   public void actionPerformed(ActionEvent evt) {
      //Handle the button events.
      JComponent btn = (JComponent)evt.getSource();

      if ((btn == itemOpen) || (btn == popOpen)) {
         //The Open button response is to let the user locate an export
         //zip file, then parse it and set up the major global variables.

         //First, we need to be sure we have a temp folder for unzipping the archives:
         if (glbl.bNoTempYet) {
            bOKSoFar = setupTempFile(evt.getModifiers());
         }
         if (bOKSoFar) {
            setMessage("Your temporary work folder is: " +
                       glbl.filTempPath.getPath(), INFO_MSG, false);
            bOKSoFar = handleNewArchive();
         }
         /*
         //TEST:
         if (glbl.objCourse != null) {
            int[] aLen = new int[50]; //length at each segment of the current path
            adjustPathLengths(glbl.objCourse, 0, aLen, 0);
         }
         */
      } else if ((btn == itemSaveFiles) || (btn == popSaveFiles)) {
         //The Extract button creates a folder hierarchy that parallels
         //the course hierarchy, and writes out the available content:
         if (bOKSoFar && (glbl.objCourse != null)) {
            outputContent();
         }
      } else if ((btn == itemSaveSite) || (btn == popSaveSite)) {
         //Create a "flat" web site, a single folder containing all
         //the web pages:
         if (bOKSoFar && (glbl.objCourse != null)) {
            outputSite(null);
         }
      } else if ((btn == itemSaveOne) || (btn == popSaveOne) ||
                 (btn == popSaveThis)) {
         //Output the selected item to the user's chosen file:
         if (bOKSoFar && (glbl.objCourse != null)) {
            outputSelection(glbl.selectedObj);
         }
      } else if ((btn == itemHide) || (btn == popHide)) {
         //Condense the display tree by removing nodes that do not have
         //available output:
         pnlGlass.setVisible(true);
         condense(glbl.objCourse);

         //System.out.println(new XMLHandler(glbl.objCourse).getXMLText(true)); //TEMP

         //Display the treetable containing our new display tree:
         modExport = new XMLTableModel(glbl.objCourse);
         glbl.tabExport = new JTreeTable(modExport);
         glbl.tabExport.setCursor(new Cursor(Cursor.HAND_CURSOR));
         glbl.tabExport.addMouseListener(popupListener);
         setTableFeatures(glbl.tabExport);
         scrExport.setViewportView(glbl.tabExport);

         //Once done (for this zip) it cannot be done again:
         itemHide.setEnabled(false);
         popHide.setEnabled(false);

         //The tree has changed, so set up again for searches:
         searcher = new Searcher(glbl.objCourse);
         pnlGlass.setVisible(false);

         //System.out.println(BbOut.getCourseDirectory(glbl.objCourse));//TEMP

      } else if (btn == btnVersion) {
         //Pop up a dialog showing the version and author:
         String msg =
            "<html><font size=\"4\" face=\"Times New Roman\"> <font size=5>bFree Version " +
            VERSION + "</font><br><br>" + "Conceived and implemented by<br>" +
            "David \"Uncle Dave\" Moffat<br>" + "uncled@email.unc.edu<br>" +
            "ITS Teaching & Learning Interactive<br>" +
            "UNC at Chapel Hill</font></html>";
         Image me = getResourceImage("MoffatPeek.gif");
         ImageIcon i = new ImageIcon(me);

         JOptionPane pane =
            new JOptionPane(msg, JOptionPane.PLAIN_MESSAGE, JOptionPane.DEFAULT_OPTION,
                            i);
         pane.setFont(new Font("Serif", Font.BOLD, 12));
         JDialog dlg = pane.createDialog(this, "About bFree");

         dlg.setVisible(true);
      } else if ((btn == btnFind) || (btn == txtFind) ||
                 (btn == itemFind1st)) {
         //NOTE: Receives a button click or a textfield Enter key.
         //Find the first occurrence of the string:
         String strToFind = txtFind.getText().trim();

         lastSelection = -1;
         if (strToFind.length() > 0) {
            //Clear the Preview tab, in case nothing is found:
            bbPreview.showPreview("Nothing is selected.", false);
            itemSaveOne.setEnabled(false);
            popSaveOne.setEnabled(false);
            popSaveThis.setEnabled(false);

            this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

            //Now search:
            searcher.searchTree(strToFind, FIND_1ST);
            this.setCursor(Cursor.getDefaultCursor());

            //There might be nothing selected:
            if ((glbl.lastPath == null) && !glbl.bWasRoot) {
               ListSelectionModel sm = glbl.tabExport.getSelectionModel();
               sm.clearSelection();
               itemSaveOne.setEnabled(false);
               popSaveOne.setEnabled(false);
               popSaveThis.setEnabled(false);
            }
         }
      } else if ((btn == btnFindNext) || (btn == itemFindNext)) {
         //Find the next occurrence:
         String strToFind = txtFind.getText().trim();

         if (strToFind.length() > 0) {
            this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            searcher.searchTree(strToFind, FIND_NEXT);
            this.setCursor(Cursor.getDefaultCursor());
         }
      } else if ((btn == btnFindPrev) || (btn == itemFindPrev)) {
         //Find the next occurrence:
         String strToFind = txtFind.getText().trim();

         if (strToFind.length() > 0) {
            this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            searcher.searchTree(strToFind, FIND_PREV);
            this.setCursor(Cursor.getDefaultCursor());
         }
      } else if ((btn == btnFindLast) || (btn == itemFindLast)) {
         //Find the last occurrence:
         String strToFind = txtFind.getText().trim();

         if (strToFind.length() > 0) {
            this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            searcher.searchTree(strToFind, FIND_LAST);
            this.setCursor(Cursor.getDefaultCursor());
         }
      } else if (btn == itemTitles) {
         searcher.setSearchTitles(((JCheckBoxMenuItem)btn).isSelected());
         chkTitles.setSelected(((JCheckBoxMenuItem)btn).isSelected());

      } else if (btn == itemTexts) {
         searcher.setSearchTexts(((JCheckBoxMenuItem)btn).isSelected());
         chkTexts.setSelected(((JCheckBoxMenuItem)btn).isSelected());

      } else if (btn == itemTidy) {
         searcher.setStayTidy(((JCheckBoxMenuItem)btn).isSelected());
         chkTidy.setSelected(((JCheckBoxMenuItem)btn).isSelected());

      } else if (btn == chkTitles) {
         searcher.setSearchTitles(((JCheckBox)btn).isSelected());
         itemTitles.setSelected(((JCheckBox)btn).isSelected());

      } else if (btn == chkTexts) {
         searcher.setSearchTexts(((JCheckBox)btn).isSelected());
         itemTexts.setSelected(((JCheckBox)btn).isSelected());

      } else if (btn == chkTidy) {
         searcher.setStayTidy(((JCheckBox)btn).isSelected());
         itemTidy.setSelected(((JCheckBox)btn).isSelected());

      } else if (btn == itemExit) {
         WindowEvent wevt = new WindowEvent(this, WindowEvent.WINDOW_CLOSING);

         dispatchEvent(wevt);
      }
   } //actionPerformed
   /* 
   //This name length correction works properly -- but I named the web
   //pages and file folders with the display porperty, not the name property!
   //I have to change all the extraction code to use the name porperty
   //for page and folder names, while using the display property for links
   //and the outline display.
   private void adjustPathLengths(XMLObject obj, int iLen, int[] aLen, int iLevel) { //TEST
   //On windows, no file path can be greater than 255 charcters, so we must
   //check for that, and trim path segments if necessary.
      int iTotLen;
      int iNext;
      
      //Record and accumulate this path segment's length:
      aLen[iLevel] = obj.getAttr("name").length();
      iTotLen = aLen[iLevel]+iLen;
      
      if (obj.getChildObject() == null) {
         //End of the path, so far:
         if (iTotLen > 255) {
            XMLObject next = obj;
            
            //TEMP: Print the path (backward):
            //System.out.println("Level: "+iLevel+" Length: "+iTotLen);
            //while (next != null) {
            //   System.out.println(next.getTag()+": "+next.getAttr("name"));
            //   next = next.getParentObject();
            //}
            //next = obj;
            
            //We don't want to shorten the current (last) name if it's not a folder:
            iNext = iLevel;
            if ((iNext >= 1) && !obj.getTag().equals("folder")) {
               next = next.getParentObject();
               iNext--;
            }
            int iExcess = 0;
            int iRem = 0; //remainder (mod)
            int iDec = 0; //decrement each
            String name;
            
            //And we don't want to shorten the course name or the section name:
            iExcess = iTotLen - 255;
            if ((iNext + 1 - 2) <= 0) {//index + 1 for count -2 for course & section
               //There is only this one path segment to work with. (Highly unlikely.)
               iDec = iExcess;
               if ((aLen[iNext] - iExcess) > 0) {//enough room?
                  name = next.getAttr("name");
                  aLen[iNext] = aLen[iNext]-iDec;//new length
                  name = name.substring(0,aLen[iNext]);
                  next.setAttr("name",name);
               }
            } else {
               //There are some segments to share the trimming:
               iRem = iExcess % (iNext+1-2);
               iDec = iExcess/(iNext+1-2);
               if (iRem > 0)
                  iDec++; //we'll take off more (a bit more than we need to)
               for (int i=iNext; i>=2; i--) {
                  name = next.getAttr("name");
                  aLen[i] = aLen[i]-iDec;//new length
                  name = name.substring(0,aLen[i]);
                  next.setAttr("name",name);
                  next = next.getParentObject();
                }
            }
            
            //System.out.println("Excess: "+iExcess+" Remainder: "+iRem+" Decrement: "+iDec);

            //System.out.println("Level now: "+iLevel);
            //next = obj;
            //while (next != null) {
            //   System.out.println(next.getTag()+": "+next.getAttr("name"));
            //   next = next.getParentObject();
            //}
            
         }
      } else {
         //Traverse the child nodes, adding the eventual delimiter to the length:
         calculatePathLengths(obj.getChildObject(), iTotLen+1, aLen, iLevel+1);
         
         //Traverse the siblings:
         if (obj.getNextObject() != null)
            calculatePathLengths(obj.getNextObject(), iLen, aLen, iLevel);
      }
   }//adjustPathLengths
   */
   private boolean setupTempFile(int iMods) {
      boolean bOK = true;

      //See if the user wants to choose the temp location:
      boolean bChooseTemp = ((iMods & ActionEvent.SHIFT_MASK) != 0);

      if (!bChooseTemp)
         bOK = createUserTemp(null); //default location

      if (bChooseTemp || !bOK) {
         //The user is choosing OR the default location is blocked:
         File filTemp;

         if (!bOK)
            setMessage("The home directory is not available for your temporary work folder!",
                       WARNING_MSG, false);
         else {
            setMessage("", INFO_MSG, false);
            setMessage("Choose a LOCATION for the temporary work folder.",
                       INFO_MSG, false);
         }
         //Get the user's chosen file, if any:
         filTemp =
               chooseFile(JFileChooser.DIRECTORIES_ONLY, false, "Choose a LOCATION for the temporary work folder.",
                          "");
         if (filTemp != null) {
            bOK = createUserTemp(filTemp);
         }
         if (bOK) {
            glbl.bNoTempYet = false; //have temp, at long last
            setMessage("The temporary work file is " + filTemp.getPath() +
                       "/" + TEMP_FOLDER, INFO_MSG, false);
            setMessage("Locate and open a course Export or Archive zip file.",
                       INFO_MSG, false);
         } else {
            setMessage("bFree cannot continue without a temporary work folder!",
                       ERROR_MSG, true);
         }
      }
      return bOK;
   } //setupTempFile

   private int lastSelection = -1; //to avoid multiple calls
   //private boolean bSelectionIsFromFind = false;

   public void valueChanged(ListSelectionEvent e) { //not used yet
      //Handle events from the content treetable.
      //NOTE: Just have this here for possible later use.
      //Ignore extra messages.
      if (e.getValueIsAdjusting()) {
         return;
      }
      ListSelectionModel lsm = (ListSelectionModel)e.getSource();
      if (lsm.isSelectionEmpty()) { //called for every new selection! seems useless
         //No rows are selected
      } else {
         //We have to avoid duplicate calls from the selection activity (I don't know why):
         int selectedRow = lsm.getMinSelectionIndex();

         int rowHeight = glbl.tabExport.getRowHeight();

         //Scroll the selection into view (including one row above and below):
         //NOTE: Make a path from the selection, then scroll if not isVisible(path).
         Rectangle rSelected =
            new Rectangle(0, selectedRow * rowHeight - rowHeight, 10,
                          rowHeight * 3);
         glbl.tabExport.scrollRectToVisible(rSelected);

         //Set up the preview:
         if (selectedRow != lastSelection) {
            lastSelection = selectedRow;

            //Set up a preview if the object is a previewable type:
            String strType = null;
            TreeTableModelAdapter modAdapt =
               (TreeTableModelAdapter)glbl.tabExport.getModel();
            //Get the object that is selected:
            XMLObject obj = (XMLObject)modAdapt.nodeForRow(selectedRow);
            searcher.setUserSelection(obj); //can be redundant
            glbl.selectedObj = obj;
            strType = obj.getAttr("type");
            if ("TextFileInfoLinkForumArchiveAnnounce".indexOf(strType) >=
                0) { //one of our types?
               setupPreview(obj, strType);
               itemSaveOne.setEnabled(true);
               popSaveOne.setEnabled(true);
               popSaveThis.setEnabled(true);
            } else if (strType.equals("Folder") || strType.equals("Section") ||
                       strType.equals("Course")) {
               //Most folder resources also contain a description:
               String strTemp = obj.getAttr("content", UNKNOWN);
               if (!strTemp.equals(UNKNOWN)) {
                  setupPreview(obj, strTemp);
                  itemSaveOne.setEnabled(true);
                  popSaveOne.setEnabled(true);
                  popSaveThis.setEnabled(true);
               } else {
                  closePreview(strType);
                  itemSaveOne.setEnabled(false);
                  popSaveOne.setEnabled(false);
                  popSaveThis.setEnabled(false);
               }
            } else {
               closePreview(strType);
               itemSaveOne.setEnabled(false);
               popSaveOne.setEnabled(false);
               popSaveThis.setEnabled(false);
            }
            glbl.selStart = -1;
            glbl.selEnd = -1;
         }
      }
   } //valueChanged

   private void closePreview(String type) {
      //bbPreview.showPreview(type + " entries cannot be previewed.", false);
      bbPreview.showPreview("[No preview available.]", false);
   } //closePreview

   private void setupPreview(XMLObject obj,
                             String type) { //Still uses Text, Info, etc.
      XMLAccessor accItem = null;
      String itemXML = null;

      this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

      itemXML = glbl.getArchiveFileText(obj.getAttr("id") + ".dat");
      if ((itemXML != null) && (itemXML.length() > 0)) {
         accItem = glbl.getAccessor(itemXML);
      }
      if (type.equals("Text")) {
         //Display the descriptor text from within the item's resource:
         String strTemp;

         strTemp = DescriptorType.fetchDescriptor(accItem, false);
         strTemp = resolveImageLinks(strTemp, obj.getAttr("id"));
         bbPreview.showPreview(strTemp, false);

      } else if (type.equals("File")) {
         //Preview handles the file, given the full path:
         String strID = obj.getAttr("id");
         String strName =
            obj.getAttr("name"); //title and name the same if not wiki

         //I don't like it, but it seems that we have to special-case wiki entries:
         if (obj.getTag().equals("entry")) {
            //We have to get the file content and handle it like text:
            String strText = "";

            strText = glbl.getArchiveFile(strName, strID);
            //If it contains links to local images, make them work:
            strText = resolveWikiImageLinks(strText, strID);
            bbPreview.showPreview(strText, false);
         } else {
            //For others, just send the file path, and review handles it:
            bbPreview.showPreview(glbl.filTempPath.getPath() + "/" + strID +
                                  "/" + strName, true);
         }
      } else if (type.equals("Info")) {
         String strTemp = InfoType.fetchInfo(obj);
         bbPreview.showPreview("<html>" + strTemp + "</html>", false);
      } else if (type.equals("Link")) {
         String strTemp;
         strTemp = accItem.getAttr("content/url", "value", UNKNOWN);
         if (!strTemp.equals(UNKNOWN)) {
            bbPreview.showPreview(strTemp, false);
         }
      } else if (type.equals("URL")) {
         String strTemp;
         strTemp = accItem.getAttr("coursetoc/url", "value", UNKNOWN);
         if (!strTemp.equals(UNKNOWN)) {
            bbPreview.showPreview(strTemp, false);
         }
      } else if (type.equals("Forum")) {
         String strTemp =
            glbl.fixHTML(glbl.stripJunk(ForumType.fetchForum(obj, true, true)));
         bbPreview.showPreview(strTemp, false);
      } else if (type.equals("Archive")) {
         String strTemp =
            glbl.fixHTML(glbl.stripJunk(ForumType.fetchForum(obj, true, false)));
         bbPreview.showPreview(strTemp, false);
      } else if (type.equals("Announce")) {
         String strTemp =
            glbl.fixHTML(glbl.stripJunk(AnnounceType.fetchAnnouncement(obj)));
         bbPreview.showPreview(strTemp, false);
      } else {
         bbPreview.showPreview(type + "s cannot be previewed.", false);
      }
      this.setCursor(Cursor.getDefaultCursor());
   } //setupPreview

   public String resolveImageLinks(String strText, String strID) {
      //Only for preview!
      //Change Bb's weird embedded-file indicator to use the page's folder name.
      int loc = strText.indexOf(EMBEDDED_FILE_PREFIX);
      String pre = "";
      String post = "";

      while (loc > 0) {
         pre = strText.substring(0, loc);
         post = strText.substring(loc + (EMBEDDED_FILE_PREFIX.length()));
         strText =
               pre + "file:///" + glbl.filTempPath.getPath() + "/" + strID +
               "/embedded/" + post;
         loc = strText.indexOf(EMBEDDED_FILE_PREFIX);
      }
      return strText;
   } //resolveImageLinks

   public String resolveWikiImageLinks(String strText, String strID) {
      //Only for preview!
      //Change Bb's cryptic references to wiki-entry images into references
      //to where (in the resource) the images actually reside.
      String str = "";
      int imgLoc = 0;
      int endLoc = 0;
      int prefixLoc = 0;
      int srcLoc = 0;
      String post = "";
      String img = "";

      post = strText;
      imgLoc = imgLoc = myIndexOf(post, "<IMG", "<img");
      while (imgLoc >= 0) {
         str = str + post.substring(0, imgLoc); //everything from last image to next
         post = post.substring(imgLoc); // from <img ... on
         endLoc = post.indexOf(">");
         img = post.substring(0, endLoc + 1); //from <img to ending >
         post = post.substring(endLoc + 1); //all after >
         //Now img has the entire img tag:
         prefixLoc = img.indexOf(WIKI_IMAGE_PREFIX);
         if (prefixLoc > 0) {
            //Must convert the Bb reference to the actual location:
            srcLoc = Math.max(img.indexOf("src=\""), img.indexOf("SRC=\""));
            str = str + img.substring(0, srcLoc + 5);
            img = img.substring(prefixLoc + WIKI_IMAGE_PREFIX.length());
            str = str + "file:///" + glbl.filTempPath.getPath() + "/" + strID + "/loi-teams/" + img;
         } else {
            //Not a Bb-prefixed image tag (if this even occurs):
            str = str + img; //just keep the whole img tag
         }
         imgLoc = myIndexOf(post, "<IMG", "<img");
      }
      str = str + post;
      return str;
   } //resolveWikiImageLinks

   //NOTE: We don't care about anchor links at this point because links
   //in the preview for wiki entries are not active.

   private static int myIndexOf(String subj, String goal1, String goal2) {
      //Find the nearest index of either goal1 or goal2 in subj.
      int loc1 = subj.indexOf(goal1);
      int loc2 = subj.indexOf(goal2);
      int loc = -1;

      if (loc1 == -1)
         loc = loc2;
      else if (loc2 == -1)
         loc = loc1;
      else if (loc1 <= loc2)
         loc = loc1;
      else
         loc = loc2;
      return loc;
   } //myIndexOf

   private boolean handleNewArchive() {
      //The Open button response is to let the user locate an export
      //zip file, then parse it and set up the major global variables.
      File filTemp;
      XMLObject objTemp = null;
      boolean bOK = true;

      //Get the user's chosen file, if any:
      setMessage("", INFO_MSG, false);
      setMessage("Select an export or archive zip file to be extracted.",
                 INFO_MSG, false);
      filTemp =
            chooseFile(JFileChooser.FILES_ONLY, false, "Select an export zip file to be extracted.",
                       "");
      if (filTemp != null) {

         //Get rid of any old stuff:
         bOK = true;
         glbl.accManifest = null;
         glbl.accItem = null;
         glbl.accRes = null;
         glbl.objCourse = null;
         glbl.objDummy = null;
         glbl.bHadErrors = false;
         pnlTabs.setForegroundAt(MESSAGE_TAB, null);

         //Notify user, and block interaction:
         this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
         pnlGlass.setVisible(true);
         setMessage("Processing: " + filTemp.getPath(), INFO_MSG, false);
         startProgress();

         deleteOldArchive(glbl.filTempPath); //if any

         //Create and save the files, XML strings, and XML accessors:
         bOK = unzipArchive(filTemp);
         /*
         //TEMP
         File filUser = new File(System.getProperty("user.home"));
         File filArchive = new File(filUser, TEMP_FOLDER);
         copyArchive(filArchive, new File(filUser, "Desktop/TempCopy"));
         //endTEMP
         */
         //Manifest accessor:
         String strManifestXML = "";
         if (bOK) {
            //The manifest's XML text from the zip:
            strManifestXML = glbl.getArchiveFileText("imsmanifest.xml");
            if ((glbl.accManifest = glbl.getAccessor(strManifestXML)) == null)
               bOK = false;
         }
         //Item hierarchy accessor:
         if (bOK) {
            if ((objTemp = glbl.accManifest.getObject(ORGANIZATION_PATH)) ==
                null)
               bOK = false;
            else
               glbl.accItem = new XMLAccessor(objTemp);
         }
         //Resource list accessor:
         if (bOK) {
            if ((objTemp = glbl.accManifest.getObject(RESOURCES_PATH)) == null)
               bOK = false;
            else
               glbl.accRes = new XMLAccessor(objTemp);
         }
         //Find out what kind of archive this is:
         if (bOK) {
            if ((strManifestXML.indexOf("res0000") > 0) &&
                (strManifestXML.indexOf("bb:file") > 0)) {
               glbl.archiveType = BLACKBOARD;
               setMessage("This is a Blackboard archive.", INFO_MSG, false);
            } else if ((strManifestXML.indexOf("RESOURCE") > 0) &&
                       (strManifestXML.indexOf("<metadata>") > 0)) {
               glbl.archiveType = SAKAI;
               setMessage("This is a Sakai archive.", INFO_MSG, false);
               bOK = false; //TEMP
            } else {
               bOK = false;
            }
         }
         //Report progress:
         if (!bOK) {
            setMessage("Error: " + filTemp.getPath() +
                       " is not a valid archive or export.", ERROR_MSG, true);
            //Set up the treetable with some dummy content:
            glbl.objDummy = makeDummyObject();
            modExport = new XMLTableModel(glbl.objDummy);

            searcher =
                  new Searcher(glbl.objDummy); //old one points to previous tree

            glbl.tabExport = new JTreeTable(modExport);
            glbl.tabExport.setCursor(new Cursor(Cursor.HAND_CURSOR));
            setTableFeatures(glbl.tabExport);
            scrExport.setViewportView(glbl.tabExport);

            //We unzipped this bad archve (maybe), so delete it:
            deleteOldArchive(glbl.filTempPath); //if any
         } else {
            setMessage(filTemp.getPath() + " is ready to be extracted.",
                       INFO_MSG, false);
         }
         //Make the "display tree":
         if (bOK) {
            //Clear the Preview tab, show the Contents tab:
            bbPreview.showPreview("Nothing is selected.", false);
            pnlTabs.setSelectedIndex(CONTENTS_TAB);
            itemSaveOne.setEnabled(false);
            popSaveOne.setEnabled(false);
            popSaveThis.setEnabled(false);
            //Clear the search text:
            txtFind.setText("");

            if (glbl.archiveType == BLACKBOARD)
               bbIn.createBbContentTree(glbl.accItem);
            else if (glbl.archiveType == SAKAI)
               sakaiIn.createSakaiContentTree(glbl.accItem);
            //System.out.println(new XMLHandler(glbl.objCourse).getXMLText(true)); //TEMP for testing

            modExport =
                  new XMLTableModel(glbl.objCourse); //NOTE changed XMLObject and XMLTableModel
            glbl.tabExport = new JTreeTable(modExport);
            glbl.tabExport.setCursor(new Cursor(Cursor.HAND_CURSOR));
            glbl.tabExport.addMouseListener(popupListener);
            setTableFeatures(glbl.tabExport);
            scrExport.setViewportView(glbl.tabExport);

            //All features are now available, so enable the menu items:
            itemHide.setEnabled(true);
            popHide.setEnabled(true);
            itemSaveFiles.setEnabled(true);
            popSaveFiles.setEnabled(true);
            itemSaveSite.setEnabled(true);
            popSaveSite.setEnabled(true);
            itemFind1st.setEnabled(true);
            itemFindNext.setEnabled(true);
            itemFindPrev.setEnabled(true);
            itemFindLast.setEnabled(true);
            itemTitles.setEnabled(true);
            itemTexts.setEnabled(true);
            itemTidy.setEnabled(true);

            //The find panel is simply revealed; all items are already enabled:
            showFind(true); //the find panel at the bottom

            //Set up for searches:
            searcher = new Searcher(glbl.objCourse);
         }
         stopProgress();
      } else {
         setMessage("\nReady (no zip file was selected).", TEXT_MSG, false);
         //The earlier archive, if any, is still there and ready to process,
         //so there is no change in buttons, search status, etc.
      }
      //User can now interact:
      this.setCursor(Cursor.getDefaultCursor());
      pnlGlass.setVisible(false);
      return bOK;
   } //handleNewArchive

   public static ZipInputStream openExportZip(File fil) {
      //Return a stream to the zip file.
      ZipInputStream zipIn = null;

      try {
         zipIn = new ZipInputStream(new FileInputStream(fil));
      } catch (FileNotFoundException e) {
         return null;
      }
      return zipIn;
   } //openExportZip

   private boolean createUserTemp(File filGiven) {
      //Create the temporary (unzipped) file area within the given folder,
      //if any, or in the user's home directory (the default).
      //Done only if the temp folder does not yet exist.
      //NOTE: In newer Java, need to make sure user home is large enough.
      File filUser = null;
      boolean bOK = true;

      if (glbl.filTempPath == null) {
         if (filGiven == null)
            filUser = new File(System.getProperty("user.home"));
         else
            filUser = filGiven;
         if (filUser == null)
            bOK = false;
         if (bOK)
            bOK = ((glbl.filTempPath = new File(filUser, TEMP_FOLDER)) != null);
         if (glbl.filTempPath == null)
            bOK = false;
         if (bOK)
            bOK = glbl.filTempPath.mkdirs();
         if (bOK) {
            //See if we can read and write there:
            File filTest = new File(glbl.filTempPath, "test");
            try {
               bOK = filTest.createNewFile();
               if (bOK)
                  bOK = (filTest.canRead() && filTest.canWrite());
               filTest.delete();
            } catch (IOException e) {
               bOK = false;
            }
         }
      }
      return bOK;
   } //createUsertemp

   private boolean unzipArchive(File filZ) { //zip file
      //Unzip the chosen archive into a temporary folder.
      //Spceial considerations:
      //  * a file "name" might include a path; use that path
      //  * the path might use "\"; change all to "/"
      //  * a file name might be "!" or "@" followed by hex; change to ASCII
      ZipEntry zipEntry = null;
      boolean bOKSoFar = true;
      String strNameIn = "";
      String strNameOut = "";
      ZipInputStream zip = openExportZip(filZ);
      BufferedInputStream ins = null;
      BufferedOutputStream outs = null;
      File filOut = null;
      File filOutPar = null;

      if (zip == null)
         return false;

      //The filTempPath is already set up (createUserTemp).
      try {
         //Simply step through all the entries and copy them out:
         zipEntry = zip.getNextEntry();
         while (zipEntry != null) {
            strNameIn = zipEntry.getName();
            
            strNameOut = strNameIn.replace('\\', '/');
            strNameOut = glbl.unhexPath(strNameOut);
            int loc = strNameOut.lastIndexOf("/"); //Note must be "/" not "\"
            if (loc >= 0) {
               strNameOut =
                     strNameOut.substring(0, loc + 1) + glbl.fixName(strNameOut.substring(loc +
                                                                                    1)); //OK
            }

            filOut = new File(glbl.filTempPath, strNameOut);
            filOutPar = filOut.getParentFile();
            filOutPar.mkdirs();
            filOut.createNewFile();

            ins = new BufferedInputStream(zip);
            outs = new BufferedOutputStream(new FileOutputStream(filOut));
            byte[] buf = new byte[16484];
            int count = 0;

            count = ins.read(buf, 0, 16484);
            while (count >= 0) {
               outs.write(buf, 0, count);
               count = ins.read(buf, 0, 16384);
            }
            //ins.close(); //Don't!
            outs.close();
            zip.closeEntry();
            zipEntry = zip.getNextEntry();
         }
         setMessage("[Temporary work file in use: " +
                    glbl.filTempPath.getPath() + ".]", INFO_MSG, false);
      } catch (FileNotFoundException e) {
         ref.setMessage("\"" + strNameIn + "\" is not in the zip file.",
                        ERROR_MSG, false);
         //(new Throwable()).printStackTrace();
      } catch (IOException e) {
         ref.setMessage("Could not read \"" + strNameIn +
                        "\" from the zip file.", ERROR_MSG, true);
         //System.out.println(strNameOut);
         //(new Throwable()).printStackTrace();
         bOKSoFar = false;
      } finally {
         try {
            if (ins != null)
               ins.close();
         } catch (IOException e) {
         }
         try {
            if (outs != null)
               outs.close();
         } catch (IOException e) {
         }
      }
      return bOKSoFar;
   } //unzipArchive

   public static void deleteOldArchive(File toDelete) {
      //Delete either the given archive (one we still have a reference to,
      //such as the user's chosen one, or the default if we haven't closed
      //the app yet), or the default archive.
      File filUser = null;
      File filArchive = null;

      if (toDelete == null) {
         filUser = new File(System.getProperty("user.home"));
         filArchive = new File(filUser, TEMP_FOLDER);
      } else
         filArchive = toDelete;
      deleteFiles(filArchive); //recurse, depth-first
      filArchive.delete(); //top-level folder
   } //deleteOldArchive

   private static void deleteFiles(File filArchive) {
      //(Must delete lower-level before higher-level.)
      if (filArchive.exists()) {
         File[] files = filArchive.listFiles();
         if (files != null) {
            for (int i = 0; i < files.length; i++) {
               if (files[i].isDirectory()) {
                  deleteFiles(files[i]);
               }
               files[i].delete();
            }
         }
      }
   } //deleteFiles
   /*
   //I don't know why these are still here.
   private boolean copyArchive(File filArchive, File filNew) {

      if (filArchive.isDirectory()) {
         //Create the target directory, then process its contents:
         filNew.mkdirs();
         File[] files = filArchive.listFiles();
         if (files != null) {
            for (int i = 0; i < files.length; i++) {
               //Extend paths and copy this one:
               File filFrom = new File(filArchive,files[i].getName());
               File filTo = new File(filNew,files[i].getName());
               //copyArchive(new File(filArchive,files[i].getName()), new File(filNew,files[i].getName()));
               copyArchive(filFrom, filTo);
               System.out.println(filFrom.getPath()+" to "+filTo.getPath());
            }
         }
      } else {
         //Simple file; copy it over:
         copyArchiveFile(filArchive, filNew);
      }
      return true;
   } //copyArchive

   private boolean copyArchiveFile(File filArchive, File filNew) {
      FileInputStream from = null;
      FileOutputStream to = null;
      boolean bOKSoFar = true;

      try {
         filNew.createNewFile();
         from = new FileInputStream(filArchive);
         to = new FileOutputStream(filNew);
         byte[] buffer = new byte[4096];
         int bytesRead;

         while ((bytesRead = from.read(buffer)) != -1)
            to.write(buffer, 0, bytesRead); // write
      } catch (IOException e) {
         setMessage("The archive could not be copied to " + filNew.getPath(),
                    WARNING_MSG, false); //don't stop
         bOKSoFar = false;
      } finally {
         if (from != null)
            try {
               from.close();
            } catch (IOException e) {
               ;
            }
         if (to != null)
            try {
               to.close();
            } catch (IOException e) {
               ;
            }
      }
      return bOKSoFar;
   } //copyArchiveFile
   */
   public void outputContent() {
      //Create the folder/file hierarchy on the user's disk.
      File filBase = null;

      setMessage("CHOOSE or MAKE a folder to hold the course content.",
                 INFO_MSG, false);
      filBase =
            chooseFile(JFileChooser.DIRECTORIES_ONLY, true, "Choose or make a folder to hold the course content.",
                       "");
      if (filBase != null) {
         filBase.mkdirs();
         setMessage("Extracting course content to " + filBase.getPath() + ".",
                    INFO_MSG, false);
         pnlGlass.setVisible(true);
         this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
         startProgress();
         filBase.mkdir();
         bbOut.doOutput(glbl.objCourse.getChildObject(), filBase,
                        new Vector());
         stopProgress();
         setMessage("The course content is now in " + filBase.getPath() +
                    ". Open that folder to view the content.", INFO_MSG,
                    false);
         this.setCursor(Cursor.getDefaultCursor());
         pnlGlass.setVisible(false);
      } else {
         setMessage("\nReady (no folder was chosen).", INFO_MSG, false);
      }
   } //outputContent

   public void outputSite(File filTemps) { //null if defaults used (v1 always; v2 maybe)
      //Create the user's web site in a folder on the user's disk.
      File filBase = null;

      setMessage("CHOOSE or MAKE a folder to contain your new web site.",
                 INFO_MSG, false);

      filBase =
            chooseFile(JFileChooser.DIRECTORIES_ONLY, true, "Choose or make a folder to hold the web site.",
                       "");
      if (filBase != null) {
         filBase.mkdirs();
         setMessage("Creating a site in " + filBase.getPath(), INFO_MSG,
                    false);
         pnlGlass.setVisible(true);
         this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
         startProgress();
         bbSite.doSiteOutput(glbl.objCourse, filBase, filTemps);
         stopProgress();
         setMessage("The new site is in " + filBase.getPath() +
                    ". Open the \"index.html\" file in your browser.",
                    INFO_MSG, false);
         this.setCursor(Cursor.getDefaultCursor());
         pnlGlass.setVisible(false);
      } else {
         setMessage("\nReady (no web site folder was chosen).", INFO_MSG,
                    false);
      }
   } //outputSite

   public void outputSelection(XMLObject obj) {
   //Output the contents of this one selected object only.
      //Assumes the type is one of: Text, Info, File, Link.
      //Outputs the item to the user's chosen file.
      File filBase = null;
      String strFullName = obj.getAttr("display").replace('\\', '/');
      String strName = strFullName;
      int loc = -1;

      //The name and the title are the same in most Files, but a wiki entry
      //has a generated display title, while name is the actual (full) file name:
      if (obj.getAttr("type").equals("File")) {
         strFullName = obj.getAttr("name").replace('\\', '/');
         strName = obj.getAttr("display"); //generated
      }
      //The full name might be a path, which we don't use in the output name:
      loc = strName.lastIndexOf("/");
      if (loc >= 0)
         strName = strName.substring(loc + 1); //last segment
      //Descriptions will be text files:
      if (strName.equals("Description"))
         strName = strName + ".txt";

      setMessage("Choose a location for the extracted file.", INFO_MSG, false);

      filBase =
            chooseFile(JFileChooser.FILES_ONLY, true, "Choose a location for the extracted file.",
                       strName);
      if (filBase != null) {
         String type = obj.getAttr("type");
         String content = obj.getAttr("content", UNKNOWN);
         String id = obj.getAttr("id");
         File filParent = filBase.getParentFile();

         strName = filBase.getName(); //user's chosen name

         this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

         if (type.equals("File")) {
            //The content is in the archive:
            FileType.copyArchiveFileOut(strFullName, strName, filParent, id);
         } else {
            //The content is in the resource XML:
            XMLAccessor accItem = null;
            String itemXML = null;
            String strTemp =
               "This item was not an extractable type."; //default

            itemXML = glbl.getArchiveFileText(obj.getAttr("id") + ".dat");
            if ((itemXML != null) && (itemXML.length() > 0)) {
               accItem = glbl.getAccessor(itemXML);
            }
            if (content.equals("Text") || type.equals("Text")) {
               strTemp = accItem.getField("content/body/text", UNKNOWN);
               if (strTemp.equals(UNKNOWN)) {
                  //Could be the Course descriptor:
                  strTemp = accItem.getField("course/description", UNKNOWN);
               }
            } else if (content.equals("Info") || type.equals("Info")) {
               strTemp = InfoType.fetchInfo(obj);
            } else if (content.equals("Link") || type.equals("Link")) {
               strTemp = accItem.getAttr("content/url", "value", UNKNOWN);
            } else {
               //Not an extractable type:
            }
            if (!strTemp.equals(UNKNOWN)) {
               bbOut.writeTextOut(strName, strTemp, filParent);
            }
         }
         this.setCursor(Cursor.getDefaultCursor());
         setMessage("Content extracted to \"" + filBase.getPath() + "\"",
                    INFO_MSG, false);
      }
   } //outputSelection

   public File chooseFile(int mode, boolean bSave, String strTitle,
                          String strName) {
      //Return the File object for the file or directory that the user chooses,
      //or null if none.
      //Param "mode" allows files or directories (files for Open, directories
      //for Save.
      //Param "bSave" is true for a Save dialog; else Open.
      JFileChooser chooser = null;
      String[] ext = { "zip" }; //only zips for Open!
      ExtensionFileFilter filter = new ExtensionFileFilter(ext);
      String strFile = null;
      File filFile = null;
      int returnVal = 0;

      String strMe =
         System.getProperty("user.home") + System.getProperty("file.separator") +
         "Desktop" + System.getProperty("file.separator");

      File filIn = new File(strMe);

      //Show the user the chooser:
      chooser = new JFileChooser(filIn);
      if (strName.equals(""))
         chooser.setSelectedFile(filFile);
      else {
         chooser.setAcceptAllFileFilterUsed(true);
         chooser.setSelectedFile(new File(filIn,
                                          strName)); //name is "" or actual name only
      }
      chooser.setFileSelectionMode(mode);
      if (bSave) {
         chooser.setDialogTitle(strTitle);
         chooser.setApproveButtonText("Select");
         returnVal = chooser.showSaveDialog(this);
      } else {
         chooser.setDialogTitle(strTitle);
         chooser.setFileFilter(filter);
         returnVal = chooser.showOpenDialog(this);
      }
      //Get the user's response:
      if (returnVal == JFileChooser.APPROVE_OPTION) {
         //User chose a file:
         filFile = chooser.getSelectedFile();
         strFile = filFile.getPath();
         glbl.strCourseName = filFile.getName();
         //Create a course name from the export zip's file name:
         //(e.g., "ExportFile_My_Course.zip" => "My_Course".)
         //Used in createContentTree only after opening a zip.
         int loc =
            Math.max(glbl.strCourseName.indexOf(".zip"), glbl.strCourseName.indexOf(".ZIP"));
         if (loc > 3)
            glbl.strCourseName = glbl.strCourseName.substring(0, loc);
         if (glbl.strCourseName.startsWith("ExportFile_"))
            glbl.strCourseName = glbl.strCourseName.substring(11);
         //NOTE: An archive contains a resource that names the course. An export does not.
      } else
         filFile = null;
      chooser = null;
      return filFile;
   } //chooseFile

   //The various panels can use these methods to get images and texts
   //out of the resources. The resources are in folders contained
   //in the "classes" folder. Their relative paths are among the constants
   //defined for this class.

   public Image getResourceImage(String strImage) {
      //Return an image from a file in our own resources.
      MediaTracker oTracker;
      Image imgOver = null;
      URL url = null;

      oTracker = new MediaTracker(this);
      if (clResources == null)
         clResources = this.getClass().getClassLoader();

      url = clResources.getResource(APP_IMAGE_BASE + strImage);
      imgOver = Toolkit.getDefaultToolkit().getImage(url);

      oTracker.addImage(imgOver, 1);

      //Wait until we have loaded the image:
      try {
         oTracker.waitForID(1);
      } catch (InterruptedException e) {
         //Do something useful here:
         setMessage("getResourceImage: interrupted", ERROR_MSG, true);
         return null;
      }
      return imgOver;
   } //getResourceImage

   public String getResourceText(String strSource) {
      //Return the contents of a text file from our own resources.
      String strLine;
      String strText = new String("");
      String strSep = System.getProperty("line.separator");

      if (clResources == null)
         clResources = this.getClass().getClassLoader();

      try {
         BufferedReader brData =
            new BufferedReader(new InputStreamReader(clResources.getResourceAsStream(APP_TEXT_BASE +
                                                                                     strSource)));
         strLine = brData.readLine();
         while (strLine != null) {
            strText = strText + strLine + strSep;
            strLine = brData.readLine();
         }
         brData.close();
      } catch (IOException e) {
         //Do something useful here.
         setMessage("getResourceText: IOException file=\"" + APP_TEXT_BASE +
                    strSource + "\"", ERROR_MSG, true);
      }
      return strText;
   } //getResourceText


   private void condense(XMLObject obj) {
      //Remove display tree nodes that have no data. Depth-first recursive
      //traversal and processing (i.e., starting from the leaves so that
      //"emptiness" propagates from the bottom up).

      if (obj == null)
         return;

      //Process my children first:
      condense(obj.getChildObject());

      //Now process me (linking me out, if necessary, from my siblings):
      if (obj.getAttr("dir").equals("true") &&
          (obj.getAttr("content", UNKNOWN).equals(UNKNOWN) || 
          obj.getAttr("content", UNKNOWN).equals("Section")) &&
          (obj.getChildObject() == null)) {
         //This node has no available content:
         XMLObject par = obj.getParentObject();
         XMLObject prev = null;
         if (par != null) {
            prev = par.getChildObject();
            if (prev == obj) {
               //I am the first node among the siblngs (could be the only).
               par.setChildObject(obj.getNextObject());
            } else {
               while (prev.getNextObject() != obj) {
                  prev = prev.getNextObject();
               }
               //Link me out:
               prev.setNextObject(obj.getNextObject());
            }
         }
      }
      //Now process my siblings, if any:
      condense(obj.getNextObject());
   } //condense

   //Panel switching: ----------------------------------------------------------

   private int iLastTab = 0;

   public void stateChanged(ChangeEvent he) {
      //Handle clicks on the pnlTabs. To repeat:
      //Set up the tabbed pane. This is complex. Each tab contains
      //a split pane. Each split pane contains the outline in the top,
      //and a particular information panel (preview, messges, ...)
      //in the bottom. But, of course, the outline cannot be in all
      //panes at once, so it must be moved from splitpane to splitpane
      //as the user clicks on the pnlTabs, giving the impression that
      //it never moves.
      int iTab = pnlTabs.getSelectedIndex();
      int iLoc = -1;
      JSplitPane spnl = (JSplitPane)pnlTabs.getComponentAt(iLastTab);

      iLastTab = iTab;

      iLoc = spnl.getDividerLocation();

      //Take the outline out of the split pane that it is in now:
      spnl.remove(pnlOutline);

      //Now put the outline into the split pane that is becoming active:
      if (iTab == CONTENTS_TAB) {
         pnlSplitContents.add(pnlOutline, JSplitPane.TOP);
         pnlSplitContents.setDividerLocation(iLoc);
         showFind(true);
      } else if (iTab == MESSAGE_TAB) {
         pnlSplitMessages.add(pnlOutline, JSplitPane.TOP);
         pnlSplitMessages.setDividerLocation(iLoc);
         showFind(false);
      } else if (iTab == HELP_TAB) {
         pnlSplitHelp.add(pnlOutline, JSplitPane.TOP);
         pnlSplitHelp.setDividerLocation(iLoc);
         showFind(false);
      } else if (iTab == COPYRIGHT_TAB) {
         pnlSplitCopyrights.add(pnlOutline, JSplitPane.TOP);
         pnlSplitCopyrights.setDividerLocation(iLoc);
         showFind(false);
      } else if (iTab == LICENSE_TAB) {
         pnlSplitLicense.add(pnlOutline, JSplitPane.TOP);
         pnlSplitLicense.setDividerLocation(iLoc);
         showFind(false);
      }
   } //stateChanged


   private void showFind(boolean bShow) {
   //Swap the find panel in or out as needed.
      if (bShow && !bIsShowing) {
         this.getContentPane().remove(pnlWarn);
         this.getContentPane().remove(pnlNoFind);
         this.getContentPane().add(pnlFind, BorderLayout.SOUTH);
         this.getContentPane().repaint();
         bIsShowing = true;
      } else if (!bShow && bIsShowing) {
         this.getContentPane().remove(pnlFind);
         this.getContentPane().add(pnlNoFind, BorderLayout.SOUTH);
         this.getContentPane().repaint();
         bIsShowing = false;
      }
   } //showFind

   //Main panels: --------------------------------------------------------------

   //Title area, held in pnlTitle:
   private JLabel lblTitle = null;
   private Image imgTitle = null;
   private ImageIcon icnTitle = null;
   //"About":
   private JButton btnVersion = null;
   //Activity message (hidden and shown when needed):
   private JLabel lblProcessing = null;
   private JPanel pnlTitle = new JPanel(new BorderLayout());

   private void setupTitlePanel() {
      //Title banner and about box. These are held in a panel
      //(pnlTitle) to be placed in the North of the window:

      //UNC logo:
      imgTitle = getResourceImage("bannerleft.gif");
      icnTitle = new ImageIcon(imgTitle);
      lblTitle = new JLabel(icnTitle);
      lblTitle.setPreferredSize(new Dimension(227, 76));
      lblTitle.setFont(new Font("Serif", Font.BOLD, 36));
      lblTitle.setHorizontalAlignment(SwingConstants.CENTER);

      //Activity label:
      lblProcessing = new JLabel("");
      lblProcessing.setPreferredSize(new Dimension(233, 76));
      lblProcessing.setFont(new Font("Sanserif", Font.BOLD, 18));
      lblProcessing.setForeground(Color.yellow);

      //"About" button (the right third of the banner):
      imgTitle = getResourceImage("bannerright.gif");
      icnTitle = new ImageIcon(imgTitle);
      btnVersion = new JButton(icnTitle);
      btnVersion.setPreferredSize(new Dimension(227, 76));
      btnVersion.setFont(new Font("Serif", Font.BOLD, 12));
      btnVersion.setCursor(new Cursor(Cursor.HAND_CURSOR));
      btnVersion.setBorderPainted(false);
      btnVersion.setContentAreaFilled(false);
      btnVersion.addActionListener(this);
      btnVersion.setToolTipText("bFree Version " + VERSION);

      pnlTitle.setBackground(new Color(96, 152, 200));
      pnlTitle.setBounds(0, 0, 980, 76);
      pnlTitle.setBorder(BorderFactory.createLineBorder(Color.white));
      pnlTitle.add(lblTitle, BorderLayout.WEST);
      pnlTitle.add(lblProcessing, BorderLayout.CENTER);
      pnlTitle.add(btnVersion, BorderLayout.EAST);
   } //setupTitlePanel

   //The underlying tabbed pane, holding all the split panes:
   private JTabbedPane pnlTabs = new JTabbedPane(JTabbedPane.TOP);

   //Content outline, held in any split pane:
   private JScrollPane scrExport = null;
   private XMLTableModel modExport = null;
   private JPanel pnlOutline = null;

   //In the Messages tab of the tabbed pane:
   private MessagePanel pnlMessages;

   //In the Contents tab of the tabbed pane:
   private PreviewPanel pnlPreview;
   private BbPreview bbPreview = null;

   //In the Help tab of the tabbed pane: (these 3 are HTML displays)
   private HelpPanel pnlHelp;

   //In the Copyright tab of the tabbed pane:
   private HelpPanel pnlCopyrights;

   //In the License tab of the tabbed pane:
   private HelpPanel pnlLicense;

   //Split panes for each view:
   private JSplitPane pnlSplitContents = new JSplitPane(); //preview
   private JSplitPane pnlSplitMessages = new JSplitPane();
   private JSplitPane pnlSplitHelp = new JSplitPane();
   private JSplitPane pnlSplitCopyrights = new JSplitPane();
   private JSplitPane pnlSplitLicense = new JSplitPane();

   private void setupTabbedPanel() {
      //Set up the tabbed pane. This is complex. Each tab contains
      //a split pane. Each split pane contains the outline in the top,
      //and a particular information panel (preview, messages, ...)
      //in the bottom. But, of course, the outline cannot be in all
      //panes at once, so it must be moved from one split pane to the next
      //as the user clicks on the pnlTabs, giving the impression that
      //it never moves.
      pnlTabs.setBounds(new Rectangle(10, 148, 960, 508)); //518
      this.getContentPane().add(pnlTabs);
      pnlTabs.setBackground(BG_COLOR);

      //Set up the treetable with some dummy content:
      glbl.objDummy = makeDummyObject();
      modExport = new XMLTableModel(glbl.objDummy);

      searcher = new Searcher(glbl.objDummy);

      glbl.tabExport = new JTreeTable(modExport);
      glbl.tabExport.setCursor(new Cursor(Cursor.HAND_CURSOR));
      setTableFeatures(glbl.tabExport);

      scrExport = new JScrollPane(glbl.tabExport);
      scrExport.setBounds(new Rectangle(0, 0, 960, 508)); //518
      pnlOutline = new JPanel(new BorderLayout());
      pnlOutline.setMinimumSize(new Dimension(582, 150));
      pnlOutline.add(scrExport, BorderLayout.CENTER);

      //Set up the preview panel and the preview object:
      pnlPreview = new PreviewPanel(this);
      pnlPreview.setMinimumSize(new Dimension(582, 150));
      bbPreview = new BbPreview(pnlPreview); //shows previews inside pnlPreview

      //Set up the messages panel:
      pnlMessages = new MessagePanel(this);
      pnlMessages.setMinimumSize(new Dimension(582, 150));
      pnlMessages.clearMessages("Processing Messages (E => error, ! => warning): ");
      pnlMessages.addMessage("Click Open, and locate a course \"zip\" file exported from Blackboard\u2122.",
                             TEXT_MSG);

      //Set up the help panel:
      pnlHelp = new HelpPanel(this, "bFreeHelp.html");
      pnlHelp.setMinimumSize(new Dimension(582, 150));

      //Set up the copyrights panel:
      pnlCopyrights = new HelpPanel(this, "bFreeCR.html");
      pnlCopyrights.setMinimumSize(new Dimension(582, 150));

      //Set up the license panel:
      pnlLicense = new HelpPanel(this, "bFreeLicense.html");
      pnlLicense.setMinimumSize(new Dimension(582, 150));

      //Set up the various split panes with content panels:
      pnlSplitContents.setOneTouchExpandable(true);
      pnlSplitContents.setDividerLocation(-1);
      pnlSplitContents.setOrientation(JSplitPane.VERTICAL_SPLIT);
      pnlSplitContents.add(pnlOutline, JSplitPane.TOP);
      pnlSplitContents.add(pnlPreview, JSplitPane.BOTTOM);

      pnlSplitMessages.setOneTouchExpandable(true);
      pnlSplitMessages.setDividerLocation(-1);
      pnlSplitMessages.setOrientation(JSplitPane.VERTICAL_SPLIT);
      pnlSplitMessages.add(pnlMessages, JSplitPane.BOTTOM);

      pnlSplitHelp.setOneTouchExpandable(true);
      pnlSplitHelp.setDividerLocation(-1);
      pnlSplitHelp.setOrientation(JSplitPane.VERTICAL_SPLIT);
      pnlSplitHelp.add(pnlHelp, JSplitPane.BOTTOM);

      pnlSplitCopyrights.setOneTouchExpandable(true);
      pnlSplitCopyrights.setDividerLocation(-1);
      pnlSplitCopyrights.setOrientation(JSplitPane.VERTICAL_SPLIT);
      pnlSplitCopyrights.add(pnlCopyrights, JSplitPane.BOTTOM);

      pnlSplitLicense.setOneTouchExpandable(true);
      pnlSplitLicense.setDividerLocation(-1);
      pnlSplitLicense.setOrientation(JSplitPane.VERTICAL_SPLIT);
      pnlSplitLicense.add(pnlLicense, JSplitPane.BOTTOM);

      //Put the split panes into the central panel:
      pnlTabs.add("Course Contents", pnlSplitContents);
      pnlTabs.add("Messages", pnlSplitMessages);
      pnlTabs.add("Help", pnlSplitHelp);
      pnlTabs.add("Copyrights", pnlSplitCopyrights);
      pnlTabs.add("License", pnlSplitLicense);
      pnlTabs.setBackground(new Color(230, 235, 245));
      pnlTabs.addChangeListener(this);

      //NOTE: Bug: the following interfere with the pnlTabs' sensitivity to clicks:
      //pnlTabs.setToolTipTextAt(0,"An outline of the contents of your course.");
      //pnlTabs.setToolTipTextAt(3,"Step-by-step help for using this application.");//(check indices)
      //pnlTabs.setToolTipTextAt(4,"Copyrights for software included in  this application.");
      //Maybe a Java update will fix this.
   } //setupTabbedPanel

   //The Find controls, in pnlFind:
   private JLabel lblFind = new JLabel();
   private JCheckBox chkTitles = new JCheckBox("Titles");
   private JCheckBox chkTexts = new JCheckBox("Texts");
   private JTextField txtFind = new JTextField();
   private JButton btnFind = new JButton("1st");
   private JButton btnFindNext = new JButton("Next");
   private JButton btnFindPrev = new JButton("Prev");
   private JButton btnFindLast = new JButton("Last");
   private JCheckBox chkTidy = new JCheckBox("Keep outline tidy");
   private JPanel pnlFind = new JPanel(new FlowLayout());

   private Image imgWarn = getResourceImage("alerticon.gif");
   private ImageIcon icnWarn = new ImageIcon(imgWarn);
   private JLabel lblWarn =
      new JLabel("<html><FONT size=4>Please make sure that your distribution of <FONT color=#990000 size=4>copyrighted " +
                 "materials</FONT> conforms to copyright laws and fair use guidelines.</FONT></html>",
                 icnWarn, SwingConstants.LEFT);
   private JPanel pnlWarn = new JPanel(new FlowLayout());

   private JPanel pnlNoFind = new JPanel();
   private boolean bIsShowing = false; //pnlNoFind is installed

   private void setupFindPanel(Insets margin) {
      //Text find area, held in the south of the frame.
      //This is displayed only when the content/preview panel
      //is showing. Otherwise, a dummy panel is substituted.
      //See valueChanged.
      lblFind.setText("Search: ");
      lblFind.setPreferredSize(new Dimension(114, 30));
      lblFind.setFont(new Font("Sanserif", Font.BOLD, 16));
      lblFind.setHorizontalAlignment(SwingConstants.RIGHT);

      chkTitles.setPreferredSize(new Dimension(70, 30));
      chkTitles.setToolTipText("Search for the text within all titles.");
      chkTitles.addActionListener(this);
      chkTitles.setSelected(true);
      chkTitles.setMargin(margin);
      chkTitles.setBackground(BG_COLOR);

      chkTexts.setPreferredSize(new Dimension(70, 30));
      chkTexts.setToolTipText("Search for the text within descriptions.");
      chkTexts.addActionListener(this);
      chkTexts.setSelected(true);
      chkTexts.setMargin(margin);
      chkTexts.setBackground(BG_COLOR);

      txtFind.setPreferredSize(new Dimension(136, 30));
      txtFind.setToolTipText("Type text to find; case is ignored.");
      txtFind.addActionListener(this);

      btnFind.setPreferredSize(new Dimension(50, 30));
      btnFind.setToolTipText("Find the first occurrence of the text.");
      btnFind.addActionListener(this);
      btnFind.setMargin(margin);

      btnFindNext.setPreferredSize(new Dimension(50, 30));
      btnFindNext.setToolTipText("Find the next occurrence of the text.");
      btnFindNext.addActionListener(this);
      btnFindNext.setMargin(margin);

      btnFindPrev.setPreferredSize(new Dimension(50, 30));
      btnFindPrev.setToolTipText("Find the previous occurrence of the text.");
      btnFindPrev.addActionListener(this);
      btnFindPrev.setMargin(margin);

      btnFindLast.setPreferredSize(new Dimension(50, 30));
      btnFindLast.setToolTipText("Find the last occurrence of the text.");
      btnFindLast.addActionListener(this);
      btnFindLast.setMargin(margin);

      chkTidy.setPreferredSize(new Dimension(200, 30));
      chkTidy.setToolTipText("Close up the previous selection, then find.");
      chkTidy.addActionListener(this);
      chkTidy.setMargin(margin);
      chkTidy.setBackground(BG_COLOR);

      pnlFind.setPreferredSize(new Dimension(600, 40));
      pnlFind.setBackground(BG_COLOR);
      pnlFind.add(lblFind);
      pnlFind.add(chkTitles);
      pnlFind.add(chkTexts);
      pnlFind.add(txtFind);
      pnlFind.add(btnFind);
      pnlFind.add(btnFindNext);
      pnlFind.add(btnFindPrev);
      pnlFind.add(btnFindLast);
      pnlFind.add(chkTidy);

      pnlWarn.setPreferredSize(new Dimension(600, 40));
      pnlWarn.setBackground(BG_COLOR);
      pnlWarn.add(lblWarn);

      pnlNoFind.setPreferredSize(new Dimension(600, 40));
      pnlNoFind.setBackground(BG_COLOR);
   } //setupFindPanel

   //Menus ---------------------------------------------------------------------

   //Popup menus and items:
   private JPopupMenu popMenu; //in outline
   private JPopupMenu popMenuP; //in preview
   private JMenuItem popOpen = new JMenuItem("Open an archive \"zip\" ...");
   private JMenuItem popHide = new JMenuItem("Hide non-content items");
   private JMenuItem popSaveFiles =
      new JMenuItem("Extract as files/folders ...");
   private JMenuItem popSaveSite = new JMenuItem("Extract as a web site ...");
   private JMenuItem popSaveOne = new JMenuItem("Save the selection only ...");

   private JMenuItem popSaveThis = new JMenuItem("Save this content item ...");

   private MouseListener popupListener = new PopupListener("O");
   private MouseListener popupListener1 = new PopupListener("O");
   private MouseListener popupListenerP = new PopupListener("P");

   class PopupListener extends MouseAdapter {
      String where;

      public PopupListener(String where) {
         this.where = where;
      }

      public void mousePressed(MouseEvent e) {
         maybeShowPopup(e);
      }

      public void mouseReleased(MouseEvent e) {
         maybeShowPopup(e);
      }

      private void maybeShowPopup(MouseEvent e) {
         if (e.isPopupTrigger()) {
            if (where.equals("O"))
               popMenu.show(e.getComponent(), e.getX(), e.getY());
            else
               popMenuP.show(e.getComponent(), e.getX(), e.getY());
         }
      }
   } //PopupListener

   private void setupPopupMenus() {
      //Popup menus:
      popMenu = new JPopupMenu(); //for outline area
      popOpen.addActionListener(this);
      popMenu.add(popOpen);
      popMenu.addSeparator();
      popHide.addActionListener(this);
      popHide.setEnabled(false);
      popMenu.add(popHide);
      popMenu.addSeparator();
      popSaveFiles.addActionListener(this);
      popSaveFiles.setEnabled(false);
      popMenu.add(popSaveFiles);
      popSaveSite.addActionListener(this);
      popSaveSite.setEnabled(false);
      popMenu.add(popSaveSite);
      popMenu.addSeparator();
      popSaveOne.addActionListener(this);
      popSaveOne.setEnabled(false);
      popMenu.add(popSaveOne);

      popMenuP = new JPopupMenu(); //for preview area
      popSaveThis.addActionListener(this);
      popSaveThis.setEnabled(false);
      popMenuP.add(popSaveThis);

      glbl.tabExport.addMouseListener(popupListener);
      scrExport.addMouseListener(popupListener1);
      pnlPreview.addMouseListener(popupListenerP);

   } //setupPopupMenus

   //Window menus:
   private JMenuBar menuBar = new JMenuBar();

   private JMenu menuFile = new JMenu("File");
   private JMenuItem itemOpen = new JMenuItem("Open an archive \"zip\" ...");
   private JMenuItem itemHide = new JMenuItem("Hide non-content items");
   private JMenuItem itemSaveFiles =
      new JMenuItem("Extract as files/folders ...");
   private JMenuItem itemSaveSite = new JMenuItem("Extract as a web site ...");
   private JMenuItem itemSaveOne =
      new JMenuItem("Save the selection only ...");
   private JMenuItem itemExit = new JMenuItem("Exit");

   private JMenu menuSearch = new JMenu("Search");
   private JMenuItem itemFind1st = new JMenuItem("Find first occurrence");
   private JMenuItem itemFindNext = new JMenuItem("Find next occurrence");
   private JMenuItem itemFindPrev = new JMenuItem("Find previous occurrence");
   private JMenuItem itemFindLast = new JMenuItem("Find last occurrence");
   private JCheckBoxMenuItem itemTitles =
      new JCheckBoxMenuItem("Find in titles");
   private JCheckBoxMenuItem itemTexts =
      new JCheckBoxMenuItem("Find in texts");
   private JCheckBoxMenuItem itemTidy =
      new JCheckBoxMenuItem("Keep the outline tidy");

   private void setupWindowMenus() {
      this.setJMenuBar(menuBar);

      itemOpen.setToolTipText("Open an archive or export zip file from Blackboard.");
      itemOpen.addActionListener(this);
      itemHide.setEnabled(false);
      itemHide.setToolTipText("Hide empty folders to reduce clutter.");
      itemHide.addActionListener(this);
      itemSaveFiles.setEnabled(false);
      itemSaveFiles.setToolTipText("Extract the course content into a set of folders and files.");
      itemSaveFiles.addActionListener(this);
      itemSaveSite.setEnabled(false);
      itemSaveSite.setToolTipText("Extract the course content as an independent web site.");
      itemSaveSite.addActionListener(this);
      itemSaveOne.setEnabled(false);
      itemSaveOne.setToolTipText("Extract only the currently-selected content item.");
      itemSaveOne.addActionListener(this);
      itemExit.addActionListener(this);

      menuFile.add(itemOpen);
      menuFile.addSeparator();
      menuFile.add(itemHide);
      menuFile.addSeparator();
      menuFile.add(itemSaveFiles);
      menuFile.add(itemSaveSite);
      menuFile.addSeparator();
      menuFile.add(itemSaveOne);
      menuFile.addSeparator();
      menuFile.add(itemExit);
      menuBar.add(menuFile);

      itemFind1st.setEnabled(false);
      itemFind1st.addActionListener(this);
      itemFindNext.setEnabled(false);
      itemFindNext.addActionListener(this);
      itemFindPrev.setEnabled(false);
      itemFindPrev.addActionListener(this);
      itemFindLast.setEnabled(false);
      itemFindLast.addActionListener(this);
      itemTitles.addActionListener(this);
      itemTitles.setSelected(true);
      itemTexts.addActionListener(this);
      itemTidy.setToolTipText("Close up the previous selection, then find.");
      itemTidy.addActionListener(this);

      menuSearch.add(itemFind1st);
      menuSearch.add(itemFindNext);
      menuSearch.add(itemFindPrev);
      menuSearch.add(itemFindLast);
      menuSearch.addSeparator();
      menuSearch.add(itemTitles);
      menuSearch.add(itemTexts);
      menuSearch.add(itemTidy);
      menuBar.add(menuSearch);

   } //setupWindowMenus

   public void processWindowEvent(WindowEvent e) {
      if (e.getID() ==
          WindowEvent.WINDOW_CLOSING) { //WINDOW_CLOSED is too late
         startProgress();
         deleteOldArchive(glbl.filTempPath);
      }
      super.processWindowEvent(e);
      if (e.getID() ==
          WindowEvent.WINDOW_CLOSING) { //WINDOW_CLOSED is too late
         System.exit(0);
      }
   } //processWindowEvent

}//Unzipper
