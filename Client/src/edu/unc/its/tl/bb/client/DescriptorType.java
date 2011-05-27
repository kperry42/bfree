package edu.unc.its.tl.bb.client;

import org.dvm.java.xml.XMLAccessor;
import org.dvm.java.xml.XMLObject;

public class DescriptorType implements BbConstants {
   
   private static Globals glbl = Globals.getInstance();
   
   public DescriptorType() {
   }
   
   public static String fetchDescriptor(XMLObject obj, boolean bReroute) {
      return fetchDescriptor(obj.getAttr("id"),bReroute);
   } //fetchDescriptor

   public static String fetchDescriptor(String id, boolean bReroute) {
      String strOut = "";
      XMLAccessor accItem = null;
      String itemXML = glbl.getArchiveFileText(id + ".dat");
      
      if ((itemXML != null) && (itemXML.length() > 0)) {
         accItem = glbl.getAccessor(itemXML);
         strOut = fetchDescriptor(accItem,bReroute);
      }
      return strOut;
   }//fetchDescriptor
   
   public static String fetchDescriptor(XMLAccessor accItem, boolean bReroute) {
      String strTemp = "";

      strTemp = accItem.getField("content/body/text", UNKNOWN);
      if (!strTemp.equals(UNKNOWN)) {
         if (bReroute)
            strTemp = glbl.rerouteFileLinks(strTemp);
         //Drop the externally meaningless assignment link in the text:
         strTemp = glbl.dropBadLink(strTemp);

         strTemp = glbl.stripJunk(strTemp);
         strTemp = glbl.fixHTML(strTemp);
      } else {
         //Could be the Course descriptor:
         strTemp = accItem.getField("course/description", UNKNOWN);
         if (!strTemp.equals(UNKNOWN)) {
            //Drop the externally meaningless assignment link in the text:
            strTemp = glbl.dropBadLink(strTemp);

            strTemp = glbl.stripJunk(strTemp);
            strTemp = glbl.fixHTML(strTemp);
         }
      }
      return strTemp;
   }//fetchDescriptor
   
}//DescriptorType
