package kr.or.yujin.yj_mms.smt_production;

import androidx.appcompat.app.AppCompatActivity;

import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TableLayout;
import android.widget.TableRow;
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
import java.util.ArrayList;
import java.util.List;

import kr.or.yujin.yj_mms.BuildConfig;
import kr.or.yujin.yj_mms.MainActivity;
import kr.or.yujin.yj_mms.R;
import kr.or.yujin.yj_mms.mmps.Mis_Check;

public class SMT_Production_Start extends AppCompatActivity {

    private String TAG = "SMT Production Start";

    private Spinner spn_Department, spn_WorkLine;
    private ArrayAdapter<String> adt_Department, adt_WorkLine; // 스피너에 사용되는 ArrayAdapter
    private ArrayList<String> list_WorkLine;
    private int firstRun_Department, firstRun_WorkLine = 0;

    private TableLayout tableLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.smt_production_start);

        control_Initialize();

        spn_Department.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (firstRun_Department == 0) { //자동실행 방지
                    firstRun_Department += 1;
                } else {
                    String factoryCode = "";
                    if (spn_Department.getSelectedItem().toString().equals("C동")){
                        factoryCode = "SC00000003";
                    } else if (spn_Department.getSelectedItem().toString().equals("D동")){
                        factoryCode = "SC00000004";
                    }
                    GetData task_VerLoad = new GetData();
                    task_VerLoad.execute( "http://" + MainActivity.server_ip + ":" + MainActivity.server_port + "/SMT_Production/Production_Start/load_work_line.php"
                            , "Select Department"
                            , factoryCode
                    );
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Dummy
            }
        });

        spn_WorkLine.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (firstRun_WorkLine == 0) { //자동실행 방지
                    firstRun_WorkLine += 1;
                } else {
                    GetData task_VerLoad = new GetData();
                    task_VerLoad.execute( "http://" + MainActivity.server_ip + ":" + MainActivity.server_port + "/SMT_Production/Production_Start/load_plan_list.php"
                            , "Load Plan List"
                            , spn_Department.getSelectedItem().toString()
                            , spn_WorkLine.getSelectedItem().toString()
                    );
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Dummy
            }
        });
    }

    private void control_Initialize(){
        spn_Department = (Spinner) findViewById(R.id.spn_Department);
        spn_WorkLine = (Spinner) findViewById(R.id.spn_WorkLine);

        String[] department = {"선택","C동","D동"};
        adt_Department = new ArrayAdapter<String>(this,
                R.layout.support_simple_spinner_dropdown_item, department);
        spn_Department.setAdapter(adt_Department);

        list_WorkLine = new ArrayList<String>();
        adt_WorkLine = new ArrayAdapter<String>(this,
                R.layout.support_simple_spinner_dropdown_item, list_WorkLine);
        spn_WorkLine.setAdapter(adt_WorkLine);

        tableLayout = (TableLayout) findViewById(R.id.tlList);
    }

    private void verCheck(){
        GetData task_VerLoad = new GetData();
        task_VerLoad.execute( "http://" + MainActivity.server_ip + ":" + MainActivity.server_port + "/yj_mms_ver.php", "ver");
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
                progressDialog = ProgressDialog.show(SMT_Production_Start.this,
                        "Connecting to server....\nPlease wait.", null, true, true);
        }

        @Override
        protected String doInBackground(String... params) {

            String serverURL = params[0];

            String secondString = (String) params[1];
            String postParameters = null;

            if (secondString.equals("ver")) {
                postParameters = "";
            } else if (secondString.equals("Select Department")) {
                postParameters = "factoryCode=" + params[2];
            } else if (secondString.equals("Load Plan List")) {
                postParameters = "factoryName=" + params[2];
                postParameters += "&workLine=" + params[3];
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

            if (progressDialog != null && progressDialog.isShowing())
                progressDialog.dismiss();

            if (result == null){
                Log.d(TAG, "서버 접속 Error - " + errorString);
                Toast.makeText(SMT_Production_Start.this, "서버에 접속 할 수 없습니다.\n상세 내용은 로그를 참조 하십시오.", Toast.LENGTH_SHORT).show();
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

                if (header.equals("CheckVer")) {
                    JSONArray jsonArray = jsonObject.getJSONArray("CheckVer");
                    JSONObject item = jsonArray.getJSONObject(0);
                    if (!item.getString("Result").equals("Ver:" + BuildConfig.VERSION_NAME)) {
                        appVerAlarm();
                    }
                } else if (header.equals("List_Work_Line")) {
                    list_WorkLine.clear();
                    list_WorkLine.add("선택");

                    JSONArray jsonArray = jsonObject.getJSONArray("List_Work_Line");
                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject item = jsonArray.getJSONObject(i);
                        list_WorkLine.add(item.getString("Work_Line"));
                    }
                    adt_WorkLine.notifyDataSetChanged();
                } else if (header.equals("List_Work_Line!")) {
                    list_WorkLine.clear();
                    list_WorkLine.add("라인이 없습니다.");
                    adt_WorkLine.notifyDataSetChanged();
                    Toast.makeText(SMT_Production_Start.this, "라인이 없습니다.", Toast.LENGTH_SHORT).show();
                } else if (header.equals("Plan_List")) {
                    //테이블 레이아웃 초기화
                    tableLayout.removeViews(1, tableLayout.getChildCount()-1);

                    JSONArray jsonArray = jsonObject.getJSONArray("Plan_List");
                    for (int i = 0; i < jsonArray.length(); i++) {
                        JSONObject item = jsonArray.getJSONObject(i);
                        final TableRow tableRow = new TableRow(SMT_Production_Start.this); //tablerow 생성
                        tableRow.setLayoutParams(new TableRow.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT
                                , ViewGroup.LayoutParams.WRAP_CONTENT
                        ));

                        int selColor = Color.WHITE;
                        if (i % 2 == 1) {
                            selColor = Color.parseColor("#00D8FF");
                        }
                        TextView textView = new TextView(SMT_Production_Start.this);
                        textView.setText(String.valueOf(tableLayout.getChildCount()));
                        textView.setGravity(Gravity.CENTER);
                        textView.setBackgroundColor(selColor);
                        textView.setMinHeight(100);
                        tableRow.addView(textView);
                        TextView textView2 = new TextView(SMT_Production_Start.this);
                        textView2.setText(item.getString("Customer_Name"));
                        textView2.setGravity(Gravity.CENTER);
                        textView2.setBackgroundColor(selColor);
                        textView2.setMinHeight(100);
                        tableRow.addView(textView2);
                        TextView textView3 = new TextView(SMT_Production_Start.this);
                        textView3.setText(item.getString("Item_Code"));
                        textView3.setGravity(Gravity.CENTER);
                        textView3.setBackgroundColor(selColor);
                        textView3.setMinHeight(100);
                        tableRow.addView(textView3);
                        TextView textView4 = new TextView(SMT_Production_Start.this);
                        textView4.setText(item.getString("Working_Count"));
                        textView4.setGravity(Gravity.CENTER);
                        textView4.setBackgroundColor(selColor);
                        textView4.setMinHeight(100);
                        tableRow.addView(textView4);

                        //Tag에 Order Index를 넣어서 클릭할 때 표시
                        tableRow.setTag(item.getString("Order_Index"));

                        tableRow.setOnClickListener(new View.OnClickListener() {
                            public void onClick(View arg0) {
                                //Log.e(TAG, "선택된 Po No.: " + tableRow.getTag().toString());
                                Intent intent = new Intent(SMT_Production_Start.this, SMT_Production_Start_Check.class);
                                intent.putExtra("Order Index", tableRow.getTag().toString());
                                startActivityForResult(intent, 0);
                            }
                        });
                        tableLayout.addView(tableRow);
                    }
                } else if (header.equals("Plan_List!")) {
                    tableLayout.removeViews(1, tableLayout.getChildCount()-1);
                    Toast.makeText(SMT_Production_Start.this, "해당 라인의 계획이 없습니다.", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(SMT_Production_Start.this, mJsonString, Toast.LENGTH_SHORT).show();
                }
            } catch (JSONException e) {
                Log.d(TAG, "showResult Error : ", e);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        Log.d(TAG, "requestCode : " + requestCode);
        if (requestCode == 49374) {
            if (result != null) {
                if (result.getContents() != null) {

                }
            } else {
                super.onActivityResult(requestCode, resultCode, data);
            }
        }
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
    }
}