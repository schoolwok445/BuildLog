package com.katza.myfirestoreapp;

import android.content.Intent;
import android.os.Bundle;
import android.graphics.Color;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.EditText;
import androidx.appcompat.app.AlertDialog;
import android.widget.TextView;
import androidx.activity.EdgeToEdge;
import androidx.activity.SystemBarStyle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.appcompat.widget.Toolbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;

public class ProjectsActivity extends AppCompatActivity {
    private FirebaseAuth auth;
    private ProjectDao projectDao;
    private LinearLayout layoutProjects;
    private TextView tvMessage;
    private Button btnFilter;
    private String filter = "All";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_projects);
        auth = FirebaseAuth.getInstance();
        projectDao = new ProjectDao();
        layoutProjects = findViewById(R.id.projects);
        tvMessage = findViewById(R.id.message);
        btnFilter = findViewById(R.id.filter);
        Button btnAdd = findViewById(R.id.add);
        Button btnRetry = findViewById(R.id.retry);
        Toolbar toolbar = findViewById(R.id.toolbar);
        EdgeToEdge.enable(this, SystemBarStyle.dark(Color.TRANSPARENT), SystemBarStyle.dark(Color.TRANSPARENT));
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.root), (view, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });
        btnFilter.setText(filter);
        btnFilter.setOnClickListener(view -> showFilter());
        btnAdd.setOnClickListener(view -> startActivity(new Intent(this, ProjectEditorActivity.class)));
        btnRetry.setOnClickListener(view -> loadProjects());
        toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.sign_out) {
                auth.signOut();
                showLogin();
                return true;
            }
            return false;
        });
    }

    private void showFilter() {
        EditText etFilter = new EditText(this);
        etFilter.setHint("Status (Planning, Wip, Finished)");
        if (!filter.equals("All")) {
            etFilter.setText(filter);
        }
        new AlertDialog.Builder(this)
                .setTitle("Filter by status")
                .setView(etFilter)
                .setPositiveButton("Apply", (dialog, which) -> {
                    filter = etFilter.getText().toString().trim();
                    if (filter.isEmpty()) {
                        filter = "All";
                    }
                    btnFilter.setText(filter);
                    loadProjects();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void loadProjects() {
        tvMessage.setText("Loading…");
        tvMessage.setVisibility(View.VISIBLE);
        projectDao.listenToProjects(auth.getCurrentUser().getUid(), filter, (snapshot, error) -> {
            if (error != null) {
                tvMessage.setText(error.getMessage());
                tvMessage.setVisibility(View.VISIBLE);
                return;
            }
            if (snapshot == null) {
                return;
            }
            layoutProjects.removeAllViews();
            for (DocumentSnapshot document : snapshot.getDocuments()) {
                Project project = document.toObject(Project.class);
                if (project == null) {
                    continue;
                }
                View projectView = getLayoutInflater().inflate(R.layout.project_item, layoutProjects, false);
                TextView tvName = projectView.findViewById(R.id.name);
                TextView tvStatus = projectView.findViewById(R.id.status);
                TextView tvDescription = projectView.findViewById(R.id.description);
                TextView tvCategory = projectView.findViewById(R.id.meta);
                tvName.setText(project.getName());
                tvStatus.setText(project.getStatus());
                tvDescription.setText(project.getDescription());
                tvCategory.setText(project.getCategory());
                projectView.setOnClickListener(view -> {
                    Intent intent = new Intent(ProjectsActivity.this, ProjectActivity.class);
                    intent.putExtra("projectId", document.getId());
                    startActivity(intent);
                });
                layoutProjects.addView(projectView);
            }
            if (snapshot.isEmpty()) {
                tvMessage.setText("no projects with this status");
            } else {
                tvMessage.setVisibility(View.GONE);
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (auth.getCurrentUser() == null) {
            showLogin();
            return;
        }
        loadProjects();
    }

    @Override
    protected void onStop() {
        projectDao.stopListening();
        super.onStop();
    }

    private void showLogin() {
        Intent intent = new Intent(this, AuthActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

}
