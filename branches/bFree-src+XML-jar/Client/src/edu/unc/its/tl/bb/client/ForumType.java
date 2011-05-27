package edu.unc.its.tl.bb.client;

import org.dvm.java.xml.XMLAccessor;
import org.dvm.java.xml.XMLObject;

public class ForumType implements BbConstants {
   
   private static Globals glbl = Globals.getInstance();

   public ForumType() {
   }
   
   public static XMLObject createObject(XMLObject objRes) {
      //Find the resource, create an object for it, and fill in the details.
      //NOTE that there is no separate input file for the forum, but BbOut
      //will create one upon extraction.
      XMLObject objForum = null;
      XMLAccessor accItem = null;
      String itemXML = null;
      String strTemp = null;
      
      strTemp = objRes.getAttr("identifier");
      
      itemXML = glbl.getArchiveFileText(strTemp + ".dat");
      if ((itemXML != null) && (itemXML.length() > 0)) {
         accItem = glbl.getAccessor(itemXML);
         
         //Create a leaf for the main topic:
         objForum = new XMLObject("topic",XMLObject.ELEMENT);
         objForum.setAttr("id",strTemp);
         objForum.setAttr("dir","false");
         objForum.setAttr("type","Forum"); //process as this doc type
         objForum.setAttr("extract","true");
         strTemp = accItem.getAttr("forum/dates/created","value","");
         objForum.setAttr("created",DateUtils.fixDate(strTemp));
         strTemp = accItem.getAttr("forum/dates/updated","value","");
         objForum.setAttr("modified",DateUtils.fixDate(strTemp));
         objForum.setAttr("content","Forum"); //need this ???
         strTemp = accItem.getAttr("forum/title", "value", UNKNOWN);
         strTemp = glbl.fixHTML(strTemp);
         objForum.setAttr("display",strTemp);
         objForum.setAttr("name",glbl.fixName(strTemp));//OK
         
         //If there is an archive in there, add it as a child, and make
         //the main topic a folder:
         if (accItem.hasField("forum/threadarchive")) {
            //The main topic is now a folder:
            objForum.setAttr("dir","true");
            //Create a node for the archive:
            XMLObject objArchive = new XMLObject("topic",XMLObject.ELEMENT);
            objArchive = new XMLObject("archive",XMLObject.ELEMENT);
            objArchive.setAttr("id",objRes.getAttr("identifier"));
            objArchive.setAttr("dir","false");
            objArchive.setAttr("type","Archive"); //process as this doc type
            objArchive.setAttr("extract","true");
            strTemp = accItem.getAttr("forum/threadarchive/dates/created","value","");
            objArchive.setAttr("created",DateUtils.fixDate(strTemp));
            strTemp = accItem.getAttr("forum/threadarchive/dates/updated","value","");
            objArchive.setAttr("modified",DateUtils.fixDate(strTemp));
            objArchive.setAttr("content","Archive"); //need this ???
            strTemp = accItem.getAttr("forum/threadarchive/title", "value", UNKNOWN);
            strTemp = glbl.fixHTML(strTemp);
            objArchive.setAttr("display",strTemp);
            objArchive.setAttr("name",glbl.fixName(strTemp));//OK
            //Add it to the main node:
            objForum.addChildObject(objArchive);
         }
      }
      return objForum;
   }//createObject
   
   public static String fetchForum(XMLObject obj, boolean bAddHead, boolean bMain) {
      //Fetch, format, and return the title, description and complete
      //message thread for the topic of the given object.
      //bMain == true iff it is a main topic, false if it's an archive
      StringBuffer buf = new StringBuffer(4096);
      XMLAccessor accItem = null;
      String itemXML = null;
      String strTemp = null;
      XMLObject objMsg = null;
      
      strTemp = obj.getAttr("id");
      
      itemXML = glbl.getArchiveFileText(strTemp + ".dat");
      if ((itemXML != null) && (itemXML.length() > 0)) {
         accItem = glbl.getAccessor(itemXML);
         if (bMain) {
            if (bAddHead)
               buf.append("<html><head><title>Topic</title></head><body>");
            buf.append("Topic: ");
            buf.append(accItem.getAttr("forum/title", "value", UNKNOWN));
            buf.append("<br><br>");
            buf.append("Description: ");
            buf.append(accItem.getField("forum/description/text",UNKNOWN));
            buf.append("<br>");
         } else {
            if (bAddHead)
               buf.append("<html><head><title>Topic</title></head><body>");
            buf.append("Topic: ");
            buf.append(accItem.getAttr("forum/threadarchive/title", "value", UNKNOWN));
            buf.append("<br><br>");
            buf.append("Description: ");
            buf.append(accItem.getField("forum/threadarchive/description/text",UNKNOWN));
            buf.append("<br>");
         }
         if (bMain)
            objMsg = accItem.getObject("forum/messagethreads/msg");
         else
            objMsg = accItem.getObject("forum/threadarchive/messagethreads/msg");
         buf.append(fetchMessages(objMsg));
         if (bAddHead)
            buf.append("</body></html>");
      }
      if (buf.length() == 0)
         return "[Nothing to preview]";
      //else
         return buf.toString();
   }//fetchForum
   
   private static String fetchMessages(XMLObject objMsg) {
      //Recurse through the nested messages
      StringBuffer buf = new StringBuffer(4096);
      XMLAccessor accMsg = new XMLAccessor(objMsg);
      String strTemp = "";
      int loc;
      
      if (objMsg == null)
         return "";
         
      buf.append("<br><ul>Created: ");
      buf.append(DateUtils.fixDate(accMsg.getAttr("msg/dates/created","value",""))+" - ");
      buf.append(glbl.fixHTML(accMsg.getAttr("msg/title","value",""))+"<br>");
      buf.append(glbl.fixHTML(glbl.stripJunk(accMsg.getField("msg/messagetext/text",UNKNOWN)))+"<br>");
      strTemp = accMsg.getAttr("msg/postedname","value",UNKNOWN);
      loc = strTemp.indexOf(", ");
      if (loc >= 0) {
         strTemp = strTemp.substring(loc+2);
         loc = strTemp.indexOf(" ");
         if ((loc > 0) && (strTemp.length() > 1))
            strTemp = strTemp.substring(0,loc);
      } else
         strTemp = "Anon.";
      buf.append("-- by "+strTemp+"<br>");
      
      //Process children:
      buf.append(fetchMessages(accMsg.getObject("msg/msg")));
      
      //Process siblings:
      buf.append(fetchMessages(accMsg.getObject("msg#1")));
      
      buf.append("</ul>");
      return buf.toString();
   }//fetchMessages
   
}//ForumType
