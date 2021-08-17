package me.jomi.orchardvision;

import android.os.Bundle;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import androidx.annotation.Nullable;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class EditTreeActivity extends DetailTreeActivity {

    private EditText mPlanted;
    private EditText mNote;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_edit_tree);

        // Views
        mType    = findViewById(R.id.editTree_Type_Text);
        mVariant = findViewById(R.id.editTree_Variant_Text);
        mPlanted = findViewById(R.id.editTree_Planted_Date);
        mNote    = findViewById(R.id.editTree_Note_Text);
        mConfirm = findViewById(R.id.editTree_Confirm_Button);

        Bundle data = getIntent().getExtras();
        mType.setText(data.getString("type"));
        mVariant.setText(data.getString("variant"));
        mPlanted.setText(data.getString("planting_date"));
        mNote.setText(data.getString("note"));

        int treeId = data.getInt("id");


        // Register
        setupTypeVariant();

        mConfirm.setOnClickListener(new View.OnClickListener() {
            private boolean ok;
            @Override
            public void onClick(View v) {
                // Validation
                ok = true;

                int id = treeId;
                String type          = mType   .getText().toString();
                String variant       = mVariant.getText().toString();
                String note          = mNote   .getText().toString();
                String planting_date = mPlanted.getText().toString();
                Date date;

                check(mType, type.isEmpty(), "To pole nie może być puste");
                check(mVariant, variant.isEmpty(), "To pole nie może być puste");
                try {
                    SimpleDateFormat format = new SimpleDateFormat("dd-MM-yyyy");
                    format.setLenient(false);
                    date = format.parse(planting_date);
                    check(mPlanted, date.after(new Date()), "Podano date z przyszłości");
                } catch (ParseException e) {
                    check(mPlanted, true, "Format: dd-MM-yyyy");
                    return;
                }

                if (ok) {
                    new Thread(() -> {
                        try {
                            Func.sendPostRequest(EditTreeActivity.this.getString(R.string.serverUrl) + "broker/edit/tree",
                                    new Pair<>("planting_data", new SimpleDateFormat("yyyy-MM-dd").format(date)),
                                    new Pair<>("id", String.valueOf(id)),
                                    new Pair<>("variant", variant),
                                    new Pair<>("type", type),
                                    new Pair<>("note", note)
                            );
                        } catch (IOException e) {
                            Func.throwEx(e);
                        }
                    }).start();

                    EditTreeActivity.this.finish();
                }
            }
            private void check(TextView view, boolean condition, String msg) {
                ok = ok & !condition;
                if (condition)
                    view.setError(msg);
            }
        });
    }
}
