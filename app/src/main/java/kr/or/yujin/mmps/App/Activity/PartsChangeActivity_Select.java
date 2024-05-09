package kr.or.yujin.mmps.App.Activity;

import androidx.appcompat.app.AppCompatActivity;
import kr.or.yujin.mmps.R;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class PartsChangeActivity_Select extends AppCompatActivity {

    private Button btnBLU, btnNonBLU;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_parts_change__select);

        btnBLU = (Button) findViewById(R.id.btnBLU);
        btnNonBLU = (Button) findViewById(R.id.btnNonBLU);

        btnBLU.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent intent = new Intent(PartsChangeActivity_Select.this, PartsChangeActivity_BLU.class);
                startActivity(intent);//액티비티 띄우기
            }
        });

        btnNonBLU.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent intent = new Intent(PartsChangeActivity_Select.this, PartsChangeActivity_New.class);
                startActivity(intent);//액티비티 띄우기
            }
        });
    }
}