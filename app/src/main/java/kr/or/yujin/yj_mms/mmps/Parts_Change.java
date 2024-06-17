package kr.or.yujin.yj_mms.mmps;

import android.app.Activity;
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

public class Parts_Change extends AppCompatActivity {

    private EditText etWorker, etMainDDCode, etMainPartNo, etFeederSN, etBefPartCode, etAftPartCode, etAftPartNo, etAftLotNo, etAftQty;
    private TextView tvMainDDCode, tvWorker, tvFeederSN, tvBefPartCode, tvAftPartCode, tvAftPartNo, tvAftLotNo, tvAftQty, tvStatus;
    private Spinner spnBefMaker, spnAftMaker;
    private CheckBox noQtyAuto;

    private boolean codeRefresh = true;

    private ArrayList<String> befMakerList, aftMakerList; // 스피너의 네임 리스트
    private ArrayAdapter<String> befMakerListADT, aftMakerListADT; // 스피너에 사용되는 ArrayAdapter

    private String checkCode;
    private String orgPartNo, factoryName, lineName, workSide, modelName, customerName, machineNo, feederNo, customerCode;

    //Server 접속주소
    //private static String MainActivity.server_ip = MMPS_Main.server_ip;
    //private static int MainActivity.server_port = MMPS_Main.server_port;

    // Scanner Setting
    private static final String TAG = "Parts Change_New";

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

    private Vibrator vibrator;

