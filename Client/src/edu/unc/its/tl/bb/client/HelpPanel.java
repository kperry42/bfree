package edu.unc.its.tl.bb.client;

import java.awt.*;
import javax.swing.*;
import javax.swing.event.*;

public final class HelpPanel extends JPanel implements HyperlinkListener {
//A static help/description panel; its content is loaded once.

	private JScrollPane        scrHelp;
	private JEditorPane        txtHelp;
	private Insets             insHelp;
	private String					strHelp;
	private Unzipper			      app;
   private String             strFileName;

	public HelpPanel(Unzipper app, String name) {
		super();
		this.app = app;
      this.strFileName = name;
		
		init();
	}//constructor
	
	private void init() {
	   //Set up the help/description panel:
	   txtHelp = new JEditorPane("text/html", "<html>Loading...</html>");
	   insHelp = new Insets(4,4,4,4);
	   txtHelp.setMargin(insHelp);
      txtHelp.setEditable(false);
	   txtHelp.addHyperlinkListener(this);
	   scrHelp = new JScrollPane(txtHelp, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS,
	                             ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
	   this.setLayout(new BorderLayout());
	   this.add(scrHelp,BorderLayout.CENTER);
		
		//The HTML file is on the application's resources:
		strHelp = this.app.getResourceText(this.strFileName);
		txtHelp.setText(strHelp);
	}//init
   
	public void hyperlinkUpdate(HyperlinkEvent he) {
	//Handle the clicks on links in the help text.
		txtHelp = (JEditorPane)he.getSource();
		//... but we aren't doing anything with it.
	}//hyperlinkUpdate

}//HelpPanel