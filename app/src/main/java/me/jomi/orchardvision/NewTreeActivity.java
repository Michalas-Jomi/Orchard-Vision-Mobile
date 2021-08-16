package me.jomi.orchardvision;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import androidx.annotation.Nullable;
import com.google.android.gms.maps.model.LatLng;

public class NewTreeActivity extends Activity {

    private double latitude;
    private double longitude;

    private AutoCompleteTextView mType;
    private AutoCompleteTextView mVariant;
    private EditText mAge;
    private Button mConfirm;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_new_tree);


        // Data
        latitude  = getIntent().getExtras().getDouble("latitude");
        longitude = getIntent().getExtras().getDouble("longitude");

        // Views
        mType = findViewById(R.id.NewTree_Type_Text);
        mVariant = findViewById(R.id.NewTree_Variant_Text);
        mAge = findViewById(R.id.NewTree_Age_Number);
        mConfirm = findViewById(R.id.NewTree_Confirm_Button);

        // Register
        mConfirm.setOnClickListener(v -> {
            Tree tree = new Tree(
                    new LatLng(latitude, longitude),
                    mType.getText().toString(),
                    mVariant.getText().toString(),
                    Func.Int(mAge.getText().toString())
            );

            NewTreeActivity.this.setResult(0, new Intent()
                    .putExtras(tree.buildBundle())
                    .putExtra("activity", NewTreeActivity.class.getName())
            );
            NewTreeActivity.this.finish();
        });
    }
}
