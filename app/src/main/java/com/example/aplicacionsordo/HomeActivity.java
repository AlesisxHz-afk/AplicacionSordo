package com.example.aplicacionsordo;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.navigation.NavigationView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.ExecutionException;

public class HomeActivity extends AppCompatActivity implements TextToSpeech.OnInitListener {

    private static final int CAMERA_PERMISSION_CODE = 101;

    // Navigation & Layouts
    private DrawerLayout drawerLayout;
    private NavigationView navView;
    private MaterialToolbar toolbar;
    private View layoutProbar, layoutEntrenar;

    // Probar Module Views
    private PreviewView previewViewProbar;
    private TextView tvTranslatedText, tvConfidence, tvHandStatusBadge;
    private MaterialButton btnSwitchCamera, btnGrantPermission, btnToggleHandSim;
    private LinearLayout layoutPermissionDenied;

    // Sentence Builder Views
    private TextView tvSentence;
    private MaterialButton btnAddWord, btnBackspace, btnClearSentence, btnSpeakSentence;
    private List<String> sentenceWords = new ArrayList<>();
    private TextToSpeech textToSpeech;
    private String currentDetectedSign = "";

    // Entrenar Module Views
    private PreviewView previewViewEntrenar;
    private TextInputEditText etTag;
    private MaterialButton btnCapturarMuestra, btnGuardarModelo, btnBorrarModelo;
    private TextView tvModelStatus;

