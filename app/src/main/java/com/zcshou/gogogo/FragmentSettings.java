package com.zcshou.gogogo;

import android.os.Bundle;
import android.text.InputType;
import android.widget.Toast;

import androidx.preference.EditTextPreference;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.SwitchPreferenceCompat;

import com.elvishew.xlog.XLog;
import com.zcshou.utils.GoUtils;
import com.zcshou.utils.RouteManager;

import java.util.Objects;

public class FragmentSettings extends PreferenceFragmentCompat {

    // Set a non-empty decimal EditTextPreference
    private void setupDecimalEditTextPreference(EditTextPreference preference) {
        if (preference != null) {
            preference.setSummaryProvider((Preference.SummaryProvider<EditTextPreference>) EditTextPreference::getText);
            preference.setOnBindEditTextListener(editText -> {
                editText.setInputType(InputType.TYPE_NUMBER_FLAG_DECIMAL | InputType.TYPE_CLASS_NUMBER);
                editText.setSelection(editText.length());
            });
            preference.setOnPreferenceChangeListener((pref, newValue) -> {
                if (newValue.toString().trim().isEmpty()) {
                    GoUtils.DisplayToast(this.getContext(), getResources().getString(R.string.app_error_input_null));
                    return false;
                }
                return true;
            });
        }
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        // Load the preferences from an XML resource
        addPreferencesFromResource(R.xml.preferences_main);

        ListPreference pfJoystick = findPreference("setting_joystick_type");
        if (pfJoystick != null) {
            // 使用自定义 SummaryProvider
            pfJoystick.setSummaryProvider((Preference.SummaryProvider<ListPreference>) preference -> Objects.requireNonNull(preference.getEntry()));
            pfJoystick.setOnPreferenceChangeListener((preference, newValue) -> !newValue.toString().trim().isEmpty());
        }

        EditTextPreference pfWalk = findPreference("setting_walk");
        setupDecimalEditTextPreference(pfWalk);

        EditTextPreference pfRun = findPreference("setting_run");
        setupDecimalEditTextPreference(pfRun);

        EditTextPreference pfBike = findPreference("setting_bike");
        setupDecimalEditTextPreference(pfBike);

        EditTextPreference pfAltitude = findPreference("setting_altitude");
        setupDecimalEditTextPreference(pfAltitude);

        EditTextPreference pfLatOffset = findPreference("setting_lat_max_offset");
        setupDecimalEditTextPreference(pfLatOffset);

        EditTextPreference pfLonOffset = findPreference("setting_lon_max_offset");
        setupDecimalEditTextPreference(pfLonOffset);

        SwitchPreferenceCompat pLog = findPreference("setting_log_off");
        if (pLog != null) {
            pLog.setOnPreferenceChangeListener((preference, newValue) -> {
                if(((SwitchPreferenceCompat) preference).isChecked() != (Boolean) newValue) {
                    XLog.d(preference.getKey() + newValue);

                    if (Boolean.parseBoolean(newValue.toString())) {
                        XLog.d("on");
                    } else {
                        XLog.d("off");
                    }
                    return true;
                } else {
                    return false;
                }
            });
        }

        EditTextPreference pfPosHisValid = findPreference("setting_history_expiration");
        setupDecimalEditTextPreference(pfPosHisValid);

        // 设置版本号
        String verName;
        verName = GoUtils.getVersionName(FragmentSettings.this.getContext());
        Preference pfVersion = findPreference("setting_version");
        if (pfVersion != null) {
            pfVersion.setSummary(verName);
        }

        setPreferencesFromResource(R.xml.preferences_main, rootKey);

        // 获取速度设置 Preference
        EditTextPreference speedPreference = findPreference("setting_move_speed");
        if (speedPreference != null) {
            speedPreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    try {
                        double speed = Double.parseDouble(newValue.toString());

                        // 验证速度范围
                        if (speed <= 0) {
                            Toast.makeText(getActivity(), "速度必须大于0", Toast.LENGTH_SHORT).show();
                            return false; // 拒绝更改
                        }

                        // 更新 RouteManager 的速度
                        RouteManager.getInstance().setmMoveSpeed(speed);

                        // 更新摘要显示
                        speedPreference.setSummary("当前速度: " + speed + " 米/秒");

                        return true; // 接受更改
                    } catch (NumberFormatException e) {
                        Toast.makeText(getActivity(), "请输入有效的数字", Toast.LENGTH_SHORT).show();
                        return false; // 拒绝更改
                    }
                }
            });

            // 初始化显示当前速度
            String currentSpeed = speedPreference.getText();
            if (currentSpeed != null) {
                speedPreference.setSummary("当前速度: " + currentSpeed + " 米/秒");
            }
        }
    }
}