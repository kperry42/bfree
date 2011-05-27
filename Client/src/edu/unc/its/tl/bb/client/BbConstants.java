package edu.unc.its.tl.bb.client;

import java.awt.Color;

public interface BbConstants {

   public final Color BG_COLOR = new Color(214, 214, 214); //#d6d6d6
   public final Color PANEL_COLOR = new Color(238, 238, 238); //#eeeeee

   public final String TEMP_FOLDER = "_bFreeTemp_";

   //Version:
   public final String VERSION = "2.0.3";
   public final String NEWS =
      "<html><font size=\"4\" face=\"Times New Roman\"><b>Look for these new features!:</b><br><br>" +
      "* bFree has an all-new, more convenient interface.<br><br>" +
      "* The search feature now searches within text entries.<br><br>" +
      "* Forums and announcements can be previewed and extracted.<br><br>" +
      "* Sections now include Course Uploads and Group Uploads.<br><br>" +
      "* Wiki entries are now simplified and conveniently arranged.<br><br>" +
      "(Please review the Help tab for these and other changes.)</font></html>";
      
   //Archive types:
   public final int UNKNOWN_ARCHIVE = 0;
   public final int BLACKBOARD = 1;
   public final int SAKAI = 2;

   //Some tab indices:
   public final int CONTENTS_TAB = 0;
   public final int MESSAGE_TAB = 1;
   public final int HELP_TAB = 2;
   public final int COPYRIGHT_TAB = 3;
   public final int LICENSE_TAB = 4;

   //Message types:
   public final int INFO_MSG = 0;
   public final int WARNING_MSG = 1;
   public final int ERROR_MSG = 2;
   public final int TEXT_MSG = 3; //for titles, spacers, etc.

   //Search directions:
   public final int FIND_1ST = 0;
   public final int FIND_NEXT = 1;
   public final int FIND_PREV = -1;
   public final int FIND_LAST = 2;

   public final int MAX_PATH = 80; //max length of selection pat in message

   //Preview display types:
   public final int SHOW_TEXT = 0;
   public final int SHOW_HTML = 1;
   public final int SHOW_LINK = 2;

   //Folders holding our interface contents:
   public final String APP_IMAGE_BASE = "images/"; //app's own images
   public static final String APP_TEXT_BASE =
      "texts/"; //app's & site's texts and templates
   public static final String SITE_IMAGE_BASE =
      "texts/images/"; //site's images
   //NOTE The website images are in texts/images.

   //Paths into the parsed XML:
   public final String ORGANIZATION_PATH =
      "manifest/organizations/organization";
   public final String RESOURCES_PATH = "manifest/resources";

   //Content types:
   //NOTE: These are the ones we will output, for now:
   public final String BB_DOCUMENT = "resource/x-bb-document";
   public final String BB_EXTERNALLINK = "resource/x-bb-externallink";
   public final String BB_ASSIGNMENT = "resource/x-bb-assignment";
   public final String BB_COURSELINK = "resource/x-bb-courselink";
   public final String BB_TESTLINK = "resource/x-bb-asmt-test-link";
   public final String BB_FOLDER = "resource/x-bb-folder";
   public final String BB_WIKI = "resource/x-lobj-teams-group";
   public final String BB_STAFF = "resource/x-bb-staffinfo";
   public final String BB_QUIZ = "resource/x-bb-asmt-survey-link";
   public final String BB_TEST = "resource/x-bb-asmt-test-link";
   public final String BB_SYLLABUS = "resource/x-bb-syllabus";
   public final String BB_SETTING = "course/x-bb-coursesetting";
   public final String BB_DISCUSSION = "resource/x-bb-discussionboard";
   public final String BB_ANNOUNCEMENT = "resource/x-bb-announcement"; 
   //XML search routines demand that "/" in an attribute be given as "%2F":
   public final String BB_DISCUSSION_ALT = "resource%2Fx-bb-discussionboard"; 
   public final String BB_ANNOUNCEMENT_ALT = "resource%2Fx-bb-announcement"; 

   public final String DOCUMENT = "doc";
   public final String EXTERNALLINK = "link";
   public final String ASSIGNMENT = "asgn";
   public final String COURSELINK = "clink";
   public final String TESTLINK = "tlink";
   public final String FOLDER = "fldr";
   public final String WIKI = "wiki";
   public final String QUIZ = "quiz";
   public final String TEST = "test";
   public final String SYLLABUS = "syllabus";

   public final String BB_COURSEUPLOAD = "course/x-bb-courseupload";
   public final String BB_GROUPUPLOAD = "course/x-bb-groupupload";

   public final String[] givenContents =
   { "resource/x-bb-document", "resource/x-bb-externallink",
     "resource/x-bb-assignment", "resource/x-bb-courselink",
     "resource/x-bb-testlink", "resource/x-lobj-teams-group" };

   //public final String[] displayContents =
   //{ "doc", "link", "asgn", "clink", "tlink" };
   public final String UNKNOWN = "???";

   //Titles of major course components, as found in the XML:
   public final String[] givenTitles =
   { "COURSE_DEFAULT.Announcements.APPLICATION.label",
     "COURSE_DEFAULT.Assignments.CONTENT_LINK.label",
     "COURSE_DEFAULT.CourseInformation.CONTENT_LINK.label",
     "COURSE_DEFAULT.CourseDocuments.CONTENT_LINK.label",
     "COURSE_DEFAULT.Communication.APPLICATION.label",
     "COURSE_DEFAULT.DiscussionBoard.APPLICATION.label",
     "COURSE_DEFAULT.ExternalLinks.CONTENT_LINK.label",
     "COURSE_DEFAULT.StaffInformation.STAFF.label",
     "COURSE_DEFAULT.Tools.APPLICATION.label",
     "course_communications.Communications.label" };

   //Strings for titles of major course components, as displayed in our tree:
   public final String[] displayTitles =
   { "Announcements", "Assignments", "Course Information", "Course Documents",
     "Communication", "Discussion Board", "External Links",
     "Staff Information", "Tools", "Communications" };

   //"Hidden" pages have no title!:
   public final String NO_TITLE = "(Missing Title)";

   //The CONTENT/BODY/TEXT may contain internal links that need to be
   //skipped; that part begins with this string:
   public final String BB_TO_SKIP = "<!--BB"; //<!--BB
   public final String BB_TO_SKIP_TOO = "&lt;!--BB";

   //There are file references that must be resolved:
   public final String EMBEDDED_FILE_PREFIX = "@X@EmbeddedFile.location@X@";
   public final String EMBEDDED_STUB_PREFIX =
      "@X@EmbeddedFile.requestUrlStub@X@";
   public final String WIKI_IMAGE_PREFIX = "?cmd=GetImage&amp;systemId=";

   //Various constants:
   public final String CRUMB_DELIM = " -> ";
   public final String CRUMB_ETC = "...";
   public final int CRUMB_SIZE = 80;

   //The names of the templates:
   public final String CSS_TEMPLATE = "bFree.css";
   public final String INDEX_TEMPLATE = "index.html";
   public final String PAGE_TEMPLATE = "bFree.html";

   //Template content replacement codes:
   public final String COURSE = "$course$";
   public final String MENU = "$menu$";
   public final String TRAIL =
      "$trail$"; //sometmes known as bread crumb (see above)
   public final String DESCR = "$description$";
   public final String LINKS = "$links$";
   public final String HEAD = "$head$";
   //Template Back button replacement codes:
   public final String BACK = "$back$";
   public final String TITLE = "$title$";
   public final String BACKURL = "$backurl$";

}//BbConstants
