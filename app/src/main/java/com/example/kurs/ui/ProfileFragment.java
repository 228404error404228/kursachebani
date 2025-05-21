package com.example.kurs.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.example.kurs.R;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.firebase.auth.FirebaseAuth;

public class ProfileFragment extends Fragment {

    private GoogleSignInClient googleSignInClient;
    private FirebaseAuth mAuth;

    public ProfileFragment() {}

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        mAuth = FirebaseAuth.getInstance();

        // Настройка GoogleSignInClient
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        googleSignInClient = GoogleSignIn.getClient(requireActivity(), gso);

        Button logoutButton = view.findViewById(R.id.logoutButton);
        logoutButton.setOnClickListener(v -> signOut());

        return view;
    }

    private void signOut() {
        // Выход из Firebase
        mAuth.signOut();

        // Выход из Google (если использовался)
        googleSignInClient.signOut().addOnCompleteListener(requireActivity(), task -> {
            Toast.makeText(getContext(), "Вы вышли из аккаунта", Toast.LENGTH_SHORT).show();

            // Навигация на экран авторизации
            NavController navController = NavHostFragment.findNavController(this);
            navController.navigate(R.id.action_profileFragment_to_authFragment);
        });
    }
}
