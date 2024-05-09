package kr.or.yujin.mmps.App.Activity;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
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
import android.widget.AdapterView;
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
import java.util.TreeSet;

import androidx.appcompat.app.AppCompatActivity;
import device.common.DecodeResult;
import device.common.DecodeStateCallback;
import device.common.ScanConst;
import device.sdk.ScanManager;

import kr.or.yujin.mmps.App.Class.BarcodeSplit;
import kr.or.yujin.mmps.App.Class.NoConvet;
import kr.or.yujin.mmps.App.Class.StringUtil;
import kr.or.yujin.mmps.R;

public class PartsChangeActivity_BLU extends AppCompatActivity {

    //Server 접속주소
    private static String server_ip = MainActivity.server_ip;
    private static int server_port = MainActivity.server_port;

    // Scanner Setting
    private static final String TAG = "Parts Change_BLU";

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

    private Spinner spnPartDivision;
    private EditText etWorker, etModelCode, etSMTLine, etMarking, etFeederSN, etBefPartNo, etAftPartNo, etAftLotNo, etAftQty;
    private EditText etBefRank, etAftRank, etModelName;
    private TextView tvWorker, tvModelCode, tvSMTLine, tvMarking, tvFeederSN, tvBefPartNo, tvBefRank;
    private TextView tvAftPartNo, tvAftRank, tvAftLotNo, tvAftQty;
    private TextView tvStatus;
    private CheckBox noMarking, firstParts;
    private Integer firstSPN = 0;
    private String ledPartNo, ledRank, thermistor, rankOrder, intensity, chromaticity, modelName;

