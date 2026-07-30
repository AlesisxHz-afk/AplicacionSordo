package com.example.aplicacionsordo;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class LoginActivity extends AppCompatActivity {

    private TextInputLayout tilEmail, tilPassword;
    private TextInputEditText etEmail, etPassword;
    private MaterialButton btnLogin, btnGoogle;
    private TextView tvRegister;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sessionManager = new SessionManager(this);

        // Check if user session is already active (Auto-Login persistence)
        if (sessionManager.isLoggedIn()) {
            User activeUser = sessionManager.getUserSession();
            Intent intent = new Intent(LoginActivity.this, HomeActivity.class);
            intent.putExtra("EXTRA_USER", activeUser);
            startActivity(intent);
            finish();
            return;
        }

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);

        // Adjust window insets for edge-to-edge
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        initViews();
        setupListeners();
    }

    private void initViews() {
        tilEmail = findViewById(R.id.tilEmail);
        tilPassword = findViewById(R.id.tilPassword);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnGoogle = findViewById(R.id.btnGoogle);
        tvRegister = findViewById(R.id.tvRegister);
    }

    private void setupListeners() {
        // Clear errors as user types
        etEmail.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                tilEmail.setError(null);
            }
        });

        etPassword.addTextChangedListener(new SimpleTextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                tilPassword.setError(null);
            }
        });

        // Login Button Click
        btnLogin.setOnClickListener(v -> attemptLogin());

        // Google Sign-In Click
        btnGoogle.setOnClickListener(v -> handleGoogleSignIn());

        // Register Link Click
        tvRegister.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
        });
    }

    private void attemptLogin() {
        String usernameInput = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
        String password = etPassword.getText() != null ? etPassword.getText().toString().trim() : "";

        boolean isValid = true;

        // Validation
        if (TextUtils.isEmpty(usernameInput)) {
            tilEmail.setError("Por favor ingresa tu usuario o correo");
            isValid = false;
        }

        if (TextUtils.isEmpty(password)) {
            tilPassword.setError(getString(R.string.error_empty_password));
            isValid = false;
        }

        if (isValid) {
            setLoadingState(true);

            DatabaseHelper.authenticateUser(usernameInput, password, new DatabaseHelper.LoginCallback() {
                @Override
                public void onSuccess(User user) {
                    setLoadingState(false);

                    // Save session so user stays logged in across app restarts
                    sessionManager.saveSession(user);

                    String welcomeMsg = "¡Bienvenido, " + (user.getNombreCompleto().isEmpty() ? user.getUsuario() : user.getNombreCompleto()) + "!";
                    Toast.makeText(LoginActivity.this, welcomeMsg, Toast.LENGTH_LONG).show();

                    // Navigate to Home Activity
                    Intent intent = new Intent(LoginActivity.this, HomeActivity.class);
                    intent.putExtra("EXTRA_USER", user);
                    startActivity(intent);
                    finish();
                }

                @Override
                public void onError(String errorMessage) {
                    setLoadingState(false);
                    Toast.makeText(LoginActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                    tilPassword.setError(errorMessage);
                }
            });
        }
    }

    private void handleGoogleSignIn() {
        String[] accounts = {"usuario.google@gmail.com", "mi.cuenta@gmail.com"};
        new MaterialAlertDialogBuilder(this)
                .setTitle("Selecciona una cuenta de Google")
                .setItems(accounts, (dialog, which) -> {
                    String selectedAccount = accounts[which];
                    setLoadingState(true);

                    // Create Google User session
                    User googleUser = new User(999, selectedAccount.split("@")[0], "Usuario", "Google", "", true);

                    sessionManager.saveSession(googleUser);

                    Toast.makeText(LoginActivity.this, "¡Bienvenido con Google, " + selectedAccount + "!", Toast.LENGTH_LONG).show();

                    Intent intent = new Intent(LoginActivity.this, HomeActivity.class);
                    intent.putExtra("EXTRA_USER", googleUser);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void setLoadingState(boolean isLoading) {
        if (isLoading) {
            btnLogin.setEnabled(false);
            btnGoogle.setEnabled(false);
            btnLogin.setText("Iniciando sesión...");
        } else {
            btnLogin.setEnabled(true);
            btnGoogle.setEnabled(true);
            btnLogin.setText(getString(R.string.btn_login));
        }
    }

    private abstract static class SimpleTextWatcher implements TextWatcher {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

        @Override
        public void afterTextChanged(Editable s) {}
    }
}
