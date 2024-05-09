package kr.or.yujin.mmps.App.Class;

import android.util.Log;

public class BarcodeSplit {
    private static final String TAG = "Barcode Split";

    public static String Barcode_Split(String inBarcode, String maker){
        String returnString = inBarcode;
        String partNo = "P:";
        String lotNo = "L:";
        String qty = "Q:";
        String rank = "R:";
        Log.d(TAG, "SCAN Result : " + inBarcode);

        /*
        char[] arrChar = inBarcode.toCharArray();

        for (int j = 0; j < arrChar.length; j++) {
            System.out.println((int)arrChar[j] + ",     " + Character.toString(arrChar[j]));
        }
        */

        try {
            switch (maker.toUpperCase()) {
                case "MOLEX":
                    if (inBarcode.substring(0, 2).equals("1P")) {
                        partNo = "P:" + inBarcode.substring(2, inBarcode.length());
                    } else if (inBarcode.substring(0, 1).equals("Q")) {
                        qty = "Q:" + inBarcode.substring(1, inBarcode.length());
                    } else if (inBarcode.substring(0, 2).equals("1T")) {
                        lotNo = "L:" + inBarcode.substring(4, inBarcode.length());
                    } else if (inBarcode.substring(0, 2).equals("9D")) {
                        lotNo = "L:" + inBarcode.substring(2, inBarcode.length());
                    } else {
                        int loc1P = inBarcode.indexOf("1P");
                        int locQ = inBarcode.indexOf("Q");
                        int loc1T = inBarcode.indexOf("S  ");
                        int loc1K = inBarcode.indexOf("13Q");
                        partNo = "P:" + inBarcode.substring(loc1P + 2, locQ);
                        lotNo = "L:" + inBarcode.substring(loc1T + 3, loc1K);
                        qty = "Q:" + Integer.parseInt(inBarcode.substring(locQ + 1, loc1T));
                    }
                    returnString = partNo + "!@" + lotNo + "!@" + qty;
                    break;
                case "ELMOS":
                    if (inBarcode.substring(0, 1).equals("P")) {
                        partNo = "P:" + inBarcode.substring(1, inBarcode.length() - 1 );
                    } else if (inBarcode.substring(0, 1).equals("Q")) {
                        qty = "Q:" + inBarcode.substring(1, inBarcode.length() - 1);
                    } else if (inBarcode.substring(0, 1).equals("D")) {
                        lotNo = "L:" + inBarcode.substring(1, inBarcode.length() - 1);
                    } else if (inBarcode.substring(0, 1).equals("H")) {
                        lotNo = "L:" + inBarcode.substring(1, inBarcode.length() - 1);
                    } else if (inBarcode.substring(0, 1).equals("S")) {
                        lotNo = "L:" + inBarcode.substring(1, inBarcode.length() - 1);
                    }
                    returnString = partNo + "!@" + lotNo + "!@" + qty;
                    break;
                case "MURATA":
                    if (inBarcode.substring(0, 1).equals("P")) {
                        partNo = "P:" + inBarcode.substring(1, inBarcode.length());
                    } else if (inBarcode.substring(0, 2).equals("1P")) {
                        partNo = "P:" + inBarcode.substring(2, inBarcode.length());
                    } else if (inBarcode.substring(0, 1).equals("Q")) {
                        qty = "Q:" + inBarcode.substring(1, inBarcode.length());
                    } else if (inBarcode.substring(0, 2).equals("1T")) {
                        lotNo = "L:" + inBarcode.substring(2, inBarcode.length());
                    }
                    if (!partNo.equals("P:") && !lotNo.equals("L:") &&
                        !qty.equals("Q:") && !rank.equals("R:")){
                        returnString = partNo + "!@" + lotNo + "!@" + qty + "!@" + rank;
                    }
                    break;
                case "NEXPERIA":
                    if (inBarcode.substring(0, 2).equals("1P")) {
                        partNo = "P:" + inBarcode.substring(2, inBarcode.length());
                        returnString = partNo + "!@" + lotNo + "!@" + qty;
                    } else if (inBarcode.substring(0, 1).equals("Q")) {
                        qty = "Q:" + inBarcode.substring(1, inBarcode.length());
                        returnString = partNo + "!@" + lotNo + "!@" + qty;
                    } else if (inBarcode.substring(0, 2).equals("1T")) {
                        lotNo = "L:" + inBarcode.substring(2, inBarcode.length());
                        returnString = partNo + "!@" + lotNo + "!@" + qty;
                    }
                    break;
                case "YAGEO":
                    if (inBarcode.substring(0, 2).equals("1P")) {
                        partNo = "P:" + inBarcode.substring(2, inBarcode.length());
                    } else if (inBarcode.substring(0, 1).equals("P")) {
                        partNo = "P:" + inBarcode.substring(1, inBarcode.length());
                    } else if (inBarcode.substring(0, 1).equals("Q")) {
                        qty = "Q:" + inBarcode.substring(1, inBarcode.length());
                    } else if (inBarcode.substring(0, 1).equals("K")) {
                        lotNo = "L:" + inBarcode.substring(1, inBarcode.length());
                    } else if (inBarcode.substring(0, 3).equals("31P")) {
                        // Datamatrix Barcode
                        // "\r\n"으로 분리하면 된다.
                        String[] splitText = inBarcode.split("\r\n");
                        partNo = "P:" + splitText[0].replace("31P", "");
                        lotNo = "L:" + splitText[2].replace("1T", "");
                        qty = "Q:" + Integer.parseInt(splitText[3].replace("Q",""));
                        // returnString = partNo + "!@" + lotNo + "!@" + qty;
                    }
                    returnString = partNo + "!@" + lotNo + "!@" + qty;
                    break;
                case "WALSIN":
                    if (inBarcode.length() == 31) {
                        partNo = "P:" + inBarcode.substring(0, 16);
                        partNo = partNo.trim();
                        lotNo = "L:" + inBarcode.substring(16, 29);
                        qty = "Q:" + (Integer.parseInt(inBarcode.substring(29, 31)) * 1000);
                    }
                    returnString = partNo + "!@" + lotNo + "!@" + qty;
                    break;
                case "OSRAM":
                    if (inBarcode.indexOf("@") > 0) {
                        String ledRank = null;
                        String[] splitText = inBarcode.split("@");
                        for (int i = 0; i < splitText.length; i++) {
                            if (splitText[i].substring(0, 2).equals("1P")) {
                                partNo = "P:" + splitText[i].replace("1P", "");
                            } else if (splitText[i].substring(0, 2).equals("1T")) {
                                lotNo = "L:" + splitText[i].replace("1T", "");
                            } else if (splitText[i].substring(0, 1).equals("Q")) {
                                qty = "Q:" + splitText[i].replace("Q", "");
                            }  else if (splitText[i].substring(0, 2).equals("1L")) {
                                ledRank = splitText[i].replace("1L", "");
                            }
                        }
                        if (!ledRank.equals("")){
                            partNo += "-" + ledRank;
                        }
                        returnString = partNo + "!@" + lotNo + "!@" + qty;
                    }
                    break;
                case "VISHAY":
                    // 특수코드가 없음 자릿수로 해독해야함.
                    //      CM3232EA3OB  29481140 04/11/20                                                    202010TW49     2500.000                               CJ    21490010                                                                                                                                            0000000000000000CPM220041070
                    //  SQ7415AEN-T1_GE3 P10D879.108/07/20    Q2033W  W06CBF                                   202032CNW3     3000.000          0000               0000000012680010                             17856000000000000000000000000000000                                                                                      000000CNW018930754
                    //SS10PH10HM3_A/I   G201671.3 07/16/20                                                     202029CNHB         650030373201  0001               2029    90100010                                                                                                                                                            HB2043284164
                    // SQD90P04-9M4L_GE3 P18D668.208/21/20    Q2036T  T06CAQ                                   202034TWT3     2000.000          0000               0000000012690010                             17856000000000000000000000000000000                                                                                      000000TWT006849236
                    if (inBarcode.length() > 99) {
                        partNo = "P:" + inBarcode.substring(0, 18).trim();
                        lotNo = "L:" + inBarcode.substring(18, 28).trim();
                        // qty = "Q:" + Integer.parseInt(inBarcode.substring(104, 143).trim());
                        qty = "Q:" + (int)Double.parseDouble(inBarcode.substring(100, 112).trim());
                        returnString = partNo + "!@" + lotNo + "!@" + qty;
                    }

                    break;
                case "SAMSUNG":
                    if (inBarcode.indexOf("/") > 0) {
                        String[] splitText = inBarcode.split("/");
                        partNo = "P:" + splitText[1];
                        lotNo = "L:" + splitText[0];
                        qty = "Q:" + splitText[3];
                        returnString = partNo + "!@" + lotNo + "!@" + qty;
                    }
                    break;
                case "SUNLOAD":
                    if (inBarcode.indexOf("/") > 0) {
                        String[] splitText = inBarcode.split("/");
                        if (splitText.length == 9) {
                            partNo = "P:" + splitText[0];
                            lotNo = "L:" + splitText[2];
                            qty = "Q:" + splitText[7];
                        } else if(splitText.length == 8) {
                            partNo = "P:" + splitText[1];
                            lotNo = "L:" + splitText[2];
                            qty = "Q:" + splitText[5];
                        }
                        returnString = partNo + "!@" + lotNo + "!@" + qty;
                    }
                    break;
                case "UNIOHM":
                    if (inBarcode.indexOf("!") > 0) {
                        String[] splitText = inBarcode.split("!");
                        partNo = "P:" + splitText[2];
                        lotNo = "L:" + splitText[3];
                        qty = "Q:";
                        returnString = partNo + "!@" + lotNo + "!@" + qty;
                    } else if (inBarcode.indexOf("-") > 0) {
                        String[] splitText = inBarcode.split("-");
                        partNo = "P:" + splitText[1];
                        lotNo = "L:";
                        qty = "Q:";
                        returnString = partNo + "!@" + lotNo + "!@" + qty;
                    }
                    break;
                case "YEONHO":
                    if (inBarcode.length() == 31) {
                        partNo = "P:" + inBarcode.substring(0, 11);
                        lotNo = "L:" + inBarcode.substring(15, 25);
                        qty = "Q:" + inBarcode.substring(25, 31);
                        returnString = partNo + "!@" + lotNo + "!@" + qty;
                    }
                    break;
                case "ALPS":
                    if (inBarcode.length() > 0) { // 일부로.. 의미없다... 바깥 변수에러 때문에..
                        int loc1P = inBarcode.indexOf("1P");
                        int locQ = inBarcode.indexOf("Q");
                        int loc1T = inBarcode.indexOf("1T");
                        int loc1K = inBarcode.indexOf("1K");
                        partNo = "P:" + inBarcode.substring(loc1P + 1, locQ);
                        lotNo = "L:" + inBarcode.substring(loc1T + 1, loc1K);
                        qty = "Q:" + inBarcode.substring(locQ, loc1T);
                        returnString = partNo + "!@" + lotNo + "!@" + qty;
                    }
                    break;
                case "KEC":
                    // KRC106S-RTK/P
                    // 9192039BN010 3000
                    if (inBarcode.length() == 13) {
                        partNo = "P:" + inBarcode;
                        lotNo = "L:";
                        qty = "Q:";
                        returnString = partNo + "!@" + lotNo + "!@" + qty;
                    } else {
                        if (inBarcode.indexOf(" ") > 0) {
                            String[] splitText = inBarcode.split(" ");
                            partNo = "P:";
                            lotNo = "L:" + splitText[0];
                            qty = "Q:" + splitText[1];
                            returnString = partNo + "!@" + lotNo + "!@" + qty;
                        }
                    }
                    break;
                case "ROHM":
                    // MCR01MZPF4702      010000192703452VA42
                    partNo = "P:" + inBarcode.substring(0, 19);
                    partNo = partNo.trim();
                    qty = "Q:" + Integer.parseInt(inBarcode.substring(19, 25));
                    lotNo = "L:" + inBarcode.substring(25, inBarcode.length());
                    lotNo = lotNo.trim();
                    returnString = partNo + "!@" + lotNo + "!@" + qty;
                    break;
                case "LUXPIA":
                    if (inBarcode.indexOf("!") > 0) {
                        String barcodeReplace = inBarcode.replace("!!", "!");
                        String[] splitText = barcodeReplace.split("!");
                        partNo = "P:" + splitText[0] + "(" + splitText[1] + ")";
                        // partNo = "P:" + splitText[0];
                        lotNo = "L:" + splitText[2];
                        qty = "Q:" + splitText[3];
                        returnString = partNo + "!@" + lotNo + "!@" + qty;
                    }
                    break;
                case "NICHIA":
                    //NFSW157FT-HG,6915L-0819A,K3T6E1-Btn25P11d21L7,Btn25P11d21L7,5000,S3335578Btn25P11d21L70022,GG0013330,3335578,,CR,,
                    if (inBarcode.indexOf(",") > 0){
                        String[] splitText = inBarcode.split(",");
                        partNo = "P:" + splitText[0];
                        lotNo = "L:" + splitText[2].split("-")[0];
                        qty = "Q:" + splitText[4];
                        rank = "R:" + splitText[3];
                        returnString = partNo + "!@" + lotNo + "!@" + qty + "!@" + rank;
                    }
                    break;
                case "ONSEMI":
                    if (inBarcode.substring(0, 2).equals("1P")) {
                        partNo = "P:" + inBarcode.substring(2, inBarcode.length());
                        returnString = partNo + "!@" + lotNo + "!@" + qty;
                    } else if (inBarcode.substring(0, 3).equals("30P")) {
                        partNo = "P:" + inBarcode.substring(3, inBarcode.length());
                        returnString = partNo + "!@" + lotNo + "!@" + qty;
                    } else if (inBarcode.substring(0, 1).equals("Q")) {
                        qty = "Q:" + inBarcode.substring(1, inBarcode.length());
                        returnString = partNo + "!@" + lotNo + "!@" + qty;
                    } else if (inBarcode.substring(0, 2).equals("1T")) {
                        lotNo = "L:" + inBarcode.substring(2, inBarcode.length());
                        returnString = partNo + "!@" + lotNo + "!@" + qty;
                    } else if (inBarcode.substring(0, 3).equals("[)>")) {
                        // [)>061PSMMBT2222ALT1G1TMQK097680R9D2010Q30004LCNSMQ2050VCX30PSMMBT2222ALT1G26LMY
                        String[] splitText = inBarcode.split("\u001D");
                        partNo = "P:" + splitText[1].replace("1P", "");
                        lotNo = "L:" + splitText[2].replace("1T", "");
                        qty = "Q:" + splitText[4].replace("Q", "");
                        returnString = partNo + "!@" + lotNo + "!@" + qty + "!@" + rank;
                        //int loc1P = inBarcode.indexOf("1P");
                        //int locQ = inBarcode.indexOf("Q");
                        //int loc1T = inBarcode.indexOf("S  ");
                        //int loc1K = inBarcode.indexOf("13Q");
                        //partNo = "P:" + inBarcode.substring(loc1P + 2, locQ);
                        //lotNo = "L:" + inBarcode.substring(loc1T + 3, loc1K);
                        //qty = "Q:" + Integer.parseInt(inBarcode.substring(locQ + 1, loc1T));
                    }
                    break;
                case "NXP":
                    if (inBarcode.indexOf("\u001D") > 0){
                        String[] splitText = inBarcode.split("\u001D");
                        partNo = "P:" + splitText[2].replace("30P", "");
                        lotNo = "L:" + splitText[5].replace("1T", "");
                        qty = "Q:" + splitText[3].replace("Q", "");
                        returnString = partNo + "!@" + lotNo + "!@" + qty;
                    }
                    break;
                case "ALLEGRO":
                    if (inBarcode.indexOf("@") > 0){
                        String[] splitText = inBarcode.split("@");
                        partNo = "P:" + splitText[3].replace("1P", "");
                        lotNo = "L:" + splitText[12].replace("1T", "");
                        qty = "Q:" + splitText[10].replace("Q", "");
                        returnString = partNo + "!@" + lotNo + "!@" + qty;
                    }
                    break;
                case "DIODES":
                    if (inBarcode.indexOf("@") > 0){
                        String[] splitText = inBarcode.split("@");
                        partNo = "P:" + splitText[4].replace("1P", "");
                        lotNo = "L:" + splitText[20].replace("1T", "");
                        qty = "Q:" + splitText[18].replace("Q", "");
                        returnString = partNo + "!@" + lotNo + "!@" + qty;
                    }
                    break;
                case "TAIWAN SEMICONDUCTOR":
                case "TDK":
                    if (inBarcode.substring(0, 2).equals("1P")) {
                        partNo = "P:" + inBarcode.substring(2, inBarcode.length()).replace("1P","");
                        returnString = partNo + "!@" + lotNo + "!@" + qty;
                    } else if (inBarcode.substring(0, 1).equals("P")) {
                        partNo = "P:" + inBarcode.substring(1, inBarcode.length()).replace("P", "");
                        returnString = partNo + "!@" + lotNo + "!@" + qty;
                    } else if (inBarcode.substring(0, 1).equals("Q")) {
                        qty = "Q:" + inBarcode.substring(1, inBarcode.length()).replace("Q", "");
                        returnString = partNo + "!@" + lotNo + "!@" + qty;
                    } else if (inBarcode.substring(0, 2).equals("1T")) {
                        lotNo = "L:" + inBarcode.substring(2, inBarcode.length()).replace("1T", "");
                        returnString = partNo + "!@" + lotNo + "!@" + qty;
                    } else if (inBarcode.substring(0, 3).equals("[)>")) {
                        //[)>06X978286481TVE023748G019D2038Q30001PSP00160436813D7Q050PARISURCLSXUMA1S08626
                        String[] splitText = inBarcode.split("\u001D");
                        partNo = "P:" + splitText[5].replace("1P", "");
                        lotNo = "L:" + splitText[2].replace("1T", "");
                        qty = "Q:" + splitText[4].replace("Q", "");
                        returnString = partNo + "!@" + lotNo + "!@" + qty + "!@" + rank;
                    }
                    break;
                case "INFINEON":
                    if (inBarcode.substring(0, 2).equals("1P")) {
                        partNo = "P:" + inBarcode.substring(2, inBarcode.length()).replace("1P","");
                        returnString = partNo + "!@" + lotNo + "!@" + qty;
                    } else if (inBarcode.substring(0, 1).equals("P")) {
                        partNo = "P:" + inBarcode.substring(1, inBarcode.length()).replace("P", "");
                        returnString = partNo + "!@" + lotNo + "!@" + qty;
                    } else if (inBarcode.substring(0, 1).equals("Q")) {
                        qty = "Q:" + inBarcode.substring(1, inBarcode.length()).replace("Q", "");
                        returnString = partNo + "!@" + lotNo + "!@" + qty;
                    } else if (inBarcode.substring(0, 2).equals("1T")) {
                        lotNo = "L:" + inBarcode.substring(2, inBarcode.length()).replace("1T", "");
                        returnString = partNo + "!@" + lotNo + "!@" + qty;
                    } else if (inBarcode.substring(0, 3).equals("[)>")) {
                        //[)>06X978286481TVE023748G019D2038Q30001PSP00160436813D7Q050PARISURCLSXUMA1S08626
                        String[] splitText = inBarcode.split("\u001D");
                        partNo = "P:" + splitText[8].replace("50P", "");
                        lotNo = "L:" + splitText[2].replace("1T", "");
                        qty = "Q:" + splitText[4].replace("Q", "");
                        returnString = partNo + "!@" + lotNo + "!@" + qty + "!@" + rank;
                    }
                    break;
                case "DONG-AN ELEC":
                    // TSL1280-330M-A0500DA20200820-056*-
                    if (inBarcode.indexOf("DA") > -1){
                        partNo = "P:" + inBarcode.substring(0, 14);
                        qty = "Q:" + inBarcode.substring(14, 18);
                        lotNo = "L:" + inBarcode.substring(20, inBarcode.length());
                        returnString = partNo + "!@" + lotNo + "!@" + qty;
                    }
                    break;
                case "KET":
                    if (inBarcode.indexOf("P") > -1 &&
                            inBarcode.indexOf("Q") > -1 &&
                            inBarcode.indexOf("S") > -1){
                        Log.d(TAG, "??????????????????????????????");
                        int pIndex = inBarcode.indexOf("P");
                        int qIndex = inBarcode.indexOf("Q");
                        int sIndex = inBarcode.indexOf("S");
                        partNo = "P:" + inBarcode.substring(pIndex, qIndex).replace("P","");
                        qty = "Q:" + inBarcode.substring(qIndex, sIndex).replace("Q","");
                        lotNo = "L:" + inBarcode.substring(sIndex, inBarcode.length()).replace("S","");
                        returnString = partNo + "!@" + lotNo + "!@" + qty;
                    }
                    break;
                case "UJU":
                    if (inBarcode.indexOf(":") > 0){
                        String[] splitText = inBarcode.split(":");
                        partNo = "P:" + splitText[0];
                        lotNo = "L:" + splitText[2];
                        qty = "Q:" + splitText[1];
                        returnString = partNo + "!@" + lotNo + "!@" + qty;
                    }
                    break;
                case "POCONS":
                    // PTC4944HEX2038V0314K001700
                    if (inBarcode.length() == 26){
                        partNo = "P:" + inBarcode.substring(0, 8);
                        lotNo = "L:" + inBarcode.substring(8, 20);
                        qty = "Q:" + inBarcode.substring(20, inBarcode.length());
                        returnString = partNo + "!@" + lotNo + "!@" + qty;
                    }
                    break;
                case "TEXAS INSTRUMENTS":
                    if (inBarcode.substring(0, 3).equals("[)>")) {
                        //[)>06P1PTPS7B6350QPWPRQ16P2PA1Q2000V00333171T8098523ZDE4WTKYD203031T0525006TW020LRFB21LUSA22LTAI23LTWNEG43Z3/260C/168HR;//;081320L15457KN02
                        String[] splitText = inBarcode.split("\u001D");
                        partNo = "P:" + splitText[2].replace("1P", "");
                        lotNo = "L:" + splitText[7].replace("1T", "");
                        qty = "Q:" + splitText[5].replace("Q", "");
                        returnString = partNo + "!@" + lotNo + "!@" + qty + "!@" + rank;
                    }
                    break;
                case "HOKURIKU":
                    if (inBarcode.substring(0, 2).equals("1P")) {
                        partNo = "P:" + inBarcode.substring(2, inBarcode.length());
                        returnString = partNo + "!@" + lotNo + "!@" + qty;
                    } else if (inBarcode.substring(0, 3).equals("3N2")) {
                        lotNo = "L:" + inBarcode.substring(3, inBarcode.length());
                        returnString = partNo + "!@" + lotNo + "!@" + qty;
                    } else if (inBarcode.substring(0, 3).equals("3N1")) {
                        returnString = partNo + "!@" + lotNo + "!@" + qty;
                    }
                    break;
                case "MAXIM":
                    if (inBarcode.substring(0, 2).equals("1P")) {
                        partNo = "P:" + inBarcode.substring(2, inBarcode.length());
                        returnString = partNo + "!@" + lotNo + "!@" + qty;
                    } else if (inBarcode.substring(0, 2).equals("1T")) {
                        lotNo = "L:" + inBarcode.substring(2, inBarcode.length());
                        returnString = partNo + "!@" + lotNo + "!@" + qty;
                    } else if (inBarcode.substring(0, 1).equals("Q")) {
                        qty = "Q:" + inBarcode.substring(1, inBarcode.length());
                        returnString = partNo + "!@" + lotNo + "!@" + qty;
                    }
                    break;
                case "MITSUMI":
                    //00146411  /R 12C388  DJ/2000  /003761/20200925
                    //00146411   R 12C388  DJ
                    if(inBarcode.indexOf("/") > 0){
                        partNo = "P:" + inBarcode.substring(0, 23).replace("/", " ");
                        lotNo = "L:" + inBarcode.substring(38, inBarcode.length()).replace("/", " ");;;
                        qty = "Q:" + inBarcode.substring(24, 30).replace("/", " ");;
                        returnString = partNo + "!@" + lotNo + "!@" + qty;
                    }
                    break;
                default:
                    returnString = inBarcode;
                    break;
            }
        } catch (Exception e) {
            Log.d("!!!!!!!!!!!!!!!!!!!!!!", "Barcode Split Error : ", e);
        }
        //Log.d("BARCODE Split", "In Barcode : " + inBarcode);
        Log.d("BARCODE Split", "Return String : " + returnString.trim());
        return returnString.trim();
    }
}
