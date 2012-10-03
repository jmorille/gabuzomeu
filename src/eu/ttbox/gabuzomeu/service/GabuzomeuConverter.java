package eu.ttbox.gabuzomeu.service;

import eu.ttbox.gabuzomeu.R;
import android.content.Context;
import android.content.res.Resources;
import android.util.Log;
import android.util.SparseArray;
import android.util.SparseIntArray;

public class GabuzomeuConverter {

    private static final String TAG = "GabuzomeuConverter";

    private Context mContext;
    public final SparseArray<String> shadokDigit;
    public final SparseArray<String> shadokDigitName;

    public GabuzomeuConverter(Context mContext) {
        super();
        this.mContext = mContext;
        Resources res = mContext.getResources();
        shadokDigit = new SparseArray<String>(4);
        shadokDigit.put('0', res.getString(R.string.digitGa));
        shadokDigit.put('1', res.getString(R.string.digitBu));
        shadokDigit.put('2', res.getString(R.string.digitZo));
        shadokDigit.put('3', res.getString(R.string.digitMeu));
        // Digit Name
        shadokDigitName = new SparseArray<String>(4);
        shadokDigitName.put('0', res.getString(R.string.digitNameGa));
        shadokDigitName.put('1', res.getString(R.string.digitNameBu));
        shadokDigitName.put('2', res.getString(R.string.digitNameZo));
        shadokDigitName.put('3', res.getString(R.string.digitNameMeu));

    }

    public String convertBase10NumberToShadokDigit(String base10String, SparseArray<String> shadokCode) {
        Integer base10 = Integer.valueOf(base10String);
        return convertBase10NumberToShadokDigit(base10, shadokCode);
    }

    public String convertBase10NumberToShadokDigit(Integer base10, SparseArray<String> shadokCode) {
        String base4 = Integer.toString(base10, 4);
        return convertBase4NumberToShadokDigit(base4, shadokCode);
    }

    public String convertBase4NumberToShadokDigit(String text, SparseArray<String> shadokCode) {
        int textSize = text.length();
        StringBuilder sb = new StringBuilder(textSize);
        for (char c : text.toCharArray()) {
            String shad = shadokCode.get(c);
            sb.append(shad);
        }
        return sb.toString();
    }

    public String encodeEquationToShadokCode(CharSequence base10, SparseArray<String> shadokCode) {
        int baseSize = base10.length();
        StringBuilder sb = new StringBuilder(baseSize + baseSize);
        StringBuilder current = new StringBuilder(baseSize + baseSize);
        for (int i = 0; i < baseSize; i++) {
            char c = base10.charAt(i);
            if (isNumberPartKey(c)) {
                current.append(c);
            } else {
                int currentSize = current.length();
                if (currentSize > 0) { 
                    String shadokCodes = convertBase10NumberToShadokDigit(current.toString(), shadokCode);
                    sb.append(shadokCodes);
                    current.delete(0, currentSize);
                }
                sb.append(c);
            }
        }
        // Clear Cache
        if (current.length() > 0) {
            String shadokCodes = convertBase10NumberToShadokDigit(current.toString(), shadokCode);
             sb.append(shadokCodes);
        }
        return sb.toString();
    }
    
    

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

    public final static boolean isNumberPartKey(char c) {
        return (c >= '0' && c <= '9');// || c == '.';
    }

}
