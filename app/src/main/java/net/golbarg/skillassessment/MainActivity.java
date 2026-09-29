package net.golbarg.skillassessment;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import net.golbarg.skillassessment.ads.AdManager;
import net.golbarg.skillassessment.databinding.ActivityMainBinding;
import net.golbarg.skillassessment.util.UiUtils;

/** Hosts the bottom-navigation tabs: topics, saved questions, progress and settings. */
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        UiUtils.enableEdgeToEdge(this);
        super.onCreate(savedInstanceState);
        ActivityMainBinding binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        NavHostFragment host = (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment_activity_main);
        if (host != null) {
            NavController navController = host.getNavController();
            NavigationUI.setupWithNavController(binding.navView, navController);
        }
        // Asks for ad consent where the law requires it; banners wait until this resolves.
        AdManager.gatherConsent(this);
    }
}
