package edu.unc.its.tl.bb.client;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;

import java.io.PrintStream;

import java.util.HashMap;

import java.util.Vector;

import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.swing.JFileChooser;

import org.dvm.java.xml.XMLAccessor;
import org.dvm.java.xml.XMLObject;

public final class BbSite implements BbConstants {
   //Methods to output the Blackboard export zip content tree as a flat
   //web site, a site contained in a single folder, with all pages
   //at the same level within it. For each page with linked files, there
   //will be a folder bearing the page's name and containing those files.

   //Since the titles of the various pages might be the same,
   //and because we are taking them out of their hierarchical
   //name spaces, we have some renaming to do during processing.
   //(This will affect HTML names, not the page titles.)

   boolean bTracing = false;

   //Map the content nodes to their (possibly new) names,
   //so we can find the working name for each node:
   private HashMap objectToName =
      new HashMap(23, 11); //"fixed" or working names

   //Map the (possibly new) names to their nodes, so that we
   //can see if a name is already taken:
   private HashMap nameToObject = new HashMap(23, 11);

   //The menu of sections, and the course title, which appear on all pages:
   private String strMenuText = "";
   private String strCourse = "";

   //The basic page template, used for most pages (read once):
   private String strBasicPage = null;

   //Where local resources (texts, images) comes from:
   private ClassLoader clResources = null;

   private Globals glbl = null;

   public BbSite() {
      glbl = Globals.getInstance();
   } //constructor

   private boolean bDefaultTemplates = true; //v1 always true
   private File filTemplates = null; //v1 always null

   public void doSiteOutput(XMLObject obj, File filDest,
                            File filTemps) { //null for default templates
      //Recursively create the directories and pages, and copy over the files,
      //for the standalone web site.
      //Param "filDest" is the File object for the directory that the user
      //chose to hold our folder.

      if (obj == null)
         return;

      //Get rid of any old name-mapping stuff:
      objectToName = new HashMap(23, 11);
      nameToObject = new HashMap(23, 11);
      strMenuText = "";
      strCourse = "";

      //Using default or custom templates?: //meaningful in version 2
      filTemplates = filTemps;
      bDefaultTemplates = (filTemps == null);

      //Set up the name mappings:
      verifyNames(obj);

      //Make the main folder and its index.html page:
      if (obj.getAttr("extract").equals("true")) {
         //Create the index.html page:
         makeIndex(obj, filDest);

         //Load the basic page once, for multiple usages:
         strBasicPage = getTemplate(PAGE_TEMPLATE);

         //Now create the other pages recursively:
         makePage(obj.getChildObject(), filDest, "");
      }
   } //doSiteOutput

   private void verifyNames(XMLObject obj) {
      //Collect up all the names of pages-to-be, and "correct" any
      //that collide by suffixing counters.
      //Note that we don't want to change the actual titles stored
      //in the nodes, because that would change the display.
      String strTitle = null;

      if (obj == null)
         return;

      //Handle the current node:
      //Pages must have unique names.
      //The files they reference will be stored in folders having the pages' names,
      //so files themselves do not need unique names.
      if (obj.getAttr("dir").equals("true")) {
         //First see if the name is taken, and change it if so:
         strTitle = obj.getAttr("display") + "_01";
         while (nameToObject.containsKey(strTitle.toLowerCase())) {
            strTitle = incrementName(strTitle);
         }
         //Remember the unique title given to this node:
         nameToObject.put(strTitle.toLowerCase(), obj);
         strTitle = glbl.fixName(strTitle);
         objectToName.put(obj, strTitle); //for later retrieval
      }
      //Handle its children:
      verifyNames(obj.getChildObject());

      //Handle its siblings:
      verifyNames(obj.getNextObject());
   } //verifyNames

   private String incrementName(String str) {
      //Increment the 2-digit suffixed counter on this string.
      String sNum = str.substring(str.length() - 2);
      int iNum = 0;

      try {
         iNum = Integer.parseInt(sNum) + 1;
      } catch (NumberFormatException e) {
         System.out.println(sNum + " is not numeric"); //not likely!
      }
      return str.substring(0, str.length() - 2) + DateUtils.toDD(iNum);
   } //incrementName

