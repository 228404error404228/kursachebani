package com.example.kurs.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.example.kurs.R;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;

import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;


public class AuthFragment extends Fragment {

    private FirebaseAuth mAuth;
    private EditText emailEditText, passwordEditText;
    private Button loginButton, registerButton;
    private GoogleSignInClient googleSignInClient;

    private DatabaseReference databaseRef;
    private static final int RC_SIGN_IN = 9001;

    public AuthFragment() {}

    private void writeUserToDatabase(String userId, String email) {
        User user = new User(email);
        databaseRef.child(userId).setValue(user)
                .addOnSuccessListener(aVoid -> {
                    // можно вывести сообщение или лог
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Ошибка при записи в БД: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.auth_fragment, container, false);
        databaseRef = FirebaseDatabase.getInstance().getReference("users");


        // Firebase
        mAuth = FirebaseAuth.getInstance();

        // Google Sign-In конфигурация
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        googleSignInClient = GoogleSignIn.getClient(requireActivity(), gso);

        // Поля ввода
        emailEditText = view.findViewById(R.id.emailEditText);
        passwordEditText = view.findViewById(R.id.passwordEditText);
        loginButton = view.findViewById(R.id.loginButton);
        registerButton = view.findViewById(R.id.registerButton);
        SignInButton googleButton = view.findViewById(R.id.googleSignInButton);

        // Обработчики кнопок
        loginButton.setOnClickListener(v -> loginUser());
        registerButton.setOnClickListener(v -> registerUser());
        googleButton.setOnClickListener(v -> signInWithGoogle());

        return view;
    }

    private void loginUser() {
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            Toast.makeText(getContext(), "Введите email и пароль", Toast.LENGTH_SHORT).show();
            return;
        }

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        Toast.makeText(getContext(), "Успешный вход: " + user.getEmail(), Toast.LENGTH_SHORT).show();
                        navigateToProfile();
                    } else {
                        Toast.makeText(getContext(), "Ошибка входа: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void registerUser() {
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        if (TextUtils.isEmpty(email) || TextUtils.isEmpty(password)) {
            Toast.makeText(getContext(), "Введите email и пароль", Toast.LENGTH_SHORT).show();
            return;
        }

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        Toast.makeText(getContext(), "Регистрация прошла успешно: " + user.getEmail(), Toast.LENGTH_SHORT).show();
                    }
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            writeUserToDatabase(user.getUid(), user.getEmail());
                        }
                        Toast.makeText(getContext(), "Регистрация прошла успешно: " + user.getEmail(), Toast.LENGTH_SHORT).show();
                    }

                    else {
                        Toast.makeText(getContext(), "Ошибка регистрации: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });

    }

    private void signInWithGoogle() {
        Intent signInIntent = googleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuthWithGoogle(account.getIdToken());
            } catch (ApiException e) {
                Toast.makeText(getContext(), "Ошибка Google входа: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(requireActivity(), task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        Toast.makeText(getContext(), "Вход через Google: " + user.getEmail(), Toast.LENGTH_SHORT).show();
                        navigateToProfile();
                    }
                    FirebaseUser user = mAuth.getCurrentUser();
                    if (user != null) {
                        writeUserToDatabase(user.getUid(), user.getEmail());
                    }
                    else {
                        Toast.makeText(getContext(), "Ошибка Google входа: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void navigateToProfile() {
        NavController navController = NavHostFragment.findNavController(AuthFragment.this);
        navController.navigate(R.id.action_authFragment_to_profileFragment);
    }
}
