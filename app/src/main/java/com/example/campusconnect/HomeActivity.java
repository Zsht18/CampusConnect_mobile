package com.example.campusconnect;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class HomeActivity extends AppCompatActivity {

    private TextView tabBest, tabHot, tabNew, tabTop;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        tabBest = findViewById(R.id.tabBest);
        tabHot = findViewById(R.id.tabHot);
        tabNew = findViewById(R.id.tabNew);
        tabTop = findViewById(R.id.tabTop);
        
        View feedBest = findViewById(R.id.feedBest);
        View feedHot = findViewById(R.id.feedHot);
        View feedNew = findViewById(R.id.feedNew);
        View feedTop = findViewById(R.id.feedTop);

        View profileIconBtn = findViewById(R.id.profileIconBtn);
        View btnAddPost = findViewById(R.id.btnAddPost);

        if (profileIconBtn != null) {
            profileIconBtn.setOnClickListener(v -> {
                Intent intent = new Intent(HomeActivity.this, ProfileActivity.class);
                startActivity(intent);
            });
        }

        if (btnAddPost != null) {
            btnAddPost.setOnClickListener(v -> {
                Intent intent = new Intent(HomeActivity.this, CreatePostActivity.class);
                startActivity(intent);
            });
        }

        if (tabBest != null) {
            tabBest.setOnClickListener(v -> {
                feedBest.setVisibility(View.VISIBLE);
                feedHot.setVisibility(View.GONE);
                feedNew.setVisibility(View.GONE);
                feedTop.setVisibility(View.GONE);
                updateTabStyles(tabBest);
            });
        }
        if (tabHot != null) {
            tabHot.setOnClickListener(v -> {
                feedBest.setVisibility(View.GONE);
                feedHot.setVisibility(View.VISIBLE);
                feedNew.setVisibility(View.GONE);
                feedTop.setVisibility(View.GONE);
                updateTabStyles(tabHot);
            });
        }
        if (tabNew != null) {
            tabNew.setOnClickListener(v -> {
                feedBest.setVisibility(View.GONE);
                feedHot.setVisibility(View.GONE);
                feedNew.setVisibility(View.VISIBLE);
                feedTop.setVisibility(View.GONE);
                updateTabStyles(tabNew);
            });
        }
        if (tabTop != null) {
            tabTop.setOnClickListener(v -> {
                feedBest.setVisibility(View.GONE);
                feedHot.setVisibility(View.GONE);
                feedNew.setVisibility(View.GONE);
                feedTop.setVisibility(View.VISIBLE);
                updateTabStyles(tabTop);
            });
        }
    }

    private void updateTabStyles(TextView selectedTab) {
        // Reset all tabs to default unselected style
        TextView[] allTabs = {tabBest, tabHot, tabNew, tabTop};
        for (TextView tab : allTabs) {
            if (tab != null) {
                tab.setBackground(null);
                tab.setTextColor(Color.parseColor("#666666"));
            }
        }

        // Apply selected style to the clicked tab
        if (selectedTab != null) {
            selectedTab.setBackgroundResource(R.drawable.tab_best_bg);
            selectedTab.setTextColor(Color.parseColor("#000000"));
        }
    }
}