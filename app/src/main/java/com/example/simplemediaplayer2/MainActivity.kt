package com.example.simplemediaplayer2

import androidx.appcompat.app.AppCompatActivity

import android.os.Bundle
import com.example.simplemediaplayer2.ui.HomeFragment

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        supportFragmentManager.beginTransaction()
            .add(R.id.container, HomeFragment.newInstance())
            .commit()
    }
}
