package edu.unc.its.tl.bb.client;

import edu.unc.its.tl.bb.TreeTableModelAdapter;

import java.util.ArrayList;

import javax.swing.tree.TreePath;

import org.dvm.java.xml.XMLObject;

import java.awt.Toolkit;

import javax.swing.JTree;

public final class Searcher implements BbConstants {
   //An object to maintain state for an incremental depth-first tree search.
   //It allows the program to step through all nodes in the content tree.

   private Globals glbl = Globals.getInstance();

   //The list and current node's tree object:
   private XMLObject currObject = null; //current find result
   private XMLObject selObject = null; //current user (or find) selection

   private ArrayList aNodes = null;
   private int iCurrNode = -1;

   //We'll need to build various paths to selected nodes:
   //private boolean havePath = false;
   private Object[] parentPath = new Object[0];

   //Flags controlling the search:
   private boolean searchTitles = true; //search titles
   private boolean searchTexts = true; //search text of descr, staff info
   private boolean stayTidy =
      false; //close last selection before selecting new

   public Searcher(XMLObject obj) {
      aNodes = new ArrayList();

      //setup(obj.getChildObject());
      setup(obj);

      if (aNodes.size() > 0)
         iCurrNode = 0;
      else
         iCurrNode = -1;
   } //constructor

   private void setup(XMLObject obj) {
      //Recursively create a list of all nodes in depth-first order:

      if (obj != null) {
         aNodes.add(obj);
         //Process children next:
         setup(obj.getChildObject());
         //Process siblings:
         setup(obj.getNextObject());
      }
   } //setup

   public void restart() {
      //Set the cursor back to the beginning:
      iCurrNode = -1;
      currObject = null;
      selObject = null;
      glbl.lastPath = null;
   } //restart

   public XMLObject nextObject() {
      //Increment the cursor, then return the next object that the cursor points to now..
      iCurrNode++;
      if (iCurrNode < aNodes.size()) {
         currObject = (XMLObject)aNodes.get(iCurrNode);
         selObject = currObject;
      } else {
         currObject = null;
         selObject = null;
      }
      return currObject;
   } //nextObject

   public XMLObject prevObject() {
      //Descrement the cursor, then return the object that the cursor points to.
      iCurrNode--;
      if (iCurrNode >= 0) {
         currObject = (XMLObject)aNodes.get(iCurrNode);
         selObject = currObject;
      } else {
         currObject = null;
         selObject = null;
      }
      return currObject;
   } //prevObject

   public boolean hasNext() {
      return (iCurrNode < aNodes.size() - 1);
   } //hasNext

   public boolean hasPrev() {
      return (iCurrNode > 0);
   } //hasPrev

   public Object[] getParentPath() {
      //Construct and return a tree path, backward, from the current tree object's
      //enclosing folder up to the tree's root object. This will be used to make
      //a TreePath object for selecting the current node's enclosing folder.
      XMLObject objNext;
      int levels = 0;
      Object[] nodes = new Object[0];

      if (currObject != null) {
         objNext = currObject;
         //Count how many levesl need to be stored:
         while (objNext != null) {
            levels++;
            objNext = objNext.getParentObject();
         }
         levels = levels - 1; //skip found folder

         //NOTE: We don't include the found object in the tree path because
         //we want to expand its enclosing folder, not the object.

         //Now fill the array, bottom to top:
         nodes = new Object[levels];
         objNext = currObject.getParentObject(); //above the found folder
         while (objNext != null) {
            nodes[levels - 1] = objNext; //0-based index
            objNext = objNext.getParentObject();
            levels--;
         }
         //havePath = true;
      }
      parentPath = nodes;
      return nodes;
   } //getParentPath