   private void makeIndex(XMLObject obj, File fil) {
      //Make an index page for the site as a whole. This will have links
      //to all the Section pages (such as Course Information).
      //NOTE This has the side effect of creating the formatted menu string,
      //strMenuText, nd course name, strCourse, which appear on all pages.
      String strWelcome = "";
      String strCSS;
      String strContentW = "";
      String strDisplay;
      String strName;
      boolean bHadContent = false; //So far
      XMLObject objNext = null;
      String eol = System.getProperty("line.separator");

      if (bTracing)
         System.out.println("BbSite.makeIndex: obj is " +
                            obj.getAttr("display"));

      //Create the index (welcome) page with the course title in it:
      strWelcome = getTemplate(INDEX_TEMPLATE);
      strCourse = obj.getAttr("display"); //used everywhere
      strWelcome =
            insertIntoPage(strCourse, strWelcome, COURSE); //used 3 times in template
      strWelcome = insertIntoPage(strCourse, strWelcome, COURSE);
      strWelcome = insertIntoPage(strCourse, strWelcome, COURSE);

      //Copy over the CSS and the image folder:
      strCSS = getTemplate(CSS_TEMPLATE);
      writePage(CSS_TEMPLATE, strCSS, fil);
      copyImages(SITE_IMAGE_BASE, fil, "images");

      if (obj.getAttr("content", UNKNOWN).equals("Text")) {
         strContentW = DescriptorType.fetchDescriptor(obj,true) + "<BR><BR>";
         strWelcome = insertIntoPage(strContentW, strWelcome, DESCR);
         bHadContent = true;
      }
      //Create the menu text with the links for the sections:
      //(All pages will use this same menu text.)
      objNext = obj.getChildObject();
      while (objNext != null) {
         if (!objNext.getAttr("extract").equals("true")) {
            objNext = objNext.getNextObject();
            continue;
         }
         if (objNext.getTag().equals("file")) {
            //Display it if it's an image, else display a link:
            strName = objNext.getAttr("name");
            if (glbl.isImage(objNext)) {
               strContentW = strContentW + "<BR><img src=\"" + strName +
                      "\"><BR><BR><a href=\""+strName+"\">"+strName+"</a><br>";
            } else {
               strContentW =
                     strContentW + "<a href=\"" + strName + "\">" + strName +
                     "</a><BR>";
            }
            //In either case, output the file:
            FileType.copyArchiveFileOut(strName, strName, fil,
                                        obj.getAttr("id"));
         } else {
            strDisplay = objNext.getAttr("display");
            strName = objNext.getAttr("name");
            if (objNext.getTag().equals("file")) {
               FileType.copyArchiveFileOut(strName, strDisplay, fil,
                                           objNext.getAttr("id"));
            }
            //Assemble the anchor, but provide alt text and truncate long titles:
            if (strDisplay.length() > 20)
               strDisplay = strDisplay.substring(0, 20) + "...";
            if (objNext.getTag().equals("file")) {
               strMenuText =
                     strMenuText + "<a href=\"" + strName + "\" class=\"menulink\" " +
                     "title=\"" + objNext.getAttr("display") + "\"" + ">" +
                     strDisplay + "</a>" + eol;
            } else {
               strMenuText =
                     strMenuText + "<a href=\"" + ((String)objectToName.get(objNext)) +
                     ".html\" class=\"menulink\" " + "title=\"" +
                     objNext.getAttr("display") + "\"" + ">" + strDisplay +
                     "</a>" + eol;
            }
            //e.g., <a href="Whatever_Page.html" title="Whatever Page" class="menulink">Whatever Page</a>
         }
         objNext = objNext.getNextObject();
      }
      if (!bHadContent) {
         //Remove the unused content placekeeper (if still there)!
         strWelcome = insertIntoPage(strContentW, strWelcome, DESCR);
      }
      //Every page has the menu:
      strWelcome = insertIntoPage(strMenuText, strWelcome, MENU);
      writePage(INDEX_TEMPLATE, strWelcome, fil);
   } //makeIndex

