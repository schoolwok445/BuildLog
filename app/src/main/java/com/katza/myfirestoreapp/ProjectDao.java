package com.katza.myfirestoreapp;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.Source;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ProjectDao {
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private ListenerRegistration listenerRegistration;

    public void get(String id, OnSuccessListener<DocumentSnapshot> onSuccess, OnFailureListener onFailure) {
        db.collection("projects").document(id).get()
                .addOnSuccessListener(onSuccess)
                .addOnFailureListener(onFailure);
    }

    public void listenToProjects(String uid, String status, EventListener<QuerySnapshot> listener) {
        stopListening();
        Query query = db.collection("projects").whereEqualTo("ownerId", uid);
        if (!status.equals("All")) {
            query = query.whereEqualTo("status", status);
        }
        listenerRegistration = query.addSnapshotListener(listener);
    }

    public void listenToProject(String id, EventListener<DocumentSnapshot> listener) {
        stopListening();
        listenerRegistration = db.collection("projects").document(id).addSnapshotListener(listener);
    }

    public void stopListening() {
        if (listenerRegistration != null) {
            listenerRegistration.remove();
            listenerRegistration = null;
        }
    }

    public void save(String id, String uid, Project project, OnSuccessListener<Void> onSuccess, OnFailureListener onFailure) {
        Map<String, Object> projectRecord = new HashMap<>();
        projectRecord.put("name", project.getName());
        projectRecord.put("description", project.getDescription());
        projectRecord.put("category", project.getCategory());
        projectRecord.put("status", project.getStatus());
        projectRecord.put("nextStep", project.getNextStep());
        projectRecord.put("repositoryUrl", project.getRepositoryUrl());
        projectRecord.put("updatedAt", FieldValue.serverTimestamp());
        if (id == null) {
            projectRecord.put("ownerId", uid);
            projectRecord.put("journal", "");
            projectRecord.put("createdAt", FieldValue.serverTimestamp());
            db.collection("projects").document().set(projectRecord)
                    .addOnSuccessListener(onSuccess)
                    .addOnFailureListener(onFailure);
        } else {
            db.collection("projects").document(id).update(projectRecord)
                    .addOnSuccessListener(onSuccess)
                    .addOnFailureListener(onFailure);
        }
    }

    public void updateJournal(String id, String journal, OnSuccessListener<Void> onSuccess, OnFailureListener onFailure) {
        Map<String, Object> journalRecord = new HashMap<>();
        journalRecord.put("journal", journal);
        journalRecord.put("updatedAt", FieldValue.serverTimestamp());
        db.collection("projects").document(id).update(journalRecord)
                .addOnSuccessListener(onSuccess)
                .addOnFailureListener(onFailure);
    }

    public void delete(String id, OnSuccessListener<Void> onSuccess, OnFailureListener onFailure) {
        DocumentReference project = db.collection("projects").document(id);
        project.collection("images").get(Source.SERVER)
                .addOnSuccessListener(snapshot -> {
                    ArrayList<DocumentSnapshot> images = new ArrayList<>(snapshot.getDocuments());
                    deletePhotos(project, images, 0, onSuccess, onFailure);
                })
                .addOnFailureListener(onFailure);
    }

    private void deletePhotos(DocumentReference project, ArrayList<DocumentSnapshot> images, int index,
                              OnSuccessListener<Void> onSuccess, OnFailureListener onFailure) {
        if (index == images.size()) {
            project.delete().addOnSuccessListener(onSuccess).addOnFailureListener(onFailure);
            return;
        }
        images.get(index).getReference().delete()
                .addOnSuccessListener(unused -> deletePhotos(project, images, index + 1, onSuccess, onFailure))
                .addOnFailureListener(onFailure);
    }
}
