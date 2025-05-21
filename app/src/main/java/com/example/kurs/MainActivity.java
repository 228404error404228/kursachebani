package com.example.kurs;

import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.android.libraries.places.api.Places;

public class MainActivity extends AppCompatActivity {

    private NavController navController;
    private BottomNavigationView bottomNav;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (!Places.isInitialized()) {
            Places.initialize(getApplicationContext(), "AIzaSyCHqNhuw8PGGOfkSDcqYCDhLHIKMESYWD0");
        }
        // Получаем NavHostFragment и NavController
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);

        if (navHostFragment != null) {
            navController = navHostFragment.getNavController();

            // Привязываем нижнюю навигацию
            bottomNav = findViewById(R.id.bottom_navigation); // Убедись, что ID такой в XML
            NavigationUI.setupWithNavController(bottomNav, navController);

            // Скрытие/показ навигации в зависимости от текущего фрагмента
            navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
                if (destination.getId() == R.id.authFragment) {
                    bottomNav.setVisibility(View.GONE);
                } else {
                    bottomNav.setVisibility(View.VISIBLE);
                }
            });

            // Проверка, авторизован ли пользователь
            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            if (currentUser == null) {
                navController.navigate(R.id.authFragment);


            }
        }
    }
}
