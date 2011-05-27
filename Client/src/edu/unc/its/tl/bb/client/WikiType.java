package edu.unc.its.tl.bb.client;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;

import java.io.StringReader;

import org.dvm.java.xml.XMLObject;

public class WikiType implements BbConstants {
//Methods for Wiki type content.

   private static Globals glbl = Globals.getInstance();

   public WikiType() {
   }
   
   public static String fetchWiki(XMLObject obj) {
      return fetchWiki(obj.getAttr("name"), obj.getAttr("id"));
   } //fetchWiki
   
   public static String fetchWiki(String strName, String strID) {
      //Return the UNMODIFIED (weird links) wiki file found in the temp folder.
      String strText = "";
      
      strText = glbl.getArchiveFile(strName, strID);
        
      return strText;
   }//fetchWiki
   
   public static void copyArchiveWikiOut(XMLObject obj, String strFindName,
                                               String strOutName, File filPath,
                                               String strID) {
      //Copy the named file out of the temp folder and into a page folder
      //on the user's disk drive.
      StringBuffer buf = new StringBuffer(16384);
      PrintStream outs = null;
      File filOut = null;
      File filOutPar = null;
     
      try {
         //Might have that "embedded" prefix (but I haven't seen it on a wiki entry):
         if (strOutName.startsWith("embedded")) //followed by /
            strOutName = strOutName.substring(9); //OK
            
         filOut = new File(filPath, strOutName);
         filOutPar = filOut.getParentFile();
         filOutPar.mkdirs();
         filOut.createNewFile();
         
         String strText = glbl.getArchiveFile(strFindName, strID);
         strText = fixWikiImageLinks(obj,strText,filPath.getPath());
         strText = fixWikiAnchorLinks(obj,strText,filPath.getPath());
         if ((glbl.myIndexOf(strText,"<html","<HTML") == -1) && strText.length() >0)
           strText = "<html><head><title>"+strOutName+"</title></head><body>"+strText+"</body></html>";
         outs = new PrintStream(filOut);
         outs.print(strText);
         outs.close();
      } catch (FileNotFoundException e) {
         Unzipper.ref.setMessage("\"" + strID + "/" + strOutName +
                                 "\" is not in the zip file.", ERROR_MSG, false);
      } catch (IOException e) {
         Unzipper.ref.setMessage("Could not read \"" + strID + "/" + strOutName +
                                 "\" from the zip file.", ERROR_MSG, true);
      } finally {
         if (outs != null)
               outs.close();
      }
   } //copyArchiveWikiOut
   
    public static String fixWikiImageLinks(XMLObject obj, String strText, String strPath) {
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
       imgLoc = imgLoc = glbl.myIndexOf(post, "<IMG", "<img");
       while (imgLoc >= 0) {
          str = str + post.substring(0,imgLoc); //everything from last image to next
          post = post.substring(imgLoc); // from <img ... on
          endLoc = post.indexOf(">");
          img = post.substring(0,endLoc+1); //from <img to ending >
          post = post.substring(endLoc+1); //all after >
          //Now img has the entire img tag:
          prefixLoc = img.indexOf(WIKI_IMAGE_PREFIX);
          if (prefixLoc > 0) {
             //Must convert the Bb reference to the actual location:
             srcLoc = Math.max(img.indexOf("src=\""),img.indexOf("SRC=\""));
             str = str+img.substring(0,srcLoc+5);
             img = img.substring(prefixLoc+WIKI_IMAGE_PREFIX.length());
             if (obj.getAttr("display").indexOf("_bak") > -1)
                 str = str +  "../" + img;
             else
                 str = str + img;
          } else {
             //Not a Bb-prefixed image tag (if this even occurs):
             str = str +img; //just keep the whole img tag
          }
          imgLoc = imgLoc = glbl.myIndexOf(post, "<IMG", "<img");
       }
       str = str + post;
       return str;
    }//fixWikiImageLinks

