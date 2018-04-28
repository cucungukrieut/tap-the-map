package com.kantoniak.discrete_fox;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.view.MotionEvent;
import android.view.View;

import com.kantoniak.discrete_fox.ar.EasyARUtils;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainMenuActivity extends AppCompatActivity {

    private static final int CAMERA_PERMISSION = 0;

    @BindView(R.id.screen_permission) View mScreenPermission;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_menu);
        ButterKnife.bind(this);
        EasyARUtils.initializeEngine(this);

        mScreenPermission.setOnTouchListener((view, event) -> true);
    }

    @OnClick(R.id.start_button)
    public void onClickStart(View view) {
        requestCameraPermission();
    }

    @OnClick(R.id.about_button)
    public void onClickAbout(View view) {
        startActivity(new Intent(this, AboutActivity.class));
    }

    private void onCameraRequestSuccess() {
        startActivity(new Intent(this, GameActivity.class));
        mScreenPermission.setVisibility(View.GONE);
    }

    private void onCameraRequestFailure() {
        mScreenPermission.setVisibility(View.VISIBLE);
    }

    @OnClick(R.id.button_try_camera_again)
    public void onRetryCamera(View view) {
        requestCameraPermission();
    }

    private void requestCameraPermission() {
        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION);
        } else {
            onCameraRequestSuccess();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == CAMERA_PERMISSION) {
            boolean executed = false;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    executed = true;
                    onCameraRequestFailure();
                }
            }
            if (!executed) {
                onCameraRequestSuccess();
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public void onBackPressed() {
        if (mScreenPermission.getVisibility() == View.VISIBLE) {
            mScreenPermission.setVisibility(View.GONE);
        } else {
            super.onBackPressed();
        }
    }
}