   private void makePage(XMLObject obj, File filDest, String strTrail) {
      //Make HTML pages for all the directories and items, and copy any
      //referenced files to the page's directory.
      String strNewTrail;
      String strMyTrail;
      String strContent = "";
      String strMyType = null;
      String strMyContent = null;
      File filFiles = null;
      XMLObject objNextChild = null;
      String strChildType = null;
      String strName = null;
      String strDisplay = null;
      String strPage = "";
      boolean bDidList = false; //true when a list of links is started
      String eol = System.getProperty("line.separator");

      if (obj == null)
         return;

      if (bTracing)
         System.out.println("BbSite.newmakePage: obj is " +
                            obj.getAttr("display"));
      filFiles =
            new File(filDest, ((String)objectToName.get(obj.getParentObject())));
      
      if (strTrail.equals(""))
         strNewTrail = obj.getAttr("display");
      else
         strNewTrail = strTrail + CRUMB_DELIM + obj.getAttr("display");

      strMyTrail = controlLength(strNewTrail);

      if (obj.getAttr("extract").equals("true")) {
         //Handle the current node first:
         strMyType = obj.getAttr("type");
         strName = obj.getAttr("name");
         strDisplay = obj.getAttr("display");
         if (strMyType.equals("File")) {
            //A link to this file has already been placed in the parent page,
            //so now we just copy the file out into that page's folder:
            if (obj.getTag().equals("entry")) {
               //It's a wiki entry. Copy to the simplified file name, adding .html:
               strDisplay = strDisplay + ".html";
               WikiType.copyArchiveWikiOut(obj, strName, strDisplay, filFiles,
                                           obj.getAttr("id"));
            } else {
               //Copy the file out to the parent page's folder:
               FileType.copyArchiveFileOut(strName, strName, filFiles,
                                           obj.getAttr("id"));
            }
         } else if (strMyType.equals("Text")) {
            //Ignored; already inserted into parent's page.
            
         } else if (strMyType.equals("Info")) {
            //Ignored; already inserted into parent's page.
            
         } else if (strMyType.equals("Link")) {
            //Ignored; already inserted into parent's page.
            
         } else if (strMyType.equals("URL")) {
            //Ignored; already inserted into parent's page.
                
         } else { //type is Course, Section, Folder (all dir="true")
            //Process this node's children, if any, including in this page
            //either links to the children, or the children's text:
            bDidList = false;

            //Folders can be Text, Info or Link:
            strMyContent = obj.getAttr("content", UNKNOWN);
            if (strMyContent.equals("Text")) {
               //Include the description on the current page:
               //(We know this child is first within a folder.)
               //strContent = DescriptorType.fetchDescriptor(obj,true) + "<br><br>";
               //strContent = resolvePageFileLinks(DescriptorType.fetchDescriptor(obj,false),strName) + "<br><br>"; //???
               strContent = resolvePageFileLinks(DescriptorType.fetchDescriptor(obj,false),(String)objectToName.get(obj)) + "<br><br>"; //???
                
            } else if (strMyContent.equals("Info")) {
               //Include the staff information on the current page:
               strContent = strContent + InfoType.fetchInfo(obj) + "<br><br>";
               
            } else if (strMyContent.equals("Link")) {
               //Include the URL of the link on the current page:
               strContent = strContent + LinkType.fetchURL(obj) + "<br><br>";
               
            } else if (strMyContent.equals("URL")) {
               //Include the URL of the link on the current page:
               strContent = strContent + LinkType.fetchTOCURL(obj,true) + "<br><br>"; //with anchor
            } else if (strMyContent.equals("Forum")) {
               //Include the text of this forum topic's threads on the current page:
               strContent = strContent + glbl.fixHTML(glbl.stripJunk(ForumType.fetchForum(obj,false,true))) + "<br><br>";
            } else if (strMyContent.equals("Archive")) {
               //Include the text of this forum topic's threads on the current page:
               strContent = strContent + glbl.fixHTML(glbl.stripJunk(ForumType.fetchForum(obj,false,false))) + "<br><br>";
            } else if (strMyContent.equals("Announce")) {
               //Include the text of this forum topic's threads on the current page:
               strContent = strContent + glbl.fixHTML(glbl.stripJunk(AnnounceType.fetchAnnouncement(obj))) + "<br><br>";
            }
            //There can be children attached, so we might need a list of links:
            objNextChild = obj.getChildObject();
            while ((objNextChild != null) &&
                   objNextChild.getAttr("extract").equals("true")) {
               strChildType = objNextChild.getAttr("type");
               if (strChildType.equals("Link")) {
                  //Include the URL of the link on the current page:
                  strContent =
                        strContent + glbl.fetchLink(objNextChild) + "<br><br>";
               } else if (strChildType.equals("File")) {
                  //Include a link to the file in this page:
                  if (!bDidList) {
                     strContent = strContent + "<ul class=\"bullet\">" + eol;
                     bDidList = true;
                  }
                  strDisplay = objNextChild.getAttr("display");
                  strName = objNextChild.getAttr("name");
                  if (objNextChild.getTag().equals("entry")) {
                     //It's a wiki entry:
                     strContent =
                           strContent + "<li><a href=\"" + ((String)objectToName.get(obj)) +
                           "/" + strDisplay + ".html\">" + strDisplay +
                           "</a><BR>";
                  } else {
                     if (strName.startsWith("embedded"))
                        strName = strName.substring(9); //incl / or \
                     if (strName.startsWith("loi-teams/"))
                        strName = strName.substring(10);

                     strContent =
                           strContent + "<li><a href=\"" + ((String)objectToName.get(obj)) +
                           "/" + strName + "\">" + strDisplay + "</a><BR>";
                  }
               } else if (strChildType.equals("Forum") || 
                           strChildType.equals("Announce") || 
                           strChildType.equals("Archive")) {
                  //Include a link to the about-to-be-created page:
                   if (!bDidList) {
                      strContent = strContent + "<ul class=\"bullet\">" + eol;
                      bDidList = true;
                   }
                   strDisplay = objNextChild.getAttr("display");
                   if (objNextChild.getAttr("dir").equals("true"))
                      strName = (String)objectToName.get(objNextChild);
                   else
                      strName = objNextChild.getAttr("name");
                   strContent =
                         strContent + "<li><a href=\"" + strName +
                         ".html" + "\">" + strDisplay + "</a><BR>";
                  
               } else { //type is Course, Section, Folder (all dir="true")
                  //Include a link to the subfolder on the current page:
                  if (!bDidList) {
                     strContent = strContent + "<ul class=\"bullet\">" + eol;
                     bDidList = true;
                  }
                  strName = objNextChild.getAttr("name");
                  strContent =
                        strContent + "<li><a href=\"" + ((String)objectToName.get(objNextChild)) +
                        ".html" + "\">" + strName + "</a><BR>";
               }
               //Look at the next child of the current node:
               objNextChild = objNextChild.getNextObject();
            }
            if (bDidList)
               strContent = strContent + "</ul>";
         }
         //Output the page for the current node:
         strPage = insertIntoPage(strCourse, strBasicPage, COURSE);
         strPage = insertIntoPage(strCourse, strPage, COURSE);
         strPage = insertIntoPage(strMenuText, strPage, MENU);
         strPage = insertIntoPage(strMyTrail, strPage, TRAIL);
         strPage = insertIntoPage(strContent, strPage, DESCR);
         //strPage = insertIntoPage(strList, strPage, LINKS); //can do links separately
         strPage = insertIntoPage("", strPage, LINKS);
         //strPage = insertIntoPage(strTitle, strPage, HEAD); //for later???

         //Return to parent (if any) or to welcome:
         boolean bHasParent =
            !(obj.getParentObject().getAttr("type").equals("Course"));
         if (bHasParent) {
            String strParent = obj.getParentObject().getAttr("display");
            if (strParent.length() > 20)
               strParent = strParent.substring(0, 20) + "...";
            strPage = insertIntoPage(strParent, strPage, BACK);
            strPage =
                  insertIntoPage(obj.getParentObject().getAttr("display"), strPage,
                                 TITLE);
            strPage =
                  insertIntoPage(objectToName.get(obj.getParentObject()) + ".html",
                                 strPage, BACKURL);
         } else {
            strPage = insertIntoPage("Course Description", strPage, BACK);
            strPage = insertIntoPage("Course Description", strPage, TITLE);
            strPage = insertIntoPage("index.html", strPage, BACKURL);
         }
         if (obj.getAttr("content",UNKNOWN).equals("Forum")) {
            if (obj.getAttr("dir").equals("true"))
               writePage(objectToName.get(obj) + ".html", strPage,filDest);
            else
               writePage(obj.getAttr("name") + ".html", strPage,filDest);
         } else if (obj.getAttr("content",UNKNOWN).equals("Archive")) {
            writePage(obj.getAttr("name") + ".html", strPage,filDest);
         } else if (obj.getAttr("content",UNKNOWN).equals("Announce")) {
            writePage(obj.getAttr("name") + ".html", strPage,filDest);
         } else {
            String strToGo = (String)objectToName.get(obj);
            if (strToGo != null)
               writePage(strToGo + ".html", strPage,filDest);
         }
         //Handle the pages for the children of this node:
         makePage(obj.getChildObject(), filDest, strNewTrail);
      }
      //Handle the siblings of this node:
      makePage(obj.getNextObject(), filDest, strTrail);

   } //makePage
   
