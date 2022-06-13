package me.ghosttypes.ghostware.utils.misc;

import java.util.Calendar;

public class GreetingHelper {

    public static String getGreeting()
    {
        Calendar c = Calendar.getInstance();
        int timeOfDay = c.get(Calendar.HOUR_OF_DAY);

        if(timeOfDay < 12){
            return "Good morning, ";
        } else if(timeOfDay < 16){
            return "Good afternoon, ";
        } else if(timeOfDay < 21){
            return "Good evening, ";
        } else {
            return "Good night, ";
        }
    }
}
