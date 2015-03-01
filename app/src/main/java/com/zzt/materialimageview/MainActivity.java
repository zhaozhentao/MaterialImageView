package com.zzt.materialimageview;

import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import com.zzt.library.MaterialImageView;


public class MainActivity extends ActionBarActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        MaterialImageView materialImageView = (MaterialImageView)findViewById(R.id.pic1);
        materialImageView.setRotation(-10);
    }

}
