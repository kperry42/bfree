package edu.unc.its.tl.bb.client;

import java.io.*;

import java.util.Enumeration;
import java.util.Properties;
import java.util.Vector;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.dvm.java.xml.*;

public final class BbOut implements BbConstants {
   //Output methods for the bFree application. These methods output
   //course contents as folders/files in the user's file system.

   private Globals glbl = Globals.getInstance();

   public BbOut() {
      glbl = Globals.getInstance();
   } //constructor

   public void doOutput(XMLObject obj, File fil, Vector vTitles) {
      //Recursively create the directories and files for the content tree.
      //It copies content from the temp folder, when indictated by the
      //content tree attributes.
      //Note that name collisions are handled differently in file-folder
      //output versus site output. The site is "flat", so names collide
      //much more frequently, whereas here the collisions can only be
      //among siblings.
      File newBase = null;
      String strName = null;
      String strDisplay;
      String strType = null;
      String strContent = null;
      String strID = null;
      XMLAccessor accItem = null;
      String itemXML = null;
      String strText = null;
      int loc;

      if (obj == null)
         return;

      if (obj.getAttr("extract").equals("true")) {
         //Handle the current item first:
         strID = obj.getAttr("id");
         if (obj.getAttr("dir").equals("true")) {
            //Create this directory:
            strDisplay = glbl.fixName(obj.getAttr("display"));
            strDisplay = disambiguate(strDisplay, vTitles);
            vTitles.addElement(strDisplay); //for checking siblings later
            newBase =
                  new File(fil, strDisplay); //file hierarchy getting deeper
            newBase.mkdirs();

            //A directory can have its own content:
            //Other content is contained within the the resource files
            //which are XML and need to be parsed:
            strContent = obj.getAttr("content", UNKNOWN);
            itemXML = glbl.getArchiveFileText(strID + ".dat");
            if ((itemXML != null) && (itemXML.length() > 0)) {
               accItem = glbl.getAccessor(itemXML);

               vTitles.addElement(strDisplay); //for checking siblings later
               if (strContent.equals("Text")) {
                  strText = DescriptorType.fetchDescriptor(accItem,true);//send to files folder
                  if (strText.length() > 0)
                     //writeTextOut("Description.txt", strText, newBase);
                     writeTextOut(strDisplay+".txt", strText, newBase);
               } else if (strContent.equals("Link")) {
                  strText = LinkType.fetchURL(accItem);
                  if (strText.length() > 0)
                     writeTextOut("Link.html", strText, newBase);
               } else if (strContent.equals("Info")) {
                  strText = InfoType.fetchInfo(accItem);
                  if (strText.length() > 0)
                     writeTextOut("Contact.txt", strText, newBase);
               } else if (strContent.equals("URL")) {
                  strText = LinkType.fetchTOCURL(obj,false); //plain, no anchor
                  if (strText.length() > 0)
                     writeTextOut("URL.txt", strText, newBase);
               } else if (strContent.equals("Forum")) {//and a directory
                  strText = ForumType.fetchForum(obj,false,true);
                  if (strText.length() > 0)
                     //writeTextOut(obj.getAttr("display")+".txt", strText, newBase);
                     writeTextOut(strDisplay+".txt", strText, newBase);
               } else if (strContent.equals("Section")) {
                  //Do nothing; it's a section
               } else {
                  //System.out.println("BbOut.doOutput: content type=" +strContent+" obj="+obj.getAttr("display"));//TEMP
                  //Unzipper.ref.setMessage("BbOut.doOutput: unknown content type=" +
                  //                        strContent, WARNING_MSG, false);
                  //do nothing
               }
            }
            //Directories can have children:
            doOutput(obj.getChildObject(), newBase, new Vector());
         } else {
            //This is actual data:
            strType = obj.getAttr("type");

            if (strType.equals("File")) {
               //Files are stored individually within the zip.
               strName = obj.getAttr("name");
               //strName = glbl.fixName(obj.getAttr("name")); //NO
               strDisplay =
                     obj.getAttr("display"); //wiki entries have generated display titles
               strName = strName.replace('\\', '/');
               loc = strName.lastIndexOf("/");
               if (loc >= 0)
                  strName = strName.substring(0,loc+1)+glbl.fixName(strName.substring(loc+1));
               else
                  strName = glbl.fixName(strName);
               //FileType.copyArchiveFileOut(strName, strDisplay, fil, strID);
               FileType.copyArchiveFileOut(strName, glbl.fixName(strDisplay), fil, strID);//NEW
            } else if (strType.equals("Forum")) {
               strText = ForumType.fetchForum(obj,true,true); //plain, no anchor, no head
               if (strText.length() > 0)
                  writeTextOut(obj.getAttr("name")+".txt", strText, fil);
            } else if (strType.equals("Archive")) {
               strText = ForumType.fetchForum(obj,true,false); //plain, no anchor, no head
               if (strText.length() > 0)
                  writeTextOut(obj.getAttr("name")+".txt", strText, fil);
            } else if (strType.equals("Announce")) {
               strText = glbl.stripJunk(AnnounceType.fetchAnnouncement(obj));
               if (strText.length() > 0)
                  writeTextOut(obj.getAttr("name")+".txt", strText, fil);
            } else if (strType.equals("Link")) {
               strText = LinkType.fetchURL(obj);
               if (strText.length() > 0)
                  writeTextOut("Link.html", strText, fil);
            }
         }
      }
      //Either a folder or a file can have siblings:
      doOutput(obj.getNextObject(), fil, vTitles); //in same folder
   } //doOutput

   private static String disambiguate(String title, Vector vTitles) {
      //If the name already exists in vTitles, increment it; when unique
      //return it.
      String strNew = title;
      String strNum = "01";
      int iNum;

      if (vTitles.contains(strNew)) {
         while (vTitles.contains(strNew + "_" + strNum)) {
            try {
               iNum = Integer.parseInt(strNum) + 1;
               strNum = DateUtils.toDD(iNum);
            } catch (NumberFormatException e) {
            }
         }
         strNew = strNew + "_" + strNum;
      }
      return strNew;
   } //disambiguate
   
   public void writeTextOut(String strTitle, String strText, File filPath) {
      //Write out the given text as a file having the given name.
      //The "#xd" elements have been changed to new line characters.
      //The file is written to the folder bearing the content item's name.
      PrintStream outs = null;
      File filOut = null;

      //Write the file:
      try {
         filOut = new File(filPath, strTitle);
         filOut.createNewFile();
         outs = new PrintStream(new FileOutputStream(filOut));
         outs.print(strText);
      } catch (IOException e) {
         Unzipper.ref.setMessage("BbOut.writeTextOut: "+e+" "+filOut.getPath(), ERROR_MSG, true);
      } finally {
         outs.close();
      }
   } //writeTextOut

   public final String getCourseDirectory(XMLObject obj) {
      //Return the textusl outline of this course.
      return getCourseDirectory(obj, "");
   } //getCourseDirectory

   protected final String getCourseDirectory(XMLObject obj, String indent) {
      //Return the XML representation of this object and all its children.
      StringBuffer buf = new StringBuffer(4096);
      String title;
      String type;
      String strSep = System.getProperty("line.separator");

      if (obj == null)
         return "";

      title = obj.getAttr("display");
      type = obj.getAttr("type");

      buf.append(indent);
      buf.append(type + ": ");
      buf.append(title);
      buf.append(strSep);

      buf.append(getCourseDirectory(obj.getChildObject(), indent + "   "));

      buf.append(getCourseDirectory(obj.getNextObject(), indent));

      return buf.toString();
   } //getCourseDirectory

}//BbOut
