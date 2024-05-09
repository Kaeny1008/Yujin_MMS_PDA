package kr.or.yujin.mmps.App.Class;

public class StringUtil {
    public static boolean isNumeric(String s) {
        try {
            Double.parseDouble(s);
            return true;
        } catch(NumberFormatException e) {
            return false;
        }
    }

    public static String barcodeChange(String s) {
        //php에서 인식 할수 없는 문자를 변환하기 위함.
        s = s.trim();
        s.replace("&", "//");
        s.replace("%", "//");
        return s;
    }
}
