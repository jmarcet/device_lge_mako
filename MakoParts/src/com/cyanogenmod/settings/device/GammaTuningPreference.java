/*
 * Copyright (C) 2011 The CyanogenMod Project
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

package com.cyanogenmod.settings.device;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.preference.DialogPreference;
import android.preference.PreferenceManager;
import android.util.AttributeSet;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;
import android.util.Log;
import android.widget.Button;

/**
 * Special preference type that allows configuration of Gamma settings on Nexus
 * Devices
 */
public class GammaTuningPreference extends DialogPreference {
    private static final String TAG = "GammaTuningPreference";

    private static final String GAMMA_R_FILE = "/sys/devices/platform/mipi_lgit.1537/kgamma_r";
    private static final String GAMMA_G_FILE = "/sys/devices/platform/mipi_lgit.1537/kgamma_g";
    private static final String GAMMA_B_FILE = "/sys/devices/platform/mipi_lgit.1537/kgamma_b";
    private static final String GAMMA_CTRL_FILE = "/sys/devices/platform/mipi_lgit.1537/kgamma_apply";
    private static final int MAX_VALUE = 31;
    private static final int RESET_VALUE = 0;

    // These arrays must all match in length and order
    private static final int[] SEEKBAR_R_ID = new int[] {
        R.id.gamma_amp0_red_seekbar,
        R.id.gamma_amp1_red_seekbar,
    };

    private static final int[] SEEKBAR_R_VALUE_ID = new int[] {
        R.id.gamma_amp0_red_value,
        R.id.gamma_amp1_red_value,
    };

    private static final int[] SEEKBAR_G_ID = new int[] {
        R.id.gamma_amp0_green_seekbar,
        R.id.gamma_amp1_green_seekbar,
    };

    private static final int[] SEEKBAR_G_VALUE_ID = new int[] {
        R.id.gamma_amp0_green_value,
        R.id.gamma_amp1_green_value,
    };

    private static final int[] SEEKBAR_B_ID = new int[] {
        R.id.gamma_amp0_blue_seekbar,
        R.id.gamma_amp1_blue_seekbar,
    };

    private static final int[] SEEKBAR_B_VALUE_ID = new int[] {
        R.id.gamma_amp0_blue_value,
        R.id.gamma_amp1_blue_value,
    };

    private rGammaSeekBar[] mSeekBarsR = new rGammaSeekBar[SEEKBAR_R_ID.length];
    private String[] mCurrentGammaR;
    private String mOriginalGammaR;
    private String[] mGammaSplitR;
    private String mGammaAbbrR;

    private gGammaSeekBar[] mSeekBarsG = new gGammaSeekBar[SEEKBAR_G_ID.length];
    private String[] mCurrentGammaG;
    private String mOriginalGammaG;
    private String[] mGammaSplitG;
    private String mGammaAbbrG;

    private bGammaSeekBar[] mSeekBarsB = new bGammaSeekBar[SEEKBAR_B_ID.length];
    private String[] mCurrentGammaB;
    private String mOriginalGammaB;
    private String[] mGammaSplitB;
    private String mGammaAbbrB;

