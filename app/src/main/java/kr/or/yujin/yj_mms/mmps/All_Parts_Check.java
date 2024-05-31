package kr.or.yujin.yj_mms.mmps;

import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TreeSet;

import androidx.appcompat.app.AppCompatActivity;
import device.common.DecodeResult;
import device.common.DecodeStateCallback;
import device.common.ScanConst;
import device.sdk.ScanManager;
import kr.or.yujin.yj_mms.MainActivity;
import kr.or.yujin.yj_mms.common.NoConvet;
import kr.or.yujin.yj_mms.BuildConfig;
import kr.or.yujin.yj_mms.R;

public class All_Parts_Check extends AppCompatActivity {

    // Scanner Setting
    private static final String TAG = "All Parts Check_New";

    private static ScanManager mScanner;
    private static DecodeResult mDecodeResult;
    private boolean mKeyLock = false;

    private AlertDialog mDialog = null;
    private int mBackupResultType = ScanConst.ResultType.DCD_RESULT_COPYPASTE;
    private Context mContext;
    private ProgressDialog mWaitDialog = null;
    private final Handler mHandler = new Handler();
    private static ScanResultReceiver mScanResultReceiver = null;
    // Scanner Setting

    private static EditText etWorker, etMainDDCode, etMachineNo, etFeederNo, etFeederSN, etPartCode, etPartNo, etLotNo, etQty = null;
    private static TextView tvWorker, tvFeederSN, tvPartCode, tvPartNo, tvLotNo, tvQty, tvStatus, tvMainDDCode = null;
    private static CheckBox noFeeder, noQTYAuto = null;

    private static Boolean firstCheck = false;

    private static Spinner spnMaker;

    private static ArrayList<String> makerList; // 스피너1의 네임 리스트
    private static ArrayAdapter<String> makerListADT; // 스피너1에 사용되는 ArrayAdapter

    private static String orgPartNo, factoryName, lineName, workSide, modelName, customerName;
    private static String checkCode;
    private static String ngReasonString = "";
    private static String ngCheckID = "";
    private static String barcodeHistory = "";

    private int selMakerNo = 0;

    //Server 접속주소
    //private static String MainActivity.server_ip = MainActivity.server_ip;
    //private static int MainActivity.server_port = MainActivity.server_port;

    private Vibrator vibrator;