    private String order_index;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.mmps_parts_change);

        vibrator = (Vibrator)getSystemService(Context.VIBRATOR_SERVICE);

        control_Initialize();
        etMainDDCode.setText(getIntent().getStringExtra("DD_Main_No"));
        order_index = getIntent().getStringExtra("Order_Index");
        etWorker.setText(getIntent().getStringExtra("Worker"));

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

        // 체크코드 생성 시작(나중에 수정해야 한다. 데이터를 저장 하기전에 체크코드를 체크한다음 바로 데이터를 저장해야 한다.
        // 미리 체크코드 생성하면 중복으로 저장 가능성이 있다.
        // checkCodeMaking();
        // 체크코드 생성 완료

        etAftQty.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                switch (actionId) {
                    case EditorInfo.IME_ACTION_SEARCH:
                        // 검색 동작
                        break;
                    default:
                        // 기본 엔터키 동작
                        etAftQty.clearFocus();
                        InputMethodManager immhide = (InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE);
                        immhide.toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0);
                        actionWrite();
                        return false;
                }
                return true;
            }
        });

        tvWorker.setOnClickListener((new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                etWorker.setText("");
                tvStatus.setText("작업자를 입력하여 주십시오.");
            }
        }));

        tvMainDDCode.setOnClickListener((new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //etMainDDCode.setText("");
                //tvStatus.setText("작업지시번호를 스캔하여 주십시오.");
            }
        }));

        tvFeederSN.setOnClickListener((new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                etFeederSN.setText("");
                etMainPartNo.setText("");
                tvStatus.setText("교환 하려는 Feeder의 Serial No를 스캔하여 주십시오.");
            }
        }));

        tvBefPartCode.setOnClickListener((new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                etBefPartCode.setText("");
                tvStatus.setText("교환 전 자재의 제조사를 선택 후 자재 정보를 입력하여 주십시오.");
            }
        }));

        tvAftPartCode.setOnClickListener((new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                etAftPartCode.setText("");
                etAftPartNo.setText("");
                etAftLotNo.setText("");
                etAftQty.setText("");
                tvStatus.setText("교환 후 자재의 제조사를 선택 후 자재 정보를 입력하여 주십시오.");
            }
        }));

        tvAftPartNo.setOnClickListener((new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                etAftPartCode.setText("");
                etAftPartNo.setText("");
                etAftLotNo.setText("");
                etAftQty.setText("");
                tvStatus.setText("교환 후 자재의 제조사를 선택 후 자재 정보를 입력하여 주십시오.");
            }
        }));

        tvAftLotNo.setOnClickListener((new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                etAftPartCode.setText("");
                etAftPartNo.setText("");
                etAftLotNo.setText("");
                etAftQty.setText("");
                tvStatus.setText("교환 후 자재의 제조사를 선택 후 자재 정보를 입력하여 주십시오.");
            }
        }));

        tvAftQty.setOnClickListener((new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                etAftPartCode.setText("");
                etAftPartNo.setText("");
                etAftLotNo.setText("");
                etAftQty.setText("");
                tvStatus.setText("교환 후 자재의 제조사를 선택 후 자재 정보를 입력하여 주십시오.");
            }
        }));
    }

    private void control_Initialize(){
        etWorker = (EditText) findViewById(R.id.etWorker);
        etMainDDCode = (EditText) findViewById(R.id.etMainDDCode);
        etMainPartNo = (EditText) findViewById(R.id.etMainPartNo);
        etFeederSN = (EditText) findViewById(R.id.etFeederSN);
        etBefPartCode = (EditText) findViewById(R.id.etBefPartCode);
        etAftPartCode = (EditText) findViewById(R.id.etAftPartCode);
        etAftPartNo = (EditText) findViewById(R.id.etAftPartNo);
        etAftLotNo = (EditText) findViewById(R.id.etAftLotNo);
        etAftQty = (EditText) findViewById(R.id.etAftQty);

        tvMainDDCode = (TextView) findViewById(R.id.tvMainDDCode);
        tvWorker = (TextView) findViewById(R.id.tvWorker);
        tvFeederSN = (TextView) findViewById(R.id.tvFeederSN);
        tvBefPartCode = (TextView) findViewById(R.id.tvBefPartCode);
        tvAftPartCode = (TextView) findViewById(R.id.tvAftPartCode);
        tvAftPartNo = (TextView) findViewById(R.id.tvAftPartNo);
        tvAftLotNo = (TextView) findViewById(R.id.tvAftLotNo);
        tvAftQty = (TextView) findViewById(R.id.tvAftQty);
        tvStatus = (TextView) findViewById(R.id.tvStatus);

        noQtyAuto = (CheckBox) findViewById(R.id.noQTYAuto);

        //데이터 준비
        befMakerList = new ArrayList<String>();
        aftMakerList = new ArrayList<String>();

        // 어댑터 생성
        befMakerListADT = new ArrayAdapter<String>(this,
                R.layout.support_simple_spinner_dropdown_item, befMakerList);
        aftMakerListADT = new ArrayAdapter<String>(this,
                R.layout.support_simple_spinner_dropdown_item, aftMakerList);

        // 어댑터 설정
        spnBefMaker = (Spinner) findViewById(R.id.spnBefMaker);
        spnBefMaker.setAdapter(befMakerListADT);
        spnAftMaker = (Spinner) findViewById(R.id.spnAftMaker);
        spnAftMaker.setAdapter(aftMakerListADT);

        tvStatus.setText("Feeder Serial No.를 스캔하여 주십시오.");
    }

    private void actionWrite() {
        if (!etWorker.getText().toString().equals("") &&
                !etMainDDCode.getText().toString().equals("") &&
                !etMainPartNo.getText().toString().equals("") &&
                !etFeederSN.getText().toString().equals("") &&
                !etBefPartCode.getText().toString().equals("") &&
                !etAftPartCode.getText().toString().equals("") &&
                !spnAftMaker.getSelectedItem().toString().equals("") &&
                !spnBefMaker.getSelectedItem().toString().equals("")){

            if (etAftLotNo.length() == 0) {
                Toast.makeText(this, "Lot No.를 입력하여 주십시오.", Toast.LENGTH_SHORT).show();
                return;
            }

            if (etAftQty.length() == 0) {
                Toast.makeText(this, "수량을 입력하여 주십시오.", Toast.LENGTH_SHORT).show();
                return;
            } else {
                //입력된 수량이 숫자인지 확인
                //noQtyAuto.isChecked() == true
                try {
                    Double.parseDouble(etAftQty.getText().toString());
                } catch (NumberFormatException e) {
                    Toast.makeText(this, "수량은 숫자만 입력하여 주십시오.", Toast.LENGTH_SHORT).show();
                }
                /*
                if (customerName.equals("전유산업") || customerName.equals("J산업")) {
                    // 전유산업 바코드의 시작이 2SMJX인지 검사
                    // 수량 입력부분에 전유산업 바코드를 읽어야 한다.
                    if (!etAftQty.getText().toString().substring(0, 5).equals("2SMJX")) {
                        Toast.makeText(this, "J산업 바코드를 스캔하여 주십시오.", Toast.LENGTH_SHORT).show();
                        etAftQty.setText("");
                        return;
                    }
                } else if(customerName.equals("CI Digital(슈나이더)") || customerName.equals("CI Digital(아이디스)")) {
                    if (!etAftQty.getText().toString().contains("_")) {
                        Toast.makeText(this, "아이디스 바코드를 스캔하여 주십시오.", Toast.LENGTH_SHORT).show();
                        etAftQty.setText("");
                        return;
                    }
                } else {
                    try {
                        Double.parseDouble(etAftQty.getText().toString());
                    } catch (NumberFormatException e) {
                        Toast.makeText(this, "수량은 숫자만 입력하여 주십시오.", Toast.LENGTH_SHORT).show();
                    }
                }
                 */
            }

            Boolean befPartExist = false;
            Boolean aftPartExist = false;
            String[] partNo = null;

            partNo = orgPartNo.split(",");

            for (int i = 0; i < partNo.length; i++){
                if (etBefPartCode.getText().toString().equals(partNo[i])){
                    befPartExist = true;
                }
                if (etAftPartCode.getText().toString().equals(partNo[i])){
                    aftPartExist = true;
                }
                if (befPartExist == true && aftPartExist == true){
                    break;
                }
            }

            if (befPartExist == false){
                codeRefresh = false;
                Mis_Check.showText = "교환 전 Part No.가 다릅니다.";
                Mis_Check.ngSection = "NG1";
                long[] pattern = {500,1000,500,1000};
                vibrator.vibrate(pattern, -1); // miliSecond, 지정한 시간동안 진동
                Intent intent = new Intent(getApplicationContext(), Mis_Check.class);
                startActivityForResult(intent, 0);
                return;
            }

            if (aftPartExist == false){
                codeRefresh = false;
                Mis_Check.showText = "교환 후 Part No.가 다릅니다.";
                Mis_Check.ngSection = "NG2";
                long[] pattern = {500,1000,500,1000};
                vibrator.vibrate(pattern, -1); // miliSecond, 지정한 시간동안 진동
                Intent intent = new Intent(getApplicationContext(), Mis_Check.class);
                startActivityForResult(intent, 0);
                return;
            }

            // 정상교환 진동
            vibrator.vibrate(100); // miliSecond, 지정한 시간동안 진동
            /*
            if (orgPartNo.indexOf(etBefPartCode.getText().toString())< 0 ) { // 교환 전 자재가 다르다면
                MisCheckActivity.showText = "교환 전 Part No.가 다릅니다.";
                MisCheckActivity.ngSection = "NG1";
                vibrator.vibrate(1000); // miliSecond, 지정한 시간동안 진동
                Intent intent = new Intent(getApplicationContext(), MisCheckActivity.class);
                startActivityForResult(intent, 0);
                return;
            }

            if (orgPartNo.indexOf(etAftPartCode.getText().toString())< 0 ) { // 교환 후 자재가 다르다면
                MisCheckActivity.showText = "교환 후 Part No.가 다릅니다.";
                MisCheckActivity.ngSection = "NG2";
                vibrator.vibrate(1000); // miliSecond, 지정한 시간동안 진동
                Intent intent = new Intent(getApplicationContext(), MisCheckActivity.class);
                startActivityForResult(intent, 0);
                return;
            }
             */

            etAftLotNo.clearFocus();
            etAftQty.clearFocus();

            // 이거 확인해봐야 한다. 자꾸 쓸때없이 뜬다.
            // 이거 확인해봐야 한다. 자꾸 쓸때없이 뜬다.// 이거 확인해봐야 한다. 자꾸 쓸때없이 뜬다.
            // 이거 확인해봐야 한다. 자꾸 쓸때없이 뜬다.// 이거 확인해봐야 한다. 자꾸 쓸때없이 뜬다.// 이거 확인해봐야 한다. 자꾸 쓸때없이 뜬다.
            // 이거 확인해봐야 한다. 자꾸 쓸때없이 뜬다.// 이거 확인해봐야 한다. 자꾸 쓸때없이 뜬다.// 이거 확인해봐야 한다. 자꾸 쓸때없이 뜬다.
            // 이거 확인해봐야 한다. 자꾸 쓸때없이 뜬다.// 이거 확인해봐야 한다. 자꾸 쓸때없이 뜬다.// 이거 확인해봐야 한다. 자꾸 쓸때없이 뜬다.
            // 이거 확인해봐야 한다. 자꾸 쓸때없이 뜬다.// 이거 확인해봐야 한다. 자꾸 쓸때없이 뜬다.// 이거 확인해봐야 한다. 자꾸 쓸때없이 뜬다.
            
            //InputMethodManager immhide = (InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE);
            //immhide.toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0);

            // 이거 확인해봐야 한다. 자꾸 쓸때없이 뜬다.
            // 이거 확인해봐야 한다. 자꾸 쓸때없이 뜬다.// 이거 확인해봐야 한다. 자꾸 쓸때없이 뜬다.
            // 이거 확인해봐야 한다. 자꾸 쓸때없이 뜬다.// 이거 확인해봐야 한다. 자꾸 쓸때없이 뜬다.// 이거 확인해봐야 한다. 자꾸 쓸때없이 뜬다.
            // 이거 확인해봐야 한다. 자꾸 쓸때없이 뜬다.// 이거 확인해봐야 한다. 자꾸 쓸때없이 뜬다.// 이거 확인해봐야 한다. 자꾸 쓸때없이 뜬다.
            // 이거 확인해봐야 한다. 자꾸 쓸때없이 뜬다.// 이거 확인해봐야 한다. 자꾸 쓸때없이 뜬다.// 이거 확인해봐야 한다. 자꾸 쓸때없이 뜬다.
            // 이거 확인해봐야 한다. 자꾸 쓸때없이 뜬다.// 이거 확인해봐야 한다. 자꾸 쓸때없이 뜬다.// 이거 확인해봐야 한다. 자꾸 쓸때없이 뜬다.

            resultWrite("OK", "", "");
        } else {
            Toast.makeText(Parts_Change.this, "모든 항목이 입력되지 않았습니다. " + checkCode, Toast.LENGTH_SHORT).show();
        }
    }

    private void resultWrite(String result, String resultReason, String checkID){
        String insertText = "";
        long now = System.currentTimeMillis();
        Date date = new Date(now);
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String getTime = df.format(date);

        if (etAftQty.length() == 0){
            etAftQty.setText("0");
        }

        etAftLotNo.clearFocus();
        etAftQty.clearFocus();
        // InputMethodManager immhide = (InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE);
        // immhide.toggleSoftInput(InputMethodManager.HIDE_IMPLICIT_ONLY, 0);

        insertText += "update tb_mmps_history_check";
        insertText += " set factory_name = '" + factoryName + "'";
        insertText += ", model_name = '" + modelName + "'";
        insertText += ", customer_name = '" + customerName + "'";
        insertText += ", work_line = '" + lineName + "'";
        insertText += ", work_side = '" + workSide + "'";
        insertText += ", dd_main_no = '" + etMainDDCode.getText().toString() + "'";
        insertText += ", machine_no = '" + machineNo + "'";
        insertText += ", feeder_no = '" + feederNo + "'";
        insertText += ", org_part_code = '" + etMainPartNo.getText().toString() + "'";
        insertText += ", bef_part_code = '" + etBefPartCode.getText().toString() + "'";
        insertText += ", chg_part_code = '" + etAftPartCode.getText().toString() + "'";
        insertText += ", chg_part_no = '" + etAftPartNo.getText().toString() + "'";
        insertText += ", chg_lot_no = '" + etAftLotNo.getText().toString() + "'";
        insertText += ", CHG_QTY = '" + Integer.parseInt(etAftQty.getText().toString()) + "'";
        /*
        if (customerName.equals("J산업") ||
                customerName.equals("전유산업") ||
                customerName.contains("CI Digital")){
            insertText += ", CHG_QTY = '" + etAftQty.getText().toString() + "'";
        } else {
            insertText += ", CHG_QTY = '" + Integer.parseInt(etAftQty.getText().toString()) + "'";
        }
         */
        insertText += ", chg_result = '" + result + "'"; // OK, NG1, NG2
        insertText += ", ng_check_id = '" + checkID + "'";
        insertText += ", ng_result = '" + resultReason + "'";
        insertText += ", check_date = '" + getTime + "'";
        insertText += ", worker = '" + etWorker.getText().toString() + "'";
        insertText += ", order_index = '" + order_index + "'";
        insertText += " where check_code = '" + checkCode + "';";

        Log.d(TAG, "Parts Change Result : " + insertText);

        //서버로 전송한다.
        getData taskSave = new getData();
        if(result.equals("OK")){
            taskSave.execute("http://" + MainActivity.server_ip + ":" + MainActivity.server_port + "/MMPS_V2/PartsChange/resultsave.php"
                    , "resultSave"
                    , insertText);
            return;
        } else if(result.equals("NG1")){
            taskSave.execute("http://" + MainActivity.server_ip + ":" + MainActivity.server_port + "/MMPS_V2/PartsChange/resultsave1.php"
                    , "resultSave"
                    , insertText);
            return;
        } else if(result.equals("NG2")){
            taskSave.execute("http://" + MainActivity.server_ip + ":" + MainActivity.server_port + "/MMPS_V2/PartsChange/resultsave2.php"
                    , "resultSave"
                    , insertText);
            return;
        }

        // 여기서 result가 "OK"가 아니면 별도로 저장하는 것을 만들자.
        // 왜냐하면 NG시 데이터가 다 없어져버린다. 기존내용을 유지 하도록..
        // saveResult 대신 saveResultNG 같이 다르게 서버에서 받을수 있게 한다.
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
                        if(etWorker.length() == 0){
                            etWorker.setText(mDecodeResult.toString());
                            tvStatus.setText("작업지시번호를 스캔하여 주십시오.");
                        } else if (etMainDDCode.length() == 0){
                            try{
                                String[] scanResultSplit = mDecodeResult.toString().split("-");
                                etMainDDCode.setText(scanResultSplit[0] + "-" + scanResultSplit[1]); // 작업지시번호를 기록
                                tvStatus.setText("교환 하려는 Feeder의 Serial No를 스캔하여 주십시오.");
                            } catch (Exception e){
                                Toast.makeText(Parts_Change.this, "작업지시번호를 스캔하여 주십시오.", Toast.LENGTH_SHORT).show();
                            }
                        } else if (etFeederSN.length() == 0) {
                            if (mDecodeResult.toString().substring(0, 3).equals("FN-")) {
                                etFeederSN.setText(mDecodeResult.toString().replace("FN-", ""));

                                //선택된 피더의 정보를 불러온다.
                                getData task = new getData();
                                task.execute("http://" + MainActivity.server_ip + ":" + MainActivity.server_port + "/MMPS_V2/PartsChange/feederinfo.php"
                                        , "feederInfo"
                                        , etMainDDCode.getText().toString()
                                        , etFeederSN.getText().toString());
                                tvStatus.setText("교환 전 자재의 정보를 입력하여 주십시오.");
                            } else {
                                Toast.makeText(Parts_Change.this, "Feeder Serial No.를 스캔하여 주십시오.", Toast.LENGTH_SHORT).show();
                            }
                        } else if (etBefPartCode.length() == 0) {
                            // 유진발행 라벨 및 공급사에서 붙혀오는 라벨을 바코드 분리작업
                            // 110404167!WR04X1202FTL!20240502001!10000!WALSIN!2024.05.02
                            String[] splitBarcode = mDecodeResult.toString().split("!");

                            if (splitBarcode.length < 5) {
                                Toast.makeText(Parts_Change.this, "유진발행 또는 공급사 부착 라벨을 스캔하여 주십시오.", Toast.LENGTH_SHORT).show();
                            } else {
                                etBefPartCode.setText(splitBarcode[0]);
                                tvStatus.setText("교환 자재의 정보를 입력하여 주십시오.");
                            }

                            // 모든정보가 입력이 되었다면 결과를 확인 한다.
                            // 자재 정보가 비워 있지 않다면.
                            if (etAftPartCode.length() != 0 &&
                                    etAftLotNo.length() != 0 &&
                                    etAftQty.length() != 0 &&
                                    etBefPartCode.length() != 0) {

                                // 결과를 확인 한다.
                                actionWrite();
                                //tvStatus.setText("확인 버튼을 눌러 주십시오.");
                            }
                        } else if (etAftPartCode.length() == 0){
                            // 유진발행 라벨 및 공급사에서 붙혀오는 라벨을 바코드 분리작업
                            // 110404167!WR04X1202FTL!20240502001!10000!WALSIN
                            String[] splitBarcode = mDecodeResult.toString().split("!");

                            if (splitBarcode.length < 5) {
                                Toast.makeText(Parts_Change.this, "유진발행 또는 공급사 부착 라벨을 스캔하여 주십시오.", Toast.LENGTH_SHORT).show();
                            } else {
                                etAftPartCode.setText(splitBarcode[0]);
                                etAftPartNo.setText(splitBarcode[1]);
                                etAftLotNo.setText(splitBarcode[2]);
                                etAftQty.setText((splitBarcode[3]));
                            }

                            //현장 출고가 된 자재인지 검증한다. ######
                            getData task = new getData();
                            task.execute("http://" + MainActivity.server_ip + ":" + MainActivity.server_port + "/MMPS_V2/PartsChange/parts_status_check.php"
                                    , "RankCheck"
                                    , customerCode
                                    , etAftPartCode.getText().toString()
                                    , etAftLotNo.getText().toString()
                            );
                        } else {
                            // 프로세스 변경으로 현재 사용하지 않음.
                            /*
                            // Barcode 분리작업 시작

                            // 자재 제조사별 바코드 해독의 편의를 위해 PHP 서버에서 해독을 실시하도록 변경
                            // 21-01-22 박시현

                            String selPartMaker = "";
                            if (etBefPartCode.length() == 0 ) {
                                selPartMaker = spnBefMaker.getSelectedItem().toString();
                            } else if (etAftPartCode.length() == 0 || etAftLotNo.length() == 0 || etAftQty.length() == 0){
                                selPartMaker = spnAftMaker.getSelectedItem().toString();
                            }

                            String replaceBarcode = StringUtil.barcodeChange(mDecodeResult.toString());
                            getData php_barcodeSplit = new getData();
                            //아이디스 일경우 해독과 비해독이 존재하므로 나눠줘야 한다.
                            if (selPartMaker.contains("아이디스") && etAftPartCode.getText().toString().length() > 0 && etAftLotNo.getText().length() > 0) {
                                php_barcodeSplit.execute("http://" + MainActivity.server_ip + ":" + MainActivity.server_port + "/MMPS_V2/BarcodeSplit/barcodesplit.php",
                                        "BarcodeSplit",
                                        "아이디스2",
                                        replaceBarcode);
                            } else {
                                php_barcodeSplit.execute("http://" + MainActivity.server_ip + ":" + MainActivity.server_port + "/MMPS_V2/BarcodeSplit/barcodesplit.php",
                                        "BarcodeSplit",
                                        selPartMaker,
                                        replaceBarcode);
                            }
                            // Barcode 분리작업 완료
                             */
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
        builder.setTitle("Parts Change");
        //타이틀설정
        builder.setMessage("교환 이력을 저장 하시겠습니까?");
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        //IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 0) { // 알람 결과가 온 경우
            //ngReasonString = data.getStringExtra("misReason");
            //ngCheckID = data.getStringExtra("checkID");
            // 결과를 DB에 저장한다.
            //resultWrite();
            resultWrite(data.getStringExtra("ngSection"), data.getStringExtra("misReason"), data.getStringExtra("checkID"));
            codeRefresh = true;
        }
    }

    private void checkCodeDelete(){
        // Parts Change Code를 삭제한다.
        String deleteText = "delete from tb_mmps_history_check";
        deleteText += " where check_code = '" + checkCode + "';";
        //서버로 전송한다.
        getData taskSave = new getData();
        taskSave.execute("http://" + MainActivity.server_ip + ":" + MainActivity.server_port + "/MMPS_V2/PartsChange/codeDelete.php"
                , "codeDelete"
                , deleteText);
    }

    private void checkCodeMaking(){
        long now = System.currentTimeMillis();
        Date date = new Date(now);
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
        String getDate = df.format(date);
        String[] splitDate = getDate.split("-");

        String cvtYY = NoConvet.noConvert(Integer.parseInt(splitDate[0].substring(2,4)));
        String cvtMM = NoConvet.noConvert(Integer.parseInt(splitDate[1]));
        String cvtDD = NoConvet.noConvert(Integer.parseInt(splitDate[2]));
        checkCode = "CCC" + cvtYY + cvtMM + cvtDD; // 임시 체크코드 저장

        getData task = new getData();
        task.execute("http://" + MainActivity.server_ip + ":" + MainActivity.server_port + "/MMPS_V2/PartsChange/codemaking.php", "codeFind"
                , getDate);
    }

    private class getData extends AsyncTask<String, Void, String> {

        ProgressDialog progressDialog;
        String errorString = null;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
            List<ActivityManager.RunningTaskInfo> info = manager.getRunningTasks(1);
            ComponentName componentName= info.get(0).topActivity;
            String ActivityName = componentName.getShortClassName().substring(1);

            if (ActivityName.equals("App.Activity.PartsChangeActivity_New"))
                progressDialog = ProgressDialog.show(Parts_Change.this,
                        "Connecting to server....\nPlease wait.", null, true, true);
        }

        @Override
        protected String doInBackground(String... params) {

            String serverURL = params[0];

            String secondString = (String) params[1];
            String postParameters = null;

            if (secondString.equals("codeFind")) {
                postParameters = "findDate=" + params[2];
            } else if (secondString.equals("codeSave")) {
                postParameters = "checkCode=" + params[2];
            } else if (secondString.equals("feederInfo")) {
                postParameters = "ddMainNo=" + params[2];
                postParameters += "&feederSN=" + params[3];
                // Log.d(TAG, "ddMainNo = " + params[2]);
                // Log.d(TAG, "Feeder SN = " + params[3]);
            } else if (secondString.equals("resultSave")) {
                postParameters = "sql=" + params[2];
            } else if (secondString.equals("codeDelete")) {
                postParameters = "sql=" + params[2];
            } else if (secondString.equals("BarcodeSplit")) {
                postParameters = "barcode=" + params[3];
                postParameters += "&maker=" + params[2];
            } else if (secondString.equals("RankCheck")) {
                postParameters = "Customer_Code=" + params[2];
                postParameters += "&Part_Code=" + params[3];
                postParameters += "&Lot_No=" + params[4];
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
                Log.d(TAG, "getData : Error ", e);
                errorString = e.toString();
                return null;
            }
        }

        @Override
        protected void onPostExecute (String result){
            super.onPostExecute(result);

            if (progressDialog != null && progressDialog.isShowing())
                progressDialog.dismiss();

            if (result == null){
                Log.d(TAG, "서버 접속 Error - " + errorString);
                Toast.makeText(Parts_Change.this, "서버에 접속 할 수 없습니다.\n상세 내용은 로그를 참조 하십시오.", Toast.LENGTH_SHORT).show();
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

                if (header.equals("makingCode")) {
                    JSONArray jsonArray = jsonObject.getJSONArray("makingCode");
                    JSONObject item = jsonArray.getJSONObject(0);

                    checkCode = item.getString("CHECK_CODE");

                    //String orderNo = item.getString("CHECK_CODE").substring(6, 10); // 조금 다르다 6번째 부터 10번 앞자리까지로 인식해라
                    //int serial = Integer.parseInt(orderNo) + 1;
                    //String number = String.format("%04d", serial);
                    //checkCode += number;

                    // 체크 코드를 임시로 저장한다.
                    getData task = new getData();
                    task.execute("http://" + MainActivity.server_ip + ":" + MainActivity.server_port + "/MMPS_V2/PartsChange/tempcodesave.php", "codeSave"
                            , checkCode);
                } else if (header.equals("makingCode!")) {
                    checkCode += "0001";
                    // 체크 코드를 임시로 저장한다.
                    getData task = new getData();
                    task.execute("http://" + MainActivity.server_ip + ":" + MainActivity.server_port + "/MMPS_V2/PartsChange/tempcodesave.php", "codeSave"
                            , checkCode);
                } else if (header.equals("codeSave")) {
                    JSONArray jsonArray = jsonObject.getJSONArray("codeSave");
                    JSONObject item = jsonArray.getJSONObject(0);
                    if (!item.getString("Result").equals("Success")) {
                        Toast.makeText(Parts_Change.this, mJsonString, Toast.LENGTH_SHORT).show();
                    }
                } else if (header.equals("codeDelete")) {
                    Log.d(TAG, "Check Code 삭제 완료");
                } else if (header.equals("feederInfo!")) {
                    Toast.makeText(Parts_Change.this, "Feeder Setial No.를 확인하여 주십시오.", Toast.LENGTH_SHORT).show();
                } else if (header.equals("feederInfo")) {
                    JSONArray jsonArray = jsonObject.getJSONArray("feederInfo");

                    //for(int i=0;i<jsonArray.length();i++){
                    //    JSONObject item = jsonArray.getJSONObject(i);
                    //}
                    JSONObject item = jsonArray.getJSONObject(0);
                    //orgPartNo = item.getString("MAIN_PART_NO") + "," + item.getString("SUB_PART_NO") + "," + item.getString("SUB_PART_NO2"); // 대표 Part No.
                    orgPartNo = item.getString("MAIN_PART_NO") + "," + item.getString("SUB_PART_NO");
                    etMainPartNo.setText(item.getString("MAIN_PART_NO"));
                    //etFeederNo.setText(item.getString("FEEDER_NO"));  // 첫번째 Feeder No.
                    factoryName = item.getString("FACTORY_NAME");
                    lineName = item.getString("LINE_NAME");
                    workSide = item.getString("WORK_SIDE");
                    modelName = item.getString("MODEL_NAME");
                    customerName = item.getString("CUSTOMER_NAME");
                    customerCode = item.getString("CUSTOMER_Code");
                    machineNo = item.getString("MACHINE_NO");
                    feederNo = item.getString("FEEDER_NO");

                    //수량 자동입력방지 설정
                    /*
                    if (customerName.equals("전유산업") || customerName.equals("J산업") || customerName.contains("CI Digital")) {
                        noQtyAuto.setVisibility(View.VISIBLE);
                        noQtyAuto.setChecked(true);
                    } else {
                        noQtyAuto.setVisibility(View.INVISIBLE);
                        noQtyAuto.setChecked(false);
                    }
                     */

                    // 불러온 제조사를 변수에 저장
                    String listPartMaker = item.getString("MAIN_PART_MAKER");
                    /*
                    if (!item.getString("SUB_PART_MAKER2").equals("")) {
                        listPartMaker = item.getString("SUB_PART_MAKER2") + "," + item.getString("MAIN_PART_MAKER");
                    } else {
                        listPartMaker = item.getString("MAIN_PART_MAKER") + "," + item.getString("SUB_PART_MAKER");
                    }
                     */
                    // 아래쪽은 거의 사용을 안하더라..
                    /*
                    if (customerName.equals("J산업") ||
                            customerName.equals("전유산업") ||
                            customerName.equals("Mangoslab") ||
                            customerName.equals("allRadio") ||
                            customerName.equals("TopRun") ||
                            customerName.equals("덕일전자") ||
                            customerName.contains("CI Digital") ||
                            customerName.equals("L-Tech")) {
                        if (!item.getString("SUB_PART_MAKER2").equals("")) {
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
                    for (int i = 0; i < makerSplit.length; i++) {
                        makerArray.add(makerSplit[i]);
                    }

                    TreeSet<String> makerArray2 = new TreeSet<String>(makerArray); //TreeSet을 이용한 중복데이터 제거
                    Object[] mStringArray = makerArray2.toArray();

                    befMakerListADT.clear();
                    aftMakerListADT.clear();
                    for (int i = 0; i < mStringArray.length; i++) {
                        befMakerListADT.add((String) mStringArray[i]);
                        aftMakerListADT.add((String) mStringArray[i]);
                    }

                    befMakerListADT.notifyDataSetChanged();
                    aftMakerListADT.notifyDataSetChanged();

                    //자재 제조사가 2개 이상일 경우 알림 진동
                    if (befMakerListADT.getCount() > 1) {
                        //long[] pattern = {500,100,500,100,500};
                        //vibrator.vibrate(pattern, -1); // miliSecond, 지정한 시간동안 진동
                    }
                    Log.d(TAG, "Original Part No : " + orgPartNo);
                } else if (header.equals("saveResult")) {
                    JSONArray jsonArray = jsonObject.getJSONArray("saveResult");
                    JSONObject item = jsonArray.getJSONObject(0);
                    if (!item.getString("Result").equals("Success")) {
                        Toast.makeText(Parts_Change.this, mJsonString, Toast.LENGTH_SHORT).show();
                        return;
                    } else {
                        Toast.makeText(Parts_Change.this, "저장 완료.", Toast.LENGTH_SHORT).show();
                        //저장완료시 컨트롤 초기화
                        //etMainDDCode.setText("");
                        etMainPartNo.setText("");
                        etFeederSN.setText("");
                        etBefPartCode.setText("");
                        etAftPartCode.setText("");
                        etAftPartNo.setText("");
                        etAftLotNo.setText("");
                        etAftQty.setText("");
                        tvStatus.setText("작업지시번호를 스캔하여 주십시오.");

                        befMakerListADT.clear();
                        aftMakerListADT.clear();
                        befMakerListADT.notifyDataSetChanged();
                        aftMakerListADT.notifyDataSetChanged();
                        checkCodeMaking();
                    }
                } else if (header.equals("saveResult1")) {
                    JSONArray jsonArray = jsonObject.getJSONArray("saveResult1");
                    JSONObject item = jsonArray.getJSONObject(0);
                    if (!item.getString("Result").equals("Success")) {
                        Toast.makeText(Parts_Change.this, mJsonString, Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(Parts_Change.this, "NG1 저장 완료.", Toast.LENGTH_SHORT).show();
                        etBefPartCode.setText("");
                        etAftPartCode.setText("");
                        etAftPartNo.setText("");
                        etAftLotNo.setText("");
                        etAftQty.setText("");
                        tvStatus.setText("교환 전 자재의 정보를 입력하여 주십시오.");

                        //checkCodeMaking();
                    }
                } else if (header.equals("saveResult2")) {
                    JSONArray jsonArray = jsonObject.getJSONArray("saveResult2");
                    JSONObject item = jsonArray.getJSONObject(0);
                    if (!item.getString("Result").equals("Success")) {
                        Toast.makeText(Parts_Change.this, mJsonString, Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(Parts_Change.this, "NG2 저장 완료.", Toast.LENGTH_SHORT).show();
                        //저장완료시 컨트롤 초기화
                        etBefPartCode.setText("");
                        etAftPartCode.setText("");
                        etAftPartNo.setText("");
                        etAftLotNo.setText("");
                        etAftQty.setText("");
                        tvStatus.setText("교환 후 자재의 정보를 입력하여 주십시오.");

                        //checkCodeMaking();
                    }
                } else if (header.equals("BarcodeSplitResult")) {
                    JSONArray jsonArray = jsonObject.getJSONArray(("BarcodeSplitResult"));
                    JSONObject item = jsonArray.getJSONObject(0);

                    try {
                        String[] partInfo = item.getString("returnStr").split("!@");
                        if (partInfo.length < 3) {
                            Toast.makeText(Parts_Change.this, "바코드 해독에 실패 하였습니다.", Toast.LENGTH_SHORT).show();
                        } else {
                            String nowPartNo = partInfo[0].replace("P:", "");
                            String nowLotNo = partInfo[1].replace("L:", "");
                            String nowQty = partInfo[2].replace("Q:", "");
                            String nowORG = partInfo[3].replace("ORG:", "");
                            if (etBefPartCode.length() == 0) {
                                if (!nowORG.equals("")) {
                                    etBefPartCode.setText(nowORG);
                                } else {
                                    etBefPartCode.setText(nowPartNo);
                                }
                                tvStatus.setText("교환 후 자재의 제조사를 선택 후 자재 정보를 입력하여 주십시오.");
                            } else if (etAftPartCode.length() == 0) {
                                if (!nowORG.equals("")) {
                                    etAftPartCode.setText(nowORG);
                                } else {
                                    if (etAftPartCode.getText().toString().equals("")) {
                                        etAftPartCode.setText(nowPartNo);
                                    }
                                    if (etAftLotNo.getText().toString().equals("")) {
                                        etAftLotNo.setText(nowLotNo);
                                    }
                                    if (noQtyAuto.isChecked() == false) {
                                        etAftQty.setText(nowQty);
                                    }
                                }
                                tvStatus.setText("교환 후 자재의 제조사를 선택 후 자재 정보를 입력하여 주십시오.");

                                // 자재 정보가 비워 있지 않다면.
                                if (etAftPartCode.length() != 0 &&
                                        etAftLotNo.length() != 0 &&
                                        etAftQty.length() != 0) {
                                    // 결과를 확인할지 물어본다.
                                    //resultWriteQuestion();

                                    // 결과를 확인 한다.
                                    actionWrite();
                                    //tvStatus.setText("확인 버튼을 눌러 주십시오.");
                                }
                            } else if (etAftLotNo.length() == 0) {
                                if (!nowORG.equals("")) {
                                    etAftLotNo.setText(nowORG);
                                } else {
                                    if (etAftPartCode.getText().toString().equals("")) {
                                        etAftPartCode.setText(nowPartNo);
                                    }
                                    if (etAftLotNo.getText().toString().equals("")) {
                                        etAftLotNo.setText(nowLotNo);
                                    }
                                    if (noQtyAuto.isChecked() == false) {
                                        etAftQty.setText(nowQty);
                                    }
                                }
                                tvStatus.setText("교환 후 자재의 제조사를 선택 후 자재 정보를 입력하여 주십시오.");
                                //qtyWriteQuestion();

                                // 자재 정보가 비워 있지 않다면.
                                if (etAftPartCode.length() != 0 &&
                                        etAftLotNo.length() != 0 &&
                                        etAftQty.length() != 0) {
                                    // 결과를 확인할지 물어본다.
                                    //resultWriteQuestion();

                                    // 결과를 확인 한다.
                                    actionWrite();
                                    //tvStatus.setText("확인 버튼을 눌러 주십시오.");
                                }
                            } else if (etAftQty.length() == 0) {
                                if (!nowORG.equals("")) {
                                    etAftQty.setText(nowORG);
                                } else {
                                    if (etAftPartCode.getText().toString().equals("")) {
                                        etAftPartCode.setText(nowPartNo);
                                    }
                                    if (etAftLotNo.getText().toString().equals("")) {
                                        etAftLotNo.setText(nowLotNo);
                                    }
                                    if (noQtyAuto.isChecked() == false) {
                                        etAftQty.setText(nowQty);
                                    }
                                }
                                tvStatus.setText("확인 버튼을 눌러 주십시오.");

                                if (etAftPartCode.length() != 0 &&
                                        etAftLotNo.length() != 0 &&
                                        etAftQty.length() != 0) {
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
                } else if (header.equals("RankCheck")) {
                    JSONArray jsonArray = jsonObject.getJSONArray("RankCheck");
                    JSONObject item = jsonArray.getJSONObject(0);
                    if (item.getString("Part_Status").equals("Run")) {
                        // 모든정보가 입력이 되었다면 결과를 확인 한다.
                        // 자재 정보가 비워 있지 않다면.
                        if (etAftPartCode.length() != 0 &&
                                etAftLotNo.length() != 0 &&
                                etAftQty.length() != 0 &&
                                etBefPartCode.length() != 0) {

                            // 결과를 확인 한다.
                            actionWrite();
                        }
                    } else {
                        String showText = "출고된 자재가 아닙니다.";
                        tvStatus.setText(showText);
                        Toast.makeText(Parts_Change.this, showText, Toast.LENGTH_SHORT).show();
                        etAftPartCode.setText("");
                        etAftPartNo.setText("");
                        etAftLotNo.setText("");
                        etAftQty.setText("");
                    }
                } else if (header.equals("RankCheck!")) {
                    String showText = "자재 상태를 확인 할 수 없습니다.";
                    tvStatus.setText(showText);
                    Toast.makeText(Parts_Change.this, showText, Toast.LENGTH_SHORT).show();
                    etAftPartCode.setText("");
                    etAftPartNo.setText("");
                    etAftLotNo.setText("");
                    etAftQty.setText("");
                } else if (header.equals("CheckVer")) {
                    JSONArray jsonArray = jsonObject.getJSONArray("CheckVer");
                    JSONObject item = jsonArray.getJSONObject(0);
                    if (!item.getString("Result").equals("Ver:"+ BuildConfig.VERSION_NAME)){
                        appVerAlarm();
                    }
                } else {
                    Toast.makeText(Parts_Change.this, mJsonString, Toast.LENGTH_SHORT).show();
                }
            } catch (JSONException e) {
                Log.d(TAG, "showResult Error : ", e);
            }
        }
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
        }
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
        getData task_VerLoad = new getData();
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

        if (codeRefresh == true){
            checkCodeMaking();
        }
    }

    @Override
    protected void onPause() {
        if (mScanner != null) {
            mScanner.aDecodeSetResultType(mBackupResultType);
            mScanner.aUnregisterDecodeStateCallback(mStateCallback);
        }
        mContext.unregisterReceiver(mScanResultReceiver);

        if (codeRefresh == true) {
            checkCodeDelete();
        }

        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (mScanner != null) {
            mScanner.aDecodeSetResultType(mBackupResultType);
        }
        mScanner = null;

        // 강제종료 되었을 경우 코드를 삭제 한다.
        // checkCodeDelete();

        super.onDestroy();
    }

    /*
    @Override
    protected void onRestart() {
        super.onRestart();
        // Activity가 재개될때
        checkCodeMaking();
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Activity가 종료될때 onStop

        checkCodeDelete();
    }
     */
}
