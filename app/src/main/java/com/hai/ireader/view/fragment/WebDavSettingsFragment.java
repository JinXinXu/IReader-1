package com.hai.ireader.view.fragment;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.text.TextUtils;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import com.hwangjr.rxbus.RxBus;
import com.hai.ireader.ReaderApplication;
import com.hai.ireader.R;
import com.hai.ireader.constant.RxBusTag;
import com.hai.ireader.help.DataRestore;
import com.hai.ireader.help.FileHelp;
import com.hai.ireader.help.ProcessTextHelp;
import com.hai.ireader.help.WebDavHelp;
import com.hai.ireader.help.permission.Permissions;
import com.hai.ireader.help.permission.PermissionsCompat;
import com.hai.ireader.utils.FileUtils;
import com.hai.ireader.utils.RxUtils;
import com.hai.ireader.utils.ZipUtils;
import com.hai.ireader.utils.theme.ATH;
import com.hai.ireader.utils.web_dav.WebDavFile;
import com.hai.ireader.view.activity.SettingActivity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import io.reactivex.Single;
import io.reactivex.SingleObserver;
import io.reactivex.SingleOnSubscribe;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import kotlin.Unit;

import static com.hai.ireader.constant.AppConstant.DEFAULT_WEB_DAV_URL;

/**
 * Created by GKF on 2017/12/16.
 * 设置
 */
public class WebDavSettingsFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {
    private SettingActivity settingActivity;
    private CompositeDisposable compositeDisposable = new CompositeDisposable();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getPreferenceManager().setSharedPreferencesName("CONFIG");
        settingActivity = (SettingActivity) this.getActivity();
        settingActivity.setupActionBar("WebDav设置");
        SharedPreferences sharedPreferences = getPreferenceManager().getSharedPreferences();
        SharedPreferences.Editor editor = sharedPreferences.edit();
        boolean processTextEnabled = ProcessTextHelp.isProcessTextEnabled();
        editor.putBoolean("process_text", processTextEnabled);
        if (Objects.equals(sharedPreferences.getString(getString(R.string.pk_download_path), ""), "")) {
            editor.putString(getString(R.string.pk_download_path), FileHelp.getCachePath());
        }
        editor.apply();
        addPreferencesFromResource(R.xml.pref_settings_web_dav);
        bindPreferenceSummaryToValue(findPreference("web_dav_url"));
        bindPreferenceSummaryToValue(findPreference("web_dav_account"));
        bindPreferenceSummaryToValue(findPreference("web_dav_password"));
    }

    private static Preference.OnPreferenceChangeListener sBindPreferenceSummaryToValueListener = (Preference preference, Object value) -> {
        String stringValue = value.toString();

        if (preference.getKey().equals("web_dav_url")) {
            if (TextUtils.isEmpty(stringValue)) {
                preference.setSummary(DEFAULT_WEB_DAV_URL);
            } else {
                preference.setSummary(stringValue);
            }
        } else if (preference.getKey().equals("web_dav_account")) {
            if (TextUtils.isEmpty(stringValue)) {
                preference.setSummary("输入你的WebDav账号");
            } else {
                preference.setSummary(stringValue);
            }
        } else if (preference.getKey().equals("web_dav_password")) {
            if (TextUtils.isEmpty(stringValue)) {
                preference.setSummary("输入你的WebDav授权密码");
            } else {
                preference.setSummary("************");
            }
        } else if (preference instanceof ListPreference) {
            ListPreference listPreference = (ListPreference) preference;
            int index = listPreference.findIndexOfValue(stringValue);
            // Set the summary to reflect the new value.
            preference.setSummary(index >= 0 ? listPreference.getEntries()[index] : null);
        } else {
            // For all other preferences, set the summary to the value's
            preference.setSummary(stringValue);
        }
        return true;
    };

    private static void bindPreferenceSummaryToValue(Preference preference) {
        // Set the listener to watch for value changes.
        preference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);
        sBindPreferenceSummaryToValueListener.onPreferenceChange(preference,
                preference.getContext().getSharedPreferences("CONFIG", Context.MODE_PRIVATE).getString(preference.getKey(), ""));
    }

    @Override
    public void onResume() {
        super.onResume();
        getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onPause() {
        getPreferenceManager().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
        super.onPause();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {

    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        if (preference.getKey().equals("web_dav_restore")) {
            if (!WebDavHelp.initWebDav())
                return super.onPreferenceTreeClick(preferenceScreen, preference);
            new PermissionsCompat.Builder(settingActivity)
                    .addPermissions(Permissions.READ_EXTERNAL_STORAGE, Permissions.WRITE_EXTERNAL_STORAGE)
                    .rationale(R.string.backup_permission)
                    .onGranted((requestCode) -> {
                        showRestoreFiles();
                        return Unit.INSTANCE;
                    })
                    .request();
        }
        return super.onPreferenceTreeClick(preferenceScreen, preference);
    }

    @Override
    public void onDestroy() {
        compositeDisposable.dispose();
        super.onDestroy();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }

    public void showRestoreFiles() {
        Single.create((SingleOnSubscribe<String[]>) emitter -> {
            List<WebDavFile> webDavFiles = new WebDavFile(WebDavHelp.getWebDavUrl() + "YueDu/").listFiles();
            Collections.reverse(webDavFiles);
            List<String> fileNames = new ArrayList<>();
            for (int i = 0; i < Math.min(webDavFiles.size(), 10); i++) {
                fileNames.add(webDavFiles.get(i).getDisplayName());
            }
            String[] strings = fileNames.toArray(new String[0]);
            emitter.onSuccess(strings);
        }).compose(RxUtils::toSimpleSingle)
                .subscribe(new SingleObserver<String[]>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        compositeDisposable.add(d);
                    }

                    @Override
                    public void onSuccess(String[] strings) {
                        if (strings.length > 0) {
                            AlertDialog dialog = new AlertDialog.Builder(settingActivity)
                                    .setTitle("选择恢复文件")
                                    .setSingleChoiceItems(strings, 0, (dialogInterface, i) -> {
                                        restore(WebDavHelp.getWebDavUrl() + "YueDu/" + strings[i]);
                                        dialogInterface.dismiss();
                                    })
                                    .create();
                            dialog.show();
                            ATH.setAlertDialogTint(dialog);
                        } else {
                            Toast.makeText(ReaderApplication.getInstance(), "没有找到备份", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onError(Throwable e) {
                        Toast.makeText(ReaderApplication.getInstance(), e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    public void restore(String url) {
        Single.create(emitter -> {
            WebDavFile webDavFile = new WebDavFile(url);
            String zipFilePath = FileHelp.getCachePath() + "/backup" + ".zip";
            webDavFile.download(zipFilePath, true);
            ZipUtils.unzipFile(zipFilePath, FileUtils.getSdCardPath() + "/YueDu");
            DataRestore.getInstance().run();
            emitter.onSuccess(new Object());
        }).compose(RxUtils::toSimpleSingle)
                .subscribe(new SingleObserver<Object>() {
                    @Override
                    public void onSubscribe(Disposable d) {
                        compositeDisposable.add(d);
                    }

                    @Override
                    public void onSuccess(Object o) {
                        Toast.makeText(ReaderApplication.getInstance(), "恢复完成", Toast.LENGTH_SHORT).show();
                        RxBus.get().post(RxBusTag.RECREATE, true);
                    }

                    @Override
                    public void onError(Throwable e) {
                        Toast.makeText(ReaderApplication.getInstance(), e.getLocalizedMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