    /*
    @Override
    protected  void onRestart() {
        super.onRestart();

        // Activity가 재개될 때
        long now = System.currentTimeMillis();
        Date date = new Date(now);
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
        String getDate = df.format(date);
        String[] splitDate = getDate.split("-");

        String cvtYY = NoConvet.noConvert(Integer.parseInt(splitDate[0].substring(2,4)));
        String cvtMM = NoConvet.noConvert(Integer.parseInt(splitDate[1]));
        String cvtDD = NoConvet.noConvert(Integer.parseInt(splitDate[2]));
        checkCode = "ACC" + cvtYY + cvtMM + cvtDD;

        GetData task = new GetData();
        task.execute("http://" + MainActivity.server_ip + ":" + MainActivity.server_port + "/MMPS_V2/AllPartsCheck/codemaking.php", "codeFind"
                , getDate);
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Activity가 종료될때 onStop

        // 취소이므로 Parts Change 이력을 삭제 한다.
        // 왜 중복 All Parts Check가 취소 되었으므로 Parts Change 이력이 필요없다.
        String deleteText = "delete from TB_HISTORY_CHECK";
        deleteText += " where CHECK_CODE = '" + checkCode + "';";
        deleteText += "delete from TB_HISTORY_ALL_DETAIL";
        deleteText += " where CHECK_CODE = '" + checkCode + "';";
        deleteText += "delete from TB_HISTORY_ALL";
        deleteText += " where CHECK_CODE = '" + checkCode + "';";
        //서버로 전송한다.
        GetData taskSave = new GetData();
        taskSave.execute("http://" + MainActivity.server_ip + ":" + MainActivity.server_port + "/MMPS_V2/PartsChange_BLU/codeDelete.php"
                , "codeDelete"
                , deleteText);

   }
   */

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.mmps_all_parts_check);

        mContext = this;
        mScanner = new ScanManager();
        mDecodeResult = new DecodeResult();
        mScanResultReceiver = new ScanResultReceiver();

        if (mScanner != null) {
            mScanner.aDecodeSetTriggerMode(ScanConst.TriggerMode.DCD_TRIGGER_MODE_ONESHOT);
            mScanner.aDecodeSetResultType(ScanConst.ResultType.DCD_RESULT_USERMSG);
            mScanner.aDecodeSetBeepEnable(1);

            int symID = ScanConst.SymbologyID.DCD_SYM_UPCA;
            int propCnt = mScanner.aDecodeSymGetLocalPropCount(symID);
            int propIndex = 0;

            for (int i = 0; i < propCnt; i++) {
                String propName = mScanner.aDecodeSymGetLocalPropName(symID, i);
                if (propName.equals("Send Check Character")) {
                    propIndex = i;
                    break;
                }
            }

            if (mKeyLock == false) {
                mKeyLock = true;
                mScanner.aDecodeSymSetLocalPropEnable(symID, propIndex, 0);
            } else {
                mKeyLock = false;
                mScanner.aDecodeSymSetLocalPropEnable(symID, propIndex, 1);
            }
        }

        vibrator = (Vibrator)getSystemService(Context.VIBRATOR_SERVICE);

        //server_ip = MMPS_Main.server_ip;
        //server_port = MMPS_Main.server_port;
        
        etWorker = (EditText) findViewById(R.id.etWorker);
        etMainDDCode = (EditText) findViewById(R.id.etMainDDCode);
        etMachineNo = (EditText) findViewById(R.id.etMachineNo);
        etFeederNo = (EditText) findViewById(R.id.etFeederNo);
        etFeederSN = (EditText) findViewById(R.id.etFeederSN);
        etPartCode = (EditText) findViewById(R.id.etPartCode);
        etPartNo = (EditText) findViewById(R.id.etPartNo);
        etLotNo = (EditText) findViewById(R.id.etLotNo);
        etQty = (EditText) findViewById(R.id.etQty);

        tvWorker = (TextView) findViewById(R.id.tvWorker);
        tvFeederSN = (TextView) findViewById(R.id.tvFeederSN);
        tvPartCode = (TextView) findViewById(R.id.tvPartCode);
        tvPartNo = (TextView) findViewById(R.id.tvPartNo);
        tvLotNo = (TextView) findViewById(R.id.tvLotNo);
        tvQty = (TextView) findViewById(R.id.tvQty);
        tvStatus = (TextView) findViewById(R.id.tvStatus);
        tvMainDDCode = (TextView) findViewById(R.id.tvMainDDCode);

        noFeeder = (CheckBox) findViewById(R.id.noFeeder);
        noQTYAuto = (CheckBox) findViewById(R.id.noQTYAuto);

        //데이터 준비
        makerList = new ArrayList<String>();

        // 어댑터 생성
        makerListADT = new ArrayAdapter<String>(this,
                R.layout.support_simple_spinner_dropdown_item, makerList);

        // 어댑터 설정
        spnMaker = (Spinner) findViewById(R.id.spnMaker);
        spnMaker.setAdapter(makerListADT);

        tvStatus.setText("작업자를 입력하여 주십시오.");

        firstCheck = true;

        long now = System.currentTimeMillis();
        Date date = new Date(now);
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
        String getDate = df.format(date);
        String[] splitDate = getDate.split("-");

        String cvtYY = NoConvet.noConvert(Integer.parseInt(splitDate[0].substring(2,4)));
        String cvtMM = NoConvet.noConvert(Integer.parseInt(splitDate[1]));
        String cvtDD = NoConvet.noConvert(Integer.parseInt(splitDate[2]));
        checkCode = "ACC" + cvtYY + cvtMM + cvtDD;

        GetData task = new GetData();
        task.execute("http://" + MainActivity.server_ip + ":" + MainActivity.server_port + "/MMPS_V2/AllPartsCheck/codemaking.php", "codeFind"
                , getDate);

        etQty.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                switch (actionId) {
                    case EditorInfo.IME_ACTION_SEARCH:
                        // 검색 동작
                        break;
                    default:
                        // 기본 엔터키 동작
                        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                        imm.toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0);
                        actionWrite(); 
                        return false;
                }
                return true;
            }
        });

        etLotNo.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                switch (actionId) {
                    case EditorInfo.IME_ACTION_SEARCH:
                        // 검색 동작
                        break;
                    default:
                        // 기본 엔터키 동작
                        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                        imm.toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0);
                        return false;
                }
                return true;
            }
        });

        tvMainDDCode.setOnClickListener((new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                etMainDDCode.setText("");
                tvStatus.setText("작업지시번호를 입력하여 주십시오.");
                etMachineNo.setText("");
                etFeederNo.setText("");
                etFeederSN.setText("");
                etPartCode.setText("");
                etPartNo.setText("");
                etLotNo.setText("");
                etQty.setText("");
                barcodeHistory = "";
                //'Parts Check만하기' 버튼 활성화
                noFeeder.setEnabled(true);

                // 왜 중복 All Parts Check가 취소 되었으므로 이력이 필요없다.
                String deleteText = "delete from TB_HISTORY_CHECK";
                deleteText += " where CHECK_CODE = '" + checkCode + "';";
                deleteText += "delete from TB_HISTORY_ALL_DETAIL";
                deleteText += " where CHECK_CODE = '" + checkCode + "';";
                //서버로 전송한다.
                GetData taskSave = new GetData();
                taskSave.execute("http://" + MainActivity.server_ip + ":" + MainActivity.server_port + "/MMPS_V2/AllPartsCheck/codeDelete.php"
                        , "codeDelete"
                        , deleteText);
            }
        }));

        tvFeederSN.setOnClickListener((new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                etFeederSN.setText("");
                tvStatus.setText("Feeder Serial No.를 스캔하여 주십시오.");
                barcodeHistory = "";
                // 현재 피더의 정보를 다시 불러온다.
                GetData task = new GetData();
                task.execute("http://" + MainActivity.server_ip + ":" + MainActivity.server_port + "/MMPS_V2/AllPartsCheck/nowfeeder.php", "nextFeeder"
                        , etMainDDCode.getText().toString()
                        , etMachineNo.getText().toString()
                        , etFeederNo.getText().toString());

            }
        }));

        tvPartCode.setOnClickListener((new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                barcodeHistory = "";
                etPartCode.setText("");
                etPartNo.setText("");
                etLotNo.setText("");
                etQty.setText("");
                tvStatus.setText("자재의 Part Code를 입력하여 주십시오.");
            }
        }));

        tvPartNo.setOnClickListener((new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                barcodeHistory = "";
                etPartCode.setText("");
                etPartNo.setText("");
                etLotNo.setText("");
                etQty.setText("");
                tvStatus.setText("자재의 Part No,를 입력하여 주십시오.");
            }
        }));

        tvLotNo.setOnClickListener((new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                barcodeHistory = "";
                etPartCode.setText("");
                etPartNo.setText("");
                etLotNo.setText("");
                etQty.setText("");
                tvStatus.setText("자재의 Part No.를 입력하여 주십시오.");
            }
        }));

        tvQty.setOnClickListener((new View.OnClickListener() {
             @Override
             public void onClick(View v) {
                 barcodeHistory = "";
                 etPartCode.setText("");
                 etPartNo.setText("");
                 etLotNo.setText("");
                 etQty.setText("");
                 tvStatus.setText("자재의 Part No.를 입력하여 주십시오.");
            }
         }));

        tvWorker.setOnClickListener((new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                etWorker.setText("");
                tvStatus.setText("작업자를 입력하여 주십시오.");
            }
        }));
    }

    private class GetData extends AsyncTask<String, Void, String> {

        ProgressDialog progressDialog;
        String errorString = null;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
            List<ActivityManager.RunningTaskInfo> info = manager.getRunningTasks(1);
            ComponentName componentName= info.get(0).topActivity;
            String ActivityName = componentName.getShortClassName().substring(1);

            if (ActivityName.equals("App.Activity.AllPartsCheck"))
            progressDialog = ProgressDialog.show(All_Parts_Check.this,
                    "Connecting to server....\nPlease wait.", null, true, true);
        }

        @Override
        protected String doInBackground(String... params) {

            String serverURL = params[0];

            String secondString = (String) params[1];
            String postParameters = null;

            if (secondString.equals("MainDDCode")) {
                postParameters = "ddMainNo=" + params[2];
                postParameters += "&machineNo=" + params[3];
            } else if (secondString.equals("firstInsert")) {
                postParameters = "sql=" + params[2];
            } else if (secondString.equals("scanErrorInsert")) {
                postParameters = "sql=" + params[2];
            } else if (secondString.equals("codeDelete")){
                postParameters = "sql=" + params[2];
            } else if (secondString.equals("codeFind")){
                postParameters = "findDate=" + params[2];
            } else if (secondString.equals("nextFeeder")){
                postParameters = "ddMainNo=" + params[2];
                postParameters += "&machineNo=" + params[3];
                postParameters += "&feederNo=" + params[4];
            } else if (secondString.equals("checkCompleted")){
                postParameters = "sql=" + params[2];
            } else if (secondString.equals("feederList")){
                postParameters = "ddMainNo=" + params[2];
                postParameters += "&machineNo=" + params[3];
            } else if (secondString.equals("feederInit")){
                postParameters = "sql=" + params[2];
            } else if (secondString.equals("codeSave")) {
                postParameters = "checkCode=" + params[2];
            } else if (secondString.equals("BarcodeSplit")) {
                postParameters = "barcode=" + params[3];
                postParameters += "&maker=" + params[2];
            } else if (secondString.equals("ver")) {
                postParameters = "";
            }

            try {
                URL url = new URL(serverURL);
                HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();

                httpURLConnection.setReadTimeout(5000);
                httpURLConnection.setConnectTimeout(5000);
                httpURLConnection.setRequestMethod("POST");
                httpURLConnection.setDoInput(true);
                httpURLConnection.connect();

                OutputStream outputStream = httpURLConnection.getOutputStream();
                outputStream.write(postParameters.getBytes("UTF-8"));
                outputStream.flush();
                outputStream.close();

                int responseStatusCode = httpURLConnection.getResponseCode();
                Log.d(TAG, "response code - " + responseStatusCode);

                InputStream inputStream;
                if (responseStatusCode == HttpURLConnection.HTTP_OK) {
                    inputStream = httpURLConnection.getInputStream();
                } else {
                    inputStream = httpURLConnection.getErrorStream();
                }

                InputStreamReader inputStreamReader = new InputStreamReader(inputStream, "UTF-8");
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                StringBuilder sb = new StringBuilder();
                String line;

                while ((line = bufferedReader.readLine()) != null) {
                    sb.append(line);
                }
                bufferedReader.close();
                return sb.toString().trim();
            } catch (Exception e) {
                //Log.d(TAG, "GetData : Error ", e);
                errorString = e.toString();
                return null;
            }
        }

        @Override
        protected void onPostExecute (String result){
            super.onPostExecute(result);

            //progressDialog.dismiss();

            if (progressDialog != null && progressDialog.isShowing())
                progressDialog.dismiss();

            if (result == null){
                Log.d(TAG, "서버 접속 Error - " + errorString);
                Toast.makeText(All_Parts_Check.this, "서버에 접속 할 수 없습니다.\n상세 내용은 로그를 참조 하십시오.", Toast.LENGTH_SHORT).show();
            } else {
                Log.d(TAG, "서버 응답 내용 - " + result);
                showResult(result);
            }
        }

        private void showResult (String mJsonString){
            try {
                JSONObject jsonObject = new JSONObject(mJsonString);

                String header = jsonObject.names().toString();
                header = header.replace("[", "");
                header = header.replace("\"", "");
                header = header.replace("]", "");

                if (header.equals("firstPartNo")) {
                    JSONArray jsonArray = jsonObject.getJSONArray("firstPartNo");

                    //for(int i=0;i<jsonArray.length();i++){
                    //    JSONObject item = jsonArray.getJSONObject(i);
                    //}
                    JSONObject item = jsonArray.getJSONObject(0);
                    //orgPartNo = item.getString("MAIN_PART_NO") + "," + item.getString("SUB_PART_NO") + "," + item.getString("SUB_PART_NO2"); // 대표 Part No.
                    orgPartNo = item.getString("MAIN_PART_NO") + "," + item.getString("SUB_PART_NO"); // 대표 Part No.
                    etFeederNo.setText(item.getString("FEEDER_NO"));  // 첫번째 Feeder No.
                    factoryName = item.getString("FACTORY_NAME");
                    lineName = item.getString("LINE_NAME");
                    workSide = item.getString("WORK_SIDE");
                    modelName = item.getString("MODEL_NAME");
                    customerName = item.getString("CUSTOMER_NAME");

                    // 불러온 제조사를 변수에 저장
                    String listPartMaker = item.getString("MAIN_PART_MAKER");
                    /*
                    if (customerName.equals("J산업") ||
                            customerName.equals("전유산업") ||
                            customerName.equals("Mangoslab") ||
                            customerName.equals("L-Tech") ||
                            customerName.equals("TopRun") ||
                            customerName.equals("덕일전자") ||
                            customerName.contains("CI Digital") ||
                            customerName.equals("allRadio")){
                        // J산업의 경우 sub part maker가 우선으로 와야 더 편함.
                        //Toast.makeText(AllPartsCheck.this, "123456789", Toast.LENGTH_SHORT).show();
                        if (!item.getString("SUB_PART_MAKER2").equals("")){
                            listPartMaker = item.getString("SUB_PART_MAKER2") + "," + item.getString("MAIN_PART_MAKER");
                        } else {
                            listPartMaker = item.getString("MAIN_PART_MAKER") + "," + item.getString("SUB_PART_MAKER");
                        }
                    } else {
                        listPartMaker = item.getString("MAIN_PART_MAKER") + "," + item.getString("SUB_PART_MAKER");
                    }
                     */

                    //수량 자동입력방지 설정
                    /*
                    if (customerName.equals("전유산업") || customerName.equals("J산업") || customerName.contains("CI Digital")) {
                        noQTYAuto.setVisibility(View.VISIBLE);
                        noQTYAuto.setChecked(true);
                    } else {
                        noQTYAuto.setVisibility(View.INVISIBLE);
                        noQTYAuto.setChecked(false);
                    }
                     */

                    //'Parts Check만하기' 버튼 비활성화
                    //noFeeder.setEnabled(false);
                    
                    Log.d(TAG, "Load Maker List : " + listPartMaker);
                    String[] makerSplit = listPartMaker.split("/");

                    ArrayList<String> makerArray = new ArrayList<>(); // 구분된 Maker를 배열에 담는다(중복제거 위한 기초작업)
                    for (int i=0; i<makerSplit.length; i++){
                        makerArray.add(makerSplit[i]);
                    }

                    TreeSet<String> makerArray2 = new TreeSet<String>(makerArray); //TreeSet을 이용한 중복데이터 제거
                    Object[] mStringArray = makerArray2.toArray();

                    makerListADT.clear();
                    for (int i=0; i<mStringArray.length; i++){
                        makerListADT.add((String)mStringArray[i]);
                    }

                    makerListADT.notifyDataSetChanged();

                    //자재 제조사가 2개 이상일 경우 알림 진동
                    if (makerListADT.getCount() > 1) {
                        long[] pattern = {500,100,500,100,500};
                        vibrator.vibrate(pattern, -1); // miliSecond, 지정한 시간동안 진동
                    }
                    Log.d(TAG, "Original Part No : " + orgPartNo);
                } else if (header.equals("firstPartNo!")) {
                    Toast.makeText(All_Parts_Check.this, "정보를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show();
                } else if (header.equals("makingCode")) {
                    firstCheck = true; // 처음 입력이라고 인지 시킨다.
                    JSONArray jsonArray = jsonObject.getJSONArray("makingCode");
                    JSONObject item = jsonArray.getJSONObject(0);

                    String orderNo = item.getString("CHECK_CODE").substring(6, 9); // 조금 다르다 6번째 부터 9번 앞자리까지로 인식해라
                    int serial = Integer.parseInt(orderNo) + 1;
                    String number = String.format("%03d", serial); // %03d 자릿수를 채운다. 앞의 숫자를 원하는 자릿수 만큼 채운다. 현재는 3자리
                    checkCode += number;
                    // CheckCode를 임시로 저장한다.
                    GetData task = new GetData();
                    task.execute("http://" + MainActivity.server_ip + ":" + MainActivity.server_port + "/MMPS_V2/AllPartsCheck/tempcodesave.php"
                            , "codeSave"
                            , checkCode);
                } else if (header.equals("makingCode!")) {
                    checkCode += "001";
                    // CheckCode를 임시로 저장한다.
                    GetData task = new GetData();
                    task.execute("http://" + MainActivity.server_ip + ":" + MainActivity.server_port + "/MMPS_V2/AllPartsCheck/tempcodesave.php", "codeSave"
                            , checkCode);
                } else if (header.equals("codeSave")) {
                    JSONArray jsonArray = jsonObject.getJSONArray("codeSave");
                    JSONObject item = jsonArray.getJSONObject(0);
                    if (!item.getString("Result").equals("Success")) {
                        Toast.makeText(All_Parts_Check.this, mJsonString, Toast.LENGTH_SHORT).show();
                    }
                } else if (header.equals("codeDelete")) {
                    Log.d(TAG, "Check Code 삭제 완료");
                } else if (header.equals("insert")) {
                    JSONArray jsonArray = jsonObject.getJSONArray("insert");
                    JSONObject item = jsonArray.getJSONObject(0);
                    if (!item.getString("Result").equals("Success")) {
                        Toast.makeText(All_Parts_Check.this, mJsonString, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    barcodeHistory = "";
                    Log.d(TAG, "barcode History 삭제 완료");
                    if (ngReasonString.equals("")) {
                        makerListADT.clear();
                        makerListADT.notifyDataSetChanged();
                        etFeederSN.setText("");
                        etPartCode.setText("");
                        etPartNo.setText("");
                        etLotNo.setText("");
                        etQty.setText("");

                        // 다음 피더의 정보를 불러온다.
                        GetData task = new GetData();
                        task.execute("http://" + MainActivity.server_ip + ":" + MainActivity.server_port + "/MMPS_V2/AllPartsCheck/nextfeeder.php", "nextFeeder"
                                , etMainDDCode.getText().toString()
                                , etMachineNo.getText().toString()
                                , etFeederNo.getText().toString());
                    } else {
                        Toast.makeText(All_Parts_Check.this, "NG 결과 저장 완료.\n데이터를 확인 후 다시 시도하여 주십시오.", Toast.LENGTH_SHORT).show();
                        ngReasonString = "";
                        etPartCode.setText("");
                        etPartNo.setText("");
                        etLotNo.setText("");
                        etQty.setText("");
                    }
                } else if (header.equals("scan_error_insert")) {
                    // 확인 결과 NG시 바코드 해독 편의를 위해 에어리스트 항목을 저장했을경우.
                    barcodeHistory = "";
                    Log.d(TAG, "barcode History 삭제 완료");
                } else if (header.equals("nextPartNo")) {
                    JSONArray jsonArray = jsonObject.getJSONArray("nextPartNo");

                    //for(int i=0;i<jsonArray.length();i++){
                    //    JSONObject item = jsonArray.getJSONObject(i);
                    //}
                    JSONObject item = jsonArray.getJSONObject(0);
                    //orgPartNo = item.getString("MAIN_PART_NO") + "," + item.getString("SUB_PART_NO") + "," + item.getString("SUB_PART_NO2"); // 대표 Part No.
                    orgPartNo = item.getString("MAIN_PART_NO") + "," + item.getString("SUB_PART_NO"); // 대표 Part No.
                    etFeederNo.setText(item.getString("FEEDER_NO"));  // 첫번째 Feeder No.
                    factoryName = item.getString("FACTORY_NAME");
                    lineName = item.getString("LINE_NAME");
                    workSide = item.getString("WORK_SIDE");
                    modelName = item.getString("MODEL_NAME");
                    customerName = item.getString("CUSTOMER_NAME");

                    //수량 자동입력방지 설정
                    /*
                    if (customerName.equals("전유산업") || customerName.equals("J산업") || customerName.contains("CI Digital")) {
                        noQTYAuto.setVisibility(View.VISIBLE);
                        noQTYAuto.setChecked(true);
                    } else {
                        noQTYAuto.setVisibility(View.INVISIBLE);
                        noQTYAuto.setChecked(false);
                    }
                     */

                    // 불러온 제조사를 변수에 저장
                    String listPartMaker = item.getString("MAIN_PART_MAKER");
                    /*
                    if (customerName.equals("J산업") ||
                            customerName.equals("전유산업") ||
                            customerName.equals("Mangoslab") ||
                            customerName.equals("allRadio") ||
                            customerName.equals("TopRun") ||
                            customerName.equals("덕일전자") ||
                            customerName.contains("CI Digital") ||
                            customerName.equals("L-Tech")) {
                        if (!item.getString("SUB_PART_MAKER2").equals("")){
                            listPartMaker = item.getString("SUB_PART_MAKER2") + "," + item.getString("MAIN_PART_MAKER");
                        } else {
                            listPartMaker = item.getString("MAIN_PART_MAKER") + "," + item.getString("SUB_PART_MAKER");
                        }
                    } else {
                        listPartMaker = item.getString("MAIN_PART_MAKER") + "," + item.getString("SUB_PART_MAKER");
                    }
                     */

                    String[] makerSplit = listPartMaker.split("/");

                    ArrayList<String> makerArray = new ArrayList<>(); // 구분된 Maker를 배열에 담는다(중복제거 위한 기초작업)
                    for (int i=0; i<makerSplit.length; i++){
                        makerArray.add(makerSplit[i]);
                    }

                    TreeSet<String> makerArray2 = new TreeSet<String>(makerArray); //TreeSet을 이용한 중복데이터 제거
                    Object[] mStringArray = makerArray2.toArray();

                    makerListADT.clear();
                    for (int i=0; i<mStringArray.length; i++){
                        makerListADT.add((String)mStringArray[i]);
                    }

                    makerListADT.notifyDataSetChanged();

                    if (makerListADT.getCount() > 1) {
                        long[] pattern = {100,500,100,500,100,500};
                        vibrator.vibrate(pattern, -1); // miliSecond, 지정한 시간동안 진동
                    }
                    tvStatus.setText("Feeder No : " + etFeederNo.getText() + "의 제조사를 선택 후 Feeder SN을 입력하여 주십시오.");
                    Log.d(TAG, "Original Part No : " + orgPartNo);
                } else if (header.equals("nextPartNo!")) {
                    etMachineNo.setText("");
                    etFeederSN.setText("");
                    etMainDDCode.setText("");
                    etFeederNo.setText("");
                    //'Parts Check만하기' 버튼 비활성화
                    noFeeder.setEnabled(false);

                    String insertText = "update tb_mmps_history_all set check_completed = 'Yes' where check_code = '" + checkCode + "';";

                    // 검사진행 결과를 완료로 업데이트 시킨다.
                    GetData task = new GetData();
                    task.execute("http://" + MainActivity.server_ip + ":" + MainActivity.server_port + "/MMPS_V2/AllPartsCheck/checkcompleted.php", "checkCompleted", insertText);
                    Toast.makeText(All_Parts_Check.this, "완료 되었습니다.", Toast.LENGTH_SHORT).show();
                    tvStatus.setText("작업지시번호를 입력하여 주십시오.");

                    // 새로운 CheckCode를 생성하기 위해 반복작업
                    long now = System.currentTimeMillis();
                    Date date = new Date(now);
                    SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
                    String getDate = df.format(date);
                    String[] splitDate = getDate.split("-");

                    String cvtYY = NoConvet.noConvert(Integer.parseInt(splitDate[0].substring(2,4)));
                    String cvtMM = NoConvet.noConvert(Integer.parseInt(splitDate[1]));
                    String cvtDD = NoConvet.noConvert(Integer.parseInt(splitDate[2]));
                    checkCode = "ACC" + cvtYY + cvtMM + cvtDD;

                    GetData task2 = new GetData();
                    task2.execute("http://" + MainActivity.server_ip + ":" + MainActivity.server_port + "/MMPS_V2/AllPartsCheck/codemaking.php", "codeFind"
                            , getDate);

                } else if (header.equals("resultinsert")) {
                    JSONArray jsonArray = jsonObject.getJSONArray("resultinsert");
                    JSONObject item = jsonArray.getJSONObject(0);
                    if (!item.getString("Result").equals("Success")){
                        Toast.makeText(All_Parts_Check.this, mJsonString, Toast.LENGTH_SHORT).show();
                    }
                } else if (header.equals("feederList")) {
                    JSONArray jsonArray = jsonObject.getJSONArray("feederList");

                    Boolean feederExistResult = false;
                    for(int i=0;i<jsonArray.length();i++){
                        JSONObject item = jsonArray.getJSONObject(i);
                        if (item.getString("FEEDER_SN").equals(etFeederSN.getText().toString())){
                            feederExistResult = true;
                            break;
                        }
                    }

                    if (feederExistResult == true){
                        Toast.makeText(All_Parts_Check.this, "이미 등록된 Feeder SN이므로 등록할 수 없습니다.", Toast.LENGTH_SHORT).show();
                        etFeederSN.setText("");
                    } else {
                        tvStatus.setText("자재의 Part No.를 입력하여 주십시오.");
                    }
                } else if (header.equals("feederList!")) {
                    tvStatus.setText("자재의 Part No.를 입력하여 주십시오.");
                } else if (header.equals("feederInit")) {
                    JSONArray jsonArray = jsonObject.getJSONArray("feederInit");
                    JSONObject item = jsonArray.getJSONObject(0);
                    if (!item.getString("Result").equals("Success")){
                        Toast.makeText(All_Parts_Check.this, mJsonString, Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(All_Parts_Check.this, "Feeder SN을 초기화 하였습니다", Toast.LENGTH_SHORT).show();
                    }
                } else if (header.equals("BarcodeSplitResult")) {
                    JSONArray jsonArray = jsonObject.getJSONArray(("BarcodeSplitResult"));
                    JSONObject item = jsonArray.getJSONObject(0);

                    try {
                        String[] partInfo = item.getString("returnStr").split("!@");
                        if (partInfo.length < 3) {
                            Toast.makeText(All_Parts_Check.this, "바코드 해독에 실패 하였습니다.", Toast.LENGTH_SHORT).show();
                        } else {
                            String nowPartNo = partInfo[0].replace("P:", "");
                            String nowLotNo = partInfo[1].replace("L:", "");
                            String nowQty = partInfo[2].replace("Q:", "");
                            String nowORG = partInfo[3].replace("ORG:", "");
                            if (etQty.length() == 0) {
                                if (!nowORG.equals("")) {
                                    etQty.setText(nowORG);
                                } else {
                                    if (etQty.getText().toString().equals("")){
                                        etQty.setText(nowPartNo);
                                    }
                                }

                                // Barcode 분리작업 완료
                                tvStatus.setText("자재의 Lot No.를 입력하여 주십시오.");
                                //etLotNo.requestFocus();
                                //키보드 보이게 하는 부분
                                //InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                                //imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, InputMethodManager.HIDE_IMPLICIT_ONLY);

                                // Parts Check만하기 버튼이 활성화 되어 있다면

                                // 자재 정보가 비워 있지 않다면.
                                if (etPartCode.length() != 0 &&
                                        etLotNo.length() != 0 &&
                                        etQty.length() != 0 ||
                                        noFeeder.isChecked()){
                                    // 결과를 확인할지 물어본다.
                                    //resultWriteQuestion();

                                    // 결과를 확인 한다.
                                    actionWrite();
                                    //tvStatus.setText("확인 버튼을 눌러 주십시오.");
                                }
                            }
                        }
                    } catch (Exception e) {
                        Log.d(TAG, "PHP에서 돌아온 바코드 정보를 확인 중 오류 발생", e);
                    }
                } else if (header.equals("CheckVer")) {
                    JSONArray jsonArray = jsonObject.getJSONArray("CheckVer");
                    JSONObject item = jsonArray.getJSONObject(0);
                    if (!item.getString("Result").equals("Ver:"+ BuildConfig.VERSION_NAME)){
                        appVerAlarm();
                    }
                } else {
                    Toast.makeText(All_Parts_Check.this, mJsonString, Toast.LENGTH_SHORT).show();
                }
            } catch (JSONException e) {
                Log.d(TAG, "showResult Error : ", e);
            }
        }
    }

    public class ScanResultReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (mScanner != null) {
                try {
                    if (ScanConst.INTENT_USERMSG.equals(intent.getAction())) {
                        mScanner.aDecodeGetResult(mDecodeResult.recycle());
                        Log.d(TAG, "Scan Result : " + mDecodeResult.toString());
                        if (mDecodeResult.toString().equals("READ_FAIL")) {
                            return;
                        }

                        barcodeHistory += " !!!! " + mDecodeResult.toString();

                        if (etWorker.length() == 0) {
                            etWorker.setText(mDecodeResult.toString());
                            tvStatus.setText("작업지시번호를 입력하여 주십시오.");
                        } else if (etMainDDCode.length() == 0) {
                            try {
                                String[] scanResultSplit = mDecodeResult.toString().split("-");
                                etMainDDCode.setText(scanResultSplit[0] + "-" + scanResultSplit[1]); // 작업지시번호를 기록
                                etMachineNo.setText(scanResultSplit[2]);  // 설비번호를 기록
                                // 서버에서 체크 정보를 불러온다.
                                GetData task = new GetData();
                                task.execute("http://" + MainActivity.server_ip + ":" + MainActivity.server_port + "/MMPS_V2/AllPartsCheck/mainddcode.php", "MainDDCode"
                                        , etMainDDCode.getText().toString()
                                        , etMachineNo.getText().toString());
                                if (noFeeder.isChecked() == false){
                                    tvStatus.setText("Feeder No : " + etFeederNo.getText() + "의 제조사를 선택 후 Feeder SN을 입력하여 주십시오.");
                                    feederReset_Question(); // 피더 초기화 문의
                                } else {
                                    tvStatus.setText("Check할 Part No.를 입력하여 주십시오.");
                                }
                            } catch (Exception e) {
                                Toast.makeText(All_Parts_Check.this, "작업지시번호를 스캔하여 주십시오.", Toast.LENGTH_SHORT).show();
                                return;
                            }
                        } else if (etFeederSN.length() == 0 && noFeeder.isChecked() == false) {
                            if (mDecodeResult.toString().substring(0, 3).equals("FN-")) {
                                etFeederSN.setText(mDecodeResult.toString().replace("FN-", ""));
                                // FeederSN의 사용여부를 확인해야한다.
                                // TB_DEVICE_DATA, FEEDER_SN을 검사
                                // DD_MAIN_NO 중 중복이 있는지 검사하면 된다.!!
                                GetData task = new GetData();
                                task.execute("http://" + MainActivity.server_ip + ":" + MainActivity.server_port + "/MMPS_V2/AllPartsCheck/feederlist.php", "feederList"
                                        , etMainDDCode.getText().toString()
                                        , etMachineNo.getText().toString());
                            } else {
                                //Log.d(TAG, "Feeder Serial No. NG");
                                Toast.makeText(All_Parts_Check.this, "Feeder Serial No.를 스캔하여 주십시오.", Toast.LENGTH_SHORT).show();
                            }
                        } else  {
                            // 유진발행 라벨 및 공급사에서 붙혀오는 라벨을 바코드 분리작업
                            // 110404167!WR04X1202FTL!20240502001!10000!WALSIN!2024.05.02
                            String[] splitBarcode = mDecodeResult.toString().split("!");

                            if (splitBarcode.length != 5) {
                                Toast.makeText(All_Parts_Check.this, "유진발행 또는 공급사 부착 라벨을 스캔하여 주십시오.", Toast.LENGTH_SHORT).show();
                            } else {
                                etPartCode.setText(splitBarcode[0]);
                                etPartNo.setText(splitBarcode[1]);
                                etLotNo.setText(splitBarcode[2]);
                                etQty.setText(splitBarcode[3]);
                            }

                            // 모든정보가 입력이 되었다면 결과를 확인 한다.
                            if (etPartCode.length() != 0 &&
                                    etPartNo.length() != 0 &&
                                    etLotNo.length() != 0 &&
                                    etQty.length() != 0 ||
                                    noFeeder.isChecked()){

                                actionWrite();
                            }
                            // 이거 임시로 여기 옮겨둠..
                            // Barcode 분리작업 시작
                            /*
                            String replaceBarcode = StringUtil.barcodeChange(mDecodeResult.toString());

                            GetData php_barcodeSplit = new GetData();
                            php_barcodeSplit.execute("http://" + MainActivity.server_ip + ":" + MainActivity.server_port + "/MMPS_V2/BarcodeSplit/barcodesplit.php",
                                    "BarcodeSplit",
                                    spnMaker.getSelectedItem().toString(),
                                    replaceBarcode);
                            */
                            // Barcode 분리작업 완료
                        }
                    }
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        }
    }

    public void resultWriteQuestion() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("All Parts Check");
        //타이틀설정
        builder.setMessage("Check 결과를 확인 하시겠습니까?");
        builder.setCancelable(false); // 뒤로가기로 취소
        //내용설정
        builder.setPositiveButton("예",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        actionWrite();
                    }
                });

        builder.setNegativeButton("아니오",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });

        builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {

            }
        });
        builder.show();
    }

    public void qtyWriteQuestion() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("확인");
        //타이틀설정
        builder.setMessage("수량을 입력 하시겠습니까?");
        builder.setCancelable(false); // 뒤로가기로 취소
        //내용설정
        builder.setPositiveButton("예",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });

        builder.setNegativeButton("아니오",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        actionWrite();
                    }
                });

        builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {

            }
        });
        builder.show();
    }

    public void WriteQuestion() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("저장");
        //타이틀설정
        builder.setMessage("저장 하시겠습니까?");
        builder.setCancelable(false); // 뒤로가기로 취소
        //내용설정
        builder.setPositiveButton("저장",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        actionWrite();
                    }
                });

        builder.setNegativeButton("취소",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });

        builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {

            }
        });
        builder.show();
    }

    private void actionWrite(){
        if (!etWorker.getText().toString().equals("") &&
                !etMainDDCode.getText().toString().equals("") &&
                !etMachineNo.getText().toString().equals("") &&
                !etFeederNo.getText().toString().equals("") &&
                !etPartCode.getText().toString().equals("")){

            if (etFeederSN.getText().toString().equals("") && noFeeder.isChecked() == false){
                Toast.makeText(All_Parts_Check.this, "Feeder SN을 입력하여 주십시오.", Toast.LENGTH_SHORT).show();
                return;
            }

            Log.d(TAG, "Parts CHeck만하기 체크 여부 : " + noFeeder.isChecked());

            if (!noFeeder.isChecked()) {
                if (etLotNo.length() == 0) {
                    Toast.makeText(All_Parts_Check.this, "Lot No.를 입력하여 주십시오.", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (etQty.length() == 0) {
                    Toast.makeText(All_Parts_Check.this, "제조사 Part No.를 스캔하여 주십시오.", Toast.LENGTH_SHORT).show();
                    return;
                } else {
                    // 현재 숫자로 사용하지 않고 제조사 바코드 검증으로 사용
                    /*
                    //입력된 수량이 숫자인지 확인
                    if (customerName.equals("전유산업") || customerName.equals("J산업")){
                        // 전유산업 바코드의 시작이 2SMJX인지 검사
                        // 수량 입력부분에 전유산업 바코드를 읽어야 한다.
                        if (!etQty.getText().toString().substring(0, 5).equals("2SMJX")){
                            Toast.makeText(AllPartsCheck.this, "J산업 바코드를 스캔하여 주십시오.", Toast.LENGTH_SHORT).show();
                            etQty.setText("");
                            return;
                        }
                    } else if(customerName.equals("CI Digital(슈나이더)") || customerName.equals("CI Digital(아이디스)")) {
                        if (!etQty.getText().toString().contains("_")) {
                            Toast.makeText(this, "아이디스 바코드를 스캔하여 주십시오.", Toast.LENGTH_SHORT).show();
                            etQty.setText("");
                            return;
                        }
                    } else {
                        try {
                            Double.parseDouble(etQty.getText().toString());
                        } catch (NumberFormatException e) {
                            Toast.makeText(this, "수량은 숫자만 입력하여 주십시오.", Toast.LENGTH_SHORT).show();
                            return;
                        }
                    }
                     */
                }
            }

            etLotNo.clearFocus();
            etQty.clearFocus();

            //여기가 자재 검증하는 부분

            Boolean partExist = false;
            String[] partNo = null;

            partNo = orgPartNo.split(",");

            for (int i = 0; i < partNo.length; i++){
                if (etPartCode.getText().toString().equals(partNo[i])){
                    partExist = true;
                    break;
                }
            }

            // if (orgPartNo.indexOf(etPartCode.getText().toString())< 0 ) { // 정상교환이라면
            if (partExist == false) { // 불량교환이라면
                //ngReasonSelect("Part No.가 다릅니다.", "사유를 선택하여 주십시오.");

                /*
                // Barcode 해독을 위해 임시 테이블 TB_SCAN_ERROR_LIST에 현재 내용을 저장한다.
                String insertText = "";

                long now = System.currentTimeMillis();
                Date date = new Date(now);
                SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                String getTime = df.format(date);

                insertText = "insert into TB_SCAN_ERROR_LIST(CHECK_DATE, FACTORY_NAME, CUSTOMER_NAME, MODEL_NAME, WORK_LINE, WORK_SIDE";
                insertText += ", MACHINE_NO, SLOT_NO, PART_MAKER, SPLIT_PART_NO, ORG_BARCODE) values";
                insertText += "('" + getTime + "'";
                insertText += ",'" + factoryName + "'";
                insertText += ",'" + customerName + "'";
                insertText += ",'" + modelName + "'";
                insertText += ",'" + lineName + "'";
                insertText += ",'" + workSide + "'";
                insertText += ",'" + etMachineNo.getText().toString() + "'";
                insertText += ",'" + etFeederNo.getText().toString() + "'";
                insertText += ",'" + spnMaker.getSelectedItem().toString() + "'";
                insertText += ",'" + etPartCode.getText().toString() + "'";
                insertText += ",'" + barcodeHistory + "');";

                //서버로 전송한다.
                GetData taskSave = new GetData();
                taskSave.execute("http://" + MainActivity.server_ip + ":" + MainActivity.server_port + "/MMPS_V2/AllPartsCheck/scan_error_insert.php"
                        , "scanErrorInsert"
                        , insertText);
                 */
                long[] pattern = {500,1000,500,1000};
                vibrator.vibrate(pattern, -1); // miliSecond, 지정한 시간동안 진동
                Mis_Check.showText = "Part No.가 다릅니다.";
                Intent intent = new Intent(getApplicationContext(), Mis_Check.class);
                startActivityForResult(intent, 0);
                return;
            } else {
                vibrator.vibrate(100); // miliSecond, 지정한 시간동안 진동
                resultWrite();
            }
        } else {
            Toast.makeText(All_Parts_Check.this, "모든 항목이 입력되지 않았습니다. " + checkCode, Toast.LENGTH_SHORT).show();
        }
    }

    // 제조사가 여러가지 일경우
    // 순차적(자동)으로 검사하기 위해 작성 중.
    private void partNo_Retest(){
        // ********** 다음 순서의 제조자를 선택해서 서버에 전송. **********
        // ********** 본코드 작성 완료 후 getData 부분에서 추가해야함 **********

        // Barcode 분리작업 시작
        // 자재 제조사별 바코드 해독의 편의를 위해 PHP 서버에서 해독을 실시하도록 변경
        // 21-01-22 박시현

        selMakerNo +=1 ; // 선택 제조사 자동 증가(다음순서)
        spnMaker.setSelection(selMakerNo);
        Log.d(TAG, "현재 선택된 제조사 : " + spnMaker.getSelectedItem().toString());
        /*
        GetData php_barcodeSplit = new GetData();
        php_barcodeSplit.execute("http://" + MainActivity.server_ip + ":" + MainActivity.server_port + "/MMPS_V2/BarcodeSplit/barcodesplit.php",
                "BarcodeSplit",
                spnMaker.getSelectedItem().toString(),
                mDecodeResult.toString().replace("&", "//"));
        */
        // Barcode 분리작업 완료
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (requestCode == 0) { // 알람 결과가 온 경우
            ngReasonString = data.getStringExtra("misReason");
            ngCheckID = data.getStringExtra("checkID");
            // 결과를 DB에 저장한다.
            resultWrite();
        }
    }

    private void resultWrite(){
        String insertText = "";
        String firstChangeText = "";
        long now = System.currentTimeMillis();
        Date date = new Date(now);
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String getTime = df.format(date);

        if (firstCheck == true){
            // 첫번째 체크이면 TB_HISTORY_ALL에 등록한다.
            /*
            insertText = "insert into TB_HISTORY_ALL(CHECK_CODE, CHECK_DATE, DD_MAIN_NO, MACHINE_NO, FACTORY_NAME";
            insertText += ", MODEL_NAME, CUSTOMER_NAME, WORK_LINE, WORK_SIDE, WORKER, CHECK_COMPLETED) values";
            insertText += "('" + checkCode + "'";
            insertText += ",'" + getTime + "'";
            insertText += ",'" + etMainDDCode.getText().toString() + "'";
            insertText += ",'" + etMachineNo.getText().toString() + "'";
            insertText += ",'" + factoryName + "'";
            insertText += ",'" + modelName + "'";
            insertText += ",'" + customerName + "'";
            insertText += ",'" + lineName + "'";
            insertText += ",'" + workSide + "'";
            insertText += ",'" + etWorker.getText().toString() + "'";
            insertText += ",'Run');";
            */

            insertText = "update tb_mmps_history_all";
            insertText += " set check_date = '" + getTime + "'";
            insertText += ", dd_main_no = '" + etMainDDCode.getText().toString() + "'";
            insertText += ", machine_no = '" + etMachineNo.getText().toString() + "'";
            insertText += ", factory_name = '" + factoryName + "'";
            insertText += ", model_name = '" + modelName + "'";
            insertText += ", customer_name = '" + customerName + "'";
            insertText += ", work_line = '" + lineName + "'";
            insertText += ", work_side = '" + workSide + "'";
            insertText += ", worker = '" + etWorker.getText().toString() + "'";
            insertText += " where check_code = '" + checkCode + "';";
        }
        insertText += "insert into tb_mmps_history_all_detail(";
        insertText += "check_code, machine_no, feeder_no, org_part_code, check_part_code, check_part_no, check_lot_no";
        insertText += ", check_qty, check_result, ng_check_id, ng_result, check_date";
        insertText += ") values(";
        insertText += "'" + checkCode + "'";
        insertText += ",'" + etMachineNo.getText().toString() + "'";
        insertText += ",'" + etFeederNo.getText().toString() + "'";
        String[] mainPartNo = orgPartNo.split(",");
        insertText += ",'" + mainPartNo[0] + "'";
        insertText += ",'" + etPartCode.getText().toString() + "'";
        insertText += ",'" + etPartNo.getText().toString() + "'";
        insertText += ",'" + etLotNo.getText().toString() + "'";
        //insertText += ",'" + etQty.getText().toString() + "'";
        /*
        if (customerName.equals("J산업") || customerName.equals("전유산업") || customerName.contains("CI Digital")){
            insertText += ",'" + etQty.getText().toString() + "'";
        } else {
            if (noFeeder.isChecked()) {
                insertText += ",'" + etQty.getText().toString() + "'";
            } else {
                insertText += ",'" + Integer.parseInt(etQty.getText().toString()) + "'";
            }
        }
        */
        if (noFeeder.isChecked()) {
            insertText += ",'" + etQty.getText().toString() + "'";
        } else {
            insertText += ",'" + Integer.parseInt(etQty.getText().toString()) + "'";
        }

        Boolean partExist = false;
        String[] partNo = null;

        partNo = orgPartNo.split(",");

        for (int i = 0; i < partNo.length; i++){
            if (etPartCode.getText().toString().equals(partNo[i])){
                partExist = true;
                break;
            }
        }
        // if (orgPartNo.indexOf(etPartCode.getText().toString()) > -1){ // 정상교환이라면 (Part No List중에 포함이 되어 있다면)
        if (partExist == true){ // 정상교환이라면 (Part No List중에 포함이 되어 있다면)
            insertText += ",'OK'";
            insertText += ",''";
            insertText += ",'" + "" + "'";
            insertText += ",'" + getTime + "');";

            if (noFeeder.isChecked() == false){
                // 신규 FeederSN을 전송
                insertText += "update tb_mmps_device_data set feeder_sn = '" + etFeederSN.getText().toString() + "'";
                insertText += ", feeder_date = '" + getTime + "'";
                insertText += " where dd_main_no = '" + etMainDDCode.getText().toString() + "'";
                insertText += " and machine_no = '" + etMachineNo.getText().toString() + "'";
                insertText += " and feeder_no = '" + etFeederNo.getText().toString() + "';";
            }
        } else { // 오삽이라면
            insertText += ",'NG'";
            insertText += ",'" + ngCheckID + "'";
            insertText += ",'" + ngReasonString + "'";
            insertText += ",'" + getTime + "');";
        }

        // Parts Change 기록만으로는 처음 부착된 자재의 이력을 알 수 없으므로
        // Parts Change에 All Parts Check의 데이터를 기록한다.
        if (noFeeder.isChecked() == false){
            firstChangeText = "insert into tb_mmps_history_check(";
            firstChangeText += "check_code, factory_name, model_name, customer_name, work_line, work_side, dd_main_no, machine_no, feeder_no";
            firstChangeText += ", org_part_code, bef_part_code, chg_part_code, chg_part_no, chg_lot_no, chg_qty, chg_result, ng_check_id, ng_result, check_date, worker";
            firstChangeText += ") values(";
            firstChangeText += "'" + checkCode + "'";
            firstChangeText += ",'" + factoryName + "'";
            firstChangeText += ",'" + modelName + "'";
            firstChangeText += ",'" + customerName + "'";
            firstChangeText += ",'" + lineName + "'";
            firstChangeText += ",'" + workSide + "'";
            firstChangeText += ",'" + etMainDDCode.getText().toString() + "'";
            firstChangeText += ",'" + etMachineNo.getText().toString() + "'";
            firstChangeText += ",'" + etFeederNo.getText().toString() + "'";
            firstChangeText += ",'" + mainPartNo[0] + "'";
            firstChangeText += ",''"; // 여기에 All Parts Check라고 표기해도 된다.(All Parts Check를 구분하기 위함)
            firstChangeText += ",'" + etPartCode.getText().toString() + "'";
            firstChangeText += ",'" + etPartNo.getText().toString() + "'";
            firstChangeText += ",'" + etLotNo.getText().toString() + "'";
            firstChangeText += ",'" + etQty.getText().toString() + "'";
            if (orgPartNo.indexOf(etPartCode.getText().toString()) > -1){ // 정상교환이라면 (Part No List중에 포함이 되어 있다면)
                firstChangeText += ",'OK'";
                firstChangeText += ",''";
                firstChangeText += ",''";
                firstChangeText += ",'" + getTime + "'";
                firstChangeText += ",'" + etWorker.getText().toString() + "');";
            } else { // 오삽이라면
                firstChangeText += ",'NG2'";
                firstChangeText += ",'" + ngCheckID + "'";
                firstChangeText += ",'" + ngReasonString + "'";
                firstChangeText += ",'" + getTime + "'";
                firstChangeText += ",'" + etWorker.getText().toString() + "');";
            }
        }

        //서버로 전송한다.
        GetData taskSave = new GetData();
        taskSave.execute("http://" + MainActivity.server_ip + ":" + MainActivity.server_port + "/MMPS_V2/AllPartsCheck/check_insert.php"
                , "firstInsert"
                , insertText + firstChangeText);
        firstCheck = false;
    }

    public void feederReset_Question() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("피더 초기화");
        //타이틀설정
        builder.setMessage("작업지시번호가 변경 되었습니다.\n Feeder SN을 초기화 하시겠습니까?");
        builder.setCancelable(false); // 뒤로가기로 취소
        //내용설정
        builder.setPositiveButton("초기화",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        String insertText = "";
                        // Feeder SN을 초기화
                        insertText += "update tb_mmps_device_data set feeder_sn = ''";
                        insertText += " where dd_main_no = '" + etMainDDCode.getText().toString() + "'";
                        insertText += " and machine_no = " + etMachineNo.getText().toString() + ";";

                        Log.d(TAG, "전송 SQL : " + insertText);
                        // 서버로 전송한다.
                        GetData taskFeederInit = new GetData();
                        taskFeederInit.execute("http://" + MainActivity.server_ip + ":" + MainActivity.server_port + "/MMPS_V2/AllPartsCheck/feederinit.php", "feederInit"
                                , insertText);
                    }
                });

        builder.setNegativeButton("취소",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });

        builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {

            }
        });
        builder.show();
    }

    private DecodeStateCallback mStateCallback = new DecodeStateCallback(mHandler) {
        public void onChangedState(int state) {
            switch (state) {
                case ScanConst.STATE_ON:
                case ScanConst.STATE_TURNING_ON:
                    if (getEnableDialog().isShowing()) {
                        getEnableDialog().dismiss();
                    }
                    break;
                case ScanConst.STATE_OFF:
                case ScanConst.STATE_TURNING_OFF:
                    if (!getEnableDialog().isShowing()) {
                        getEnableDialog().show();
                    }
                    break;
            }
        };
    };

    private void initScanner() {
        if (mScanner != null) {
            mScanner.aRegisterDecodeStateCallback(mStateCallback);
            mBackupResultType = mScanner.aDecodeGetResultType();
            mScanner.aDecodeSetResultType(ScanConst.ResultType.DCD_RESULT_USERMSG);
        }
    }

    private Runnable mStartOnResume = new Runnable() {
        @Override
        public void run() {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    initScanner();
                    if (mWaitDialog != null && mWaitDialog.isShowing()) {
                        mWaitDialog.dismiss();
                    }
                }
            });
        }
    };

    private AlertDialog getEnableDialog() {
        if (mDialog == null) {
            AlertDialog dialog = new AlertDialog.Builder(this).create();
            dialog.setTitle(R.string.app_name);
            dialog.setMessage("Your scanner is disabled. Do you want to enable it?");

            dialog.setButton(AlertDialog.BUTTON_NEGATIVE, getString(android.R.string.cancel),
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            finish();
                        }
                    });
            dialog.setButton(AlertDialog.BUTTON_POSITIVE, getString(android.R.string.ok),
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Intent intent = new Intent(ScanConst.LAUNCH_SCAN_SETTING_ACITON);
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(intent);
                            dialog.dismiss();
                        }
                    });
            dialog.setCancelable(false);
            mDialog = dialog;
        }
        return mDialog;
    }

    private void verCheck(){
        GetData task_VerLoad = new GetData();
        task_VerLoad.execute( "http://" + MainActivity.server_ip + ":" + MainActivity.server_port + "/yj_mms_ver.php", "ver");
    }

    private void appVerAlarm() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("사용 경고");
        builder.setMessage("프로그램 업데이트가 필요합니다.\n담당자에게 요청하십시오.");
        builder.setPositiveButton("확인",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        //확인 눌렀을때의 이벤트 처리
                        dialog.dismiss();
                        //android.os.Process.killProcess(android.os.Process.myPid());
                        finish();
                    }
                });
        builder.show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        verCheck();
        mWaitDialog = ProgressDialog.show(mContext, "", "Scanner Running...", true);
        mHandler.postDelayed(mStartOnResume, 1000);
        IntentFilter filter = new IntentFilter();
        filter.addAction(ScanConst.INTENT_USERMSG);
        filter.addAction(ScanConst.INTENT_EVENT);
        mContext.registerReceiver(mScanResultReceiver, filter);
    }

    @Override
    protected void onPause() {
        if (mScanner != null) {
            mScanner.aDecodeSetResultType(mBackupResultType);
            mScanner.aUnregisterDecodeStateCallback(mStateCallback);
        }
        mContext.unregisterReceiver(mScanResultReceiver);
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (mScanner != null) {
            mScanner.aDecodeSetResultType(mBackupResultType);
        }
        mScanner = null;

        // 취소이므로 Parts Change 이력을 삭제 한다.
        // 왜 중복 All Parts Check가 취소 되었으므로 Parts Change 이력이 필요없다.
        String deleteText = "delete from tb_mmps_history_check";
        deleteText += " where check_code = '" + checkCode + "';";
        deleteText += "delete from tb_mmps_history_all_detail";
        deleteText += " where check_code = '" + checkCode + "';";
        deleteText += "delete from tb_mmps_history_all";
        deleteText += " where check_code = '" + checkCode + "';";
        //서버로 전송한다.
        GetData taskSave = new GetData();
        taskSave.execute("http://" + MainActivity.server_ip + ":" + MainActivity.server_port + "/MMPS_V2/AllPartsCheck/codeDelete.php"
                , "codeDelete"
                , deleteText);
        super.onDestroy();
    }
}