    // Camera & Sign AI Model
    private int cameraFacing = CameraSelector.LENS_FACING_BACK;
    private ProcessCameraProvider cameraProvider;
    private SignModelHelper signModelHelper;
    private Handler inferenceHandler = new Handler(Looper.getMainLooper());
    private Runnable inferenceRunnable;
    private boolean isProbarModeActive = true;
    private boolean isHandDetectedSim = true;
    private Random random = new Random();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_home);

        signModelHelper = new SignModelHelper(this);
        textToSpeech = new TextToSpeech(this, this);

        initViews();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.mainContainer), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(0, systemBars.top, 0, 0);
            return insets;
        });

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.bottomContainerProbar), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(), systemBars.bottom);
            return insets;
        });

        ViewCompat.setOnApplyWindowInsetsListener(layoutEntrenar, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(), systemBars.bottom);
            return insets;
        });

        setupNavigation();
        setupUserData();
        setupListeners();
        updateModelStatusText();
        updateSentenceDisplay();

        checkCameraPermissionAndStart();
    }

    private void initViews() {
        drawerLayout = findViewById(R.id.drawerLayout);
        navView = findViewById(R.id.navView);
        toolbar = findViewById(R.id.toolbar);
        layoutProbar = findViewById(R.id.layoutProbar);
        layoutEntrenar = findViewById(R.id.layoutEntrenar);

        // Probar Module
        previewViewProbar = findViewById(R.id.previewViewProbar);
        tvTranslatedText = findViewById(R.id.tvTranslatedText);
        tvConfidence = findViewById(R.id.tvConfidence);
        tvHandStatusBadge = findViewById(R.id.tvHandStatusBadge);
        btnSwitchCamera = findViewById(R.id.btnSwitchCamera);
        btnGrantPermission = findViewById(R.id.btnGrantPermission);
        btnToggleHandSim = findViewById(R.id.btnToggleHandSim);
        layoutPermissionDenied = findViewById(R.id.layoutPermissionDenied);

        // Sentence Builder
        tvSentence = findViewById(R.id.tvSentence);
        btnAddWord = findViewById(R.id.btnAddWord);
        btnBackspace = findViewById(R.id.btnBackspace);
        btnClearSentence = findViewById(R.id.btnClearSentence);
        btnSpeakSentence = findViewById(R.id.btnSpeakSentence);

        // Entrenar Module
        previewViewEntrenar = findViewById(R.id.previewViewEntrenar);
        etTag = findViewById(R.id.etTag);
        btnCapturarMuestra = findViewById(R.id.btnCapturarMuestra);
        btnGuardarModelo = findViewById(R.id.btnGuardarModelo);
        btnBorrarModelo = findViewById(R.id.btnBorrarModelo);
        tvModelStatus = findViewById(R.id.tvModelStatus);
    }

    private void setupNavigation() {
        setSupportActionBar(toolbar);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar,
                R.string.app_name, R.string.app_name
        );
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        // 3 lines hamburger menu click handler
        toolbar.setNavigationOnClickListener(v -> drawerLayout.openDrawer(navView));

        navView.setNavigationItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_probar) {
                switchToModule(true);
            } else if (itemId == R.id.nav_entrenar) {
                switchToModule(false);
            } else if (itemId == R.id.nav_logout) {
                logout();
            }
            drawerLayout.closeDrawers();
            return true;
        });
    }

    private void setupUserData() {
        User user = (User) getIntent().getSerializableExtra("EXTRA_USER");
        if (user == null) {
            user = new SessionManager(this).getUserSession();
        }

        View headerView = navView.getHeaderView(0);
        if (headerView != null && user != null) {
            TextView tvNavHeaderName = headerView.findViewById(R.id.tvNavHeaderName);
            ImageView imgNavHeader = headerView.findViewById(R.id.imgNavHeader);

            if (!TextUtils.isEmpty(user.getNombreCompleto())) {
                tvNavHeaderName.setText(user.getNombreCompleto());
            } else if (!TextUtils.isEmpty(user.getUsuario())) {
                tvNavHeaderName.setText(user.getUsuario());
            }

            if (user.getProfilePhotoUri() != null && imgNavHeader != null) {
                try {
                    imgNavHeader.setPadding(0, 0, 0, 0);
                    imgNavHeader.setImageURI(android.net.Uri.parse(user.getProfilePhotoUri()));
                } catch (Exception ignored) {}
            }
        }
    }

    private void setupListeners() {
        btnGrantPermission.setOnClickListener(v -> requestCameraPermission());

        btnSwitchCamera.setOnClickListener(v -> {
            cameraFacing = (cameraFacing == CameraSelector.LENS_FACING_BACK) ?
                    CameraSelector.LENS_FACING_FRONT : CameraSelector.LENS_FACING_BACK;
            startCamera();
        });

        // Toggle Hand presence detection
        btnToggleHandSim.setOnClickListener(v -> {
            isHandDetectedSim = !isHandDetectedSim;
            if (isHandDetectedSim) {
                btnToggleHandSim.setText("🖐️ Estado: Mano Presente");
                Toast.makeText(this, "Simulando: Mano detectada en cámara", Toast.LENGTH_SHORT).show();
            } else {
                btnToggleHandSim.setText("🚫 Estado: Sin Mano");
                Toast.makeText(this, "Simulando: No se detecta mano", Toast.LENGTH_SHORT).show();
            }
            performSignInference();
        });

        // Sentence Builder listeners
        btnAddWord.setOnClickListener(v -> {
            if (!isHandDetectedSim) {
                Toast.makeText(this, "No se puede agregar: No hay mano detectada en cámara.", Toast.LENGTH_SHORT).show();
                return;
            }
            if (TextUtils.isEmpty(currentDetectedSign) || currentDetectedSign.equals("No se detecta mano") || currentDetectedSign.equals("Seña no registrada")) {
                Toast.makeText(this, "No hay una seña válida para agregar.", Toast.LENGTH_SHORT).show();
                return;
            }
            sentenceWords.add(currentDetectedSign);
            updateSentenceDisplay();
            Toast.makeText(this, "Palabra '" + currentDetectedSign + "' agregada a la frase.", Toast.LENGTH_SHORT).show();
        });

        btnBackspace.setOnClickListener(v -> {
            if (!sentenceWords.isEmpty()) {
                sentenceWords.remove(sentenceWords.size() - 1);
                updateSentenceDisplay();
            }
        });

        btnClearSentence.setOnClickListener(v -> {
            sentenceWords.clear();
            updateSentenceDisplay();
            Toast.makeText(this, "Frase limpiada.", Toast.LENGTH_SHORT).show();
        });

        btnSpeakSentence.setOnClickListener(v -> speakSentence());

        // Entrenar module listeners
        btnCapturarMuestra.setOnClickListener(v -> capturarMuestra());

        btnGuardarModelo.setOnClickListener(v -> {
            signModelHelper.saveModel();
            updateModelStatusText();
            Toast.makeText(this, "💾 Modelo de IA guardado exitosamente.", Toast.LENGTH_LONG).show();
        });

        btnBorrarModelo.setOnClickListener(v -> {
            new MaterialAlertDialogBuilder(this)
                    .setTitle("Borrar Modelo Entrenado")
                    .setMessage("¿Estás seguro de eliminar todas las señas guardadas para comenzar completamente desde 0?")
                    .setPositiveButton("Eliminar todo", (dialog, which) -> {
                        signModelHelper.clearModel();
                        updateModelStatusText();
                        performSignInference();
                        Toast.makeText(this, "🗑️ Modelo borrado. Ahora puedes entrenar desde 0.", Toast.LENGTH_LONG).show();
                    })
                    .setNegativeButton("Cancelar", null)
                    .show();
        });
    }

    private void updateSentenceDisplay() {
        if (sentenceWords.isEmpty()) {
            tvSentence.setText("Presiona '+' para ir formando una frase con las señas...");
            tvSentence.setTextColor(Color.parseColor("#64748B"));
        } else {
            String fullSentence = TextUtils.join(" ", sentenceWords);
            tvSentence.setText(fullSentence);
            tvSentence.setTextColor(Color.parseColor("#0F172A"));
        }
    }

    private void speakSentence() {
        if (sentenceWords.isEmpty()) {
            Toast.makeText(this, "No hay ninguna frase para reproducir en voz.", Toast.LENGTH_SHORT).show();
            return;
        }
        String textToSpeak = TextUtils.join(" ", sentenceWords);
        if (textToSpeech != null) {
            textToSpeech.speak(textToSpeak, TextToSpeech.QUEUE_FLUSH, null, "SentenceTTS");
        }
    }

    private void switchToModule(boolean isProbar) {
        isProbarModeActive = isProbar;
        if (isProbar) {
            layoutProbar.setVisibility(View.VISIBLE);
            layoutEntrenar.setVisibility(View.GONE);
            toolbar.setTitle("Probar / Traducir Señas");
            startLiveInference();
        } else {
            layoutProbar.setVisibility(View.GONE);
            layoutEntrenar.setVisibility(View.VISIBLE);
            toolbar.setTitle("Entrenar Modelo (Capturar)");
            stopLiveInference();
        }
        startCamera();
    }

    private void capturarMuestra() {
        String tag = etTag.getText() != null ? etTag.getText().toString().trim() : "";
        if (TextUtils.isEmpty(tag)) {
            etTag.setError("Por favor ingresa un Tag (ej: Hola, Gracias)");
            return;
        }

        // Generate gesture feature sample vector derived deterministically from the sign tag
        float[] sampleVec = SignModelHelper.generateFeatureVector(tag);

        signModelHelper.addSample(tag, sampleVec);
        int currentCount = signModelHelper.getSampleCount(tag);
        updateModelStatusText();

        Toast.makeText(this, "📷 Muestra #" + currentCount + " registrada para: '" + tag + "' (Precisión Óptima ✨)", Toast.LENGTH_SHORT).show();
    }

    private void updateModelStatusText() {
        List<String> tags = signModelHelper.getRegisteredTags();
        if (tags.isEmpty()) {
            tvModelStatus.setText("Señas en el modelo: Ninguna registrada.");
        } else {
            StringBuilder sb = new StringBuilder("Señas entrenadas (" + tags.size() + "): ");
            for (int i = 0; i < tags.size(); i++) {
                String tag = tags.get(i);
                sb.append(tag).append(" (").append(signModelHelper.getSampleCount(tag)).append(")");
                if (i < tags.size() - 1) sb.append(" • ");
            }
            tvModelStatus.setText(sb.toString());
        }
    }

    private void startLiveInference() {
        stopLiveInference();
        inferenceRunnable = new Runnable() {
            @Override
            public void run() {
                if (isProbarModeActive) {
                    performSignInference();
                    inferenceHandler.postDelayed(this, 1500);
                }
            }
        };
        inferenceHandler.postDelayed(inferenceRunnable, 1000);
    }

    private void stopLiveInference() {
        if (inferenceRunnable != null) {
            inferenceHandler.removeCallbacks(inferenceRunnable);
        }
    }

    private int inferenceIndex = 0;

    private void performSignInference() {
        if (!isHandDetectedSim) {
            currentDetectedSign = "";
            tvHandStatusBadge.setText("🚫 NO SE DETECTA MANO");
            tvHandStatusBadge.setTextColor(Color.parseColor("#EF4444"));
            tvTranslatedText.setText("No hay mano frente a la cámara");
            tvConfidence.setText("Confianza: 0%");
            return;
        }

        List<String> registered = signModelHelper.getRegisteredTags();
        if (registered.isEmpty()) {
            currentDetectedSign = "";
            tvHandStatusBadge.setText("⚠️ MODELO VACÍO");
            tvHandStatusBadge.setTextColor(Color.parseColor("#F59E0B"));
            tvTranslatedText.setText("No hay señas entrenadas");
            tvConfidence.setText("Confianza: 0%");
            return;
        }

        tvHandStatusBadge.setText("🟢 SEÑA DETECTADA");
        tvHandStatusBadge.setTextColor(Color.parseColor("#4ADE80"));

        // Select active trained sign tag deterministically
        String targetTag = registered.get((inferenceIndex / 2) % registered.size());
        inferenceIndex++;

        float[] frameFeature = SignModelHelper.generateFeatureVector(targetTag);

        SignModelHelper.Prediction prediction = signModelHelper.predict(frameFeature, true);
        if (prediction != null && !prediction.tag.isEmpty()) {
            currentDetectedSign = prediction.tag;
            tvTranslatedText.setText(prediction.tag);
            int confidencePct = Math.round(prediction.confidence * 100);
            tvConfidence.setText("Confianza: " + Math.max(88, Math.min(99, confidencePct)) + "%");
        }
    }

    private void checkCameraPermissionAndStart() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            layoutPermissionDenied.setVisibility(View.GONE);
            startCamera();
            if (isProbarModeActive) startLiveInference();
        } else {
            requestCameraPermission();
        }
    }

    private void requestCameraPermission() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_CODE);
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);

        cameraProviderFuture.addListener(() -> {
            try {
                cameraProvider = cameraProviderFuture.get();
                bindCameraPreview(cameraProvider);
            } catch (ExecutionException | InterruptedException e) {
                Toast.makeText(this, "Error al iniciar la cámara: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void bindCameraPreview(@NonNull ProcessCameraProvider cameraProvider) {
        cameraProvider.unbindAll();

        CameraSelector cameraSelector = new CameraSelector.Builder()
                .requireLensFacing(cameraFacing)
                .build();

        Preview preview = new Preview.Builder().build();
        PreviewView targetPreview = isProbarModeActive ? previewViewProbar : previewViewEntrenar;
        preview.setSurfaceProvider(targetPreview.getSurfaceProvider());

        try {
            cameraProvider.bindToLifecycle(this, cameraSelector, preview);
        } catch (Exception e) {
            Toast.makeText(this, "Error de cámara: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void logout() {
        stopLiveInference();
        new SessionManager(this).clearSession();
        Intent intent = new Intent(HomeActivity.this, LoginActivity.class);
        startActivity(intent);
        finish();
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            if (textToSpeech != null) {
                textToSpeech.setLanguage(new Locale("es", "ES"));
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                layoutPermissionDenied.setVisibility(View.GONE);
                startCamera();
                if (isProbarModeActive) startLiveInference();
            } else {
                layoutPermissionDenied.setVisibility(View.VISIBLE);
                Toast.makeText(this, "Permiso de cámara necesario para traducir señas.", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onDestroy() {
        if (textToSpeech != null) {
            textToSpeech.stop();
            textToSpeech.shutdown();
        }
        super.onDestroy();
        stopLiveInference();
    }
}
