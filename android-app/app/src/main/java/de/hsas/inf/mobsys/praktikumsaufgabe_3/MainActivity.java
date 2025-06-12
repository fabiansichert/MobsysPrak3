package de.hsas.inf.mobsys.praktikumsaufgabe_2;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Collections;

public class MainActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private EditText noteInput, emailInput, passwordInput;
    private Button saveBtn, loginBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        emailInput = findViewById(R.id.emailEditText);
        passwordInput = findViewById(R.id.passwordEditText);
        noteInput = findViewById(R.id.noteText);
        saveBtn = findViewById(R.id.saveButton);
        loginBtn = findViewById(R.id.loginButton);

        loginBtn.setOnClickListener(v -> {
            String email = emailInput.getText().toString().trim();
            String password = passwordInput.getText().toString().trim();

            auth.signInWithEmailAndPassword(email, password)
                    .addOnSuccessListener(result -> {
                        Toast.makeText(this, "Login erfolgreich", Toast.LENGTH_SHORT).show();
                        enableNoteSaving();
                        loadNote();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Login fehlgeschlagen: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        });

        saveBtn.setOnClickListener(v -> {
            FirebaseUser user = auth.getCurrentUser();
            if (user != null) {
                String note = noteInput.getText().toString();
                db.collection("notes").document(user.getUid())
                        .set(Collections.singletonMap("text", note))
                        .addOnSuccessListener(aVoid -> Toast.makeText(this, "Gespeichert!", Toast.LENGTH_SHORT).show())
                        .addOnFailureListener(e -> Toast.makeText(this, "Fehler beim Speichern", Toast.LENGTH_SHORT).show());
            } else {
                Toast.makeText(this, "Bitte zuerst einloggen!", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void enableNoteSaving() {
        saveBtn.setEnabled(true);
        noteInput.setEnabled(true);
    }

    private void loadNote() {
        FirebaseUser user = auth.getCurrentUser();
        if (user != null) {
            DocumentReference noteRef = db.collection("notes").document(user.getUid());
            noteRef.get()
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String note = documentSnapshot.getString("text");
                            noteInput.setText(note);
                        } else {
                            Toast.makeText(this, "Keine Notiz gefunden", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Fehler beim Laden der Notiz: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser != null) {
            enableNoteSaving();
            loadNote();
        } else {
            saveBtn.setEnabled(false);
            noteInput.setEnabled(false);
        }
    }
}
