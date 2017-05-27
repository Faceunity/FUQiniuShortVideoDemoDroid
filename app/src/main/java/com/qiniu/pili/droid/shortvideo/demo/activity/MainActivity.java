package com.qiniu.pili.droid.shortvideo.demo.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import com.qiniu.pili.droid.shortvideo.demo.R;
import com.qiniu.pili.droid.shortvideo.demo.utils.RecordSettings;

public class MainActivity extends AppCompatActivity {

    private Spinner mPreviewSizeRatioSpinner;
    private Spinner mPreviewSizeLevelSpinner;
    private Spinner mEncodingSizeLevelSpinner;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mPreviewSizeRatioSpinner = (Spinner) findViewById(R.id.PreviewSizeRatioSpinner);
        mPreviewSizeLevelSpinner = (Spinner) findViewById(R.id.PreviewSizeLevelSpinner);

        mEncodingSizeLevelSpinner = (Spinner) findViewById(R.id.EncodingSizeLevelSpinner);

        ArrayAdapter<String> adapter1 = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, RecordSettings.PREVIEW_SIZE_RATIO_TIPS_ARRAY);
        mPreviewSizeRatioSpinner.setAdapter(adapter1);

        ArrayAdapter<String> adapter2 = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, RecordSettings.PREVIEW_SIZE_LEVEL_TIPS_ARRAY);
        mPreviewSizeLevelSpinner.setAdapter(adapter2);
        mPreviewSizeLevelSpinner.setSelection(3);

        ArrayAdapter<String> adapter3= new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, RecordSettings.ENCODING_SIZE_LEVEL_TIPS_ARRAY);
        mEncodingSizeLevelSpinner.setAdapter(adapter3);
        mEncodingSizeLevelSpinner.setSelection(10);
    }

    public void onClickCapture(View v) {
        jumpToCaptureActivity(VideoRecordActivity.class);
    }

    public void jumpToCaptureActivity(Class<?> cls) {
        Intent intent = new Intent(MainActivity.this, cls);
        intent.putExtra("PreviewSizeRatio", mPreviewSizeRatioSpinner.getSelectedItemPosition());
        intent.putExtra("PreviewSizeLevel", mPreviewSizeLevelSpinner.getSelectedItemPosition());
        intent.putExtra("EncodingSizeLevel", mEncodingSizeLevelSpinner.getSelectedItemPosition());
        startActivity(intent);
    }
}
