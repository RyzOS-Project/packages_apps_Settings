/*
 * Copyright (C) 2019 The LineageOS Project
 * Copyright (C) 2019-2021 The Evolution X Project
 * Copyright (C) 2025 The RyzOS Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ryz.settings.deviceinfo.firmwareversion;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.UserManager;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.VisibleForTesting;
import androidx.preference.Preference;

import com.android.settings.R;
import com.android.settings.Utils;
import com.android.settings.core.BasePreferenceController;
import com.android.settings.slices.Sliceable;
import com.android.settingslib.RestrictedLockUtils;
import com.android.settingslib.RestrictedLockUtilsInternal;

public class RyzVersionDetailPreferenceController extends BasePreferenceController {

    private static final String TAG = "RyzVersionDialogCtrl";
    private static final int DELAY_TIMER_MILLIS = 500;
    private static final int ACTIVITY_TRIGGER_COUNT = 3;

    private static final String KEY_RYZ_VERSION_PROP = "ro.ryz.version";
    private static final String KEY_RYZ_ANDROID_PROP = "ro.ryz.android";
    private static final String KEY_RYZ_DEVICE_PROP = "ro.product.device";
    private static final String KEY_RYZ_BUILDTYPE_PROP = "ro.ryz.buildtype";
    private static final String KEY_RYZ_BUILDVER_PROP = "ro.ryz.display.version";

    private static final String PLATLOGO_PACKAGE_NAME = "org.lineageos.lineageparts";
    private static final String PLATLOGO_ACTIVITY_CLASS =
            PLATLOGO_PACKAGE_NAME + ".EasterEgg";

    private final UserManager mUserManager;
    private final long[] mHits = new long[ACTIVITY_TRIGGER_COUNT];

    private RestrictedLockUtils.EnforcedAdmin mFunDisallowedAdmin;
    private boolean mFunDisallowedBySystem;
    private boolean fullRomVersion = false;

    public RyzVersionDetailPreferenceController(Context context, String key) {
        super(context, key);
        mUserManager = (UserManager) mContext.getSystemService(Context.USER_SERVICE);
        initializeAdminPermissions();
    }

    @Override
    public int getAvailabilityStatus() {
        return AVAILABLE;
    }

    @Override
    public boolean useDynamicSliceSummary() {
        return true;
    }

    @Override
    public boolean isSliceable() {
        return true;
    }

    @Override
    public CharSequence getSummary() {
        return shortRomVersion();
    }

    @Override
    public boolean handlePreferenceTreeClick(Preference preference) {
        if (!TextUtils.equals(preference.getKey(), getPreferenceKey())) {
            return false;
        }
        if (fullRomVersion) {
            preference.setSummary(shortRomVersion());
            fullRomVersion = false;
        } else {
            preference.setSummary(SystemProperties.get(KEY_RYZ_BUILDVER_PROP,
                mContext.getString(R.string.unknown)));
            fullRomVersion = true;
        }
        if (Utils.isMonkeyRunning()) {
            return false;
        }
        arrayCopy();
        mHits[mHits.length - 1] = SystemClock.uptimeMillis();
        if (mHits[0] >= (SystemClock.uptimeMillis() - DELAY_TIMER_MILLIS)) {
            if (mUserManager.hasUserRestriction(UserManager.DISALLOW_FUN)) {
                if (mFunDisallowedAdmin != null && !mFunDisallowedBySystem) {
                    RestrictedLockUtils.sendShowAdminSupportDetailsIntent(mContext,
                            mFunDisallowedAdmin);
                }
                Log.d(TAG, "Sorry, no fun for you!");
                return true;
            }

	        final Intent intent = new Intent(Intent.ACTION_MAIN)
                    .setClassName(PLATLOGO_PACKAGE_NAME, PLATLOGO_ACTIVITY_CLASS);
            try {
                mContext.startActivity(intent);
            } catch (Exception e) {
                Log.e(TAG, "Unable to start activity " + intent.toString());
            }
        }
        return true;
    }

    private String shortRomVersion() {
        String romVersion = SystemProperties.get(KEY_RYZ_VERSION_PROP,
                this.mContext.getString(R.string.device_info_default));
        String romAndroid = SystemProperties.get(KEY_RYZ_ANDROID_PROP,
                this.mContext.getString(R.string.device_info_default));
        String romDevice = SystemProperties.get(KEY_RYZ_DEVICE_PROP,
                this.mContext.getString(R.string.device_info_default));
        String romBuildtype = SystemProperties.get(KEY_RYZ_BUILDTYPE_PROP,
                this.mContext.getString(R.string.device_info_default));
        String shortVersion = romAndroid + " (" + romVersion + ") | " + romDevice + " | " + romBuildtype;
        return shortVersion;
    }

    /**
     * Copies the array onto itself to remove the oldest hit.
     */
    @VisibleForTesting
    void arrayCopy() {
        System.arraycopy(mHits, 1, mHits, 0, mHits.length - 1);
    }

    @VisibleForTesting
    void initializeAdminPermissions() {
        mFunDisallowedAdmin = RestrictedLockUtilsInternal.checkIfRestrictionEnforced(
                mContext, UserManager.DISALLOW_FUN, UserHandle.myUserId());
        mFunDisallowedBySystem = RestrictedLockUtilsInternal.hasBaseUserRestriction(
                mContext, UserManager.DISALLOW_FUN, UserHandle.myUserId());
    }
}