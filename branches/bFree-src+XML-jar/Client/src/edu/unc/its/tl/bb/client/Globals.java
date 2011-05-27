package edu.unc.its.tl.bb.client;

import edu.unc.its.tl.bb.JTreeTable;

import java.io.BufferedReader;
import java.io.File;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;

import javax.swing.tree.TreePath;

import org.dvm.java.xml.XMLAccessor;
import org.dvm.java.xml.XMLException;
import org.dvm.java.xml.XMLObject;

public class Globals implements BbConstants {
   //Globals used in most classes; a singleton.

   private static Globals refToMe = null;

   //The incremental search object's last-found path:
   public TreePath lastPath =
      null; //so we can close it up when we do a "close/next" find
   public boolean bWasRoot = false;
   public int selStart = -1; //location of found text string
   public int selEnd = -1;

   //Course name (not always known), and path to the temp in user.dir:
   public String strCourseName = "unknown";
   public File filTempPath = null; //default or user's choice
   public boolean bNoTempYet = true;

   //Accessors to the main parts of the currently parsed manifest:
   public XMLAccessor accManifest = null; //the entire manifest
   public XMLAccessor accItem = null; //the item hierarchy
   public XMLAccessor accRes = null; //the resource list

   public int archiveType = UNKNOWN_ARCHIVE;

   //The object for the "display tree", an XMLObject containing
   //the information we need to locate, name, and output the course
   //contents:
   public boolean bHadErrors = false;
   public XMLObject selectedObj = null; //selected content object, if any

   //Dummy placeholder for the JTreeTable:
   public XMLObject objDummy = null;

   public JTreeTable tabExport = null;

   //New:
   public XMLObject objCourse = null;

   private Globals() {
   }

   public static Globals getInstance() {
      if (refToMe == null)
         refToMe = new Globals();
      return refToMe;
   } //getInstance

   public String getArchiveFileText(String strToFind) {
      //Return the text of the indicated XML file from within the temp archive folder.
      String strXML = "";
      File filText = new File(filTempPath, strToFind);
      BufferedReader inb = null;
      StringBuffer buf = null;
      String line;

      try {
         inb = new BufferedReader(new InputStreamReader(new FileInputStream(filText)));
         buf = new StringBuffer(8192);

         line = inb.readLine();
         while (line != null) {
            buf.append(line);
            buf.append("\n");
            line = inb.readLine();
         }
         strXML = buf.toString();
         inb.close();
      } catch (FileNotFoundException e) {
         if (!strToFind.equals("res00000.dat") &&
             !strToFind.equals("res99999.dat"))
            Unzipper.ref.setMessage("\"" + strToFind +
                                    "\" was not in the export zip!",
                                    WARNING_MSG, false);
      } catch (IOException e) {
         Unzipper.ref.setMessage("IOException: \"" + strToFind +
                                 "\" could not be read from the export zip!",
                                 ERROR_MSG, true);
      } finally {
         try {
            if (inb != null)
               inb.close();
         } catch (IOException e) {
         }
      }
      return strXML; //the XML text
   } //getArchiveFileText

   public String getArchiveFile(String strFindName, String strID) {
      //Retrieve and return the text of the named file out of the temp folder.
      BufferedReader ins = null;
      String line = "";
      StringBuffer buf = new StringBuffer(4096);

      strFindName = strFindName.replace('\\', '/'); //NOTE necessary???

      try {
         ins =
new BufferedReader(new FileReader(new File(filTempPath, "/" + strID + "/" +
                                           strFindName)));
         line = ins.readLine();
         while (line != null) {
            buf.append(line);
            buf.append("\n");
            line = ins.readLine();
         }
         ins.close();
      } catch (FileNotFoundException e) {
         Unzipper.ref.setMessage("\"" + strID + "/" + strFindName +
                                 "\" is not in the archive.", ERROR_MSG,
                                 false);
      } catch (IOException e) {
         Unzipper.ref.setMessage("Could not read \"" + strID + "/" +
                                 strFindName + "\" from the archive.",
                                 ERROR_MSG, true);
      } finally {
         try {
            if (ins != null)
               ins.close();
         } catch (IOException e) {
            //
         }
      }
      return buf.toString();
   } //getArchiveFile

