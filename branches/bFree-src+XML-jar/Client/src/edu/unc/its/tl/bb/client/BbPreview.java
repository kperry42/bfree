package edu.unc.its.tl.bb.client;

import java.io.IOException;

import javax.swing.JEditorPane;

public final class BbPreview implements BbConstants {
   //Handles two types of previews:
   //   * display of a text or file in this application's display area, or
   //   * to be opened in a document's default application (browser, Word, etc.)

   private PreviewPanel display = null;

   public BbPreview(PreviewPanel pnl) {
      this.display = pnl;
   } //constructor

   public boolean showPreview(String strTextOrPath, boolean bIsFile) {
      //If the given text or path can be previewed, do it, returning true;
      //otherwise return false. The alternative to this can be to open
      //the indicated document in its appropriate application.
      boolean bCanShow = true;

      strTextOrPath = strTextOrPath.replace('\\', '/');

      if (bIsFile) {
         //This is a file name (the complete path):
         if (strTextOrPath.endsWith(".html") ||
             strTextOrPath.endsWith(".htm") ||
             strTextOrPath.endsWith(".HTML") ||
             strTextOrPath.endsWith(".HTM")) {
            displayHTMLFile(strTextOrPath);
         } else if (strTextOrPath.endsWith(".txt") ||
                    strTextOrPath.endsWith(".text") ||
                    strTextOrPath.endsWith(".TXT") ||
                    strTextOrPath.endsWith(".TEXT")) {
            displayTextFile(strTextOrPath);
         } else if (strTextOrPath.endsWith(".gif") ||
                    strTextOrPath.endsWith(".GIF") ||
                    strTextOrPath.endsWith(".jpg") ||
                    strTextOrPath.endsWith(".jpeg") ||
                    strTextOrPath.endsWith(".JPG") ||
                    strTextOrPath.endsWith(".JPEG") ||
                    strTextOrPath.endsWith(".tiff") ||
                    strTextOrPath.endsWith(".TIFF") ||
                    strTextOrPath.endsWith(".png") ||
                    strTextOrPath.endsWith(".PNG")) {
            displayImageFile(strTextOrPath); // (will be wrapped)
         } else {
            bCanShow = false;
            displayHTML("\"" + strTextOrPath +
                        "\" <b>might</b> be viewable in a separate application. <h3><a href=\"$bFreeTEXT$" +
                        strTextOrPath + "\">View.</a></h3>");
         }
      } else {
         //This is the actual text, containing a link or html or plain text:
         if (strTextOrPath.startsWith("http:") ||
             strTextOrPath.startsWith("HTTP:") ||
             strTextOrPath.startsWith("https:") || //actually, https won't display in our window
             strTextOrPath.startsWith("HTTPS:")) {
            //Check for URLs to non-HTML locations!
            if (strTextOrPath.endsWith("/") ||
                strTextOrPath.contains(".html") ||
                strTextOrPath.contains(".htm") ||
                strTextOrPath.contains(".HTML") ||
                strTextOrPath.contains(".HTM") ||
               strTextOrPath.contains(".edu") ||
               strTextOrPath.contains(".com") ||
               strTextOrPath.contains(".org") ||
               strTextOrPath.contains(".EDU") ||
               strTextOrPath.contains(".COM") ||
               strTextOrPath.contains(".ORG") ||
                strTextOrPath.contains(".SHTML") ||
                strTextOrPath.contains(".shtml") ||
                strTextOrPath.contains(".PHP") ||
                strTextOrPath.contains(".php") ||
                strTextOrPath.contains(".ASP") ||
                strTextOrPath.contains(".asp") ||
                strTextOrPath.contains(".JSP") ||
                strTextOrPath.contains(".jsp")) {
               //Safe to open in our display:
               displayLink(strTextOrPath);
            } else {
               //NOTE: look for non-"." 3 or 4 from end. if so, display

               //No, it's a document of some kind: //??? do this, or return false???
               displayHTML("\"" + strTextOrPath +
                           "\" <b>might</b> be viewable in a separate application. <h3><a href=\"$bFreeHTML$" +
                           strTextOrPath + "\">View.</a></h3>");
            }
         } else if (strTextOrPath.startsWith("<html>") ||
                    strTextOrPath.startsWith("<HTML>")) {
            displayHTML(strTextOrPath);
         } else if (strTextOrPath.startsWith("<?xml") ||
                    strTextOrPath.startsWith("<?XML") ||
                    strTextOrPath.startsWith("<!")) {
            //Could be an HTML file that starts with <?xml or <!DOCTYPE, etc.:
            int loc = strTextOrPath.indexOf("<HTML>");

            if (loc == -1)
               loc = strTextOrPath.indexOf("<html>");
            if (loc != -1)
               //HTML:
               displayHTML(strTextOrPath.substring(loc)); //trim <?xml or <! ...
            else
               //No, perhaps plain text, XML, etc.:
               displayText(strTextOrPath);
         } else {
            //Apparently plain text:
            displayText(strTextOrPath);
         }
      }
      return bCanShow;
   } //showPreview


   private void displayHTMLFile(String strPath) {
      //Display the given HTML from a file:
      if (!display.show("file:///" + strPath, SHOW_LINK))
         display.show("Cannot preview the link \"" + strPath +
                      "\"; it might be broken.", SHOW_TEXT);
   } //displayHTMLFile

   private void displayTextFile(String strPath) {
      //Display the file by wrapping it in a file URL.
      if (!display.show("file:///" + strPath, SHOW_LINK))
         display.show("Cannot preview the file \"" + strPath + "\"",
                      SHOW_TEXT);
   } //displayTextFile

   private void displayImageFile(String strPath) {
      //Display the image file by wrapping it in HTML:
      display.show("<html><img src=\"file:///" + strPath + "\"><html>",
                   SHOW_HTML);
   } //displayImageFile

   private void displayLink(String strText) {
      //Display the link directly:
      if (strText.length() == 0)
         display.show("(This link was empty.)", SHOW_TEXT);
      else {
         if (!display.show(strText, SHOW_LINK))
            //display.show("Cannot preview the link \"" + strText + "\"",
            //             SHOW_TEXT);
             displayHTML("\"" + strText +
                         "\" <b>might</b> be viewable in a separate application. <h3><a href=\"$bFreeHTML$" +
                         strText + "\">View.</a></h3>");
      }
   } //displayLink

   private void displayHTML(String strText) {
      display.show(strText, SHOW_HTML);
   } //displayHTML

   private void displayText(String strText) {
      display.show(strText, SHOW_HTML);
   } //displayText

}//BbPreview
