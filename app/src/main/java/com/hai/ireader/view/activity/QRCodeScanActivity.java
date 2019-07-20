package com.hai.ireader.view.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.hai.basemvplib.impl.IPresenter;
import com.hai.ireader.R;
import com.hai.ireader.base.MBaseActivity;
import com.hai.ireader.help.permission.Permissions;
import com.hai.ireader.help.permission.PermissionsCompat;
import com.hai.ireader.utils.FileUtils;
import com.hai.ireader.widget.filepicker.picker.FilePicker;

import butterknife.BindView;
import butterknife.ButterKnife;
import cn.bingoogolapple.qrcode.core.QRCodeView;
import cn.bingoogolapple.qrcode.zxing.ZXingView;
import kotlin.Unit;

/**
 * Created by GKF on 2018/1/29.
 */

public class QRCodeScanActivity extends MBaseActivity implements QRCodeView.Delegate {

    @BindView(R.id.zxingview)
    ZXingView zxingview;
    @BindView(R.id.toolbar)
    Toolbar toolbar;
    @BindView(R.id.action_bar)
    AppBarLayout actionBar;
    @BindView(R.id.fab_flashlight)
    FloatingActionButton fabFlashlight;

    private final int REQUEST_QR_IMAGE = 202;
    private boolean flashlightIsOpen;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    /**
     * P层绑定   若无则返回null;
     */
    @Override
    protected IPresenter initInjector() {
        return null;
    }

    /**
     * 布局载入  setContentView()
     */
    @Override
    protected void onCreateActivity() {
        setContentView(R.layout.activity_qrcode_capture);
        ButterKnife.bind(this);
        this.setSupportActionBar(toolbar);
        setupActionBar();
    }

    /**
     * 数据初始化
     */
    @Override
    protected void initData() {
        zxingview.setDelegate(this);
        fabFlashlight.setOnClickListener(view -> {
            if (flashlightIsOpen) {
                flashlightIsOpen = false;
                zxingview.closeFlashlight();
            } else {
                flashlightIsOpen = true;
                zxingview.openFlashlight();
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        startCamera();
    }

    private void startCamera() {
        new PermissionsCompat.Builder(this)
                .addPermissions(Permissions.CAMERA)
                .rationale(R.string.qr_per)
                .onGranted((requestCode) -> {
                    zxingview.setVisibility(View.VISIBLE);
                    zxingview.startSpotAndShowRect(); // 显示扫描框，并开始识别
                    return Unit.INSTANCE;
                })
                .request();
    }

    @Override
    protected void onStop() {
        zxingview.stopCamera(); // 关闭摄像头预览，并且隐藏扫描框
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        zxingview.onDestroy(); // 销毁二维码扫描控件
        super.onDestroy();
    }

    @Override
    public void onScanQRCodeSuccess(String result) {
        Intent intent = new Intent();
        intent.putExtra("result", result);
        setResult(RESULT_OK, intent);
        finish();
    }

    @Override
    public void onCameraAmbientBrightnessChanged(boolean isDark) {

    }

    @Override
    public void onScanQRCodeOpenCameraError() {

    }

    //设置ToolBar
    private void setupActionBar() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle(R.string.scan_qr_code);
        }
    }

    // 添加菜单
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_qr_code_scan, menu);
        return super.onCreateOptionsMenu(menu);
    }

    //菜单
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case R.id.action_choose_from_gallery:
                new PermissionsCompat.Builder(this)
                        .addPermissions(Permissions.READ_EXTERNAL_STORAGE, Permissions.WRITE_EXTERNAL_STORAGE)
                        .rationale(R.string.get_storage_per)
                        .onGranted((requestCode) -> {
                            chooseFromGallery();
                            return Unit.INSTANCE;
                        })
                        .request();
                break;
            case android.R.id.home:
                finish();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        zxingview.startSpotAndShowRect(); // 显示扫描框，并开始识别

        if (resultCode == Activity.RESULT_OK && requestCode == REQUEST_QR_IMAGE) {
            final String picturePath = FileUtils.getPath(this, data.getData());
            // 本来就用到 QRCodeView 时可直接调 QRCodeView 的方法，走通用的回调
            zxingview.decodeQRCode(picturePath);
        }
    }

    private void chooseFromGallery() {
        try {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("image/*");
            startActivityForResult(intent, REQUEST_QR_IMAGE);
        } catch (Exception ignored) {
            FilePicker picker = new FilePicker(this, FilePicker.FILE);
            picker.setBackgroundColor(getResources().getColor(R.color.background));
            picker.setTopBackgroundColor(getResources().getColor(R.color.background));
            picker.setItemHeight(30);
            picker.setOnFilePickListener(currentPath -> zxingview.decodeQRCode(currentPath));
            picker.show();
        }
    }
}