   public String resolvePageFileLinks(String strText, String strPageName) {
      //Change Bb's weird embedded-file indicator to use the page's folder name.
      int loc = strText.indexOf(EMBEDDED_FILE_PREFIX);
      String pre = "";
      String post = "";
      
      while (loc > 0) {
         pre = strText.substring(0, loc);
         post = strText.substring(loc + (EMBEDDED_FILE_PREFIX.length()));
         strText = pre + strPageName + "/" + post;
         loc = strText.indexOf(EMBEDDED_FILE_PREFIX);
      }
      return strText;
   } //resolvePageFileLinks

   private static String controlLength(String str) {
      //Keep the trail (path or breadcrumb) shorter than a declared limit,
      //using an ellipsis at the beginning.
      String s = new String(str);
      int loc;

      if (s.length() > CRUMB_SIZE) {
         loc = s.indexOf(CRUMB_DELIM);
         while ((s.length() > CRUMB_SIZE) && (loc > 0)) {
            s = s.substring(loc + CRUMB_DELIM.length());
            loc = s.indexOf(CRUMB_DELIM);
         }
         s = CRUMB_ETC + CRUMB_DELIM + s; //currently "..."+" -> "
         //Unfortunately, it still might be too long:
         if (s.length() > CRUMB_SIZE)
            s = "... " + s.substring(s.length() - CRUMB_SIZE);
      }
      return s;
   } //controlLength

