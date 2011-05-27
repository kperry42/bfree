package edu.unc.its.tl.bb.client;

import org.dvm.java.xml.XMLAccessor;
import org.dvm.java.xml.XMLObject;

public class LinkType  implements BbConstants {
   
   private static Globals glbl = Globals.getInstance();

   public LinkType() {
   }
   
   public static String fetchURL(XMLObject obj) {
      return fetchURL(obj.getAttr("id"));
   } //fetchURL

   public static String fetchURL(String id) {
      String strOut = "";
      XMLAccessor accItem = null;
      String itemXML = glbl.getArchiveFileText(id + ".dat");
      
      if ((itemXML != null) && (itemXML.length() > 0)) {
         accItem = glbl.getAccessor(itemXML);
         strOut = fetchURL(accItem);
      }
      return strOut;
   }//fetchURL
   
   public static String fetchURL(XMLAccessor accItem) {
      //See if the item has a URL in its XML file, and put it into a file.
      String strTemp;

      strTemp = accItem.getAttr("content/url", "value", "unknown link");
      
      return strTemp;
   }//fetchURL
   
   public static String fetchTOCURL(XMLObject obj, boolean bAnchor) {
      String strOut = "";
      XMLAccessor accItem = null;
      String eol = System.getProperty("line.separator");
      String itemXML = glbl.getArchiveFileText(obj.getAttr("id") + ".dat");
      
      if ((itemXML != null) && (itemXML.length() > 0)) {
         accItem = glbl.getAccessor(itemXML);
         strOut = accItem.getAttr("coursetoc/url", "value", UNKNOWN);
         if (bAnchor)
            strOut = "<a href=\"" + strOut + "\">" + strOut + "</a>" + eol;
      }
      return strOut;
   }//fetchTOCURL
   
}//URLType
