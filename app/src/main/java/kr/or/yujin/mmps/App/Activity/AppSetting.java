package kr.or.yujin.mmps.App.Activity;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;

import androidx.appcompat.app.AppCompatActivity;
import kr.or.yujin.mmps.R;

public class AppSetting extends AppCompatActivity{

    private Button btnSave;
    private EditText serverIP, serverPort;
    private Spinner barcodeType;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_app_setting);

        btnSave = (Button) findViewById(R.id.btnSave);
        serverIP = (EditText) findViewById(R.id.serverIP);
        serverPort = (EditText) findViewById(R.id.serverPort);
        barcodeType = (Spinner) findViewById(R.id.barcodeType);

        SharedPreferences setting = getSharedPreferences("setting", Activity.MODE_PRIVATE);
        serverIP.setText(setting.getString("serverIP","125.137.78.158"));
        serverPort.setText(setting.getString("serverPort","10520"));

        String loadType = setting.getString("barcodeType","ALL Type");

        for(int i=0;i<barcodeType.getCount();i++) {
            if(barcodeType.getItemAtPosition(i).toString().equals(loadType)){
                barcodeType.setSelection(i);
                break;
            }
        }

        btnSave.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                SharedPreferences setting = getSharedPreferences("setting", Activity.MODE_PRIVATE);
                SharedPreferences.Editor appsetting = setting.edit();
                appsetting.putString("serverIP", serverIP.getText().toString());
                MainActivity.server_ip = serverIP.getText().toString();
                appsetting.putString("serverPort", serverPort.getText().toString());
                MainActivity.server_port = Integer.parseInt(serverPort.getText().toString());
                String barType = barcodeType.getSelectedItem().toString();
                MainActivity.barcodeType = barcodeType.getSelectedItem().toString();
                appsetting.putString("barcodeType", barType);
                //꼭 commit()을 해줘야 값이 저장됩니다
                appsetting.commit();

                finish();
            }
        });
    }
}
