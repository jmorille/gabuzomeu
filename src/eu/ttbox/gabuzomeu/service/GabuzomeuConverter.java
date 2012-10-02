package eu.ttbox.gabuzomeu.service;

import android.util.Log;

public class GabuzomeuConverter {

    private static final String TAG = "GabuzomeuConverter";

    public static String encodeTobase4(int base10) { 
        String converted = Integer.toString(base10, 4);
        // String shadok = converted.replaceAll("0", "G").replaceAll("1", "B").replaceAll("2", "Z").replaceAll("3", "M");
        String shadok = converted.replaceAll("0", "GA ").replaceAll("1", "BU ").replaceAll("2", "ZO ").replaceAll("3", "MEU ");
        Log.d(TAG, String.format("Nombre %s  =>  %s  => %s", base10, converted, shadok));
        return shadok;
    }
    
    

    public static Integer decodeTobase10(String shadok) { 
            String base4 = shadok.replaceAll("GA", "0").replaceAll("BU", "1").replaceAll("ZO", "2").replaceAll("MEU", "3").replaceAll(" ", "");
            Integer base10 = Integer.valueOf(base4, 4);
            // String shadok = converted.replaceAll("0", "G").replaceAll("1", "B").replaceAll("2", "Z").replaceAll("3", "M");
            Log.d(TAG, String.format("Shadok %s  =>  %s  => %s", shadok, base4, base10));
            return base10;
    }

}
