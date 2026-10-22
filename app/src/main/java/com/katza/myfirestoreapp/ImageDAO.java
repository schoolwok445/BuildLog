package com.katza.myfirestoreapp;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.Source;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;
import java.util.HashMap;
import java.util.Map;

public class ImageDAO {
    private CollectionReference images;
    private ListenerRegistration listenerRegistration;

    public ImageDAO(String projectId) {
        images = FirebaseFirestore.getInstance().collection("projects")
                .document(projectId).collection("images");
    }

    public void save(String storage, String data, OnSuccessListener<DocumentReference> onSuccess, OnFailureListener onFailure) {
        Map<String, Object> imageRecord = new HashMap<>();
        imageRecord.put("storage", storage);
        imageRecord.put("data", data);
        imageRecord.put("createdAt", FieldValue.serverTimestamp());
        images.add(imageRecord)
                .addOnSuccessListener(onSuccess)
                .addOnFailureListener(onFailure);
    }

    public void getImages(OnSuccessListener<QuerySnapshot> onSuccess, OnFailureListener onFailure) {
        images.get(Source.SERVER)
                .addOnSuccessListener(onSuccess)
                .addOnFailureListener(onFailure);
    }

    public void listenToImages(EventListener<QuerySnapshot> listener) {
        stopListening();
        listenerRegistration = images.addSnapshotListener(listener);
    }

    public void delete(String imageId, OnSuccessListener<Void> onSuccess, OnFailureListener onFailure) {
        images.document(imageId).delete()
                .addOnSuccessListener(onSuccess)
                .addOnFailureListener(onFailure);
    }

    public void stopListening() {
        if (listenerRegistration != null) {
            listenerRegistration.remove();
            listenerRegistration = null;
        }
    }
}