    private String checkCode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_parts_change__b_l_u);

        vibrator = (Vibrator)getSystemService(Context.VIBRATOR_SERVICE);

        server_ip = MainActivity.server_ip;
        server_port = MainActivity.server_port;

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

        // checkCodeMaking();

        tvStatus = (TextView) findViewById(R.id.tvStatus);
        tvStatus.setText("작업자를 입력하여 주십시오.");

        spnPartDivision = (Spinner) findViewById(R.id.spnPartDivison);
        ArrayAdapter workSideADT = ArrayAdapter.createFromResource(this, R.array.blu_PartDivison, android.R.layout.simple_spinner_dropdown_item);
        spnPartDivision.setAdapter(workSideADT);

        etWorker = (EditText) findViewById(R.id.etWorker);
        etModelCode = (EditText) findViewById(R.id.etModelCode);
        etSMTLine = (EditText) findViewById(R.id.etSMTLine);
        etMarking = (EditText) findViewById(R.id.etMarking);
        etFeederSN = (EditText) findViewById(R.id.etFeederSN);
        etBefPartNo = (EditText) findViewById(R.id.etBefPartNo);
        etBefRank = (EditText) findViewById(R.id.etBefRank);
        etAftPartNo = (EditText) findViewById(R.id.etAftPartNo);
        etAftRank = (EditText) findViewById(R.id.etAftRank);
        etAftLotNo = (EditText) findViewById(R.id.etAftLotNo);
        etAftQty = (EditText) findViewById(R.id.etAftQty);
        etModelName = (EditText) findViewById(R.id.etModelName);

        noMarking = (CheckBox) findViewById(R.id.noMarking);

        noMarking.setOnClickListener(new CheckBox.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO : process the click event.
                if (((CheckBox)v).isChecked()) {
                    // TODO : CheckBox is checked.
                    //etMarking.setEnabled(false);
                    etMarking.setText("");
                } else {
                    // TODO : CheckBox is unchecked.
                    //etMarking.setEnabled(true);
                }
            }
        }) ;

        firstParts = (CheckBox) findViewById(R.id.firstParts);

        firstParts.setOnClickListener(new CheckBox.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO : process the click event.
                if (((CheckBox)v).isChecked()) {
                    // TODO : CheckBox is checked.
                    //etBefPartNo.setEnabled(false);
                    etBefPartNo.setText("");
                    //etBefRank.setEnabled(false);
                    etBefRank.setText("");
                } else {
                    // TODO : CheckBox is unchecked.
                    //etBefPartNo.setEnabled(true);
                    //etBefRank.setEnabled(true);
                }
            }
        }) ;

        etMarking.setOnEditorActionListener(new TextView.OnEditorActionListener() {
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

        spnPartDivision.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                // Your code here
                if (firstSPN == 0){
                    firstSPN += 1; // 처음 실행될때는 무시하도록
                } else {
                    if (spnPartDivision.getSelectedItemPosition() == 0) {
                        tvStatus.setText("자재 구분을 선택하여 주십시오.");
                    } else if (spnPartDivision.getSelectedItemPosition() == 1) {
                        if (firstParts.isChecked()){
                            //etBefRank.setEnabled(false);
                            //etAftRank.setEnabled(true);
                            tvStatus.setText("자재의 정보를 입력하여 주십시오.");
                        } else {
                            //etBefRank.setEnabled(true);
                            //etAftRank.setEnabled(true);
                            tvStatus.setText("교환 전 자재의 정보를 입력하여 주십시오.");
                        }
                        Log.d(TAG, "RANK 입력 활성화");
                    } else if (spnPartDivision.getSelectedItemPosition() == 2) {
                        //etBefRank.setEnabled(false);
                        //etAftRank.setEnabled(false);
                        Log.d(TAG, "RANK 입력 비활성화");
                        tvStatus.setText("교환 전 자재의 정보를 입력하여 주십시오.");
                    }
                }
            }

            public void onNothingSelected(AdapterView<?> adapterView) {
                return;
            }
        });

        tvWorker = (TextView) findViewById(R.id.tvWorker);
        tvModelCode = (TextView) findViewById(R.id.tvModelCode);
        tvSMTLine = (TextView) findViewById(R.id.tvSMTLine);
        tvMarking = (TextView) findViewById(R.id.tvMarking);
        tvFeederSN = (TextView) findViewById(R.id.tvFeederSN);
        tvBefPartNo = (TextView) findViewById(R.id.tvBefPartNo);
        tvBefRank = (TextView) findViewById(R.id.tvBefRank);
        tvAftPartNo = (TextView) findViewById(R.id.tvAftPartNo);
        tvAftRank = (TextView) findViewById(R.id.tvAftRank);
        tvAftLotNo = (TextView) findViewById(R.id.tvAftLotNo);
        tvAftQty = (TextView) findViewById(R.id.tvAftQty);

        tvWorker.setOnClickListener((new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                etWorker.setText("");
                tvStatus.setText("작업자를 입력하여 주십시오.");
            }
        }));

        tvModelCode.setOnClickListener((new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                etModelCode.setText("");
                etModelName.setText("");
                tvStatus.setText("BLU 모델코드를 스캔하여 주십시오.");
            }
        }));

        tvSMTLine.setOnClickListener((new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                etSMTLine.setText("");
                tvStatus.setText("SMT Line을 스캔하여 주십시오.");
            }
        }));

        tvMarking.setOnClickListener((new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!noMarking.isChecked()){
                    etMarking.setText("");
                    tvStatus.setText("Marking or Lot No.를 입력하여 주십시오.");
                }
            }
        }));

        tvFeederSN.setOnClickListener((new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                etFeederSN.setText("");
                etBefPartNo.setText("");
                etBefRank.setText("");
                etAftPartNo.setText("");
                etAftRank.setText("");
                etAftLotNo.setText("");
                etAftQty.setText("");
                tvStatus.setText("Feeder SN을 입력(스캔)하여 주십시오.");
            }
        }));

        tvBefPartNo.setOnClickListener((new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                etBefPartNo.setText("");
                tvStatus.setText("교환 전 자재의 정보를 입력하여 주십시오.");
            }
        }));

        tvBefRank.setOnClickListener((new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                etBefRank.setText("");
                tvStatus.setText("교환 전 자재의 정보를 입력하여 주십시오.");
            }
        }));

        tvAftPartNo.setOnClickListener((new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                etAftPartNo.setText("");
                tvStatus.setText("교환 후 자재의 정보를 입력하여 주십시오.");
            }
        }));

        tvAftRank.setOnClickListener((new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                etAftRank.setText("");
                tvStatus.setText("교환 전 자재의 정보를 입력하여 주십시오.");
            }
        }));

        tvAftLotNo.setOnClickListener((new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                etAftLotNo.setText("");
                tvStatus.setText("교환 전 자재의 정보를 입력하여 주십시오.");
            }
        }));

        tvAftQty.setOnClickListener((new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                etAftQty.setText("");
                tvStatus.setText("교환 전 자재의 정보를 입력하여 주십시오.");
            }
        }));
    }

    private void checkCodeDelete(){
        // 취소이므로 Parts Change 이력을 삭제 한다.
        // 왜 중복 All Parts Check가 취소 되었으므로 Parts Change 이력이 필요없다.
        String deleteText = "delete from TB_HISTORY_BLU";
        deleteText += " where CHECK_CODE = '" + checkCode + "';";
        //서버로 전송한다.
        getData taskSave = new getData();
        taskSave.execute("http://" + server_ip + ":" + server_port + "/MMPS/PartsChange_BLU/codeDelete.php"
                , "codeDelete"
                , deleteText);
        checkCode = "";
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
        checkCode = "BCC" + cvtYY + cvtMM + cvtDD;

        getData task = new getData();
        task.execute("http://" + server_ip + ":" + server_port + "/MMPS/PartsChange_BLU/codemaking.php"
                , "codeFind"
                , getDate);
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
                        if (etWorker.length() == 0) {
                            etWorker.setText(mDecodeResult.toString());
                            tvStatus.setText("BLU 모델코드를 스캔하여 주십시오.");
                            return;
                        } else if (etModelCode.length() == 0) {
                            Log.d(TAG, "Model Code = " + mDecodeResult.toString().substring(0, 4));
                            if (!mDecodeResult.toString().substring(0, 4).equals("BLU-")) {
                                tvStatus.setText("BLU 모델코드를 스캔하여 주십시오.");
                                return;
                            }
                            etModelCode.setText(mDecodeResult.toString().replace("BLU-", ""));
                            tvStatus.setText("SMT Line을 스캔하여 주십시오.");
                            // Model Code를 조회해서 Led와 Thermistor의 Part No, Rank 정보를 불러온다.
                            getData task = new getData();
                            task.execute("http://" + server_ip + ":" + server_port + "/MMPS/PartsChange_BLU/modelInfo.php"
                                    , "modelInfo"
                                    , etModelCode.getText().toString());
                            return;
                        } else if (etSMTLine.length() == 0) {
                            etSMTLine.setText(mDecodeResult.toString());
                            if (noMarking.isChecked()) {
                                tvStatus.setText("자재 구분을 선택하여 주십시오.");
                            } else {
                                tvStatus.setText("Marking or Lot No.를 입력하여 주십시오.");
                            }
                            return;
                        } else if (etMarking.length() == 0 && !noMarking.isChecked()) {
                            etMarking.setText(mDecodeResult.toString());
                            // tvStatus.setText("교환 하려는 Feeder의 Serial No를 스캔하여 주십시오.");
                            tvStatus.setText("재재 구분을 선택하여 주십시오.");
                            return;
                        } else if (etFeederSN.length() == 0) {
                            if (spnPartDivision.getSelectedItem().toString().equals("자재 구분을 선택하여 주십시오.")){
                                Toast.makeText(PartsChangeActivity_BLU.this, "자재 구분을 먼저 선택하여 주십시오.", Toast.LENGTH_SHORT).show();
                                return;
                            }

                            if (mDecodeResult.toString().indexOf("-") < 0){
                                Toast.makeText(PartsChangeActivity_BLU.this, "Feeder SN을 입력(스캔)하여 주십시오.", Toast.LENGTH_SHORT).show();
                                return;
                            }
                            etFeederSN.setText(mDecodeResult.toString());
                            // tvStatus.setText("교환 전 자재의 정보를 입력하여 주십시오.");
                            tvStatus.setText("자재 구분을 선택하여 주십시오.");
                            return;
                        } else if (etBefPartNo.length() == 0 && !firstParts.isChecked()) {
                            String barcodeReturn = "";
                            // LED일때
                            if (spnPartDivision.getSelectedItem().toString().equals("LED")) {
                                barcodeReturn = BarcodeSplit.Barcode_Split(mDecodeResult.toString(), "NICHIA");
                            } else if (spnPartDivision.getSelectedItem().toString().equals("Thermistor")) {
                                barcodeReturn = BarcodeSplit.Barcode_Split(mDecodeResult.toString(), "MURATA");
                            }

                            if (barcodeReturn.indexOf("!@") > 0) { // 분리가 된 데이터가 들어 온것이라면
                                try {
                                    String[] partInfo = barcodeReturn.split("!@");
                                    if (etBefPartNo.length() == 0) {
                                        etBefPartNo.setText(partInfo[0].replace("P:", ""));
                                    }
                                    if (etBefRank.length() == 0) {
                                        etBefRank.setText(partInfo[3].replace("R:", ""));
                                    }
                                } catch (Exception e) {
                                    Log.d("!!!!!!!!!!!!!!!!!!!!!!", "분리한 결과가 다시 분리되지 않는다 : ", e);
                                    return;
                                }
                            } else { // 원래 내용이 다시 돌아 온 것이라면
                                etBefPartNo.setText(mDecodeResult.toString());
                            }
                            if (etBefRank.isEnabled()) {
                                tvStatus.setText("교환 전 자재의 Rank를 입력하여 주십시오.");
                            } else {
                                tvStatus.setText("교환 후 자재의 정보를 입력하여 주십시오.");
                            }
                            if (etAftPartNo.length() > 0 &&
                                    etAftLotNo.length() > 0 && etAftQty.length() > 0) {
                                actionWrite();
                            }
                            return;
                        } else if (etBefRank.length() == 0 && !firstParts.isChecked()
                                && spnPartDivision.getSelectedItem().toString().equals("LED")) {
                            String barcodeReturn = "";
                            barcodeReturn = BarcodeSplit.Barcode_Split(mDecodeResult.toString(), "NICHIA");

                            if (barcodeReturn.indexOf("!@") > 0) { // 분리가 된 데이터가 들어 온것이라면
                                try {
                                    String[] partInfo = barcodeReturn.split("!@");
                                    etBefRank.setText(partInfo[3].replace("R:", ""));
                                } catch (Exception e) {
                                    Log.d("!!!!!!!!!!!!!!!!!!!!!!", "분리한 결과가 다시 분리되지 않는다 : ", e);
                                    return;
                                }
                            } else { // 원래 내용이 다시 돌아 온 것이라면
                                etBefRank.setText(mDecodeResult.toString());
                            }
                            tvStatus.setText("교환 후 자재의 정보를 입력하여 주십시오.");
                            if (etAftPartNo.length() > 0 &&
                                    etAftLotNo.length() > 0 && etAftQty.length() > 0) {
                                actionWrite();
                            }
                            return;
                        } else if (etAftPartNo.length() == 0) {
                            String barcodeReturn = "";
                            // LED일때
                            if (spnPartDivision.getSelectedItem().toString().equals("LED")) {
                                barcodeReturn = BarcodeSplit.Barcode_Split(mDecodeResult.toString(), "NICHIA");
                            } else if (spnPartDivision.getSelectedItem().toString().equals("Thermistor")) {
                                barcodeReturn = BarcodeSplit.Barcode_Split(mDecodeResult.toString(), "MURATA");
                            }

                            if (barcodeReturn.indexOf("!@") > 0) { // 분리가 된 데이터가 들어 온것이라면
                                try {
                                    String[] partInfo = barcodeReturn.split("!@");
                                    if (etAftPartNo.length() == 0) {
                                        etAftPartNo.setText(partInfo[0].replace("P:", ""));
                                    }
                                    if (etAftLotNo.length() == 0) {
                                        etAftLotNo.setText(partInfo[1].replace("L:", ""));
                                    }
                                    if (etAftQty.length() == 0) {
                                        etAftQty.setText(partInfo[2].replace("Q:", ""));
                                    }
                                    if (etAftRank.length() == 0) {
                                        etAftRank.setText(partInfo[3].replace("R:", ""));
                                    }
                                } catch (Exception e) {
                                    Log.d("!!!!!!!!!!!!!!!!!!!!!!", "분리한 결과가 다시 분리되지 않는다 : ", e);
                                    return;
                                }
                            } else { // 원래 내용이 다시 돌아 온 것이라면
                                etAftPartNo.setText(mDecodeResult.toString());
                            }
                            if (etAftPartNo.length() > 0 &&
                                    etAftLotNo.length() > 0 && etAftQty.length() > 0) {
                                actionWrite();
                            }
                            return;
                        } else if (etAftRank.length() == 0 && spnPartDivision.getSelectedItem().toString().equals("LED")) {
                            String barcodeReturn = "";
                            // LED일때
                            if (spnPartDivision.getSelectedItem().toString().equals("LED")) {
                                barcodeReturn = BarcodeSplit.Barcode_Split(mDecodeResult.toString(), "NICHIA");
                            } else if (spnPartDivision.getSelectedItem().toString().equals("Thermistor")) {
                                barcodeReturn = BarcodeSplit.Barcode_Split(mDecodeResult.toString(), "MURATA");
                            }

                            if (barcodeReturn.indexOf("!@") > 0) { // 분리가 된 데이터가 들어 온것이라면
                                try {
                                    String[] partInfo = barcodeReturn.split("!@");
                                    if (etAftPartNo.length() == 0) {
                                        etAftPartNo.setText(partInfo[0].replace("P:", ""));
                                    }
                                    if (etAftLotNo.length() == 0) {
                                        etAftLotNo.setText(partInfo[1].replace("L:", ""));
                                    }
                                    if (etAftQty.length() == 0) {
                                        etAftQty.setText(partInfo[2].replace("Q:", ""));
                                    }
                                    if (etAftRank.length() == 0) {
                                        etAftRank.setText(partInfo[3].replace("R:", ""));
                                    }
                                } catch (Exception e) {
                                    Log.d("!!!!!!!!!!!!!!!!!!!!!!", "분리한 결과가 다시 분리되지 않는다 : ", e);
                                    return;
                                }
                            } else { // 원래 내용이 다시 돌아 온 것이라면
                                etAftRank.setText(mDecodeResult.toString());
                            }
                            if (etAftPartNo.length() > 0 &&
                                    etAftLotNo.length() > 0 && etAftQty.length() > 0) {
                                actionWrite();
                            }
                            return;
                        } else if (etAftLotNo.length() == 0) {
                            String barcodeReturn = "";
                            // LED일때
                            if (spnPartDivision.getSelectedItem().toString().equals("LED")) {
                                barcodeReturn = BarcodeSplit.Barcode_Split(mDecodeResult.toString(), "NICHIA");
                            } else if (spnPartDivision.getSelectedItem().toString().equals("Thermistor")) {
                                barcodeReturn = BarcodeSplit.Barcode_Split(mDecodeResult.toString(), "MURATA");
                            }

                            if (barcodeReturn.indexOf("!@") > 0) { // 분리가 된 데이터가 들어 온것이라면
                                try {
                                    String[] partInfo = barcodeReturn.split("!@");
                                    if (etAftPartNo.length() == 0) {
                                        etAftPartNo.setText(partInfo[0].replace("P:", ""));
                                    }
                                    if (etAftLotNo.length() == 0) {
                                        etAftLotNo.setText(partInfo[1].replace("L:", ""));
                                    }
                                    if (etAftQty.length() == 0) {
                                        etAftQty.setText(partInfo[2].replace("Q:", ""));
                                    }
                                    if (etAftRank.length() == 0) {
                                        etAftRank.setText(partInfo[3].replace("R:", ""));
                                    }
                                } catch (Exception e) {
                                    Log.d("!!!!!!!!!!!!!!!!!!!!!!", "분리한 결과가 다시 분리되지 않는다 : ", e);
                                    return;
                                }
                            } else { // 원래 내용이 다시 돌아 온 것이라면
                                etAftLotNo.setText(mDecodeResult.toString());
                            }
                            if (etAftPartNo.length() > 0 &&
                                    etAftLotNo.length() > 0 && etAftQty.length() > 0) {
                                actionWrite();
                            }
                            return;
                        } else if (etAftQty.length() == 0) {
                            String barcodeReturn = "";
                            // LED일때
                            if (spnPartDivision.getSelectedItem().toString().equals("LED")) {
                                barcodeReturn = BarcodeSplit.Barcode_Split(mDecodeResult.toString(), "NICHIA");
                            } else if (spnPartDivision.getSelectedItem().toString().equals("Thermistor")) {
                                barcodeReturn = BarcodeSplit.Barcode_Split(mDecodeResult.toString(), "MURATA");
                            }

                            if (barcodeReturn.indexOf("!@") > 0) { // 분리가 된 데이터가 들어 온것이라면
                                try {
                                    String[] partInfo = barcodeReturn.split("!@");
                                    if (etAftPartNo.length() == 0) {
                                        etAftPartNo.setText(partInfo[0].replace("P:", ""));
                                    }
                                    if (etAftLotNo.length() == 0) {
                                        etAftLotNo.setText(partInfo[1].replace("L:", ""));
                                    }
                                    if (etAftQty.length() == 0) {
                                        etAftQty.setText(partInfo[2].replace("Q:", ""));
                                    }
                                    if (etAftRank.length() == 0) {
                                        etAftRank.setText(partInfo[3].replace("R:", ""));
                                    }
                                } catch (Exception e) {
                                    Log.d("!!!!!!!!!!!!!!!!!!!!!!", "분리한 결과가 다시 분리되지 않는다 : ", e);
                                    return;
                                }
                            } else { // 원래 내용이 다시 돌아 온 것이라면
                                if (StringUtil.isNumeric(mDecodeResult.toString()) == false) {
                                    Toast.makeText(PartsChangeActivity_BLU.this, "숫자만 입력(스캔) 하여주십시오.", Toast.LENGTH_SHORT).show();
                                    return;
                                } else {
                                    etAftQty.setText(mDecodeResult.toString());
                                }
                            }
                            if (etAftPartNo.length() > 0 &&
                                    etAftLotNo.length() > 0 && etAftQty.length() > 0) {
                                actionWrite();
                            }
                            return;
                        }
                    }
                }catch (Exception e){
                    e.printStackTrace();
                }
            }
        }
    }

    private void actionWrite() {
        if (etWorker.length() == 0){
            Toast.makeText(PartsChangeActivity_BLU.this, "작업자를 입력(스캔)하여 주십시오.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (etModelCode.length() == 0){
            Toast.makeText(PartsChangeActivity_BLU.this, "모델코드를 입력(스캔)하여 주십시오.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (etSMTLine.length() == 0){
            Toast.makeText(PartsChangeActivity_BLU.this, "SMT Line을 입력(스캔)하여 주십시오.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (etMarking.length() == 0 && noMarking.isChecked() == false){
            Toast.makeText(PartsChangeActivity_BLU.this, "Marking or Lot No.를 입력(스캔)하여 주십시오.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (spnPartDivision.getSelectedItem().toString().equals("자재 구분을 선택하여 주십시오.")){
            Toast.makeText(PartsChangeActivity_BLU.this, "자재 구분을 선택하여 주십시오.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (etFeederSN.length() == 0){
            Toast.makeText(PartsChangeActivity_BLU.this, "Feeder SN을 입력(스캔)하여 주십시오.", Toast.LENGTH_SHORT).show();
            return;
        }
        if (spnPartDivision.getSelectedItem().toString().equals("LED")){
            if (etBefRank.length() == 0 && !firstParts.isChecked()){
                Toast.makeText(PartsChangeActivity_BLU.this, "교환 전 자재의 Rank를 입력(스캔)하여 주십시오.", Toast.LENGTH_SHORT).show();
                return;
            }
            if (etAftRank.length() == 0){
                Toast.makeText(PartsChangeActivity_BLU.this, "교환 후 자재의 Rank를 입력(스캔)하여 주십시오.", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        String nowPartNo = null;
        if (spnPartDivision.getSelectedItem().toString().equals("LED")){
            nowPartNo = ledPartNo;
        } else {
            nowPartNo = thermistor;
        }

        if (!etBefPartNo.getText().toString().equals(nowPartNo) && !firstParts.isChecked()){
            MisCheckActivity.showText = "교환 전 Part No.가 다릅니다.";
            MisCheckActivity.ngSection = "NG1";
            vibrator.vibrate(1000); // miliSecond, 지정한 시간동안 진동
            Intent intent = new Intent(getApplicationContext(), MisCheckActivity.class);
            startActivityForResult(intent, 0);
            return;
        }

        if (!etAftPartNo.getText().toString().equals(nowPartNo)){
            MisCheckActivity.showText = "교환 후 Part No.가 다릅니다.";
            MisCheckActivity.ngSection = "NG2";
            vibrator.vibrate(1000); // miliSecond, 지정한 시간동안 진동
            Intent intent = new Intent(getApplicationContext(), MisCheckActivity.class);
            startActivityForResult(intent, 0);
            return;
        }

        // LED 교환일때 RANK 양불판정
        if (spnPartDivision.getSelectedItem().toString().equals("LED")){
            Boolean befRankExist = false;
            Boolean aftRankExist = false;
            String[] rankList = null;

            rankList = ledRank.split(",");

            for (int i = 0; i < rankList.length; i++){
                if (etBefRank.getText().toString().equals(rankList[i])){
                    befRankExist = true;
                }
                if (etAftRank.getText().toString().equals(rankList[i])){
                    aftRankExist = true;
                }
                if (befRankExist == true && aftRankExist == true){
                    break;
                }
            }

            if (befRankExist == false && !firstParts.isChecked()){
                MisCheckActivity.showText = "교환 전 Rank 오류 입니다.";
                MisCheckActivity.ngSection = "NG3";
                vibrator.vibrate(1000); // miliSecond, 지정한 시간동안 진동
                Intent intent = new Intent(getApplicationContext(), MisCheckActivity.class);
                startActivityForResult(intent, 0);
                return;
            }

            if (aftRankExist == false){
                MisCheckActivity.showText = "교환 후 Rank 오류 입니다.";
                MisCheckActivity.ngSection = "NG4";
                vibrator.vibrate(1000); // miliSecond, 지정한 시간동안 진동
                Intent intent = new Intent(getApplicationContext(), MisCheckActivity.class);
                startActivityForResult(intent, 0);
                return;
            }
        }

        resultWrite("OK","","");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        //IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (requestCode == 0) { // 알람 결과가 온 경우
            //ngReasonString = data.getStringExtra("misReason");
            //ngCheckID = data.getStringExtra("checkID");
            // 결과를 DB에 저장한다.
            //resultWrite();
            if (resultCode == 1){ // 알람 확인 결과가 온 경우.
                resultWrite(data.getStringExtra("ngSection"), data.getStringExtra("misReason"), data.getStringExtra("checkID"));
            }
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

        String nowPartNo = null;
        String writeRank = null;
        if (spnPartDivision.getSelectedItem().toString().equals("LED")){
            nowPartNo = ledPartNo;
            writeRank = ledRank;
        } else {
            nowPartNo = thermistor;
            writeRank = "";
        }

        insertText += "update TB_HISTORY_BLU";
        insertText += " set MODEL_CODE = '" + etModelCode.getText().toString() + "'";
        insertText += ", MODEL_NAME = '" + etModelName.getText().toString() + "'";
        insertText += ", WORK_LINE = '" + etSMTLine.getText().toString() + "'";
        insertText += ", MARKING = '" + etMarking.getText().toString() + "'";
        insertText += ", PART_DIVISION = '" + spnPartDivision.getSelectedItem().toString() + "'";
        insertText += ", FEEDER_NO = '" + etFeederSN.getText().toString() + "'";
        insertText += ", ORG_PART_NO = '" + nowPartNo + "'";
        insertText += ", BEF_PART_NO = '" + etBefPartNo.getText().toString() + "'";
        insertText += ", BEF_RANK = '" + etBefRank.getText().toString() + "'";
        insertText += ", CHG_PART_NO = '" + etAftPartNo.getText().toString() + "'";
        insertText += ", CHG_RANK = '" + etAftRank.getText().toString() + "'";
        insertText += ", CHG_LOT_NO = '" + etAftLotNo.getText().toString() + "'";
        insertText += ", CHG_QTY = '" + Integer.parseInt(etAftQty.getText().toString()) + "'";
        insertText += ", CHG_RESULT = '" + result + "'"; // OK, NG1, NG2, NG3, NG4
        insertText += ", NG_CHECK_ID = '" + checkID + "'";
        insertText += ", NG_RESULT = '" + resultReason + "'";
        insertText += ", CHECK_DATE = '" + getTime + "'";
        insertText += ", WORKER = '" + etWorker.getText().toString() + "'";
        insertText += ", AVAILABLE_RANK = '" + writeRank + "'";
        insertText += " where CHECK_CODE = '" + checkCode + "';";

        Log.d(TAG, "Parts Change Result : " + result);
        Log.d(TAG, "insertText : " + insertText);

        //서버로 전송한다.
        getData taskSave = new getData();
        if(result.equals("OK")){
            taskSave.execute("http://" + server_ip + ":" + server_port + "/MMPS/PartsChange_BLU/resultsave.php"
                    , "resultSave"
                    , insertText);
            return;
        } else if(result.equals("NG1") || result.equals("NG3")){
            taskSave.execute("http://" + server_ip + ":" + server_port + "/MMPS/PartsChange_BLU/resultsave1.php"
                    , "resultSave"
                    , insertText);
            return;
        } else if(result.equals("NG2") || result.equals("NG4")){
            taskSave.execute("http://" + server_ip + ":" + server_port + "/MMPS/PartsChange_BLU/resultsave2.php"
                    , "resultSave"
                    , insertText);
            return;
        }
    }

    private class getData extends AsyncTask<String, Void, String> {

        String errorString = null;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected String doInBackground(String... params) {

            String serverURL = params[0];

            String secondString = (String) params[1];
            String postParameters = null;

            if (secondString.equals("codeFind")) {
                postParameters = "findDate=" + params[2];
            } else if (secondString.equals("codeDelete")) {
                postParameters = "sql=" + params[2];
            } else if (secondString.equals("codeFind")){
                postParameters = "findDate=" + params[2];
            } else if (secondString.equals("codeSave")) {
                postParameters = "checkCode=" + params[2];
            } else if (secondString.equals("modelInfo")){
                postParameters = "modelCode=" + params[2];
            } else if (secondString.equals("resultSave")) {
                postParameters = "sql=" + params[2];
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

            Log.d(TAG, "showResult 이전 response - " + result);
            showResult(result);
        }

        private void showResult (String mJsonString){
            try {
                JSONObject jsonObject = new JSONObject(mJsonString);

                String header = jsonObject.names().toString();
                header = header.replace("[", "");
                header = header.replace("\"", "");
                header = header.replace("]", "");

                if (header.equals("codeDelete")) {
                    Log.d(TAG, "Check Code 삭제 완료");
                } else if (header.equals("codeSave")) {
                    JSONArray jsonArray = jsonObject.getJSONArray("codeSave");
                    JSONObject item = jsonArray.getJSONObject(0);
                    if (!item.getString("Result").equals("Success")) {
                        Toast.makeText(PartsChangeActivity_BLU.this, mJsonString, Toast.LENGTH_SHORT).show();
                    }
                } else if (header.equals("makingCode")) {
                    JSONArray jsonArray = jsonObject.getJSONArray("makingCode");
                    JSONObject item = jsonArray.getJSONObject(0);

                    checkCode = item.getString("CHECK_CODE");

                    //String orderNo = item.getString("CHECK_CODE").substring(6, 10); // 조금 다르다 6번째 부터 10번 앞자리까지로 인식해라
                    //int serial = Integer.parseInt(orderNo) + 1;
                    //String number = String.format("%04d", serial);
                    //checkCode += number;

                    // 체크 코드를 임시로 저장한다.
                    getData task = new getData();
                    task.execute("http://" + server_ip + ":" + server_port + "/MMPS/PartsChange_BLU/tempcodesave.php"
                            , "codeSave"
                            , checkCode);
                } else if (header.equals("makingCode!")) {
                    checkCode += "0001";
                    // CheckCode를 임시로 저장한다.
                    getData task = new getData();
                    task.execute("http://" + server_ip + ":" + server_port + "/MMPS/PartsChange_BLU/tempcodesave.php"
                            , "codeSave"
                            , checkCode);
                } else if (header.equals("modelInfo")) {
                    JSONArray jsonArray = jsonObject.getJSONArray("modelInfo");

                    //for(int i=0;i<jsonArray.length();i++){
                    //    JSONObject item = jsonArray.getJSONObject(i);
                    //}
                    JSONObject item = jsonArray.getJSONObject(0);
                    ledPartNo = item.getString("LED_PARTNO");
                    thermistor = item.getString("TH_PARTNO");
                    intensity = item.getString("INTENSITY");
                    chromaticity = item.getString("CHROMATICITY");
                    rankOrder = item.getString("RANK_ORDER");
                    etModelName.setText(item.getString("MODEL_NAME"));

                    String rank1[], rank2[];

                    if (rankOrder.equals("휘도-색좌표")){
                        rank1 = intensity.replace(" ", "").split(",");
                        rank2 = chromaticity.replace(" ", "").split(",");
                    } else {
                        rank1 = chromaticity.replace(" ", "").split(",");
                        rank2 = intensity.replace(" ", "").split(",");
                    }

                    ledRank = ""; // 초기화

                    for (int i = 0; i < rank1.length; i++){
                        for (int j = 0; j < rank2.length; j++){
                            if (ledRank.isEmpty()){
                                ledRank = rank1[i] + rank2[j];
                            } else {
                                ledRank += "," + rank1[i] + rank2[j];
                            }
                        }
                    }

                    Log.d(TAG, "LED RANK List : " + ledRank);
                } else if (header.equals("modelInfo!")) {
                    Toast.makeText(PartsChangeActivity_BLU.this, "모델 정보를 찾을 수 없습니다.", Toast.LENGTH_SHORT).show();
                } else if (header.equals("saveResult")) {
                    JSONArray jsonArray = jsonObject.getJSONArray("saveResult");
                    JSONObject item = jsonArray.getJSONObject(0);
                    if (!item.getString("Result").equals("Success")){
                        Toast.makeText(PartsChangeActivity_BLU.this, mJsonString, Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(PartsChangeActivity_BLU.this, "저장 완료.", Toast.LENGTH_SHORT).show();
                        //저장완료시 컨트롤 초기화
                        etFeederSN.setText("");
                        etBefPartNo.setText("");
                        etBefRank.setText("");
                        etAftPartNo.setText("");
                        etAftRank.setText("");
                        etAftLotNo.setText("");
                        etAftQty.setText("");
                        tvStatus.setText("Feeder SN을 입력(스캔)하여 주십시오.");
                        checkCodeMaking();
                    }
                } else if (header.equals("saveResult1")) {
                    JSONArray jsonArray = jsonObject.getJSONArray("saveResult1");
                    JSONObject item = jsonArray.getJSONObject(0);
                    if (!item.getString("Result").equals("Success")){
                        Toast.makeText(PartsChangeActivity_BLU.this, mJsonString, Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(PartsChangeActivity_BLU.this, "NG1 저장 완료.", Toast.LENGTH_SHORT).show();
                        etBefPartNo.setText("");
                        etBefRank.setText("");
                        tvStatus.setText("교환 전 자재의 정보를 입력하여 주십시오.");

                        checkCodeMaking();
                    }
                } else if (header.equals("saveResult2")) {
                    JSONArray jsonArray = jsonObject.getJSONArray("saveResult2");
                    JSONObject item = jsonArray.getJSONObject(0);
                    if (!item.getString("Result").equals("Success")){
                        Toast.makeText(PartsChangeActivity_BLU.this, mJsonString, Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(PartsChangeActivity_BLU.this, "NG2 저장 완료.", Toast.LENGTH_SHORT).show();
                        //저장완료시 컨트롤 초기화
                        etAftPartNo.setText("");
                        etAftRank.setText("");
                        etAftLotNo.setText("");
                        etAftQty.setText("");
                        tvStatus.setText("교환 후 자재의 정보를 입력하여 주십시오.");

                        checkCodeMaking();
                    }
                } else {
                    Toast.makeText(PartsChangeActivity_BLU.this, mJsonString, Toast.LENGTH_SHORT).show();
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

    @Override
    protected void onResume() {
        super.onResume();
        mWaitDialog = ProgressDialog.show(mContext, "", "Scanner Running...", true);
        mHandler.postDelayed(mStartOnResume, 1000);
        IntentFilter filter = new IntentFilter();
        filter.addAction(ScanConst.INTENT_USERMSG);
        filter.addAction(ScanConst.INTENT_EVENT);
        mContext.registerReceiver(mScanResultReceiver, filter);

        checkCodeMaking();
    }

    @Override
    protected void onPause() {
        if (mScanner != null) {
            mScanner.aDecodeSetResultType(mBackupResultType);
            mScanner.aUnregisterDecodeStateCallback(mStateCallback);
        }
        mContext.unregisterReceiver(mScanResultReceiver);

        checkCodeDelete();

        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (mScanner != null) {
            mScanner.aDecodeSetResultType(mBackupResultType);
        }
        mScanner = null;

        checkCodeDelete();

        super.onDestroy();
    }

    /*
    @Override
    protected void onRestart(){
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
