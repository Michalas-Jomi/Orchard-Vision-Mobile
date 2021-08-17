package me.jomi.orchardvision;

import android.app.Activity;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;

public abstract class DetailTreeActivity extends Activity {
    protected AutoCompleteTextView mType;
    protected AutoCompleteTextView mVariant;

    protected Button mConfirm;

    protected void setupTypeVariant() {
        mType   .setAdapter(new ArrayAdapter<>(this, R.layout.support_simple_spinner_dropdown_item, Data.getTypes()));
        mVariant.setAdapter(new ArrayAdapter<>(this, R.layout.support_simple_spinner_dropdown_item, Data.getVariants(null)));

        for (AutoCompleteTextView view : new AutoCompleteTextView[]{mType, mVariant}) {
            view.setOnClickListener(v -> view.showDropDown());
            view.setOnFocusChangeListener((v, hasFocus) -> {
                if (hasFocus) {
                    view.showDropDown();
                }
            });
        }

        mType.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                mVariant.setAdapter(new ArrayAdapter<>(DetailTreeActivity.this, R.layout.support_simple_spinner_dropdown_item, Data.getVariants(s.toString())));
            }
            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }
}
