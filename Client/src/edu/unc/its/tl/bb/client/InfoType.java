package edu.unc.its.tl.bb.client;

import org.dvm.java.xml.XMLAccessor;
import org.dvm.java.xml.XMLObject;

public class InfoType {
   //Methods for the Info type content.

   private static Globals glbl = Globals.getInstance();

   public InfoType() {
   }

   public static String fetchInfo(XMLObject obj) {
      return fetchInfo(obj.getAttr("id"));
   } //fetchInfo

   public static String fetchInfo(String id) {
      String strOut = "";
      XMLAccessor accItem = null;
      String itemXML = glbl.getArchiveFileText(id + ".dat");

      if ((itemXML != null) && (itemXML.length() > 0)) {
         accItem = glbl.getAccessor(itemXML);
         strOut = fetchInfo(accItem);
      }
      return strOut;
   } //fetchInfo
   
   public static String fetchInfo(XMLAccessor accItem) {
      String strTemp = "";
      String strInfo = "";
      String eol = System.getProperty("line.separator");

      strTemp =
            accItem.getAttr("staffinfo/contact/name/formaltitle", "value", "No Title");
      strInfo = strInfo + strTemp + " ";
      strTemp =
            accItem.getAttr("staffinfo/contact/name/given", "value", "First Name");
      strInfo = strInfo + strTemp + " ";
      strTemp =
            accItem.getAttr("staffinfo/contact/name/family", "value", "Last Name");
      strInfo = strInfo + strTemp + "<br>" + eol;
      strTemp =
            accItem.getAttr("staffinfo/contact/email", "value", "No Email");
      strInfo = strInfo + "Email: " + glbl.stripJunk(strTemp) + "<br>" + eol;
      strTemp =
            accItem.getAttr("staffinfo/contact/phone", "value", "No Phone");
      strInfo = strInfo + "Phone: " + glbl.stripJunk(strTemp) + "<br>" + eol;
      strTemp =
            accItem.getAttr("staffinfo/contact/office/address", "value", "No Office");
      strInfo = strInfo + "Office: " + glbl.stripJunk(strTemp) + "<br>" + eol;
      strTemp =
            accItem.getAttr("staffinfo/contact/office/hours", "value", "No Hours");
      strInfo = strInfo + "Hours: " + glbl.stripJunk(strTemp) + "<br>" + eol;
      strTemp = accItem.getAttr("staffinfo/homepage", "value", "No Home Page");
      strInfo =
            strInfo + "Home Page: " + glbl.stripJunk(strTemp) + "<br>" + eol;
      strTemp = accItem.getField("staffinfo/biography/text", "No Bio");
      strInfo = strInfo + "Biography:" + "<br>" + eol;
      strInfo = strInfo + glbl.fixHTML(glbl.stripJunk(strTemp)) + "<br>" + eol;

      return strInfo;
   } //fetchInfo

}//InfoType
