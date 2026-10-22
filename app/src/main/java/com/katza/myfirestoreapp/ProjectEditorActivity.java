package com.katza.myfirestoreapp;

import android.content.Intent;
import android.os.Bundle;
import android.graphics.Color;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import androidx.activity.EdgeToEdge;
import androidx.activity.SystemBarStyle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.google.firebase.auth.FirebaseAuth;

public class ProjectEditorActivity extends AppCompatActivity {
    private FirebaseAuth auth;
    private EditText etCategory;
    private EditText etDescription;
    private TextView tvMessage;
    private EditText etName;
    private EditText etNextStep;
    private EditText etRepository;
    private Button btnRetry;
    private Button btnSave;
    private EditText etStatus;
    private TextView tvTitle;
    private String projectId;
    private boolean loaded;
    private boolean saving;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        auth = FirebaseAuth.getInstance();
        setContentView(R.layout.activity_project_editor);
        etCategory = findViewById(R.id.category);
        etDescription = findViewById(R.id.description);
        tvMessage = findViewById(R.id.message);
        etName = findViewById(R.id.name);
        etNextStep = findViewById(R.id.nextStep);
        etRepository = findViewById(R.id.repository);
        btnRetry = findViewById(R.id.retry);
        btnSave = findViewById(R.id.save);
        etStatus = findViewById(R.id.status);
        tvTitle = findViewById(R.id.toolbar);
        EdgeToEdge.enable(this, SystemBarStyle.dark(Color.TRANSPARENT), SystemBarStyle.dark(Color.TRANSPARENT));
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.root), (view, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.ime());
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        projectId = getIntent().getStringExtra("projectId");
        if (projectId == null) {
            tvTitle.setText("Add project");
            loaded = true;
        } else {
            tvTitle.setText("Edit project");
        }
        btnSave.setOnClickListener(v -> save());
        btnRetry.setOnClickListener(v -> load());
        if (!loaded) {
            load();
        }
    }
    private void load() {
        btnSave.setEnabled(false);
        btnRetry.setVisibility(View.GONE);
        tvMessage.setVisibility(View.GONE);
        new ProjectDao().get(projectId, document -> {
            btnSave.setEnabled(true);
            if (!document.exists()) {
                finish();
                return;
            }
            Project project = document.toObject(Project.class);
            if (project == null) {
                finish();
                return;
            }
            etName.setText(project.getName());
            etDescription.setText(project.getDescription());
            etNextStep.setText(project.getNextStep());
            etRepository.setText(project.getRepositoryUrl());
            etCategory.setText(project.getCategory());
            etStatus.setText(project.getStatus());
            loaded = true;
        }, error -> {
            error(error.getMessage());
            btnRetry.setVisibility(View.VISIBLE);
            btnSave.setEnabled(false);
        });
    }

    private void save() {
        if (auth.getCurrentUser() == null || !loaded || saving) {
            return;
        }
        String name = etName.getText().toString().trim();
        if (name.isEmpty()) {
            etName.setError("Give your project a name.");
            return;
        }
        Project project = new Project();
        project.setName(name);
        project.setDescription(etDescription.getText().toString().trim());
        project.setCategory(etCategory.getText().toString().trim());
        project.setStatus(etStatus.getText().toString().trim());
        project.setNextStep(etNextStep.getText().toString().trim());
        project.setRepositoryUrl(etRepository.getText().toString().trim());
        saving = true;
        btnSave.setEnabled(false);
        new ProjectDao().save(projectId, auth.getCurrentUser().getUid(), project, unused -> {
            saving = false;
            finish();
        }, error -> {
            saving = false;
            btnSave.setEnabled(true);
            error(error.getMessage());
        });
    }
    private void error(String message) {
        tvMessage.setText(message);
        tvMessage.setVisibility(View.VISIBLE);
    }

    private void showLogin() {
        Intent intent = new Intent(this, AuthActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (auth.getCurrentUser() == null) {
            showLogin();
        }
    }
}