   private void writePage(String strName, String strContent, File fil) {
      //Write the modified page template out, into a new file.
      File filOut = null;
      PrintStream outs = null;
            
      //Create and write the file:
      try {
         filOut = new File(fil, strName);
         filOut.createNewFile();
         outs = new PrintStream(new FileOutputStream(filOut));
         outs.print(strContent);
      } catch (IOException e) {
         Unzipper.ref.setMessage("BbSite.writePage: " + e, ERROR_MSG, true);
      } finally {
         if (outs != null)
            outs.close();
      }

   } //writepage

   private String insertIntoPage(String strContent, String strPage,
                                 String strPattern) {
      //Insert the given content into the given (template) page in place of the pattern.
      StringBuffer buf =
         new StringBuffer(strPage.length() + strContent.length());
      int loc = strPage.indexOf(strPattern);

      if (loc >= 0) {
         buf.append(strPage.substring(0, loc));
         buf.append(strContent);
         buf.append(strPage.substring(loc + strPattern.length()));
      }
      return buf.toString();
   } //insertIntoPage

   private String getTemplate(String strName) {
      //Read and return the named template from the current template source.
      String strPage = "";

      if (bDefaultTemplates) {
         strPage = Unzipper.ref.getResourceText(strName);
      } else { //in version 2
         strPage = getTextFile(strName);
         if (strPage == null) {
            //File missing or empty; use resource default instead:
            strPage = Unzipper.ref.getResourceText(strName);
            Unzipper.ref.setMessage("Could not read \"" + strName +
                                    "\" from the templates folder.", ERROR_MSG,
                                    true);
         }
      }
      return strPage;
   } //getTemplate

