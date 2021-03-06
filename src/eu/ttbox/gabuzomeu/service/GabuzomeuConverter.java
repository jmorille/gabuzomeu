package eu.ttbox.gabuzomeu.service;

import java.math.BigDecimal;
import java.math.BigInteger;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Typeface;
import android.util.Log;
import android.util.SparseArray;
import eu.ttbox.gabuzomeu.R;

public class GabuzomeuConverter {

	private static final String TAG = "GabuzomeuConverter";

	private static boolean isInitShadokDigit = false;

	private static SparseArray<Character> BASE10_TO_SHADOK_DIGIT;
	private static SparseArray<Character> SHADOK_DIGIT_TO_BASE10;
	private static SparseArray<String> SHADOK_DIGIT_TO_SHADOK_DIGIT_NAME;
	private static SparseArray<String> SHADOK_DIGIT_NAME;

	private static char DIGIT_GA;
	private static char DIGIT_BU;
	private static char DIGIT_ZO;
	private static char DIGIT_MEU;

	private static String IS_SHADOK_DIGIT;
	public final char digitGa;
	public final char digitBu;
	public final char digitZo;
	public final char digitMeu;

	private Context mContext;

	public final static boolean isNumberPartKey(char c) {
		return (c >= '0' && c <= '9');// || c == '.';
	}

	public final static boolean isDottKey(char c) {
        return (c == '.'  );// || c == '.';
    }
	
	public GabuzomeuConverter(Context mContext) {
		super();
		this.mContext = mContext;
		intitShadokDigit(mContext);
		// Init Instance char
				digitGa = DIGIT_GA;
				digitBu = DIGIT_BU;
				digitZo = DIGIT_ZO;
				digitMeu = DIGIT_MEU;
	}

	private void intitShadokDigit(Context mContext) {
		if (!isInitShadokDigit) {
			Resources res = mContext.getResources();
			// Digit
			String digitNameGa = res.getString(R.string.digitNameGa);
			String digitNameBu = res.getString(R.string.digitNameBu);
			String digitNameZo = res.getString(R.string.digitNameZo);
			String digitNameMeu = res.getString(R.string.digitNameMeu);

			DIGIT_GA = res.getString(R.string.digitGa).charAt(0);
			DIGIT_BU = res.getString(R.string.digitBu).charAt(0);
			DIGIT_ZO = res.getString(R.string.digitZo).charAt(0);
			DIGIT_MEU = res.getString(R.string.digitMeu).charAt(0);
			// is Shadow digit
			StringBuilder isDigitBuilder = new StringBuilder();
			isDigitBuilder.append(DIGIT_GA).append(DIGIT_BU).append(DIGIT_ZO).append(DIGIT_MEU);
			IS_SHADOK_DIGIT = isDigitBuilder.toString();
			// Digit Map
			SparseArray<Character> shadokDigit = new SparseArray<Character>(4);
			shadokDigit.put('0', DIGIT_GA);
			shadokDigit.put('1', DIGIT_BU);
			shadokDigit.put('2', DIGIT_ZO);
			shadokDigit.put('3', DIGIT_MEU);
			BASE10_TO_SHADOK_DIGIT = shadokDigit;

			// Digit Name Map
			SparseArray<String> shadokDigitName = new SparseArray<String>(4);
			shadokDigitName.put('0', digitNameGa);
			shadokDigitName.put('1', digitNameBu);
			shadokDigitName.put('2', digitNameZo);
			shadokDigitName.put('3', digitNameMeu);
			SHADOK_DIGIT_NAME = shadokDigitName;

			SparseArray<Character> nbToshadokDigit = new SparseArray<Character>(4);
			nbToshadokDigit.put(DIGIT_GA, '0');
			nbToshadokDigit.put(DIGIT_BU, '1');
			nbToshadokDigit.put(DIGIT_ZO, '2');
			nbToshadokDigit.put(DIGIT_MEU, '3');
			SHADOK_DIGIT_TO_BASE10 = nbToshadokDigit;

			SparseArray<String> nbToshadokDigitName = new SparseArray<String>(4);
			nbToshadokDigitName.put(DIGIT_GA, digitNameGa);
			nbToshadokDigitName.put(DIGIT_BU, digitNameBu);
			nbToshadokDigitName.put(DIGIT_ZO, digitNameZo);
			nbToshadokDigitName.put(DIGIT_MEU, digitNameMeu);
			SHADOK_DIGIT_TO_SHADOK_DIGIT_NAME = nbToshadokDigitName;

			// Mark as Init
			isInitShadokDigit = true;
		}
		

	}

