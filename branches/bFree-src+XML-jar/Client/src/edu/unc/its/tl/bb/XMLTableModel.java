package edu.unc.its.tl.bb;

import org.dvm.java.xml.XMLObject;

public class XMLTableModel extends AbstractTreeTableModel implements TreeTableModel {
   //Implements the TreeTableModel for an XMLObject (parse tree).

   // Names of the columns.
   protected static String[] cNames =
   { "Title", "Type", "Extract?", "Created", "Modified" };

   // Types of the columns.
   protected static Class[] cTypes =
   { TreeTableModel.class, String.class, Boolean.class, String.class,
     String.class };

   public XMLTableModel(XMLObject objTT) {
      super(objTT);
      setChildrenOnOrOff(objTT, true);
   } //constructor

   public int getChildCount(Object node) {
      if (node == null)
         return 0;

      int iCount = 0;
      XMLObject objNext = null;

      objNext = ((XMLObject)node).getChildObject();

      while (objNext != null) {
         iCount++;
         objNext = objNext.getNextObject();
      }
      return iCount;
   } //getChildCount

   public Object getChild(Object node, int i) {
      if (node == null)
         return null;

      int iCount = 0;
      XMLObject objNext = null;

      objNext = ((XMLObject)node).getChildObject();

      while ((objNext != null) && (iCount < i)) {
         iCount++;
         objNext = objNext.getNextObject();
      }
      if ((iCount == i) && (objNext != null))
         return objNext;
      else
         return null;
   } //getChild

   // The superclass's implementation would work, but this is more efficient.

   public boolean isLeaf(Object node) {
      String attr = ((XMLObject)node).getAttr("dir");
      if (attr == null)
         return false;
      else
         return attr.equals("false");
   } //isLeaf

   //
   //  The TreeTableNode interface.
   //

   public int getColumnCount() {
      return cNames.length;
   }

   public String getColumnName(int column) {
      return cNames[column];
   }

   public Class getColumnClass(int column) {
      return cTypes[column];
   }

   public Object getValueAt(Object node, int column) {
      String strTemp = "";
      
      try {
         switch (column) {
         case 0:
            return ((XMLObject)node).getAttr("display");
         case 1:
            return ((XMLObject)node).getTag();
            /*
            strTemp = ((XMLObject)node).getAttr("type");
            if ((strTemp == null) || strTemp.equals(""))
               return "Unknown";
            else
               return strTemp;
            */
         case 2:
            strTemp = ((XMLObject)node).getAttr("extract");
            if (strTemp.equals("true"))
               return new Boolean(true);
            else
               return new Boolean(false);
         case 3:
            strTemp = ((XMLObject)node).getAttr("created");
            return strTemp;
         case 4:
            strTemp = ((XMLObject)node).getAttr("modified");
            return strTemp;
         }
      } catch (SecurityException se) {
      }

      return null;
   } //getvalueAt

   public void setValueAt(Object aValue, Object node, int column) {
      boolean b = ((Boolean)aValue).booleanValue();

      if (column == 2) {
         ((XMLObject)node).setAttr("extract", "" + b);
         //If a lower-level item is selected for output, its parents
         //must also be selected for output:
         if (b)
            turnParentsOn(((XMLObject)node).getParentObject());
         //However this node is set, its children must be set the same:
         setChildrenOnOrOff(((XMLObject)node).getChildObject(), b);
      }
   } //setValueAt

   private void turnParentsOn(XMLObject obj) {
      if (obj == null)
         return;
      obj.setAttr("extract", "true");
      turnParentsOn(obj.getParentObject());
   } //turnParentsOn

   private void setChildrenOnOrOff(XMLObject obj, boolean b) {
      if (obj == null)
         return;
      obj.setAttr("extract", "" + b);
      if ((obj.getChildObject() == null) && obj.getAttr("dir").equals("true")) {
         //if (obj.getAttr("content","???").equals("???"))
         if (obj.getAttr("content","???").equals("Section") || obj.getAttr("content","???").equals("???"))
            obj.setAttr("extract", "false");
      } else {
         setChildrenOnOrOff(obj.getChildObject(), b);
      }
      setChildrenOnOrOff(obj.getNextObject(), b);
   } //setChildrenOnOrOff

} //XMLTableModel