   public void searchTree(String str, int iDirection) {
      //Search the tree for occurrences of the given string (ignoring lettercase),
      //and fully expand the paths leading to any occurrences.
      //Close the last found path before continuing, if stayTidy is true.
      String strToFind = str.toLowerCase();
      boolean bFound = false;
      XMLObject objFound = null;
      Object[] nodes = null;
      TreePath path = null;
      TreeTableModelAdapter mod = null;

      if (iDirection == FIND_1ST)
         restart();
      else
         alignToSelection();

      mod = (TreeTableModelAdapter)glbl.tabExport.getModel();

      if (stayTidy) {
         //Close (collapse) the last found path before finding the next instance:
         if (glbl.lastPath != null)
            collapseAll(glbl.tabExport.getTree(), glbl.lastPath);
      }
      if ((iDirection == FIND_NEXT) || (iDirection == FIND_1ST)) {
         while (hasNext() && !bFound) {
            objFound = nextObject();
            bFound = containsText(objFound, strToFind);
         }
      } else if (iDirection == FIND_PREV) {
         while (hasPrev() && !bFound) {
            objFound = prevObject();
            bFound = containsText(objFound, strToFind);
         }
      } else if (iDirection == FIND_LAST) {
         if (aNodes.size() > 0) {
            iCurrNode = aNodes.size();
            while (hasPrev() && !bFound) {
               objFound = prevObject();
               bFound = containsText(objFound, strToFind);
            }
         }
      }
      glbl.bWasRoot = false;
      if (bFound) {
         nodes = getParentPath();
         if (nodes.length > 0) {
            path = new TreePath(nodes);
            glbl.tabExport.getTree().expandPath(path); //to enclosing folder
            glbl.lastPath = path;
            glbl.tabExport.getTree().setSelectionPath(path.pathByAddingChild(objFound)); //found title
         } else { //just the root
            glbl.tabExport.getTree().setSelectionPath(new TreePath(objFound)); //found title
            glbl.lastPath = null;
            glbl.bWasRoot = true;
         }
      } else {
         Toolkit.getDefaultToolkit().beep();
         glbl.lastPath = null;
      }
   } //searchTree

   public void setUserSelection(XMLObject obj) {
      selObject = obj;
   } //setUserSelection

   private void alignToSelection() {
      //Align the search to the currently-selected item (if any),
      //so that the search can start there rather than at the last find.
      //Need to rebuild path, etc.
      int nextNode = 0;
      Object[] nodes = null;
      TreePath path = null;

      if ((selObject != currObject) && (selObject != null)) {
         while ((nextNode < aNodes.size()) &&
                (selObject != aNodes.get(nextNode))) {
            nextNode++;
         }
         //It must be there somewhere!
         iCurrNode = nextNode;
         currObject = (XMLObject)aNodes.get(iCurrNode);
         nodes = getParentPath();
         if (nodes.length > 0) {
            path = new TreePath(nodes);
            glbl.lastPath = path;
         } else {
            glbl.lastPath = null;
         }
      }
   } //alignToSelection

   private boolean containsText(XMLObject obj, String strToFind) {
      boolean bFound = false;
      String strType;
      int loc;
      String strToSearch;

      if (searchTitles) {
         //Search the displayed title:
         bFound =  (obj.getAttr("display").toLowerCase().contains(strToFind));
         glbl.selStart = -1; //title text not to be selected
         glbl.selEnd = -1;
      }
      if (!bFound && searchTexts) {
         strType = obj.getAttr("content", UNKNOWN);
         if (obj.getAttr("dir").equals("true")) {
            //A folder can have a description text, staff info or link in its file:
            if (strType.equals("Text")) {
               strToSearch = DescriptorType.fetchDescriptor(obj, true);
            } else if (strType.equals("Link")) {
               strToSearch = LinkType.fetchURL(obj);
            } else if (strType.equals("Info")) {
               strToSearch = InfoType.fetchInfo(obj);
            } else {
               strToSearch = "";
            }
         } else if (obj.getTag().equals("entry")) {
            //Wiki entries are text files:
            strToSearch = WikiType.fetchWiki(obj);
         } else if (strType.equals("Forum")) {
            strToSearch = ForumType.fetchForum(obj, false, true);
         } else if (strType.equals("Archive")) {
            strToSearch = ForumType.fetchForum(obj, false, false);
         } else if (strType.equals("Announce")) {
            strToSearch = AnnounceType.fetchAnnouncement(obj);
         } else {
            strToSearch = "";
         }
         loc = strToSearch.toLowerCase().indexOf(strToFind);
         bFound = (loc >= 0);
         if (bFound) {
            glbl.selStart = loc;
            glbl.selEnd = loc + strToFind.length();
         } else {
            glbl.selStart = -1;
            glbl.selEnd = -1;
         }
      }
      return bFound;
   } //containsText

   public void collapseAll(JTree tree, TreePath path) {
      //Collapse the path from the bottom up.
      TreePath aPath = path;
      while (aPath.getPathCount() > 1) {
         tree.collapsePath(aPath);
         aPath = aPath.getParentPath();
      }
   } //collapseAll

   protected void finalize() throws Throwable {
      try {
         aNodes = null;
         parentPath = null;
      } finally {
         super.finalize();
      }
   } //finalize

   public void setSearchTitles(boolean bTitles) {
      this.searchTitles = bTitles;
   }

   public boolean isSearchTitles() {
      return searchTitles;
   }

   public void setSearchTexts(boolean bTexts) {
      this.searchTexts = bTexts;
   }

   public boolean isSearchTexts() {
      return searchTexts;
   }

   public void setStayTidy(boolean bTidy) {
      this.stayTidy = bTidy;
   }

   public boolean isStayTidy() {
      return stayTidy;
   }
} //SearchStatus