   private String getTextFile(String strName) { //for version 2
      //Read and return the named template file from the templates folder.
      BufferedReader ins = null;
      StringBuffer buf = new StringBuffer(4096);
      ;
      File filIn = new File(filTemplates, strName);
      String strLine;
      String eol = System.getProperty("line.separator");
      boolean bOKSoFar = true;

      try {
         ins = new BufferedReader(new FileReader(filIn));

         strLine = ins.readLine();
         while (strLine != null) {
            buf.append(strLine + eol);
            strLine = ins.readLine();
         }
      } catch (IOException e) {
         bOKSoFar = false;
      } finally {
         try {
            if (ins != null)
               ins.close();
         } catch (IOException e) {
         }
      }
      if (buf.length() == 0)
         bOKSoFar = false;
      if (bOKSoFar)
         return buf.toString();
      //else
      return null;
   } //getTextFile

   private void copyImages(String strFrom, File filToBase, String strTo) {
      //Copy the named folder and all its images into a new folder
      //with the given name.
      File filTo = new File(filToBase, strTo);

      filTo.mkdirs();

      copyResImage(strFrom + "button.gif", filTo, "button.gif");
      copyResImage(strFrom + "buttonover.gif", filTo, "buttonover.gif");
      copyResImage(strFrom + "cbox_botbg.gif", filTo, "cbox_botbg.gif");
      copyResImage(strFrom + "cbox_lbot.gif", filTo, "cbox_lbot.gif");
      copyResImage(strFrom + "cbox_rbot.gif", filTo, "cbox_rbot.gif");
      copyResImage(strFrom + "circlebullet.gif", filTo, "circlebullet.gif");
      copyResImage(strFrom + "clearspacer.gif", filTo, "clearspacer.gif");
      copyResImage(strFrom + "contable_lcorner.gif", filTo,
                   "contable_lcorner.gif");
      copyResImage(strFrom + "contable_rcorner.gif", filTo,
                   "contable_rcorner.gif");
      copyResImage(strFrom + "contenttable_bkgtile.gif", filTo,
                   "contenttable_bkgtile.gif");
      copyResImage(strFrom + "leftbkg.gif", filTo, "leftbkg.gif");
      copyResImage(strFrom + "leftbkgtable.gif", filTo, "leftbkgtable.gif");
      copyResImage(strFrom + "topbkg.gif", filTo, "topbkg.gif");

   } //copyImages

   public void copyResImage(String strResName, File filBase, String strName) {
      //Copy an image (for the Site) from the resources to the site folder.
      File filWrite = null;
      filWrite = new File(filBase, strName);

      if (clResources == null)
         clResources = Unzipper.ref.getClass().getClassLoader();

      try {
         BufferedInputStream ins =
            new BufferedInputStream(clResources.getResourceAsStream(strResName));
         BufferedOutputStream outs =
            new BufferedOutputStream(new FileOutputStream(filWrite));

         byte[] buf = new byte[8192];
         int count = 0;

         count = ins.read(buf, 0, 8192);
         while (count >= 0) {
            outs.write(buf, 0, count);
            count = ins.read(buf, 0, 8192);
         }
         ins.close();
         outs.close();
      } catch (IOException e) {
         //Do something useful here.
         Unzipper.ref.setMessage("Unzipper.copyImage: IOException for resource \"" +
                                 strResName + "\"", ERROR_MSG, true);
      }
   } //copyResImage

}//BbFlatSite