	public void convertBase10NumberToShadokDigit(String base10String, StringBuilder shadokDigit, StringBuilder shadokDigitName) {
		BigInteger base10 = new BigInteger(base10String);
		String base4 = base10.toString(4);
		convertBase4NumberToShadokDigit(base4, shadokDigit, shadokDigitName);
	}

	public void convertShadokDigitToBase10Digit(String shadokString, StringBuilder base10DigitDest, StringBuilder shadokDigitName) { // TODO
		int shadokStringSize = shadokString.length();
		boolean isShadokDigitName = shadokDigitName != null;
		boolean isBase10DigitDest = base10DigitDest != null;
		// Convert To 0123 format
		StringBuilder sb = null;
		if (isBase10DigitDest) {
			sb = new StringBuilder(shadokStringSize);
		}
		for (char c : shadokString.toCharArray()) {
			if (isBase10DigitDest) {
				char base10Digit = SHADOK_DIGIT_TO_BASE10.get(c);
				sb.append(base10Digit);
			}
			if (isShadokDigitName) {
				String name = SHADOK_DIGIT_TO_SHADOK_DIGIT_NAME.get(c);
				shadokDigitName.append(name);
			}
		}
		// Chnaging to Base4
		if (isBase10DigitDest) {
			BigInteger base10 = new BigInteger(sb.toString(), 4);
			base10DigitDest.append(base10.toString());
		}
	}

	public void convertBase4NumberToShadokDigit(String text, StringBuilder shadokDigitDest, StringBuilder shadokDigitNameDest) {
		for (char c : text.toCharArray()) {
			if (shadokDigitDest != null) {
				char shadDigit = BASE10_TO_SHADOK_DIGIT.get(c);
				shadokDigitDest.append(shadDigit);
			}
			if (shadokDigitNameDest != null) {
				String shadName = SHADOK_DIGIT_NAME.get(c);
				shadokDigitNameDest.append(shadName);
			}
		}
	}

	public void encodeEquationToShadokCode(CharSequence base10, StringBuilder shadokDigit, StringBuilder shadokDigitName) {
		int baseSize = base10.length();
		StringBuilder current = new StringBuilder(baseSize + baseSize);
		boolean isShadokDigit = shadokDigit != null;
		boolean isShadokDigitName = shadokDigitName != null;
		boolean isAfterDot =false;
		for (int i = 0; i < baseSize; i++) {
			char c = base10.charAt(i);
			if (isNumberPartKey(c)) {
				current.append(c);
			} else {
			   
				int currentSize = current.length();
				if (currentSize > 0) {
//				    if (isAfterDot) {
//				        String numberAfterDot = current.toString();
//				        BigInteger nb = new BigInteger(numberAfterDot);
//				        BigInteger nbd=  nb.divide(new BigInteger("0.25"));
//				        convertBase10NumberToShadokDigit(nbd.toString(), shadokDigit, shadokDigitName);
// 				    } else {
//				        convertBase10NumberToShadokDigit(current.toString(), shadokDigit, shadokDigitName);
//				    }
				    convertBase10NumberToShadokDigit(current.toString(), shadokDigit, shadokDigitName);
					current.delete(0, currentSize);
				}
                isAfterDot =false;
                // Check
				if (isDottKey(c)) {
				    isAfterDot =true;
				}
				// Write Char
				if (isShadokDigit) {
					shadokDigit.append(c);
				}
				if (isShadokDigitName) {
					shadokDigitName.append(c);
				}
			}
		}
		// Clear Cache
		if (current.length() > 0) {
		    if (isAfterDot) {
//                String numberAfterDot = current.toString();
//                BigInteger base10Dot = new BigInteger(numberAfterDot);
//                String base4 = base10Dot.toString(4);
//                shadokDigitName.append(base4);
//                Log.e(TAG, "Convert after dot "+ isAfterDot + " : " + base4);
              convertBase10NumberToShadokDigit(current.toString(), shadokDigit, shadokDigitName);
            } else {
                convertBase10NumberToShadokDigit(current.toString(), shadokDigit, shadokDigitName);
            }
		}
	}

