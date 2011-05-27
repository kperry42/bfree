package edu.unc.its.tl.bb.client;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ScrollPaneConstants;

public final class MessagePanel extends JPanel implements BbConstants {
//This panel logs messages of various severities.
   private JScrollPane        scrMsg;
   private JTextArea          txtMsg;
   private Insets             insMsg;
   private Unzipper              app;

   public MessagePanel(Unzipper app) {
      super();
      this.app = app;
      
      init();
   }//constructor
   
   private void init() {
      //Set up the panel:
      txtMsg = new JTextArea();
      insMsg = new Insets(8,8,8,8);
      txtMsg.setMargin(insMsg);
      txtMsg.setEditable(false);
      txtMsg.setFont(new Font("Dialog",Font.PLAIN,12));
      txtMsg.setBackground(PANEL_COLOR);
      scrMsg = new JScrollPane(txtMsg, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
                                ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
      this.setLayout(new BorderLayout());
      this.add(scrMsg,BorderLayout.CENTER);
   }//init
   
   public void addMessage(String msg, int level) {
      switch(level) {
         case INFO_MSG:
            txtMsg.append("---");
            break;
         case WARNING_MSG:
            txtMsg.append("!--");
            break;
         case ERROR_MSG:
            txtMsg.append("E--");
            break;
         case TEXT_MSG:
            //No prefix.
            break;
      }
      txtMsg.append(msg+"\n");
   }//addMessage
   
   public void clearMessages(String title) {
      txtMsg.setText(title+"\n\n");
   }//clearMessages
   
}//MessagePanel
