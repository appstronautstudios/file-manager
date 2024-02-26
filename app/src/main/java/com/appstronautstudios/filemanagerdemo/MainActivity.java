package com.appstronautstudios.filemanagerdemo;

import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.appstronautstudios.filemanager.FileUtils;

import org.json.JSONObject;

import java.io.File;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.btn_filepath_test).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(MainActivity.this, FileUtils.getInternalFilePath(MainActivity.this, Environment.DIRECTORY_PICTURES, null), Toast.LENGTH_SHORT).show();
            }
        });
        findViewById(R.id.btn_filepath_test_2).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(MainActivity.this, FileUtils.getInternalFilePath(MainActivity.this, Environment.DIRECTORY_DOCUMENTS, "out.json"), Toast.LENGTH_SHORT).show();
            }
        });
        findViewById(R.id.btn_write_json_test).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    JSONObject jsonObject = new JSONObject();
                    jsonObject.put("a", 1);
                    jsonObject.put("b", "test");
                    jsonObject.put("c", "the quick brown fox");
                    FileUtils.writeJsonToFile(jsonObject, new File(FileUtils.getInternalFilePath(MainActivity.this, Environment.DIRECTORY_DOCUMENTS, "out.json")));
                    Toast.makeText(MainActivity.this, "write json success", Toast.LENGTH_SHORT).show();
                } catch (Exception e) {
                    Toast.makeText(MainActivity.this, "write json failure", Toast.LENGTH_SHORT).show();
                    e.printStackTrace();
                }

            }
        });
        findViewById(R.id.btn_get_mimetype_test).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                File file = new File(FileUtils.getInternalFilePath(MainActivity.this, Environment.DIRECTORY_DOCUMENTS, "out.json"));
                Toast.makeText(MainActivity.this, FileUtils.getMimeType(file.getPath()), Toast.LENGTH_SHORT).show();
            }
        });
        findViewById(R.id.btn_share_file_test).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                File file = new File(FileUtils.getInternalFilePath(MainActivity.this, Environment.DIRECTORY_DOCUMENTS, "out.json"));
                FileUtils.shareFile(MainActivity.this, Uri.fromFile(file), FileUtils.getMimeType(file.getPath()));
            }
        });
    }
}
