package com.duy.calculator.tiles;

import android.content.Intent;
import android.os.Build;
import android.service.quicksettings.TileService;
import android.support.annotation.RequiresApi;
import android.util.Log;

import com.duy.calculator.floating.FloatingOpenShortCutActivity;

/**
 * Created by Duy on 9/5/2017.
 */

@RequiresApi(api = Build.VERSION_CODES.N)
public class CalculatorTileService extends TileService {
    private static final String TAG = "CalculatorTileService";

    @Override
    public void onStartListening() {
        super.onStartListening();
        Log.d(TAG, "onStartListening() called");


    }

    @Override
    public void onStopListening() {
        super.onStopListening();
        Log.d(TAG, "onStopListening() called");

    }

    @Override
    public void onTileAdded() {
        super.onTileAdded();
        Log.d(TAG, "onTileAdded() called");

    }

    @Override
    public void onClick() {
        super.onClick();
        sendBroadcast(new Intent(Intent.ACTION_CLOSE_SYSTEM_DIALOGS));
        startActivity(new Intent(this, FloatingOpenShortCutActivity.class));
    }
}
