package eu.ttbox.gabuzomeu.test.service;

import java.math.BigDecimal;

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

	public void testIsNumberShadokKey() {

		GabuzomeuConverter converter = new GabuzomeuConverter(getContext());

		assertEquals(true, converter.isNumberShadokKey(converter.getShadokKey('0')));
		assertEquals(true, converter.isNumberShadokKey(converter.getShadokKey('1')));
		assertEquals(true, converter.isNumberShadokKey(converter.getShadokKey('2')));
		assertEquals(true, converter.isNumberShadokKey(converter.getShadokKey('3')));
		assertEquals(false, converter.isNumberShadokKey('+'));
		assertEquals(false, converter.isNumberShadokKey('-'));
	}
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
	
	public void testEncodeEquationTobase4() {
		GabuzomeuConverter converter = new GabuzomeuConverter(getContext());
		// Encode
		String[] equations = new String[] { //
		"0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "42", "73", "37", "48", "77" //
				, "1+1", "0+1+2+3", "0+1+2+3+4+5"//
				, "1+", "0+1+2+", "0+1+2+3+4+"//
				, "123+4355"//
		};
		// Encode
		String[] shadokEquation = new String[equations.length];
		int i = 0;
		for (String equa : equations) {
			StringBuilder shadok = new StringBuilder(equa.length() * 4);
			converter.encodeEquationToShadokCode(equa, shadok, null);
			shadokEquation[i++] = shadok.toString();
			Log.d(TAG, String.format("Equation %s  =>  %s", equa, shadok));
		}
		for (String equa : equations) {
			StringBuilder shadokName = new StringBuilder(equa.length() * 4);
			converter.encodeEquationToShadokCode(equa, null, shadokName);
			Log.d(TAG, String.format("Equation %s  =>  %s", equa, shadokName));
		}
		// Decode
		int j = 0;
		for (String shadokEqua : shadokEquation) {
			StringBuilder digitName = new StringBuilder(shadokEqua.length());
			StringBuilder digitShadokName = new StringBuilder(shadokEqua.length());
			Log.i(TAG, String.format("Shadok Equation to resolved : %s  (expected %s)", shadokEqua, equations[j]));
			converter.decodeShadokDigitEquationToBase10Code(shadokEqua, digitName, digitShadokName);
			Log.i(TAG, String.format("Shadok Equation %s  =  %s / name = %s", shadokEqua, digitName, digitShadokName)); 
			
			assertEquals(equations[j], digitName.toString());
			j++;
		}
	}
	
	
}
