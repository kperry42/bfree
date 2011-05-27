package edu.unc.its.tl.bb.client;

import org.dvm.java.xml.XMLAccessor;
import org.dvm.java.xml.XMLObject;

public class AnnounceType implements BbConstants {
//
   
   private static Globals glbl = Globals.getInstance();

   public AnnounceType() {
   }
   
   public static XMLObject createObject(XMLObject objRes) {
      //Find the resource, create an object for it, and fill in the details.
      XMLObject objAnnounce = null;
      XMLAccessor accItem = null;
      String itemXML = null;
      String strTemp = null;
      
      strTemp = objRes.getAttr("identifier");
      
      itemXML = glbl.getArchiveFileText(strTemp + ".dat");
      if ((itemXML != null) && (itemXML.length() > 0)) {
         accItem = glbl.getAccessor(itemXML);

         objAnnounce = new XMLObject("topic",XMLObject.EMPTY);
         objAnnounce.setAttr("id",strTemp);
         objAnnounce.setAttr("dir","false");
         objAnnounce.setAttr("type","Announce"); //process as this doc type
         objAnnounce.setAttr("extract","true");
         strTemp = accItem.getAttr("announcement/dates/created","value","");
         objAnnounce.setAttr("created",DateUtils.fixDate(strTemp));
         strTemp = accItem.getAttr("announcement/dates/updated","value","");
         objAnnounce.setAttr("modified",DateUtils.fixDate(strTemp));
         objAnnounce.setAttr("content","Announce"); //need this ???
         strTemp = accItem.getAttr("announcement/title", "value", UNKNOWN).trim();
         strTemp = glbl.fixHTML(strTemp);
         objAnnounce.setAttr("display",strTemp);
         objAnnounce.setAttr("name",glbl.fixName(strTemp)); //OK
      }
      return objAnnounce;
   }//createObject
   
    public static String fetchAnnouncement(XMLObject obj) {
       //Fetch, format, and return the title, description and complete
       //message thread for the topic of the given object.
       StringBuffer buf = new StringBuffer(4096);
       XMLAccessor accItem = null;
       String itemXML = null;
       String strTemp = null;
       
       strTemp = obj.getAttr("id");
       
       itemXML = glbl.getArchiveFileText(strTemp + ".dat");
       if ((itemXML != null) && (itemXML.length() > 0)) {
          accItem = glbl.getAccessor(itemXML);
         
          buf.append(accItem.getField("announcement/description/text",UNKNOWN));
       }
      if (buf.length() == 0)
         return "[Nothing to preview]";
      //else
      return buf.toString();
   }//fetchAnnouncement
    
}//AnnounceType
