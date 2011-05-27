package edu.unc.its.tl.bb.client;

import java.util.Calendar;

public class DateUtils {
//Some date-handling methods.

   public DateUtils() {
   }
   
   public static String fixDate(String d) {
      //Clean up the Bb date string.
      //NOTE: This keeps the date only, stripping the time.
      int loc = d.indexOf(" ");
      if (loc > 0)
         return d.substring(0, loc);
      else if (d.equals(""))
         return "---";
      else
         return d;
   } //fixDate
   
   
    //*************** Date and time stamps **********************

    public static String kDateSep = "-";

    public static String getDateStamp() {
       //Return yyyy-mm-dd for this very moment.
       Calendar cal = Calendar.getInstance();
       return getDateStamp(cal);
    } //getDateStamp

    public static String getDateStamp(Calendar cal) {
       //Return yyyy-mm-dd for the given date.
       String strDate;
       //Format the date:
       strDate =
             "" + cal.get(Calendar.YEAR) + kDateSep + toDD(cal.get(Calendar.MONTH) +
                                                           1) + kDateSep +
             toDD(cal.get(Calendar.DAY_OF_MONTH));
       return strDate;
    } //getDateStamp

    public static String getMonthDay(Calendar cal) {
       //Return yyyy-mm-dd for the given date.
       String strDate;
       //Format the month and day:
       strDate =
             "" + toDD(cal.get(Calendar.MONTH) + 1) + kDateSep + toDD(cal.get(Calendar.DAY_OF_MONTH));
       return strDate;
    } //getMonthDay

    public static String getTimeStamp() {
       //Return yyyy-mm-dd hh:mm:ss for this very moment.
       Calendar cal = Calendar.getInstance();
       String strDate;
       String strTime;
       strTime =
             "" + toDD(cal.get(Calendar.HOUR_OF_DAY)) + ":" + toDD(cal.get(Calendar.MINUTE)) +
             ":" + toDD(cal.get(Calendar.SECOND));
       return strTime;
    } //getTimeStamp

    public static String toDD(int i) {
       //Convert the given integer to a two-digit string.
       String str = "" + i;
       if (str.length() == 1)
          str = "0" + str;
       return str;
    } //toDD

    public static String toDDD(int i) {
       //Convert the given integer to a three-digit string.
       String str = "" + i;
       if (str.length() == 1)
          str = "00" + str;
       else if (str.length() == 2)
          str = "0" + str;
       return str;
    } //toDD

    public static String toDDDD(int i) {
       //Convert the given integer to a four-digit string.
       String str = "" + i;
       if (str.length() == 1)
          str = "000" + str;
       else if (str.length() == 2)
          str = "00" + str;
       else if (str.length() == 3)
          str = "0" + str;
       return str;
    } //toDD

    public static String dateOnly(String strDate) {
       //Return the yyyy-mm-dd only, where strDate might also
       //be yyyy-mm-dd hh:mm:ss.s
       int iLoc = strDate.indexOf(" ");
       if (iLoc == -1)
          iLoc = strDate.indexOf("+"); //in case it's URL encoded
       if (iLoc > -1)
          strDate = strDate.substring(0, strDate.indexOf(" "));
       return strDate;
    } //dateOnly
    /*
     public static String assembleDate(String yy, String mm, String dd) {
        //Return the date as a properly-formatted string, given the values
        //of the three popups. (NOTE:  mm is really name;mm)
        String strOut;
        //The date has to be all or nothing:
        if (yy.equals(kNoData) || mm.startsWith(kNoData) || dd.equals(kNoData))
           strOut = kNullDate;
        else if ((yy.length() == 0) || (mm.length() == 0) || (dd.length() == 0))
           strOut = kNullDate;
        else //mm is really name;mm
           strOut = yy + kDateSep + getIDPart(mm) + kDateSep + dd;
        return strOut;
     } //assembleDate
   */
}//DateUtils