   public String fetchLink(XMLObject obj) {
      //Return the item's URL, if any.
      String strLink = "Can't find the link.";
      XMLAccessor accItem = null;
      String itemXML = null;
      String eol = System.getProperty("line.separator");

      itemXML = getArchiveFileText(obj.getAttr("id") + ".dat");
      if ((itemXML != null) && (itemXML.length() > 0)) {
         accItem = getAccessor(itemXML);

         strLink = accItem.getAttr("content/url", "value", UNKNOWN);
         if (!strLink.equals(UNKNOWN) && !strLink.equals("http://"))
            strLink =
                  "<a href=\"" + strLink + "\">" + strLink + "</a><br>" + eol;
      }
      return strLink + eol;
   } //fetchLink

   public String rerouteFileLinks(String strText) {
      //Change the weird Bb embedded-file indicator to our output "files" folder.
      int loc = strText.indexOf(EMBEDDED_FILE_PREFIX);
      String pre = "";
      String post = "";

      while (loc > 0) {
         pre = strText.substring(0, loc);
         post = strText.substring(loc + (EMBEDDED_FILE_PREFIX.length()));
         strText = pre + "files/" + post;
         loc = strText.indexOf(EMBEDDED_FILE_PREFIX);
      }
      return strText;
   } //rerouteFileLinks

   public String fixName(String s) {
      //Get rid of all non-valid characters in the given file name.
      //NOTE: This does not address the issue of special HTML entities,
      //such as "&amp;".

      if ((s == null) || (s.length() == 0))
         return "";

      String newS = s.replace(' ', '_');

      int loc = newS.indexOf("%20");
      if (loc >= 0) {
         StringBuffer buf = new StringBuffer(newS.length());
         String strRest = new String(newS);

         while (loc >= 0) {
            buf.append(strRest.substring(0, loc));
            buf.append("_");
            strRest = strRest.substring(loc + 3);
            loc = strRest.indexOf("%20");
         }
         if (strRest.length() > 0)
            buf.append(strRest);
         newS = buf.toString();
      }

      newS = newS.replace('\\', '_');
      newS = newS.replace('/', '_');
      newS = newS.replace('|', '_');
      //newS = newS.replace('.', '_');//Don't! Ruins ".txt", etc.
      newS = newS.replace('*', '_');
      newS = newS.replace(':', '_');
      newS = newS.replace(';', '_');
      newS = newS.replace('<', '_');
      newS = newS.replace('>', '_');
      newS = newS.replace('?', '_');
      newS = newS.replace('%', '_');
      newS = newS.replace('#', '_');
      newS = newS.replace('$', '_');
      newS = newS.replace('&', '+');
      newS = newS.replace(',', '_');
      newS = newS.replace('"', '_');
      newS = newS.replace('\t', '_');
      newS = newS.replace('\n', '_');
      if (newS.startsWith("."))
         newS = "_" + newS.substring(1);
      if (newS.endsWith(".")) //on Windows
         newS = newS.substring(0,newS.length()-1)+"_";
      return newS;
   } //fixName

   public String stripJunk(String str) {
      //Strip Bb's weird "&#xd;" (etc.) line-end indicators.
      //Convert wierd stuff to EOL.
      String eol = System.getProperty("line.separator");
      int loc = 0;
      StringBuffer buf = null;
      String strRest = new String(str);

      strRest = strRest.replace((char)0x94, '"');
      strRest = strRest.replace((char)0x93, '"');
      strRest = strRest.replace((char)0x91, '\'');
      strRest = strRest.replace((char)0x92, '\'');
      /*
      strRest = strRest.replace('\u201C','"');
      strRest = strRest.replace('\u201D','"');
      strRest = strRest.replace('\u2018','\'');
      strRest = strRest.replace('\u2019','\'');

      strRest = strRest.replace('\u0094','"');
      strRest = strRest.replace('\u0093','"');
      strRest = strRest.replace('\u0091','\'');
      strRest = strRest.replace('\u0092','\'');
      */
      loc = strRest.indexOf("&#xd;");
      buf = new StringBuffer(str.length() + 1); //could be empty
      while (loc >= 0) {
         buf.append(strRest.substring(0, loc));
         buf.append(eol);
         strRest = strRest.substring(loc + 5);
         loc = strRest.indexOf("&#xd;");
      }
      if (strRest.length() > 0)
         buf.append(strRest);

      strRest = buf.toString();
      loc = strRest.indexOf("&#xa;");
      buf = new StringBuffer(strRest.length() + 1); //could be empty
      while (loc >= 0) {
         buf.append(strRest.substring(0, loc));
         strRest = strRest.substring(loc + 5);
         loc = strRest.indexOf("&#xa;");
      }
      if (strRest.length() > 0)
         buf.append(strRest);

      strRest = buf.toString();
      loc = strRest.indexOf(EMBEDDED_STUB_PREFIX);
      buf = new StringBuffer(strRest.length() + 1); //could be empty
      while (loc >= 0) {
         buf.append(strRest.substring(0, loc));
         buf.append("unknown");
         strRest = strRest.substring(loc + EMBEDDED_STUB_PREFIX.length());
         loc = strRest.indexOf(EMBEDDED_STUB_PREFIX);
      }
      if (strRest.length() > 0)
         buf.append(strRest);

      return buf.toString();
   } //stripJunk

