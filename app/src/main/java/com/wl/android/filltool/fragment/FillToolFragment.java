package com.wl.android.filltool.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.wl.android.filltool.FillSmsActivity;
import com.wl.android.filltool.R;

/**
 * Created by D22397 on 2017/8/28.
 */

public class FillToolFragment extends Fragment implements View.OnClickListener {

    private Button mRamButton;
    private Button mRomButton;
    private Button mSmsButton;
    private Button mContactButton;
    private Button mCallLogButton;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_fill_tool, container, false);

        mRamButton = (Button) view.findViewById(R.id.ram_fill_button);
        mRomButton = (Button) view.findViewById(R.id.rom_fill_button);
        mSmsButton = (Button) view.findViewById(R.id.sms_fill_button);
        mContactButton = (Button) view.findViewById(R.id.contact_fill_button);
        mCallLogButton = (Button) view.findViewById(R.id.call_log_fill_button);

        mRamButton.setOnClickListener(this);
        mRomButton.setOnClickListener(this);
        mSmsButton.setOnClickListener(this);
        mContactButton.setOnClickListener(this);
        mCallLogButton.setOnClickListener(this);

        return view;
    }

    @Override
    public void onClick(View view) {
        FragmentManager fm = getActivity().getSupportFragmentManager();
        FragmentTransaction transaction = fm.beginTransaction();
        switch (view.getId()) {
            case R.id.ram_fill_button:
                Fragment ramFillFragment = new RamFillFragment();
                transaction.hide(this);
                transaction.add(R.id.contentfill_fragment_container, ramFillFragment);
                transaction.addToBackStack(null);
                break;
            case R.id.rom_fill_button:
                Fragment romFillFragment = new RomFillFragment();
                transaction.hide(this);
                transaction.add(R.id.contentfill_fragment_container, romFillFragment);
                transaction.addToBackStack(null);
                break;
            case R.id.sms_fill_button:
                startActivity(new Intent(getActivity(), FillSmsActivity.class));
                break;
            case R.id.contact_fill_button:
                Fragment contactFillFragment = new ContactFillFragment();
                transaction.hide(this);
                transaction.add(R.id.contentfill_fragment_container, contactFillFragment);
                transaction.addToBackStack(null);
                break;
            case R.id.call_log_fill_button:
                Fragment callLogFragment = new CallLogFillFragment();
                transaction.hide(this);
                transaction.add(R.id.contentfill_fragment_container, callLogFragment);
                transaction.addToBackStack(null);
                break;
            default:
                break;
        }
        transaction.commit();
    }

}