	public void decodeShadokDigitEquationToBase10Code(CharSequence base4, StringBuilder base10Digit, StringBuilder shadokDigitName) {
		int baseSize = base4.length();
		StringBuilder current = new StringBuilder(baseSize);
		for (int i = 0; i < baseSize; i++) {
			char c = base4.charAt(i);
			if (isNumberShadokKey(c)) {
				current.append(c);
			} else {
				int currentSize = current.length();
				if (currentSize > 0) {
					convertShadokDigitToBase10Digit(current.toString(), base10Digit, shadokDigitName);
					current.delete(0, currentSize);
				}
				if (base10Digit != null) {
					base10Digit.append(c);
				}
				if (shadokDigitName != null) {
					shadokDigitName.append(c);
				}
			}
		}
		// Clear Cache
		if (current.length() > 0) {
			convertShadokDigitToBase10Digit(current.toString(), base10Digit, shadokDigitName);
		}
	}

	public final boolean isNumberShadokKey(char c) {
		return IS_SHADOK_DIGIT.indexOf(c) > -1;
	}

	public final char getShadokKey(char c) {
		return BASE10_TO_SHADOK_DIGIT.get(c);
	}

	public static Typeface getSymbolFont(Context context) {
		Typeface font = Typeface.createFromAsset(context.getAssets(), "dejavu_serif.ttf");
		return font;
	}

	// @Deprecated
	// public String convertBase10NumberToShadokDigit(String base10String,
	// SparseArray<String> shadokCode) {
	// BigInteger base10 = new BigInteger(base10String);
	// return convertBase10NumberToShadokDigit(base10, shadokCode);
	// }
	//
	// @Deprecated
	// public String convertBase10NumberToShadokDigit(BigInteger base10,
	// SparseArray<String> shadokCode) {
	// String base4 = base10.toString(4);
	// return convertBase4NumberToShadokDigit(base4, shadokCode);
	// }
	//
	// @Deprecated
	// public String convertBase4NumberToShadokDigit(String text,
	// SparseArray<String> shadokCode) {
	// int textSize = text.length();
	// StringBuilder sb = new StringBuilder(textSize);
	// for (char c : text.toCharArray()) {
	// String shad = shadokCode.get(c);
	// sb.append(shad);
	// }
	// return sb.toString();
	// }

	// @Deprecated
	// public String encodeEquationToShadokCode(CharSequence base10,
	// SparseArray<String> shadokCode) {
	// int baseSize = base10.length();
	// StringBuilder sb = new StringBuilder(baseSize + baseSize);
	// StringBuilder current = new StringBuilder(baseSize + baseSize);
	// for (int i = 0; i < baseSize; i++) {
	// char c = base10.charAt(i);
	// if (isNumberPartKey(c)) {
	// current.append(c);
	// } else {
	// int currentSize = current.length();
	// if (currentSize > 0) {
	// String shadokCodes = convertBase10NumberToShadokDigit(current.toString(),
	// shadokCode);
	// sb.append(shadokCodes);
	// current.delete(0, currentSize);
	// }
	// sb.append(c);
	// }
	// }
	// // Clear Cache
	// if (current.length() > 0) {
	// String shadokCodes = convertBase10NumberToShadokDigit(current.toString(),
	// shadokCode);
	// sb.append(shadokCodes);
	// }
	// return sb.toString();
	// }

	public static String encodeTobase4(int base10) {
		String converted = Integer.toString(base10, 4);
		// String shadok = converted.replaceAll("0", "G").replaceAll("1",
		// "B").replaceAll("2", "Z").replaceAll("3", "M");
		String shadok = converted.replaceAll("0", "GA ").replaceAll("1", "BU ").replaceAll("2", "ZO ").replaceAll("3", "MEU ");
		Log.d(TAG, String.format("Nombre %s  =>  %s  => %s", base10, converted, shadok));
		return shadok;
	}

	public static String encodeNumberTobase4(Integer base10) {
		return Integer.toString(base10, 4);
	}

	public static Integer decodeTobase10(String shadok) {
		String base4 = shadok.replaceAll("GA", "0").replaceAll("BU", "1").replaceAll("ZO", "2").replaceAll("MEU", "3").replaceAll(" ", "");
		Integer base10 = Integer.valueOf(base4, 4);
		// String shadok = converted.replaceAll("0", "G").replaceAll("1",
		// "B").replaceAll("2", "Z").replaceAll("3", "M");
		Log.d(TAG, String.format("Shadok %s  =>  %s  => %s", shadok, base4, base10));
		return base10;
	}

}
