package eu.ttbox.gabuzomeu.test.service;

import android.test.AndroidTestCase;
import eu.ttbox.gabuzomeu.service.GabuzomeuConverter;

public class GabuzomeuDecimal2ConverterTest  extends AndroidTestCase {

	private static final String TAG = "GabuzomeuConverterTest";
	
	   public void testEncodeDecimalTobase4() {
	        GabuzomeuConverter converter = new GabuzomeuConverter(getContext());
	        // Encode
	        String[][]  base10To4s = new String[][] {
	                {"0.5", "Ga.Bu" } // 0.2
	        };
	        for (String[] equa : base10To4s) {
	            StringBuilder shadok = new StringBuilder(equa[0].length() * 4);
	            converter.encodeEquationToShadokCode(equa[0], null, shadok);
	            assertEquals(equa[1], shadok.toString());
	        }
	    };
}
