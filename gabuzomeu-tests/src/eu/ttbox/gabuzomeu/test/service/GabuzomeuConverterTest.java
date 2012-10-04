package eu.ttbox.gabuzomeu.test.service;

import eu.ttbox.gabuzomeu.service.GabuzomeuConverter;
import android.test.AndroidTestCase;
import android.util.Log;

public class GabuzomeuConverterTest extends AndroidTestCase {

    private static final String TAG = "GabuzomeuConverterTest";

    public void testEncode() {
        int[] numbers = new int[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 42, 73, 37, 48, 77 };
        for (int nb : numbers) {
            String shadok = GabuzomeuConverter.encodeTobase4(nb);
            Log.d(TAG, String.format("Nombre %s  =>  %s", nb, shadok));
        }
    }

    public void testDecodet() {
        String[] numbers = new String[] { "GA", //
                "BU", //
                "ZO", //
                "MEU", //
                "ZO ZO ZO", //
                "BU GA ZO BU",//
                "BUGAZOBU" //
        };
        for (String nb : numbers) {
            Integer base4 = GabuzomeuConverter.decodeTobase10(nb);
            Log.d(TAG, String.format("Shadok %s  =>  %s", nb, base4));
        }
    }

    public void testEncodeEquationTobase4() {
        GabuzomeuConverter converter = new GabuzomeuConverter(getContext());
        String[] equations = new String[] { //
        "0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "42", "73", "37", "48", "77" //
                , "1+1", "0+1+2+3" , "0+1+2+3+4+5"//
                , "1+", "0+1+2+" , "0+1+2+3+4+"//
                , "123+4355"//
        };
        for (String equa : equations) {
        	StringBuilder shadok = new StringBuilder(equa.length()*4);
        	converter.encodeEquationToShadokCode(equa,shadok, null);
            Log.d(TAG, String.format("Equation %s  =>  %s", equa, shadok)); 
        }
        for (String equa : equations) { 
        	StringBuilder shadokName = new StringBuilder(equa.length()*4);
             converter.encodeEquationToShadokCode(equa,null, shadokName);
            Log.d(TAG, String.format("Equation %s  =>  %s", equa, shadokName));
        }
    }
}
