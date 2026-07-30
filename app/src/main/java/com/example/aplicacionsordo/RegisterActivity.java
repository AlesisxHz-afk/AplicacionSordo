package com.example.aplicacionsordo;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;

public class RegisterActivity extends AppCompatActivity {

    private ImageView imgProfilePhoto;
    private MaterialButton btnSelectPhoto, btnRegisterSubmit;
    private TextInputLayout tilNombres, tilApellidos, tilUsuario, tilPassword, tilConfirmPassword;
    private TextInputEditText etNombres, etApellidos, etUsuario, etPassword, etConfirmPassword;
    private TextView tvBackToLogin;

    private Uri selectedPhotoUri = null;
    private SessionManager sessionManager;

    private final ActivityResultLauncher<String> photoPickerLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    selectedPhotoUri = uri;
                    imgProfilePhoto.setPadding(0, 0, 0, 0);
                    imgProfilePhoto.setImageURI(uri);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_register);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        sessionManager = new SessionManager(this);

        initViews();
        setupListeners();
    }

    private void initViews() {
        imgProfilePhoto = findViewById(R.id.imgProfilePhoto);
        btnSelectPhoto = findViewById(R.id.btnSelectPhoto);
        btnRegisterSubmit = findViewById(R.id.btnRegisterSubmit);

        tilNombres = findViewById(R.id.tilNombres);
        tilApellidos = findViewById(R.id.tilApellidos);
        tilUsuario = findViewById(R.id.tilUsuario);
        tilPassword = findViewById(R.id.tilPassword);
        tilConfirmPassword = findViewById(R.id.tilConfirmPassword);

        etNombres = findViewById(R.id.etNombres);
        etApellidos = findViewById(R.id.etApellidos);
        etUsuario = findViewById(R.id.etUsuario);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);

        tvBackToLogin = findViewById(R.id.tvBackToLogin);
    }

    private void setupListeners() {
        btnSelectPhoto.setOnClickListener(v -> photoPickerLauncher.launch("image/*"));
        imgProfilePhoto.setOnClickListener(v -> photoPickerLauncher.launch("image/*"));

        btnRegisterSubmit.setOnClickListener(v -> attemptRegistration());

        tvBackToLogin.setOnClickListener(v -> {
            finish();
        });
    }

    private void attemptRegistration() {
        String nombres = etNombres.getText() != null ? etNombres.getText().toString().trim() : "";
        String apellidos = etApellidos.getText() != null ? etApellidos.getText().toString().trim() : "";
        String usuario = etUsuario.getText() != null ? etUsuario.getText().toString().trim() : "";
        String password = etPassword.getText() != null ? etPassword.getText().toString().trim() : "";
        String confirmPassword = etConfirmPassword.getText() != null ? etConfirmPassword.getText().toString().trim() : "";

        boolean isValid = true;

        if (TextUtils.isEmpty(nombres)) {
            tilNombres.setError("Por favor ingresa tus nombres");
            isValid = false;
        } else {
            tilNombres.setError(null);
        }

        if (TextUtils.isEmpty(apellidos)) {
            tilApellidos.setError("Por favor ingresa tus apellidos");
            isValid = false;
        } else {
            tilApellidos.setError(null);
        }

        if (TextUtils.isEmpty(usuario)) {
            tilUsuario.setError("Por favor ingresa un nombre de usuario");
            isValid = false;
        } else {
            tilUsuario.setError(null);
        }

        if (TextUtils.isEmpty(password) || password.length() < 4) {
            tilPassword.setError("La contraseña debe tener al menos 4 caracteres");
            isValid = false;
        } else {
            tilPassword.setError(null);
        }

        if (!password.equals(confirmPassword)) {
            tilConfirmPassword.setError("Las contraseñas no coinciden");
            isValid = false;
        } else {
            tilConfirmPassword.setError(null);
        }

        if (isValid) {
            String[] apellidoParts = apellidos.split(" ");
            String apePaterno = apellidoParts.length > 0 ? apellidoParts[0] : apellidos;
            String apeMaterno = apellidoParts.length > 1 ? apellidoParts[1] : "";
            String photoUriString = selectedPhotoUri != null ? selectedPhotoUri.toString() : null;

            User newUser = new User((int) (System.currentTimeMillis() % 10000), usuario, nombres, apePaterno, apeMaterno, true, photoUriString);

            // Persist session so user stays logged in
            sessionManager.saveSession(newUser);

            Toast.makeText(this, "¡Registro exitoso! Bienvenido, " + newUser.getNombreCompleto(), Toast.LENGTH_LONG).show();

            Intent intent = new Intent(RegisterActivity.this, HomeActivity.class);
            intent.putExtra("EXTRA_USER", newUser);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        }
    }
}