    public GammaTuningPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        setDialogLayoutResource(R.layout.preference_dialog_gamma_tuning);
    }

    @Override
    protected void onPrepareDialogBuilder(AlertDialog.Builder builder) {
        builder.setNeutralButton(R.string.defaults_button, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
            }
        });
    }

    @Override
    protected void onBindDialogView(View view) {
        super.onBindDialogView(view);

        mOriginalGammaR = readGammaR();
        mGammaSplitR = mOriginalGammaR.split("\\s+");
        mGammaAbbrR = mGammaSplitR[5]+" "+mGammaSplitR[6];
        mCurrentGammaR = mGammaAbbrR.split("\\s+");
        mOriginalGammaG = readGammaG();
        mGammaSplitG = mOriginalGammaG.split("\\s+");
        mGammaAbbrG = mGammaSplitG[5]+" "+mGammaSplitG[6];
        mCurrentGammaG = mGammaAbbrG.split("\\s+");
        mOriginalGammaB = readGammaB();
        mGammaSplitB = mOriginalGammaB.split("\\s+");
        mGammaAbbrB = mGammaSplitB[5]+" "+mGammaSplitB[6];
        mCurrentGammaB = mGammaAbbrB.split("\\s+");


        for (int i = 0; i < SEEKBAR_R_ID.length; i++) {
            SeekBar seekBarR = (SeekBar) view.findViewById(SEEKBAR_R_ID[i]);
            TextView valueR = (TextView) view.findViewById(SEEKBAR_R_VALUE_ID[i]);
            mSeekBarsR[i] = new rGammaSeekBar(seekBarR, valueR, i, Integer.valueOf(mCurrentGammaR[i]));
        }

        for (int j = 0; j < SEEKBAR_G_ID.length; j++) {
            SeekBar seekBarG = (SeekBar) view.findViewById(SEEKBAR_G_ID[j]);
            TextView valueG = (TextView) view.findViewById(SEEKBAR_G_VALUE_ID[j]);
            mSeekBarsG[j] = new gGammaSeekBar(seekBarG, valueG, j, Integer.valueOf(mCurrentGammaG[j]));
        }
        for (int k = 0; k < SEEKBAR_B_ID.length; k++) {
            SeekBar seekBarB = (SeekBar) view.findViewById(SEEKBAR_B_ID[k]);
            TextView valueB = (TextView) view.findViewById(SEEKBAR_B_VALUE_ID[k]);
            mSeekBarsB[k] = new bGammaSeekBar(seekBarB, valueB, k, Integer.valueOf(mCurrentGammaB[k]));
        }
    }

    @Override
    protected void showDialog(Bundle state) {
        super.showDialog(state);

        // can't use onPrepareDialogBuilder for this as we want the dialog
        // to be kept open on click
        AlertDialog d = (AlertDialog) getDialog();
        Button defaultsButton = d.getButton(DialogInterface.BUTTON_NEUTRAL);
        defaultsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                for (rGammaSeekBar csb : mSeekBarsR) {
                    csb.mSeekBarR.setProgress(RESET_VALUE);
                }
                for (gGammaSeekBar csb : mSeekBarsG) {
                    csb.mSeekBarG.setProgress(RESET_VALUE);
                }
                for (bGammaSeekBar csb : mSeekBarsB) {
                    csb.mSeekBarB.setProgress(RESET_VALUE);
                }
            }
       });
    }

    @Override
    protected void onDialogClosed(boolean positiveResult) {
        super.onDialogClosed(positiveResult);

        if (positiveResult) {
            Editor editor = getEditor();
            editor.putString(GAMMA_R_FILE, readGammaR());
            editor.putString(GAMMA_G_FILE, readGammaG());
            editor.putString(GAMMA_B_FILE, readGammaB());
            editor.commit();
        } else {
            writeGammaR(mOriginalGammaR);
            writeGammaG(mOriginalGammaG);
            writeGammaB(mOriginalGammaB);
        }
    }

    public static void restore(Context context) {
        if (!isSupported()) {
            return;
        }

        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        final String valueR = prefs.getString(GAMMA_R_FILE, null);
        final String valueG = prefs.getString(GAMMA_G_FILE, null);
        final String valueB = prefs.getString(GAMMA_B_FILE, null);

        if (valueR != null) {
            Log.d(TAG, "Restoring gamma values: " + valueR);
            writeGammaR(valueR);
        }
        if (valueG != null) {
            Log.d(TAG, "Restoring gamma values: " + valueG);
            writeGammaG(valueG);
        }
        if (valueB != null) {
            Log.d(TAG, "Restoring gamma values: " + valueB);
            writeGammaB(valueB);
        }
    }

    public static boolean isSupported() {
        return Utils.fileExists(GAMMA_R_FILE) && Utils.fileExists(GAMMA_G_FILE) && Utils.fileExists(GAMMA_B_FILE) && Utils.fileExists(GAMMA_CTRL_FILE);
    }

    private class rGammaSeekBar implements SeekBar.OnSeekBarChangeListener {
        private int mIndexR;
        private int mOriginalR;
        private SeekBar mSeekBarR;
        private TextView mValueR;
        private String mNewvalGammaR;

        public rGammaSeekBar(SeekBar seekBarR, TextView valueR, int indexR, int originalR) {
            mSeekBarR = seekBarR;
            mValueR = valueR;
            mIndexR = indexR;
            mOriginalR = originalR;

            mSeekBarR.setMax(MAX_VALUE);
            mSeekBarR.setOnSeekBarChangeListener(this);
            mSeekBarR.setProgress(mOriginalR);
        }

        @Override
        public void onProgressChanged(SeekBar seekBarR, int progressR, boolean fromUser) {
            mCurrentGammaR[mIndexR] = String.valueOf(progressR);

            String mGammaConcR = "";
            int gammaconcr = 0;

        for (int i = 1; i < 5; i++) {
            int gamma_r = Integer.parseInt(mGammaSplitR[i]);
            gammaconcr += gamma_r;
            mGammaConcR = mGammaConcR + " " + gamma_r;
        }

        mGammaConcR = mGammaConcR + " " + mCurrentGammaR[0] + " " + mCurrentGammaR[1];
        gammaconcr += Integer.parseInt(mCurrentGammaR[0]);
        gammaconcr += Integer.parseInt(mCurrentGammaR[1]);

        for (int i = 7; i < 10; i++) {
            int gamma_r = Integer.parseInt(mGammaSplitR[i]);
            gammaconcr += gamma_r;
            mGammaConcR = mGammaConcR + " " + gamma_r;
        }

        mNewvalGammaR = gammaconcr + mGammaConcR;
            writeGammaR(mNewvalGammaR);

            mValueR.setText(String.format("%d", progressR));
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBarR) {
            // Do nothing
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBarR) {
            // Do nothing
        }
    }

    private class gGammaSeekBar implements SeekBar.OnSeekBarChangeListener {
        private int mIndexG;
        private int mOriginalG;
        private SeekBar mSeekBarG;
        private TextView mValueG;
        private String mNewvalGammaG;

        public gGammaSeekBar(SeekBar seekBarG, TextView valueG, int indexG, int originalG) {
            mSeekBarG = seekBarG;
            mValueG = valueG;
            mIndexG = indexG;
            mOriginalG = originalG;

            mSeekBarG.setMax(MAX_VALUE);
            mSeekBarG.setOnSeekBarChangeListener(this);
            mSeekBarG.setProgress(mOriginalG);
        }

        @Override
        public void onProgressChanged(SeekBar seekBarG, int progressG, boolean fromUser) {
            mCurrentGammaG[mIndexG] = String.valueOf(progressG);

            String mGammaConcG = "";
            int gammaconcg = 0;

        for (int i = 1; i < 5; i++) {
            int gamma_g = Integer.parseInt(mGammaSplitG[i]);
            gammaconcg += gamma_g;
            mGammaConcG = mGammaConcG + " " + gamma_g;
        }

        mGammaConcG = mGammaConcG + " " + mCurrentGammaG[0] + " " + mCurrentGammaG[1];
        gammaconcg += Integer.parseInt(mCurrentGammaG[0]);
        gammaconcg += Integer.parseInt(mCurrentGammaG[1]);

        for (int i = 7; i < 10; i++) {
            int gamma_g = Integer.parseInt(mGammaSplitG[i]);
            gammaconcg += gamma_g;
            mGammaConcG = mGammaConcG + " " + gamma_g;
        }

        mNewvalGammaG = gammaconcg + mGammaConcG;
            writeGammaG(mNewvalGammaG);

            mValueG.setText(String.format("%d", progressG));
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBarG) {
            // Do nothing
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBarG) {
            // Do nothing
        }
    }

    private class bGammaSeekBar implements SeekBar.OnSeekBarChangeListener {
        private int mIndexB;
        private int mOriginalB;
        private SeekBar mSeekBarB;
        private TextView mValueB;
        private String mNewvalGammaB;

        public bGammaSeekBar(SeekBar seekBarB, TextView valueB, int indexB, int originalB) {
            mSeekBarB = seekBarB;
            mValueB = valueB;
            mIndexB = indexB;
            mOriginalB = originalB;

            mSeekBarB.setMax(MAX_VALUE);
            mSeekBarB.setOnSeekBarChangeListener(this);
            mSeekBarB.setProgress(mOriginalB);
        }

        @Override
        public void onProgressChanged(SeekBar seekBarB, int progressB, boolean fromUser) {
            mCurrentGammaB[mIndexB] = String.valueOf(progressB);

            String mGammaConcB = "";
            int gammaconcb = 0;

        for (int i = 1; i < 5; i++) {
            int gamma_b = Integer.parseInt(mGammaSplitB[i]);
            gammaconcb += gamma_b;
            mGammaConcB = mGammaConcB + " " + gamma_b;
        }

        mGammaConcB = mGammaConcB + " " + mCurrentGammaB[0] + " " + mCurrentGammaB[1];
        gammaconcb += Integer.parseInt(mCurrentGammaB[0]);
        gammaconcb += Integer.parseInt(mCurrentGammaB[1]);

        for (int i = 7; i < 10; i++) {
            int gamma_b = Integer.parseInt(mGammaSplitB[i]);
            gammaconcb += gamma_b;
            mGammaConcB = mGammaConcB + " " + gamma_b;
        }

        mNewvalGammaB = gammaconcb + mGammaConcB;
            writeGammaB(mNewvalGammaB);

            mValueB.setText(String.format("%d", progressB));
        }

        @Override
        public void onStartTrackingTouch(SeekBar seekBarB) {
            // Do nothing
        }

        @Override
        public void onStopTrackingTouch(SeekBar seekBarB) {
            // Do nothing
        }
    }

    private static String readGammaR() {
        return Utils.readOneLine(GAMMA_R_FILE);
    }

    private static String readGammaG() {
        return Utils.readOneLine(GAMMA_G_FILE);
    }

    private static String readGammaB() {
        return Utils.readOneLine(GAMMA_B_FILE);
    }

    private static void writeGammaR(String gammaR) {
        Utils.writeValue(GAMMA_R_FILE, gammaR);
        Utils.writeValue(GAMMA_CTRL_FILE, "1");
    }

    private static void writeGammaG(String gammaG) {
        Utils.writeValue(GAMMA_G_FILE, gammaG);
        Utils.writeValue(GAMMA_CTRL_FILE, "1");
    }

    private static void writeGammaB(String gammaB) {
        Utils.writeValue(GAMMA_B_FILE, gammaB);
        Utils.writeValue(GAMMA_CTRL_FILE, "1");
    }
}
