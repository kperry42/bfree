package edu.unc.its.tl.bb.client;

import org.dvm.java.xml.XMLAccessor;
import org.dvm.java.xml.XMLHandler;
import org.dvm.java.xml.XMLObject;

public class BbInput implements BbConstants {
   //Handle the creation of the content tree for Blackboard archives.
   //These follow the IMS Content Package standard only in outline,
   //with many details that are unique to Blackboard.

   private Globals glbl = null;

   public BbInput() {
      glbl = Globals.getInstance();
   }

   public void createBbContentTree(XMLAccessor accItem) {
      //Build a skeletal tree that outlines all the content,
      //using information from the item hierarchy:
      XMLAccessor acc = null;
      String itemXML = null;
      String strTemp = null;
      boolean bHasExtra = false;
      XMLObject objExtra = null;

      //Create the root node:
      glbl.objCourse = new XMLObject("course", XMLObject.ELEMENT);
      glbl.objCourse.setAttr("id", "res00000"); //dummy ID
      glbl.objCourse.setAttr("display", glbl.strCourseName);
      glbl.objCourse.setAttr("name", glbl.strCourseName);
      //glbl.objCourse.setAttr("name", "");//OK folder name will be given by user
      glbl.objCourse.setAttr("dir", "true");
      glbl.objCourse.setAttr("type", "Course");
      glbl.objCourse.setAttr("extract", "true");
      glbl.objCourse.setAttr("created", "---");
      glbl.objCourse.setAttr("modified", "---");

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
            strTemp = glbl.fixHTML(strTemp);
            glbl.objCourse.setAttr("display", strTemp);
            glbl.objCourse.setAttr("name", glbl.fixName(strTemp)); //OK
            strTemp = acc.getAttr("course/dates/created", "value", "---");
            glbl.objCourse.setAttr("created", DateUtils.fixDate(strTemp));
            strTemp = acc.getAttr("course/dates/updated", "value", "---");
            glbl.objCourse.setAttr("modified", DateUtils.fixDate(strTemp));
         }
         attachContentFiles("res00001", glbl.objCourse);
      }
      //Now process the rest of the tree:
      addBbSections(accItem.getXMLObject().getChildObject(), glbl.objCourse);

      //See if there are any discussion board items. An archive will have
      //a section called "Discussion Board", but an export will not, even
      //if there are entries for one!
      objExtra =
            glbl.accRes.getObject("resources/resource.type=" + BB_DISCUSSION_ALT);
      bHasExtra = (objExtra != null);

      if (bHasExtra) {
         //Add a section for the discussion board if it does not exist,
         //then add all the forums to it:
         XMLObject objSection = null;
         XMLAccessor accDisc = new XMLAccessor(glbl.objCourse);
         objExtra =
               accDisc.getObject("course/section.display=Discussion Board");
         if (objExtra == null) {
            objSection = new XMLObject("section", XMLObject.ELEMENT);
            objSection.setAttr("id", "res99999"); //ID never used
            objSection.setAttr("display", "Discussion Board");
            objSection.setAttr("name", "Discussion_Board"); //OK
            objSection.setAttr("dir", "true");
            objSection.setAttr("type", "Section");
            objSection.setAttr("extract", "true");
            objSection.setAttr("created", "---");
            objSection.setAttr("modified", "---");
            objSection.setAttr("content", UNKNOWN);
            glbl.objCourse.addChildObject(objSection, true); //after others
         } else
            objSection = objExtra;
         //Now add the forum entries, which can have recursive content:
         addForums(objSection);
      }

      //See if there are any announcements. An archive will have
      //a section called "Announcements", but an export will not, even
      //if there are entries for one!
      objExtra =
            glbl.accRes.getObject("resources/resource.type=" + BB_ANNOUNCEMENT_ALT);
      bHasExtra = (objExtra != null);

      if (bHasExtra) {
         //Add a section for the announcements if it does not exist,
         //then add all the forums to it:
         XMLObject objSection = null;
         XMLAccessor accAnnounce = new XMLAccessor(glbl.objCourse);
         objExtra =
               accAnnounce.getObject("course/section.display=Announcements");
         if (objExtra == null) {
            objSection = new XMLObject("section", XMLObject.ELEMENT);
            objSection.setAttr("id", "res99999"); //ID never used
            objSection.setAttr("display", "Announcements");
            objSection.setAttr("name", "Announcements"); //OK
            objSection.setAttr("dir", "true");
            objSection.setAttr("type", "Section");
            objSection.setAttr("extract", "true");
            objSection.setAttr("created", "---");
            objSection.setAttr("modified", "---");
            objSection.setAttr("content", UNKNOWN);
            glbl.objCourse.addChildObject(objSection, true); //after others
         } else
            objSection = objExtra;
         //Now add the anouncements, which are singular, unlike forum entries:
         addAnnouncements(objSection);
      }

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
   } //createBbContentTree

   private void addBbSections(XMLObject objIn, XMLObject objOut) {
      //Attach the sections found in objIn (manifest) to the output tree,
      //then fill out the subsections, etc.
      XMLObject objSection = null; //the section to be created
      XMLAccessor accSection =
         null; //for the section's resource in the manifest
      String xmlSection = null; //for the section's resource in the manifest
      String strTemp;
      String strID;
      String strNew = UNKNOWN;

      XMLObject objNextIn = objIn;

      while (objNextIn != null) {
         strID = objNextIn.getAttr("identifierref");
         //Start a new node for this section:
         objSection = new XMLObject("section", XMLObject.ELEMENT);
         objSection.setAttr("id", strID);

         //Attach the new section after any others:
         objOut.addChildObject(objSection, true);

         //Get its resource and fill out the details:
         xmlSection = glbl.getArchiveFileText(strID + ".dat");
         if ((xmlSection != null) && (xmlSection.length() > 0)) {
            accSection = glbl.getAccessor(xmlSection);
            //The section title:
            strTemp =
                  accSection.getAttr("coursetoc/label", "value", UNKNOWN).trim();
            if (!strTemp.equals(UNKNOWN)) {
               //Could be a Bb internal title:
               strNew = getDisplayTitle(strTemp);
               if (strNew.equals(UNKNOWN)) {
                  //Probably a user-named section:
                  strTemp = glbl.fixHTML(strTemp);
                  objSection.setAttr("display", strTemp);
                  objSection.setAttr("name", glbl.fixName(strTemp)); //OK
               } else {
                  //Internal, now our version:
                  objSection.setAttr("display", strNew);
                  objSection.setAttr("name", glbl.fixName(strNew)); //OK
               }
            } else {
               //Title missing completely:
               objSection.setAttr("display", "Unknown Section");
               objSection.setAttr("name", "Unknown_Section");//OK
            }
            //NOTE: Could also check the file for a URL.

            //Other (default) attributes:
            objSection.setAttr("dir", "true");
            objSection.setAttr("type", "Section");
            objSection.setAttr("extract", "true");
            objSection.setAttr("created", "---");
            objSection.setAttr("modified", "---");
            strTemp =
                  accSection.getAttr("coursetoc/url", "value", UNKNOWN).trim();
            if (strTemp.equals(UNKNOWN) || (strTemp.length() == 0))
               //objSection.setAttr("content", UNKNOWN);
               objSection.setAttr("content", "Section");
            else
               objSection.setAttr("content", "URL");
         }
         //Attach this section's subsections:
         addBbSubsections(objNextIn.getChildObject(), objSection);

         //Step to the next section (if any):
         objNextIn = objNextIn.getNextObject();
      }
      //Scan for course and group uploads, making new sections for each:
      if (objSection != null)
         addUploads(objSection); //siblings of last section
   } //addBbSections

   private class ObjList {
      XMLObject objFirst = null;
      XMLObject objLast = null;

      public void insertInOrder(XMLObject objNew) {
         boolean bFound = false;

         if (objFirst == null) {
            objFirst = objNew;
            objLast = objNew;
         } else {
            String strNewName = objNew.getAttr("display");
            String strName = "";
            XMLObject objNext = objFirst;
            XMLObject objPrev = null;

            while ((objNext != null) && !bFound) {
               strName = objNext.getAttr("display");
               if (strNewName.compareToIgnoreCase(strName) <
                   0) { //new name < name from list
                  bFound = true;
               } else {
                  objPrev = objNext;
                  objNext = objNext.getNextObject();
               }
            }
            if (bFound) {
               //Insert before name in list
               objNew.setNextObject(objNext);
               if (objPrev == null)
                  objFirst = objNew;
               else
                  objPrev.setNextObject(objNew);
            } else {
               //Append to list
               objLast.setNextObject(objNew);
               objLast = objNew;
            }
         }
      } //insertInOrder

      public void dumpList() {
         XMLObject obj = objFirst;

         while (obj != null) {
            System.out.println(obj.getAttr("display"));
            obj = obj.getNextObject();
         }
      } //dumpList

      public void setParents(XMLObject objParent) {
         XMLObject obj = objFirst;

         while (obj != null) {
            obj.setParentObject(objParent);
            obj = obj.getNextObject();
         }
      } //setParents

   } //objList

   private void addForums(XMLObject obj) {
      //Add all the forum entries, as children, in alphabetical order.
      XMLObject objRes = null;
      XMLObject objForum = null;
      ObjList objList = new ObjList();

      objRes = glbl.accRes.getObject("resources/resource");
      while (objRes != null) {
         if (objRes.getAttr("type").equals(BB_DISCUSSION)) {
            //This is a forum topic resource; add a subsection:
            objForum = ForumType.createObject(objRes);
            if (objRes.getChildObject() != null) {
               attachContentFiles(objForum.getAttr("id"), objForum);
               objForum.setAttr("dir", "true");
            }
            objList.insertInOrder(objForum);
         }
         objRes = objRes.getNextObject();
      }
      objList.setParents(obj);
      obj.setChildObject(objList.objFirst);
   } //addForums

   private void addAnnouncements(XMLObject obj) {
      //Add all the announcements, as children, in alphabetical order.
      XMLObject objRes = null;
      XMLObject objAnnounce = null;
      ObjList objList = new ObjList();

      objRes = glbl.accRes.getObject("resources/resource");
      while (objRes != null) {
         if (objRes.getAttr("type").equals(BB_ANNOUNCEMENT)) {
            //This is an announcement resource; add a subsection:
            objAnnounce = AnnounceType.createObject(objRes);
            objList.insertInOrder(objAnnounce);
         }
         objRes = objRes.getNextObject();
      }
      objList.setParents(obj);
      obj.setChildObject(objList.objFirst);
   } //addAnnouncements

   private void addBbSubsections(XMLObject objIn, XMLObject objOut) {
      //Add the children of this object, which can be subsections or files.
      XMLObject objSubsection = null; //the subsection to be created
      XMLAccessor accSubsection =
         null; //for the subsection's resource in the manifest
      String xmlSubsection =
         null; //for the subsection's resource in the manifest
      String strTemp;
      String strTag;
      String strID;
      String strFamily;
      String strGiven;
      String strCreated;
      String strModified;
      XMLObject objTemp;
      boolean bIsWiki = false; //so far

      XMLObject objNextIn = objIn;

      while (objNextIn != null) {
         if (objNextIn.getTag().equals("title")) {
            //Ignored: (have the title from the resource) step to the next section (if any).
         } else {
            strID = objNextIn.getAttr("identifierref");
            //Start a new node for this subsection (the tag will be adjusted later):
            objSubsection = new XMLObject("folder", XMLObject.ELEMENT);
            objSubsection.setAttr("id", strID);
            objSubsection.setAttr("dir", "true");
            objSubsection.setAttr("type", "Folder");
            objSubsection.setAttr("extract", "true");
            objSubsection.setAttr("created", "---");
            objSubsection.setAttr("modified", "---");

            //Attach the new subsection after any others:
            objOut.addChildObject(objSubsection, true);

            //Get its resource and fill out the details:
            xmlSubsection = glbl.getArchiveFileText(strID + ".dat");
            if ((xmlSubsection != null) && (xmlSubsection.length() > 0)) {
               accSubsection = glbl.getAccessor(xmlSubsection);
               strTag = accSubsection.getXMLObject().getTag();

               if (strTag.equals("content")) {
                  //It's a subsection of some kind; get its subsection title:
                  strTemp =
                        accSubsection.getAttr("content/title", "value", UNKNOWN).trim();
                  bIsWiki =
                        accSubsection.getAttr("content/contenthandler", "value",
                                              UNKNOWN).equals(BB_WIKI);
                  if (!strTemp.equals(UNKNOWN)) {
                     strTemp = glbl.fixHTML(strTemp);
                     objSubsection.setAttr("display", strTemp);
                     objSubsection.setAttr("name", glbl.fixName(strTemp)); //OK
                  } else {
                     objSubsection.setAttr("display", "Unknown Content");
                     objSubsection.setAttr("name", "Unknown_Content");//OK
                  }
                  strCreated =
                        accSubsection.getAttr("content/dates/created", "value",
                                              "---");
                  strCreated = DateUtils.fixDate(strCreated);
                  objSubsection.setAttr("created", strCreated);
                  strModified =
                        accSubsection.getAttr("content/dates/updated", "value",
                                              "---");
                  strModified = DateUtils.fixDate(strModified);
                  objSubsection.setAttr("modified", strModified);
                  //Find and attach its description, if any:
                  //NOTE: The description is represented with a node here,
                  //but the actual text will be retrieved from the resource
                  //when needed for display or output.
                  strTemp = accSubsection.getField("content/body/text", "");

                  //See if it's got a description:
                  if (strTemp.trim().length() > 0) {
                     //Mark the section as having a description:
                     objSubsection.setAttr("content", "Text");
                  } else {
                     //The section has no description:
                     objSubsection.setAttr("content", UNKNOWN);
                  }
                  //NOTE: The following might not be necessary--or wise; perhaps have type attribute,
                  //folder, text (incl. html), link, file (any)
                  strTemp =
                        accSubsection.getAttr("content/contenthandler", "value",
                                              UNKNOWN);
                  if (strTemp.equals(BB_DOCUMENT)) {
                     objSubsection.setTag("folder"); //tag already set
                  } else if (strTemp.equals(BB_EXTERNALLINK)) {
                     objSubsection.setTag("website");
                  } else if (strTemp.equals(BB_ASSIGNMENT)) {
                     objSubsection.setTag("assignment");
                  } else if (strTemp.equals(BB_WIKI)) {
                     objSubsection.setTag("wiki");
                     bIsWiki = true;
                  } else if (strTemp.equals(BB_FOLDER)) {
                     objSubsection.setTag("folder"); //stays
                  } else if (strTemp.equals(BB_QUIZ)) {
                     objSubsection.setTag("survey");
                  } else if (strTemp.equals(BB_TEST)) {
                     objSubsection.setTag("quiz/test");
                  } else if (strTemp.equals(BB_SYLLABUS)) {
                     objSubsection.setTag("syllabus");
                  } else {
                     objSubsection.setAttr("extract", "true");
                     objSubsection.setTag("folder");
                  }
                  //NOTE: We do need to distinguish stuff we don't process???

                  //Might have files as well.

               } else if (strTag.equals("staffinfo")) {
                  objSubsection.setTag("staff");
                  //It's a staff information page, which now becomes a folder
                  //containing the information and any image:
                  strFamily =
                        accSubsection.getAttr("staffinfo/contact/name/family",
                                              "value", UNKNOWN).trim();
                  strGiven =
                        accSubsection.getAttr("staffinfo/contact/name/given",
                                              "value", UNKNOWN).trim();

                  strCreated =
                        accSubsection.getAttr("staffinfo/dates/created", "value",
                                              "---");
                  strCreated = DateUtils.fixDate(strCreated);
                  objSubsection.setAttr("created", strCreated);
                  strModified =
                        accSubsection.getAttr("staffinfo/dates/updated",
                                              "value", "---");
                  strModified = DateUtils.fixDate(strModified);
                  objSubsection.setAttr("modified", strModified);

                  if (strFamily.equals("") && strGiven.equals("")) {
                     //It's just a folder, not a person:
                     objSubsection.setTag("folder");
                     strTemp =
                           accSubsection.getAttr("staffinfo/title", "value",
                                                 "Staff Members");
                     strTemp = glbl.fixHTML(strTemp);
                     objSubsection.setAttr("display", strTemp);
                     objSubsection.setAttr("name", glbl.fixName(strTemp)); //OK
                  } else {
                     //It's a person
                     objSubsection.setAttr("display",
                                           strFamily + ", " + strGiven);
                     objSubsection.setAttr("name",
                                           glbl.fixName(strFamily + ", " +
                                                        strGiven)); //OK
                     //Mark the section as containing info:
                     objSubsection.setAttr("content", "Info");
                  }
                  //Might have files as well.
               }
               //Find and attach its files, if any:
               if (bIsWiki) {
                  attachWikiFiles(strID, objSubsection);
               } else {
                  attachContentFiles(strID, objSubsection);
               }
               //Find and attach a link, if any: (If there is a file associated with this
               //link [e.g., clickable image], it is attached separately.)
               strTemp =
                     accSubsection.getAttr("content/url", "value", UNKNOWN);
               if (!strTemp.equals(UNKNOWN) && !strTemp.equals("") &&
                   !strTemp.equals("http://")) { //sometimes Bb inserts a plain "http://"
                  strTemp =
                        accSubsection.getAttr("content/files/file/linkname",
                                              "value", UNKNOWN).trim();
                  if (strTemp.equals(UNKNOWN))
                     strTemp = "Link";
                  //Create a node for this link:
                  objTemp = new XMLObject("link", XMLObject.EMPTY);
                  strTemp = glbl.fixHTML(strTemp);
                  objTemp.setAttr("display", strTemp);
                  objTemp.setAttr("name", glbl.fixName(strTemp)); //OK
                  objTemp.setAttr("dir", "false");
                  objTemp.setAttr("type", "Link");
                  objTemp.setAttr("content", "Link");
                  objTemp.setAttr("extract", "true");
                  objTemp.setAttr("id", strID);
                  //If there is a file, there should be a date:
                  strTemp =
                        accSubsection.getAttr("content/files/file/dates/updated",
                                              "value", UNKNOWN);
                  if (strTemp.equals(UNKNOWN)) {
                     objTemp.setAttr("created", "---");
                     objTemp.setAttr("modified", "---");
                  } else {
                     objTemp.setAttr("created", "---");
                     objTemp.setAttr("modified", DateUtils.fixDate(strTemp));
                  }
                  objSubsection.addChildObject(objTemp, true);
                  //Mark the section as containing a link:
                  if (!objSubsection.getTag().equals("website")) //NOTE: Also keeping link subnode
                     objSubsection.setAttr("content", "Link");
               }
               //NOTE: If there is no file/linkname, it might be better to put this link
               //literally on the page (when output) rather than in a file. This seems
               //to happen only if there are other items on the page. (That is, the resource
               //contains other resources in the manifest.) If the link is
               //the only thing for the resource, it is put directly on the page.

               //Adjust the tag name: (redundant if we're checking the contenthandler)
               if (bIsWiki)
                  objSubsection.setTag("wiki");
            }
         }
         //Find and attach its subfolders, if any:
         addBbSubsections(objNextIn.getChildObject(), objSubsection);

         //Step to the next section (if any):
         objNextIn = objNextIn.getNextObject();
      }
   } //addBbSubsections

   private void attachContentFiles(String strID, XMLObject objOut) {
      //Find and attach all the files associated with the resource for this section:
      //File names might be simple, or have a dddddd/ prefix, or (if the name
      //contains spaces or other "invalid" characters) it might be hexadecimal,
      //which is prefixed with "@" or "!". This occurs only when getting file names
      //from a resource, since the unzipArchive method has ensured that
      //the temp folder contains only file names that have had unhexName and fixname
      //applied to them.
      XMLObject objNextFile = null;
      XMLObject objNew = null;
      String strName = null;
      String strDisplay = null;
      int iLoc = -1;

      objNextFile =
            glbl.accRes.getObject("resources/resource.identifier=" + strID +
                                  "/file");
      while (objNextFile != null) {
         strName = objNextFile.getAttr("href").trim();
         objNew = new XMLObject("file", XMLObject.EMPTY);
         objNew.setAttr("type", "File");
         objNew.setAttr("created", "---");
         objNew.setAttr("modified", "---");

         strName = strName.replace('\\', '/');
         strName = glbl.unhexPath(strName);
         int loc = strName.lastIndexOf("/"); //Note must be "/" not "\"
         if (loc >= 0) {
            strDisplay = strName.substring(loc + 1);
            strName = strName.substring(0, loc + 1) + glbl.fixName(strDisplay);
         } else {
            strDisplay = strName;
         }
         objNew.setAttr("display", strDisplay);

         objNew.setAttr("name", strName); //OK
         objNew.setAttr("dir", "false");
         objNew.setAttr("extract", "true");
         objNew.setAttr("id", objOut.getAttr("id")); //same as parent item
         objOut.addChildObject(objNew, true);

         objNextFile = objNextFile.getNextObject();
      }
   } //attachContentFiles

   private void attachWikiFiles(String strID, XMLObject objOut) {
      //Find and attach the wiki pages and other files associated with the resource
      //for this section.
      //Only the final version of each wiki page is kept, and the display names are simplified.
      //The display names for the other files are simplified as well.
      XMLObject objNextFile = null;
      XMLObject objNew = null;
      String strTitle = null;
      String strTemp = null;
      String strWikiPage = "Wiki_Page_000";
      String strAttachment = "Attachment_000";

      //Wiki entries are gathered up as they are found, so that they can be inserted
      //at the beginning of the file list:
      XMLObject objWikiFirst = null;
      XMLObject objWikiLast = null;
      XMLObject objAttachFirst = null;
      XMLObject objAttachLast = null;
      XMLObject objFileLast = null;

      objNextFile =
            glbl.accRes.getObject("resources/resource.identifier=" + strID +
                                  "/file");

      while (objNextFile != null) {
         strTitle = objNextFile.getAttr("href").trim();
         //Unfortunately, some of the wiki files are such junk that we should
         //special-case them out of our processing altogether:
         if //intermediate page stages
             (strTitle.contains("loi-teams/_bak/") ||
             //strTitle.contains("loi-teams/attach/") || //NOTE: Drop these until we find how to name them!
             strTitle.endsWith(".edit") || strTitle.endsWith(".properties") ||
             strTitle.endsWith(".cmnt") || strTitle.endsWith(".props") ||
             strTitle.endsWith(".lck") || strTitle.endsWith(".diff")) {
            //NOTE Ignore these; assumes these types found only among wiki entries!
            //NOTE A more sophisticated method would be to find something
            //unique about the resource node (parent) of these files ...
         } else {
            objNew = new XMLObject("file", XMLObject.EMPTY);
            objNew.setAttr("id", strID);
            objNew.setAttr("created", "---");
            objNew.setAttr("modified", "---");
            
            //Change name to ASCII if it starts with "@", "!" or "attach/":
            strTitle.replace('\\', '/');
            strTitle = glbl.unhexPath(strTitle);

            if (strTitle.endsWith(".wiki") || strTitle.endsWith(".WIKI")) {
               WikiType.setWikiDisplayAndDate(strTitle,
                                              objNew); //sets some attributes!
               strTemp = objNew.getAttr("display");
               if (strTemp.length() == 0) {
                  strWikiPage = WikiType.incrementWiki(strWikiPage);
                  strTemp = strWikiPage;
                  objNew.setAttr("display", strTemp);
               }
               objNew.setAttr("name", strTitle); //OK
               objNew.setAttr("type", "File");
               objNew.setTag("entry");
               //Save in the entry list, in the order of occurrence:
               if (objWikiFirst == null) {
                  objWikiFirst = objNew;
                  objNew.setParentObject(objOut);
                  objWikiLast = objWikiFirst;
               } else {
                  objWikiLast.setNextObject(objNew);
                  objWikiLast = objNew;
                  objNew.setParentObject(objOut);
               }
               //Not linked to objOut yet.
            } else if (strTitle.startsWith("loi-teams/attach/")) {
               //objNew.setAttr("display", glbl.fixHTML(strTitle.substring(17)));
               strAttachment = WikiType.incrementWiki(strAttachment);
               strTemp = strAttachment;
               objNew.setAttr("display", strTemp);
               objNew.setAttr("name", strTitle); //OK
               objNew.setAttr("type", "File");
               
               //Save in the attachment list, in the order of occurrence:
               if (objAttachFirst == null) {
                  objAttachFirst = objNew;
                  objNew.setParentObject(objOut);
                  objAttachLast = objAttachFirst;
               } else {
                  objAttachLast.setNextObject(objNew);
                  objAttachLast = objNew;
                  objNew.setParentObject(objOut);
               }
               //Not linked to obj out yet.
            } else {
               if (strTitle.startsWith("loi-teams/")) {
                  objNew.setAttr("display",
                                 glbl.fixHTML(strTitle.substring(10)));
               } else {
                  objNew.setAttr("display", glbl.fixHTML(strTitle));
               }
               
               int loc = strTitle.lastIndexOf("/"); //Note must be "/" not "\"
               if (loc >= 0) {
                  strTitle =
                        strTitle.substring(0, loc + 1) + glbl.fixName(strTitle.substring(loc +
                                                                                       1)); //OK
               }
               objNew.setAttr("name", strTitle); //OK
               objNew.setAttr("type", "File");
               objOut.addChildObject(objNew, true);
               objFileLast = objNew;
            }
            objNew.setAttr("dir", "false");
            objNew.setAttr("extract", "true");
         }
         objNextFile = objNextFile.getNextObject();
      }
      //If there are any wiki entries, link them in BEFORE the children:
      XMLObject objChild = objOut.getChildObject();
      if (objWikiFirst != null) {
         if (objChild == null) {
            //There were no named files:
            objOut.setChildObject(objWikiFirst);
         } else if (objChild.getTag().equals("text")) {
            //Insert after the description:
            objWikiLast.setNextObject(objChild.getNextObject());
            objChild.setNextObject(objWikiFirst);
         } else { //children, no description
            objWikiLast.setNextObject(objChild);
            objOut.setChildObject(objWikiFirst);
         }
      }
      //If there are any anonymous attachments, link them in AFTER the others:
      if (objFileLast != null) {
         //There were named files; append these:
         objFileLast.setNextObject(objAttachFirst);
      } else if (objWikiLast != null) {
         //There were no named files, but there were wiki pages; append these:
         objWikiLast.setNextObject(objAttachFirst);
      } else if (objChild != null) {
         //There were neither wiki pages, nor named files, but a description:
         objAttachLast.setNextObject(objChild.getNextObject());
         objChild.setNextObject(objAttachFirst);
      } else { //no children
         objOut.setChildObject(objAttachFirst);
      }

   } //attachWikiFiles

   private String getDisplayTitle(String xmlTitle) {
      //Return our simple section title for the ugly one found in the XML.
      int i = 0;
      boolean found = false;
      String str = "";

      while (!found && (i < givenTitles.length)) {
         if (xmlTitle.equals(givenTitles[i]))
            found = true;
         else
            i++;
      }
      if (found)
         return displayTitles[i];
      else if (xmlTitle.endsWith(".label")) {
         str = xmlTitle.substring(0, xmlTitle.length() - 6);
         if (xmlTitle.startsWith("content.")) {
            str = str.substring(8);
         } else if (xmlTitle.startsWith("resources.")) {
            str = str.substring(10);
         } else if (xmlTitle.startsWith("groups.")) {
            str = str.substring(7);
         } else if (xmlTitle.startsWith("course_tools_area.")) {
            str = str.substring(18);
         }
         return str;
      }
      return UNKNOWN;
   } //getDisplayTitle

   private void addUploads(XMLObject objSection) {
      //Scan for course and group uploads, making new sections for each:
      XMLObject objNextRes = null;
      String strType = null;
      String strID = null;
      XMLObject objUploads = null;
      XMLObject objLastSection = objSection;

      objNextRes =
            glbl.accRes.getXMLObject().getChildObject(); //first resource

      while (objNextRes != null) {
         strType = objNextRes.getAttr("type");
         if (strType.equals(BB_COURSEUPLOAD)) {
            strID = objNextRes.getAttr("identifier");
            objUploads = new XMLObject("uploads", XMLObject.ELEMENT);
            objUploads.setAttr("display", "Course Uploads");
            objUploads.setAttr("name", "Course_Uploads");//OK
            objUploads.setAttr("id", strID);
            objUploads.setAttr("dir", "true");
            objUploads.setAttr("type", "Folder");
            objUploads.setAttr("extract", "true");
            objUploads.setAttr("created", "---");
            objUploads.setAttr("modified", "---");
            //attachContentFiles(strID, objUploads);
            attachUploadFiles(strID, objUploads, true); //true => course
            objLastSection.setNextObject(objUploads);
            objLastSection = objUploads;
         } else if (strType.equals(BB_GROUPUPLOAD)) {
            strID = objNextRes.getAttr("identifier");
            objUploads = new XMLObject("uploads", XMLObject.ELEMENT);
            objUploads.setAttr("display", "Group Uploads");
            objUploads.setAttr("name", "Group_Uploads");//OK
            objUploads.setAttr("id", strID);
            objUploads.setAttr("dir", "true");
            objUploads.setAttr("type", "Folder");
            objUploads.setAttr("extract", "true");
            objUploads.setAttr("created", "---");
            objUploads.setAttr("modified", "---");
            //attachContentFiles(strID, objUploads);
            attachUploadFiles(strID, objUploads, false);
            objLastSection.setNextObject(objUploads);
            objLastSection = objUploads;
         }
         objNextRes = objNextRes.getNextObject();
      }
   } //addUploads

   private void attachUploadFiles(String strID, XMLObject objUploads,
                                  boolean bIsCourse) {
      //Using the file list inside the uploads section resource, attach nodes for all files.
      XMLObject objNextUpload = null;
      XMLObject objNew = null;
      XMLAccessor accSection = null;
      XMLAccessor accUpload = new XMLAccessor();
      String strXML = null;
      String strTemp = null;
      String strName = null;
      String strDisplay = null;
      int iLoc = -1;

      //Get its resource and fill out the details:
      strXML = glbl.getArchiveFileText(strID + ".dat");
      if ((strXML != null) && (strXML.length() > 0)) {
         accSection = glbl.getAccessor(strXML);

         //Cycle through the uploads, if any. This code assumes that certain
         //pieces of information always exist
         if (bIsCourse)
            objNextUpload = accSection.getObject("courseuploads/upload");
         else
            objNextUpload = accSection.getObject("groupuploads/upload");

         while (objNextUpload != null) {
            accUpload.setXMLObject(objNextUpload);
            //For uploads, we construct the path from the file prefix (the upload ID)
            //and the file name:
            strName =
                  accUpload.getAttr("upload", "id", UNKNOWN).trim(); //prefix
            strTemp =
                  accUpload.getField("upload/file/name", UNKNOWN); //filename
            //It appears that Blackboard hexes names with spaces, so unhex, then fix spaces:
            if (strTemp.startsWith("@") || strTemp.startsWith("!")) {
               strTemp = glbl.unhexName(strTemp, true);
            }
            strTemp = glbl.fixName(strTemp);

            strName = strName + "/" + strTemp;
            strDisplay = glbl.fixHTML(strTemp);

            //Make and initialize a new subnode for this file:
            objNew = new XMLObject("file", XMLObject.EMPTY);
            objNew.setAttr("type", "File");
            objNew.setAttr("display", strDisplay);
            objNew.setAttr("name", strName);//OK
            objNew.setAttr("dir", "false");
            objNew.setAttr("extract", "true");
            objNew.setAttr("id", strID);
            strTemp = accUpload.getAttr("upload/dates/created", "value", "");
            objNew.setAttr("created", DateUtils.fixDate(strTemp));
            strTemp = accUpload.getAttr("upload/dates/updated", "value", "");
            objNew.setAttr("modified", DateUtils.fixDate(strTemp));
            //Append it to the other child nodes:
            objUploads.addChildObject(objNew, true);

            objNextUpload = objNextUpload.getNextObject();
         }
      }
   } //attachUploadFiles

}//BbInput
