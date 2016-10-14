package me.tahnok.bluefruit.le.connect.app;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.widget.AppCompatCheckBox;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.TextView;

import com.larswerkman.holocolorpicker.ColorPicker;
import com.larswerkman.holocolorpicker.SaturationBar;
import com.larswerkman.holocolorpicker.ValueBar;

import java.nio.ByteBuffer;

import me.tahnok.bluefruit.le.connect.R;
import me.tahnok.bluefruit.le.connect.ble.BleManager;

public class WesleyControllerActivity extends UartInterfaceActivity implements ColorPicker.OnColorChangedListener, CompoundButton.OnCheckedChangeListener {
    // Log
    private final static String TAG = ColorPickerActivity.class.getSimpleName();

    // Constants
    private final static boolean kPersistValues = true;
    private final static String kPreferences = "ColorPickerActivity_prefs";
    private final static String kPreferences_color = "color";

    private final static int kFirstTimeColor = 0x0000ff;

    // UI
    private ColorPicker mColorPicker;
    private View mRgbColorView;
    private TextView mRgbTextView;
    private Button offButton;
    private AppCompatCheckBox checkbox1;
    private AppCompatCheckBox checkbox2;
    private AppCompatCheckBox checkbox3;
    private AppCompatCheckBox checkbox4;
    private AppCompatCheckBox checkbox5;
    private AppCompatCheckBox checkbox6;
    private AppCompatCheckBox checkbox7;
    private AppCompatCheckBox checkbox8;

    private int mSelectedColor;
    private boolean[] seed = new boolean[8];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wesley_controller);

        mBleManager = BleManager.getInstance(this);

        // UI
        mRgbColorView = findViewById(R.id.rgbColorView);
        mRgbTextView = (TextView) findViewById(R.id.rgbTextView);
        offButton = (Button) findViewById(R.id.off_button);
        offButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendDataWithCRC("!X");
            }
        });

        checkbox1 = (AppCompatCheckBox) findViewById(R.id.seed_checkbox_1);
        checkbox1.setOnCheckedChangeListener(this);
        checkbox2 = (AppCompatCheckBox) findViewById(R.id.seed_checkbox_2);
        checkbox2.setOnCheckedChangeListener(this);
        checkbox3 = (AppCompatCheckBox) findViewById(R.id.seed_checkbox_3);
        checkbox3.setOnCheckedChangeListener(this);
        checkbox4 = (AppCompatCheckBox) findViewById(R.id.seed_checkbox_4);
        checkbox4.setOnCheckedChangeListener(this);
        checkbox5 = (AppCompatCheckBox) findViewById(R.id.seed_checkbox_5);
        checkbox5.setOnCheckedChangeListener(this);
        checkbox6 = (AppCompatCheckBox) findViewById(R.id.seed_checkbox_6);
        checkbox6.setOnCheckedChangeListener(this);
        checkbox7 = (AppCompatCheckBox) findViewById(R.id.seed_checkbox_7);
        checkbox7.setOnCheckedChangeListener(this);
        checkbox8 = (AppCompatCheckBox) findViewById(R.id.seed_checkbox_8);
        checkbox8.setOnCheckedChangeListener(this);

        SaturationBar mSaturationBar = (SaturationBar) findViewById(R.id.saturationbar);
        ValueBar mValueBar = (ValueBar) findViewById(R.id.valuebar);
        mColorPicker = (ColorPicker) findViewById(R.id.colorPicker);
        if (mColorPicker != null) {
            mColorPicker.addSaturationBar(mSaturationBar);
            mColorPicker.addValueBar(mValueBar);
            mColorPicker.setOnColorChangedListener(this);
        }

        if (kPersistValues) {
            SharedPreferences preferences = getSharedPreferences(kPreferences, MODE_PRIVATE);
            mSelectedColor = preferences.getInt(kPreferences_color, kFirstTimeColor);
        } else {
            mSelectedColor = kFirstTimeColor;
        }

        mColorPicker.setOldCenterColor(mSelectedColor);
        mColorPicker.setColor(mSelectedColor);
        onColorChanged(mSelectedColor);

        // Start services
        onServicesDiscovered();
    }

    @Override
    public void onStop() {
        // Preserve values
        if (kPersistValues) {
            SharedPreferences settings = getSharedPreferences(kPreferences, MODE_PRIVATE);
            SharedPreferences.Editor editor = settings.edit();
            editor.putInt(kPreferences_color, mSelectedColor);
            editor.apply();
        }

        super.onStop();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_color_picker, menu);
        return true;
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        int pos = Integer.valueOf((String) buttonView.getTag());
        seed[pos - 1] = isChecked;
        sendSeed();
    }

    private void sendSeed() {
        ByteBuffer buffer = ByteBuffer.allocate(2 + 8).order(java.nio.ByteOrder.LITTLE_ENDIAN);

        // prefix
        String prefix = "!S";
        buffer.put(prefix.getBytes());

        //Seed
        for(int i = 0; i < seed.length; i++) {
            if(seed[i]) {
                buffer.put((byte) 1);
            } else {
                buffer.put((byte) 0);
            }
        }

        sendDataWithCRC(buffer.array());

    }

    @Override
    public void onDisconnected() {
        super.onDisconnected();
        Log.d(TAG, "Disconnected. Back to previous activity");
        setResult(-1);      // Unexpected Disconnect
        finish();
    }

    @Override
    public void onColorChanged(int color) {
        // Save selected color
        mSelectedColor = color;

        // Update UI
        mRgbColorView.setBackgroundColor(color);

        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = (color >> 0) & 0xFF;
        String text = String.format(getString(R.string.colorpicker_rgbformat), r, g, b);
        mRgbTextView.setText(text);
    }


    public void onClickSend(View view) {
        // Set the old color
        mColorPicker.setOldCenterColor(mSelectedColor);

        // Send selected color !Crgb
        byte r = (byte) ((mSelectedColor >> 16) & 0xFF);
        byte g = (byte) ((mSelectedColor >> 8) & 0xFF);
        byte b = (byte) ((mSelectedColor >> 0) & 0xFF);

        ByteBuffer buffer = ByteBuffer.allocate(2 + 3 * 1).order(java.nio.ByteOrder.LITTLE_ENDIAN);

        // prefix
        String prefix = "!C";
        buffer.put(prefix.getBytes());

        // values
        buffer.put(r);
        buffer.put(g);
        buffer.put(b);

        byte[] result = buffer.array();
        sendDataWithCRC(result);
    }
}