   public String fixHTML(String str) {
      //Change "&lt;" to < (for example).

      if (str.length() == 0)
         return str;

      int loc = 0;
      int len = 0;
      StringBuffer buf = null;
      String strRest = new String(str);
      //NOTE: HTML coding must be UTF-8 to handle curly quotes.

      loc = strRest.indexOf("View&lt;/a&gt;");
      if (loc > -1)
         strRest = strRest.substring(loc + 14);

      strRest = strRest.replace((char)0x94, '"');
      strRest = strRest.replace((char)0x93, '"');
      strRest = strRest.replace((char)0x91, '\'');
      strRest = strRest.replace((char)0x92, '\'');
      /*
      strRest = strRest.replace('\u201C','"');
      strRest = strRest.replace('\u201D','"');
      strRest = strRest.replace('\u2018','\'');
      strRest = strRest.replace('\u2019','\'');

      strRest = strRest.replace('\u0094','"');
      strRest = strRest.replace('\u0093','"');
      strRest = strRest.replace('\u0091','\'');
      strRest = strRest.replace('\u0092','\'');
      */
      loc = strRest.indexOf("&lt;");
      buf = new StringBuffer(strRest.length() + 1); //could be empty
      while (loc >= 0) {
         buf.append(strRest.substring(0, loc));
         buf.append("<");
         strRest = strRest.substring(loc + 4);
         loc = strRest.indexOf("&lt;");
      }
      if (strRest.length() > 0)
         buf.append(strRest);
      strRest = new String(buf.toString());

      loc = strRest.indexOf("&amp;");
      buf = new StringBuffer(strRest.length() + 1); //could be empty
      while (loc >= 0) {
         buf.append(strRest.substring(0, loc));
         buf.append("&"); //NOTE site output needs &amp; back again!
         strRest = strRest.substring(loc + 5); //len of &amp;
         loc = strRest.indexOf("&amp;");
      }
      if (strRest.length() > 0)
         buf.append(strRest);
      strRest = new String(buf.toString());

      loc = strRest.indexOf("&#");
      len = strRest.indexOf(";") - loc + 1;
      buf = new StringBuffer(strRest.length() + 1); //could be empty
      while ((loc >= 0) && ((len == 5) || (len == 6))) {
         buf.append(strRest.substring(0, loc));
         if (len == 5) {
            buf.append(getCharForDec(strRest.substring(loc, loc + 5)));
            strRest = strRest.substring(loc + 5); //len of &#dd;
         } else if (len == 6) { //6, I assume
            buf.append(getCharForDec(strRest.substring(loc, loc + 6)));
            strRest = strRest.substring(loc + 6); //len of &#ddd;
         }
         loc = strRest.indexOf("&#");
      }
      if (strRest.length() > 0)
         buf.append(strRest);
      strRest = new String(buf.toString());

      loc = strRest.indexOf("&quot;");
      buf = new StringBuffer(strRest.length() + 1); //could be empty
      while (loc >= 0) {
         buf.append(strRest.substring(0, loc));
         buf.append("\"");
         strRest = strRest.substring(loc + 6); //len of &quot;
         loc = strRest.indexOf("&quot;");
      }
      if (strRest.length() > 0)
         buf.append(strRest);
      strRest = new String(buf.toString());

      loc = strRest.indexOf("&nbsp;");
      buf = new StringBuffer(strRest.length() + 1); //could be empty
      while (loc >= 0) {
         buf.append(strRest.substring(0, loc));
         buf.append(" ");
         strRest = strRest.substring(loc + 6); //len of &quot;
         loc = strRest.indexOf("&nbsp;");
      }
      if (strRest.length() > 0)
         buf.append(strRest);
      strRest = new String(buf.toString());

      loc = strRest.indexOf("&gt;");
      buf = new StringBuffer(strRest.length() + 1); //could be empty
      while (loc >= 0) {
         buf.append(strRest.substring(0, loc));
         buf.append(">");
         strRest = strRest.substring(loc + 4);
         loc = strRest.indexOf("&gt;");
      }
      if (strRest.length() > 0)
         buf.append(strRest);

      return buf.toString();
   } //fixHTML

