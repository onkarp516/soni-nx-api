package com.truethic.soninx.SoniNxAPI.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class DateAndTimeUtil {
    /* A private Constructor prevents any other
     * class from instantiating.
     */
    private DateAndTimeUtil() {
    }

    public static String getDateAndTimeUtil() {
        LocalDateTime myDateObj = LocalDateTime.now();
        DateTimeFormatter myFormatObj = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String mDateAndTime = myDateObj.format(myFormatObj);
        return mDateAndTime;
    }
}
