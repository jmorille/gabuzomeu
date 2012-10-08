package eu.ttbox.gabuzomeu.test.service;

import java.math.BigInteger;

import android.test.AndroidTestCase;
import android.util.Log;

public class GabuzomeuDecimalConverterTest  extends AndroidTestCase {

	private static final String TAG = "GabuzomeuConverterTest";
	
	 private static final char[] DIGITS = {
	        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
	        'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j',
	        'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't',
	        'u', 'v', 'w', 'x', 'y', 'z'
	    };
	 
//	 public static String intToString(int i, int radix) {
//	        /*
//	         * If i is positive, negate it. This is the opposite of what one might
//	         * expect. It is necessary because the range of the negative values is
//	         * strictly larger than that of the positive values: there is no
//	         * positive value corresponding to Integer.MIN_VALUE.
//	         */
//	        boolean negative = false;
//	        if (i < 0) {
//	            negative = true;
//	        } else {
//	            i = -i;
//	        }
//
//	        int bufLen = radix < 8 ? 33 : 12;  // Max chars in result (conservative)
//	        char[] buf = new char[bufLen];
//	        int cursor = bufLen;
//
//	        do {
//	            int q = i / radix;
//	            buf[--cursor] = DIGITS[radix * q - i];
//	            i = q;
//	        } while (i != 0);
//
//	        if (negative) {
//	            buf[--cursor] = '-';
//	        }
//	        return new String(buf, cursor, bufLen - cursor);
//
////	        return new String(cursor, bufLen - cursor, buf);
//	    }
//	 
//	 
	 public static String intToString(int i, int radix) {
	        /*
	         * If i is positive, negate it. This is the opposite of what one might
	         * expect. It is necessary because the range of the negative values is
	         * strictly larger than that of the positive values: there is no
	         * positive value corresponding to Integer.MIN_VALUE.
	         */
	        boolean negative = false;
	        if (i < 0) {
	            negative = true;
	        } else {
	            i = -i;
	        }

	        int bufLen = radix < 8 ? 33 : 12;  // Max chars in result (conservative)
	        char[] buf = new char[bufLen];
	        int cursor = bufLen;
	    	Log.d(TAG, String.format("***** Converter %s in radix %s", i, radix));
	        do {
	            int q = i / radix;
	        	Log.d(TAG, String.format("*1* int q(%s) = i(%s) / radix(%s)", q, i, radix));
	            buf[--cursor] = DIGITS[radix * q - i];
	        	Log.d(TAG, String.format(" 2* buf[%s] = DIGITS[radix * q(%s) - i(%s)]  ==> %s   ==> %s", cursor, radix * q, i, buf[cursor], new String(buf, cursor, bufLen - cursor) ));
	            i = q;
	        } while (i != 0);
	        
	        if (negative) {
	            buf[--cursor] = '-';
	        }
	        return new String(buf, cursor, bufLen - cursor);

//	        return new String(cursor, bufLen - cursor, buf);
	    }
	 
	// converts integer n into a base b string
	    public static String toString(int n, int base) {
	       // special case
	       if (n == 0) return "0";

	       String digits = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";
	       String s = "";
	       while (n > 0) {
	          int d = n % base;
	          s = digits.charAt(d) + s;
	          n = n / base;
	       }
	       return s;
	    }
	    
		public void testConverterReal() {
			String[] nbs = new String[] {//
					"0", "1", "2", "3", "4", "5" , "6", "7", "8", "9", "10"// 
					, "42" , "73" //
					, "48973168" //
//					, "0.5" , "73" //
			};
			 
			for (String nb : nbs) {
				String result = intToString(Integer.valueOf(nb), 4);
				Log.i(TAG, String.format("Convert %s : %s", nb, result));
			}

			
		}
		
		
		public void testConverterDecimal() {
			String[][] nbs = new String[][] {//
					{"0.5", "0.2"} //
					,{"1", "0.2" } //
					 
			};
		}
		
}