   public String unhexPath(String strPath) {
      //Convert a hex name contained in a file path from hex to ASCII.
      //If bHasAtSign, this processes the string IFF there is an @ or !.
      //(And the string might be refused.)
      //If the path is coming from a newer version of the wiki tool,
      //attached files can be in a path containng "/attach/" followed by
      //the file name in hex.
      String strPrefix = "";
      String strName = "";
      int loc = -1;

      if (strPath.contains("!") || strPath.contains("@")) { //Bb
         //Convert just the name part:
         loc = strPath.lastIndexOf("/"); //Note must be "/" not "\"
         if (loc >= 0) {
            strPrefix = strPath.substring(0, loc + 1);
            strName = strPath.substring(loc + 1);
         } else {
            strName = strPath;
         }
         if (strName.startsWith("!") || strName.startsWith("@")) {
            strName = unhexName(strName,true); //this will handle any extension
         }
      /*} // NOTE Have to skip this for now; can't get actual names
        else if (strPath.contains("/attach/")) {
         loc = strPath.indexOf("attach/"); //Note must be "/" not "\"
          if (loc >= 0) {
             strPrefix = strPath.substring(0, loc + 7);
             strName = strPath.substring(loc + 7);
          } 
          strName = unhexName(strName,false); */
      } else
         strName = strPath;

      return strPrefix + strName;
   } //unhexPath

   public String unhexName(String strName, boolean bHasAtSign) {
      //IFF bHasAtSign, convert a name in the format "!" or "@" + hexstring 
      //into an ASCII string.
      //Otherwise, convert the name as given (no @ or !).
      //Leaves the extension as-is.
      StringBuffer buf = new StringBuffer(strName.length());
      String strTemp = "";
      String strExt = "";
      int loc = 0;
      String hex;

      if (bHasAtSign)
         strTemp = strName.substring(1);//skip @ or !
      else
         strTemp = strName;
         
      loc = strTemp.lastIndexOf(".");

      if ((loc >= 0) && (loc < strTemp.length())) {
         strExt = strTemp.substring(loc);
         strTemp = strTemp.substring(0, loc);
      }
      for (int i = 0; i < strTemp.length(); i = i + 2) {
         hex = strTemp.substring(i, i + 2);
         buf.append((char)Integer.parseInt(hex, 16));
      }
      return fixName(buf.toString()) + strExt;
   } //unhexName

   private String getCharForDec(String str) {
      int iVal;
      char cVal;

      if ((str.length() != 5) && (str.length() != 6))
         return str;
      try {
         if (str.length() == 5)
            iVal = Integer.parseInt(str.substring(2, 4));
         else
            iVal = Integer.parseInt(str.substring(2, 5));
         cVal = (char)iVal;
      } catch (NumberFormatException e) {
         cVal = '?';
      }
      return Character.toString(cVal);
   } //getCharForDec

   public String dropBadLink(String str) {
      //Drop the externally meaningless assignment link in the text:
      int loc = str.indexOf(BB_TO_SKIP);

      if (loc < 0)
         loc = str.indexOf(BB_TO_SKIP_TOO);

      if (loc >= 0) //0???
         str = str.substring(0, loc) + " [Unresolved link removed.]";
      return str;
   } //dropBadLink

   public XMLAccessor getAccessor(String strXML) {
      //Return an XMLAccessor to handle the XMLObject parsed
      //from the given XML text.
      XMLAccessor accObj = null;
      try {
         accObj = new XMLAccessor();
         accObj.setXMLText(strXML);
      } catch (XMLException e) {
         accObj = null;
         Unzipper.ref.setMessage(e.error, ERROR_MSG, true);
      }
      return accObj;
   } //getAccessor

   public int myIndexOf(String subj, String goal1, String goal2) {
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

   public boolean isImage(XMLObject obj) {
      String name = obj.getAttr("name");
      if (name.endsWith(".gif") || name.endsWith(".jpg") ||
          name.endsWith(".jpeg") || name.endsWith(".GIF") ||
          name.endsWith(".JPG") || name.endsWith(".JPEG") ||
          name.endsWith(".png") || name.endsWith(".PNG") ||
          name.endsWith(".tiff") || name.endsWith(".TIFF"))
         return true;
      //else
      return false;
   } //isImage

}//Globals
