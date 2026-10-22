package com.katza.myfirestoreapp;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.graphics.Color;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.EdgeToEdge;
import androidx.activity.SystemBarStyle;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.appcompat.widget.Toolbar;
import androidx.appcompat.app.AlertDialog;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import java.io.IOException;
import java.util.ArrayList;

public class ProjectActivity extends AppCompatActivity {
    private ProjectDao projectDao;
    private ImageDAO imageDAO;
    private String projectId;
    private String savedPhotoId;
    private TextView tvName, tvDetails, tvDescription, tvNextStep, tvPhoto, tvMessage;
    private EditText etJournal;
    private TextView tvRepository;
    private String journalFromDatabase = "";
    private ImageView ivImage;
    private Button btnSaveJournal, btnSavePhoto, btnChoose, btnPrevious, btnNext, btnDeletePhoto, btnClear, btnRetry;
    private RadioGroup storageGroup;
    private ArrayList<DocumentSnapshot> photos = new ArrayList<>();
    private int photoIndex = 0;
    private Uri selectedUri, cameraUri;
    private boolean journalLoaded, saving;
    private ActivityResultLauncher<String[]> galleryLauncher;
    private ActivityResultLauncher<Uri> cameraLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_project);
        projectId = getIntent().getStringExtra("projectId");
        if (projectId == null) {
            finish();
            return;
        }
        projectDao = new ProjectDao();
        imageDAO = new ImageDAO(projectId);
        tvName = findViewById(R.id.name);
        tvDetails = findViewById(R.id.meta);
        tvDescription = findViewById(R.id.description);
        tvNextStep = findViewById(R.id.nextStep);
        tvRepository = findViewById(R.id.repositoryUrl);
        tvPhoto = findViewById(R.id.photoStatus);
        tvMessage = findViewById(R.id.message);
        etJournal = findViewById(R.id.journalText);
        ivImage = findViewById(R.id.image);
        btnSaveJournal = findViewById(R.id.saveJournal);
        btnSavePhoto = findViewById(R.id.savePhoto);
        btnChoose = findViewById(R.id.choosePhoto);
        btnPrevious = findViewById(R.id.previousPhoto);
        btnNext = findViewById(R.id.nextPhoto);
        btnDeletePhoto = findViewById(R.id.deletePhoto);
        btnClear = findViewById(R.id.clearPhoto);
        storageGroup = findViewById(R.id.storageGroup);
        btnRetry = findViewById(R.id.retry);
        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationIcon(R.drawable.ic_back);
        toolbar.setNavigationContentDescription("Back");
        toolbar.setNavigationOnClickListener(view -> finish());
        toolbar.setOnMenuItemClickListener(item -> {
            if (saving) {
                return true;
            }
            if (item.getItemId() == R.id.edit) {
                Intent intent = new Intent(this, ProjectEditorActivity.class);
                intent.putExtra("projectId", projectId);
                startActivity(intent);
                return true;
            }
            if (item.getItemId() == R.id.delete) {
                confirmDeleteProject();
                return true;
            }
            return false;
        });
        EdgeToEdge.enable(this, SystemBarStyle.dark(Color.TRANSPARENT), SystemBarStyle.dark(Color.TRANSPARENT));
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.root), (view, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.ime());
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });
        btnSaveJournal.setEnabled(journalLoaded);
        registerImageLaunchers();
        btnSaveJournal.setOnClickListener(view -> saveJournal());
        btnChoose.setOnClickListener(view -> choosePhoto());
        btnSavePhoto.setOnClickListener(view -> savePhoto());
        btnClear.setOnClickListener(view -> {
            selectedUri = null;
            tvMessage.setVisibility(View.GONE);
            showPhoto();
        });
        btnPrevious.setOnClickListener(view -> {
            photoIndex--;
            showPhoto();
        });
        btnNext.setOnClickListener(view -> {
            photoIndex++;
            showPhoto();
        });
        btnDeletePhoto.setOnClickListener(view -> deletePhoto());
        btnRetry.setOnClickListener(view -> refreshPhotos());
        showPhoto();
    }

    private void registerImageLaunchers() {
        galleryLauncher = registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> {
            if (uri == null) {
                return;
            }
            try {
                getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
            } catch (RuntimeException ignored) {

            }
            selectedUri = uri;
            showPhoto();
        });
        cameraLauncher = registerForActivityResult(new ActivityResultContracts.TakePicture(), success -> {
            if (cameraUri == null) {
                showMessage("Please choose or take the photo again.");
                return;
            }
            if (success) {
                selectedUri = cameraUri;
            } else {
                ImageUtils.discardPhoto(this, cameraUri);
            }
            cameraUri = null;
            showPhoto();
        });
    }

    private void loadProject() {
        tvMessage.setVisibility(View.GONE);
        projectDao.listenToProject(projectId, (document, error) -> {
            if (error != null) {
                showError(error);
                return;
            }
            if (document == null) {
                return;
            }
            if (!document.exists()) {
                finish();
                return;
            }
            Project project = document.toObject(Project.class);
            if (project == null) {
                return;
            }
            tvName.setText(project.getName());
            String details = project.getCategory() + " | " + project.getStatus();
            tvDetails.setText(details);
            tvDescription.setText(project.getDescription());
            tvNextStep.setText(project.getNextStep());
            tvRepository.setText(project.getRepositoryUrl());

            if (!journalLoaded || etJournal.getText().toString().equals(journalFromDatabase)) {
                etJournal.setText(project.getJournal());
            }
            journalFromDatabase = project.getJournal();
            journalLoaded = true;
            btnSaveJournal.setEnabled(!saving);
        });
        imageDAO.listenToImages((snapshot, error) -> {
            if (error != null) {
                showError(error);
                return;
            }
            if (snapshot == null) {
                return;
            }
            displayPhotos(snapshot);
        });
    }

    private void displayPhotos(QuerySnapshot snapshot) {
        photos.clear();
        photos.addAll(snapshot.getDocuments());
        if (photoIndex >= photos.size()) {
            photoIndex = Math.max(0, photos.size() - 1);
        }
        showPhoto();
    }

    private void refreshPhotos() {
        loadProject();
        imageDAO.getImages(snapshot -> {
            displayPhotos(snapshot);
            showMessage("Photos refreshed from Firestore.");
        }, error -> showError(error));
    }

    private void saveJournal() {
        if (saving || !journalLoaded) {
            return;
        }
        setSaving(true);
        btnSaveJournal.setText("Saving...");
        showMessage("Saving journal...");
        projectDao.updateJournal(projectId, etJournal.getText().toString(), unused -> {
            setSaving(false);
            showMessage("Journal saved.");
            Toast.makeText(this, "Journal saved", Toast.LENGTH_SHORT).show();
        }, error -> {
            setSaving(false);
            showError(error);
        });
    }

    private void choosePhoto() {
        PopupMenu menu = new PopupMenu(this, btnChoose);
        menu.getMenu().add("Choose from gallery");
        menu.getMenu().add("Take a photo");
        menu.setOnMenuItemClickListener(item -> {
            if (item.getTitle().equals("Choose from gallery")) {
                galleryLauncher.launch(new String[]{"image/*"});
            } else {
                try {
                    cameraUri = ImageUtils.createCameraUri(this);
                    cameraLauncher.launch(cameraUri);
                } catch (Exception error) {
                    ImageUtils.discardPhoto(this, cameraUri);
                    cameraUri = null;
                    showError(error);
                }
            }
            return true;
        });
        menu.show();
    }

    private void showPhoto() {
        if (savedPhotoId != null) {
            for (int i = 0; i < photos.size(); i++) {
                if (photos.get(i).getId().equals(savedPhotoId)) {
                    photoIndex = i;
                    savedPhotoId = null;
                    break;
                }
            }
        }
        btnSavePhoto.setEnabled(selectedUri != null && !saving);
        btnClear.setEnabled(selectedUri != null && !saving);
        btnPrevious.setEnabled(selectedUri == null && photoIndex > 0 && !saving);
        btnNext.setEnabled(selectedUri == null && photoIndex < photos.size() - 1 && !saving);
        btnDeletePhoto.setEnabled(selectedUri == null && !photos.isEmpty() && !saving);
        String storage = "LOCAL_URI";
        String data;
        if (selectedUri != null) {
            data = selectedUri.toString();
            tvPhoto.setText("New photo selected");
        } else if (photos.isEmpty()) {
            tvPhoto.setText("No photos saved for this project.");
            ivImage.setImageDrawable(null);
            return;
        } else {
            DocumentSnapshot photo = photos.get(photoIndex);
            storage = photo.getString("storage");
            data = photo.getString("data");
            tvPhoto.setText("Photo " + (photoIndex + 1) + " of " + photos.size());
        }
        try {
            Bitmap bitmap;
            if ("LOCAL_URI".equals(storage)) {
                bitmap = ImageUtils.loadBitmap(this, Uri.parse(data));
            } else {
                bitmap = ImageUtils.convertBase64StringToBitmap(data);
            }
            ivImage.setImageBitmap(bitmap);
            if (bitmap == null) {
                tvPhoto.setText("This image is unavailable. Its local file may have been moved or deleted.");
            }
        } catch (Exception error) {
            ivImage.setImageDrawable(null);
            showError(error);
        }
    }

    private void savePhoto() {
        if (selectedUri == null || saving) {
            return;
        }
        String storage;
        if (storageGroup.getCheckedRadioButtonId() == R.id.localGallery) {
            storage = "LOCAL_URI";
        } else {
            storage = "BASE64";
        }
        setSaving(true);
        btnSavePhoto.setText("Saving...");
        showMessage("Saving photo...");
        try {
            String data;
            if (storage.equals("LOCAL_URI")) {
                Uri savedUri = ImageUtils.copyToGallery(this, selectedUri);
                data = savedUri.toString();
            } else {
                Bitmap bitmap = ImageUtils.loadBitmap(this, selectedUri);
                try {
                    data = ImageUtils.convertBitmapToString(bitmap);
                } finally {
                    bitmap.recycle();
                }
                if (data == null) {
                    throw new IOException("The image could not be compressed.");
                }
            }
            imageDAO.save(storage, data, document -> {
                selectedUri = null;
                savedPhotoId = document.getId();
                setSaving(false);
                showMessage("Photo saved to Firestore.");
                Toast.makeText(this, "Photo saved", Toast.LENGTH_SHORT).show();
            }, error -> {
                setSaving(false);
                showError(error);
            });
        } catch (Exception error) {
            setSaving(false);
            showError(error);
        }
    }

    private void deletePhoto() {
        if (photos.isEmpty() || saving || selectedUri != null) {
            return;
        }
        setSaving(true);
        btnDeletePhoto.setText("Deleting...");
        showMessage("Deleting photo...");
        imageDAO.delete(photos.get(photoIndex).getId(), unused -> {
            setSaving(false);
            showMessage("Photo deleted from Firestore.");
            Toast.makeText(this, "Photo deleted", Toast.LENGTH_SHORT).show();
        }, error -> {
            setSaving(false);
            showError(error);
        });
    }

    private void confirmDeleteProject() {
        new AlertDialog.Builder(this).setTitle("Delete this project?")
                .setMessage("This removes the project, its journal, and its saved photo records. Files in your device gallery stay there.").setNegativeButton("Cancel", null)
                .setPositiveButton("Delete", (dialog, which) -> {
                    setSaving(true);
                    projectDao.delete(projectId, unused -> finish(), error -> {
                        setSaving(false);
                        showError(error);
                    });
                }).show();
    }

    private void setSaving(boolean value) {
        saving = value;
        btnSaveJournal.setEnabled(!saving && journalLoaded);
        btnChoose.setEnabled(!saving);
        for (int i = 0; i < storageGroup.getChildCount(); i++) {
            storageGroup.getChildAt(i).setEnabled(!saving);
        }
        btnRetry.setEnabled(!saving);
        etJournal.setEnabled(!saving);
        if (!saving) {
            btnSavePhoto.setText("Save photo");
            btnSaveJournal.setText("Save journal");
            btnDeletePhoto.setText("Delete displayed photo");
        }
        showPhoto();
    }

    private void showError(Exception error) {
        Log.e("BuildLog", "Project operation failed", error);
        Toast.makeText(this, error.getMessage(), Toast.LENGTH_LONG).show();
        showMessage(error.getMessage());
    }

    private void showMessage(String message) {
        tvMessage.setText(message);
        tvMessage.setVisibility(View.VISIBLE);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (projectId == null) {
            return;
        }
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            Intent intent = new Intent(this, AuthActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return;
        }
        loadProject();
    }

    @Override
    protected void onStop() {
        if (projectDao != null) {
            projectDao.stopListening();
            imageDAO.stopListening();
        }
        super.onStop();
    }

}
