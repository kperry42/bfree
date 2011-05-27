package edu.unc.its.tl.bb.client;

import java.awt.*;

import java.awt.event.MouseListener;

import java.io.IOException;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

public final class PreviewPanel extends JPanel implements BbConstants,
                                                    HyperlinkListener {
   //Panel for previewing content -- text, HTML, and images.
   //Displayed below the outline panel in the Contents tab view.
   
   private Globals glbl = Globals.getInstance();
   private JScrollPane scrPrev;
   private JEditorPane txtPrev;
   private Insets insPrev;
   private String strPrev;
   private Unzipper app;

   public PreviewPanel(Unzipper app) {
      super();
      this.app = app;

      init();
   } //constructor

   private void init() {
      //Set up the preview panel:
      txtPrev =
            new JEditorPane("text/html; charset=UTF-8", "<html>Content previews appear here.</html>");
      insPrev = new Insets(4, 4, 4, 4);
      txtPrev.setMargin(insPrev);
      txtPrev.setEditable(false);
      txtPrev.addHyperlinkListener(this);
      scrPrev =
            new JScrollPane(txtPrev, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
                            ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
      this.setLayout(new BorderLayout());
      this.add(scrPrev, BorderLayout.CENTER);

   } //init
   
   public void addMouseListener(MouseListener ml) {
      txtPrev.addMouseListener(ml);
   }//addMouseListener
   
   private String lastURL = "";
   
   public boolean show(String str, int type) {
      boolean bOK = true;

      try {
         if (type == SHOW_TEXT) {
            txtPrev.setContentType("text/plain; charset=UTF-8");
            txtPrev.setText(str);
         } else if (type == SHOW_LINK) { //SHOW_LINK
            if (str.equals(lastURL)) {
               //Darned thing won't show same page twice in a row!
               //set to plain, then back to html
               txtPrev.setContentType("text/plain; charset=UTF-8");
               txtPrev.setText("Loading...");
            }
            txtPrev.setContentType("text/html; charset=UTF-8");
            txtPrev.setPage(str);
            lastURL = str;
         } else { //SHOW_HTML (literal text)
            txtPrev.setContentType("text/html; charset=UTF-8");
            txtPrev.setText(str);
         }
         if (glbl.selStart >= 0) {
            txtPrev.setCaretPosition(glbl.selStart);
            //I can't seem to find a way to select the text that was found.
            //txtPrev.getCaret().setVisible(true);
            //txtPrev.select(glbl.selStart,glbl.selEnd);
            /*
            txtPrev.setSelectionStart(glbl.selStart);
            txtPrev.setSelectionEnd(glbl.selEnd);
            txtPrev.setSelectionColor(Color.YELLOW);
            */
            //System.out.println("Selection");//TEMP
         } else {
            txtPrev.setCaretPosition(0);
            //System.out.println("Caret");//TEMP
         }
      } catch (Exception e) {
         bOK = false;
      }
      return bOK;
   } //show
   
   /*
   public JEditorPane getEditorPane() {
      return txtPrev;
   } //getEditorPane
   */
   
   public void hyperlinkUpdate(HyperlinkEvent he) {
      //Handle the clicks on links in the help text.
      if (he.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
         strPrev = he.getDescription();
         //Open it in the browser only if it is a URL that we sent in:
         if (strPrev.startsWith("$bFreeHTML$")) {
            openHelperApp(strPrev.substring(11));
         } else if (strPrev.startsWith("$bFreeTEXT$")) {
            openHelperApp(strPrev.substring(11));
         } else if (strPrev.startsWith("http://") ||
            strPrev.startsWith("HTTP://") ||
            strPrev.startsWith("https://") ||
            strPrev.startsWith("HTTPS://")) { //I changed my mind for now; open any
            openHelperApp(strPrev);
         }
      }
   } //hyperlinkUpdate

   public boolean openHelperApp(String strURL) {
      //Display the given URI in the user's preferred application.
      Runtime r = Runtime.getRuntime();
      Process p = null;
      boolean bCanShow = true;
      String[] params = new String[2];
      
      //if (strURL.contains(",_")) System.out.println("openHelperApp "+strURL);//TEMP
      
      if (bFree.strOS.equals(bFree.kTypeMacintosh)) {
         params[0] = "open";
         params[1] = strURL;
         
         try {
            p = r.exec(params);
         } catch (IOException e) {
            show("Error: "+e, SHOW_TEXT);
            bCanShow = false;
         }
      } else {
         try {
            p = r.exec("rundll32 url.dll,FileProtocolHandler " +
                                      strURL);
            //Also opens correct app for (e.g.) file://C:\blah.doc
            //(use \\ for \ within string)
         } catch (IOException e) {
            show("Error: "+e, SHOW_TEXT);
            bCanShow = false;
         }
      }
      return bCanShow;
   } //openHelperApp

   private String unixEncode(String str) {
      //Encode spaces by prefixing them with a back slash.
      StringBuffer buf = new StringBuffer(str.length() * 2);
      char ch;

      for (int i = 0; i < str.length(); i++) {
         ch = str.charAt(i);
         if (ch == ' ')
            buf.append("%20");
         else if (ch == '$')
            buf.append("\\$");
         else
            buf.append(ch);
      }
      return buf.toString();
   } //unixEncode

}//PreviewPanel
