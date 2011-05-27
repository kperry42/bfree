package edu.unc.its.tl.bb.client;

import org.dvm.java.xml.XMLAccessor;
import org.dvm.java.xml.XMLObject;

public class SakaiInput implements BbConstants {
   //handle the creationof the content tree for Sakai archives, which appear
   //to adhere strictly to the IMS Content Package standard. In particular,
   //they make much use of metadata.

   private Globals glbl = null;

   public SakaiInput() {
      glbl = Globals.getInstance();
   }

   public void createSakaiContentTree(XMLAccessor accItem) {
      //Build a skeletal tree that outlines all the content,
      //using information from the item hierarchy:
      XMLAccessor acc = null;
      String itemXML = null;
      String strTemp = null;
      boolean bHasDiscussions = false;
      XMLObject objDisc = null;

      //Create the root node:
      glbl.objCourse = new XMLObject("course", XMLObject.ELEMENT);
      glbl.objCourse.setAttr("id", "res00000"); //dummy ID
      glbl.objCourse.setAttr("display",
                             glbl.strCourseName); //init is file name
      glbl.objCourse.setAttr("name", "");
      glbl.objCourse.setAttr("dir", "true");
      glbl.objCourse.setAttr("type", "Course");
      glbl.objCourse.setAttr("extract", "true");
      glbl.objCourse.setAttr("created", "---");
      glbl.objCourse.setAttr("modified", "---");
      /*
      //See if there is a resource for the overall course. It would be
      //res00001, but this could also be a content resource.
      //Note: I believe that the res00001 resource applies to the course
      //iff the user checks the "Settings" box when exporting.
      XMLObject objRes =
         glbl.accRes.getObject("resources/resource.identifier=res00001");
      if ((objRes != null) && objRes.getAttr("type").equals(BB_SETTING)) {
         //Get the course resource info:
         glbl.objCourse.setAttr("id", "res00001"); //real ID

         //Get the name and dates from the actual resource file:
         itemXML = glbl.getArchiveFileText("res00001.dat");
         if ((itemXML != null) && (itemXML.length() > 0)) {
            acc = glbl.getAccessor(itemXML);
            strTemp =
                  acc.getAttr("course/title", "value", "Course (title unknown)").trim();
            if (strTemp.trim().length() == 0)
               strTemp = "Course (title unknown)";
            //glbl.objCourse.setAttr("display", glbl.convertAmpersands(strTemp));
            glbl.objCourse.setAttr("display", glbl.fixHTML(strTemp));
            glbl.objCourse.setAttr("name", strTemp);
            strTemp = acc.getAttr("course/dates/created", "value", "---");
            glbl.objCourse.setAttr("created", DateUtils.fixDate(strTemp));
            strTemp = acc.getAttr("course/dates/updated", "value", "---");
            glbl.objCourse.setAttr("modified", DateUtils.fixDate(strTemp));
         }
         attachContentFiles("res00001", glbl.objCourse);
      }
      */

      //Get the course title from the metadata in the manifest:
      strTemp =
            glbl.accManifest.getField("manifest/metadata/imsmd:lom/imsmd:general/imsmd:title/imsmd:langstring",
                                      UNKNOWN);
      if (!strTemp.equals(UNKNOWN)) {
         glbl.objCourse.setAttr("display", glbl.fixHTML(strTemp));
         glbl.objCourse.setAttr("name", strTemp);
      }

      //Get the course description from the metadata in the manifest:
      strTemp =
            glbl.accManifest.getField("manifest/metadata/imsmd:lom/imsmd:general/imsmd:description/imsmd:langstring",
                                      UNKNOWN);
      if (!strTemp.equals(UNKNOWN)) {
         glbl.objCourse.setAttr("content", "Text");
      } else {
         //No description text:
         glbl.objCourse.setAttr("content", UNKNOWN);
      }

      //Now process the rest of the tree:
      //addSections(accItem.getXMLObject().getChildObject(), glbl.objCourse);

      /*
      //See if there are any discussion board items. An archive will have
      //a section called "Discussion Board", but an export will not, even
      //if there are entries for one!
      objDisc =
            glbl.accRes.getObject("resources/resource.type=" + BB_DISCUSSION_ALT);
      bHasDiscussions = (objDisc != null);
      //System.out.println("Disc="+bHasDiscussions);//TEMP

      if (bHasDiscussions) {
         //Add a section for the discussion board if it does not exist,
         //then add all the forums to it:
         XMLObject objSection = null;
         XMLAccessor accDisc = new XMLAccessor(glbl.objCourse);
         objDisc =
               accDisc.getObject("course/section.display=Discussion Board");
         if (objDisc == null) {
            objSection = new XMLObject("section", XMLObject.ELEMENT);
            objSection.setAttr("id", "res99999"); //ID never used
            objSection.setAttr("display", "Discussion Board");
            objSection.setAttr("name", "Discussion Board");
            objSection.setAttr("dir", "true");
            objSection.setAttr("type", "Section");
            objSection.setAttr("extract", "true");
            objSection.setAttr("created", "---");
            objSection.setAttr("modified", "---");
            objSection.setAttr("content", UNKNOWN);
            glbl.objCourse.addChildObject(objSection, true); //after others
         } else
            objSection = objDisc;
         //Now add the forum entries:
         addForums(objSection);
      }
      */
      /*
      if (acc != null) { //new
         //See if there is a descriptor:
         strTemp = acc.getField("course/description", "");
         if (!strTemp.equals("")) {
            //Mark the root as having a description text:
            glbl.objCourse.setAttr("content", "Text");
         } else {
            //No description text:
            glbl.objCourse.setAttr("content", UNKNOWN);
         }
      }
      */
   } //createSakaiContentTree


}//SakaiInput
