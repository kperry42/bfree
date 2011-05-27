package edu.unc.its.tl.bb.client;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class FileType implements BbConstants {
//Methods for handling File type content.
   
   private static Globals glbl = Globals.getInstance();
   
   public FileType() {
   }
   
   public static void copyArchiveFileOut(String strFindName, String strOutName, File filPath,
                                            String strID) {
      //Copy the named file out of the temp folder and into a page folder
      //on the user's disk drive.
      BufferedInputStream ins = null;
      BufferedOutputStream outs = null;
      File filOut = null;
      File filOutPar = null;
      
      try {
         if (strOutName.startsWith("embedded")) //followed by /
            strOutName = strOutName.substring(9); //OK
         if (strOutName.startsWith("loi-teams/"))
            strOutName = strOutName.substring(10);
         filOut = new File(filPath, strOutName);
         filOutPar = filOut.getParentFile();
         filOutPar.mkdirs();
         filOut.createNewFile();

         ins = new BufferedInputStream( new FileInputStream(
                              new File(glbl.filTempPath, "/" + strID + "/" + strFindName)));
         outs = new BufferedOutputStream(new FileOutputStream(filOut));
         byte[] buf = new byte[16384];
         int count = 0;

         count = ins.read(buf, 0, 16384);
         while (count >= 0) {
            outs.write(buf, 0, count);
            count = ins.read(buf, 0, 16384);
         }
         ins.close();
         outs.close();
      } catch (FileNotFoundException e) {
         Unzipper.ref.setMessage("\"" + strID + "/" + strFindName +
                                 "\" is not in the zip file.", ERROR_MSG, false);
      } catch (IOException e) {
         Unzipper.ref.setMessage("Could not read \"" + strID + "/" + strFindName +
                                 "\" from the zip file.", ERROR_MSG, true);
      } finally {
         try {
            if (ins != null)
               ins.close();
         } catch (IOException e) {
         }
         try {
            if (outs != null)
               outs.close();
         } catch (IOException e) {
         }
      }
   } //copyArchiveFileOut
   
}//FileType