     public static String fixWikiAnchorLinks(XMLObject obj, String strText, String strPath) {
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
        imgLoc = imgLoc = glbl.myIndexOf(post, "<a ", "<A ");
        while (imgLoc >= 0) {
           str = str + post.substring(0,imgLoc); //everything from last image to next
           post = post.substring(imgLoc); // from <img ... on
           endLoc = post.indexOf(">");
           img = post.substring(0,endLoc+1); //from <img to ending >
           post = post.substring(endLoc+1); //all after >
           //Now img has the entire img tag:
           prefixLoc = img.indexOf(WIKI_IMAGE_PREFIX);
           if (prefixLoc > 0) {
              //Must convert the Bb reference to the actual location:
              srcLoc = Math.max(img.indexOf("href=\""),img.indexOf("HREF=\""));
              str = str+img.substring(0,srcLoc+6);
              img = img.substring(prefixLoc+WIKI_IMAGE_PREFIX.length());
              if (obj.getAttr("display").indexOf("_bak") > -1)
                  str = str +  "../" + img;
              else
                  str = str + img;
           } else {
              //Not a Bb-prefixed image tag (if this even occurs):
              str = str +img; //just keep the whole img tag
           }
           imgLoc = imgLoc = glbl.myIndexOf(post, "<a ", "<A ");
        }
        str = str + post;
        return str;
     }//fixWikiAnchorLinks
     
      public static String incrementWiki(String str) {
         //Increment the 3-digit suffixed counter on this string.
         String sNum = str.substring(str.length() - 3);
         int iNum = 0;

         try {
            iNum = Integer.parseInt(sNum) + 1;
         } catch (NumberFormatException e) {
            System.out.println(sNum + " is not numeric"); //not likely!
         }
         return str.substring(0, str.length() - 3) + DateUtils.toDDD(iNum);
      } //incrementWiki
      
      public static String fetchWikiName(String strFile, String strID) {
         String strText = "";
         String strName = "";
         int loc;
         
         //Fetch the ".properties" companion to ".wiki":
         loc = glbl.myIndexOf(strFile,".wiki",".WIKI");
         strFile = strFile.substring(0,loc) + ".properties";
         strText = glbl.getArchiveFile(strFile, strID);
         
         //The wiki name is actually the "name=" property in that file:
         BufferedReader br = new BufferedReader(new StringReader(strText));
         try {
            strName = br.readLine();
            while ((strName != null) && !strName.startsWith("Name=")) {
               strName = br.readLine();
            }
            if (strName != null) {
               strName = strName.substring(5); //strip "Name="
            } else {
               strName = "";
            }
         } catch(IOException e) {
            strName = "";
         }
         return strName;
      }//fetchWikiName
      
      public static void setWikiDisplayAndDate(String strFile, XMLObject obj) {
         String strText = "";
         String strTemp = "";
         String strDisplay = "";
         String strDate = "";
         int loc;
         
         //Fetch the ".properties" companion to ".wiki":
         loc = glbl.myIndexOf(strFile,".wiki",".WIKI");
         strFile = strFile.substring(0,loc) + ".properties";
         strText = glbl.getArchiveFile(strFile, obj.getAttr("id"));
         
         //The wiki name is actually the "Name=" property in that file,
         //and the date is the "Date" property:
         BufferedReader br = new BufferedReader(new StringReader(strText));
         try {
            strTemp = br.readLine();
            while (strTemp != null) {
               if (strTemp.startsWith("Name=")) {
                  strDisplay = strTemp.substring(5); //strip "Name="
               } else if (strTemp.startsWith("Date=")) {
                  strDate = strTemp.substring(5); //strip "Date="
                  strDate = strDate.substring(0,4)+"-"+strDate.substring(4,6) +"-"+strDate.substring(6,8);
               }
               strTemp = br.readLine();
            }
         } catch(IOException e) {
            strDisplay = "";
            strDate = "---";
         }
         obj.setAttr("display",strDisplay);
         obj.setAttr("created",strDate);
         obj.setAttr("modified",strDate);
      }//setWikiDisplayAndDate
      
}//WikiType
