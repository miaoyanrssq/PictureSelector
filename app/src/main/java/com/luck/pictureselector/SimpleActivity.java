package com.luck.pictureselector;

import android.content.Intent;
import android.net.Uri;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import com.luck.picture.lib.PictureSelector;
import com.luck.picture.lib.config.PictureMimeType;
import com.luck.picture.lib.entity.LocalMedia;
import com.luck.picture.lib.tools.AttrsUtils;
import com.luck.picture.lib.tools.PictureFileUtils;
import com.yalantis.ucrop.UCrop;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class SimpleActivity extends AppCompatActivity implements View.OnClickListener {
    private Button btn_activity, btn_fragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_other);
        btn_activity = (Button) findViewById(R.id.btn_activity);
        btn_fragment = (Button) findViewById(R.id.btn_fragment);
        btn_activity.setOnClickListener(this);
        btn_fragment.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        Intent intent;
        switch (v.getId()) {
            case R.id.btn_activity:
                intent = new Intent(SimpleActivity.this, MainActivity.class);
                startActivity(intent);
                break;
            case R.id.btn_fragment:
//                intent = new Intent(SimpleActivity.this, PhotoFragmentActivity.class);
//                startActivity(intent);
                startCrop("https://timgsa.baidu.com/timg?image&quality=80&size=b9999_10000&sec=1573625635439&di=b440e9e433dc2360d7aa3c8994252fa3&imgtype=0&src=http%3A%2F%2Fpic15.nipic.com%2F20110812%2F2786001_084429179000_2.jpg");
//                startCrop("/storage/emulated/0/Android/data/com.luck.pictureselector/cache/1573693540284.jpg");
//                startCrop("https://csi.tmuyun.com/webpic/W0201910/W020191025/W020191025677815391637.png");
                break;
        }
    }


    protected void startCrop(String originalPath) {
        UCrop.Options options = new UCrop.Options();
//        int toolbarColor = AttrsUtils.getTypeValueColor(this, R.color._4786ff);
//        int statusColor = AttrsUtils.getTypeValueColor(this,  R.color._4786ff);
//        int titleColor = AttrsUtils.getTypeValueColor(this,  R.color._4786ff);
//        options.setToolbarColor(toolbarColor);
//        options.setStatusBarColor(statusColor);
//        options.setToolbarWidgetColor(titleColor);
//        options.setCircleDimmedLayer(config.circleDimmedLayer);
//        options.setShowCropFrame(config.showCropFrame);
//        options.setShowCropGrid(config.showCropGrid);
//        options.setDragFrameEnabled(true);
//        options.setScaleEnabled(true);
//        options.setRotateEnabled(true);
//        options.setCompressionQuality(config.cropCompressQuality);
        options.setHideBottomControls(true);
//        options.setFreeStyleCropEnabled(true);
        boolean isHttp = PictureMimeType.isHttp(originalPath);
        String imgType = PictureMimeType.getLastImgType(originalPath);
        Uri uri = isHttp ? Uri.parse(originalPath) : Uri.fromFile(new File(originalPath));
        UCrop.of(uri, Uri.fromFile(new File(PictureFileUtils.getDiskCacheDir(this),
                System.currentTimeMillis() + imgType)))
                .withAspectRatio(2, 1)
                .withMaxResultSize(400, 200)
                .withOptions(options)
                .start(this, 69);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == 69 && data != null) {
            Uri uri = UCrop.getOutput(data);
            if(uri != null) {
                String url = uri.getPath();
            }
        }
    }
}
