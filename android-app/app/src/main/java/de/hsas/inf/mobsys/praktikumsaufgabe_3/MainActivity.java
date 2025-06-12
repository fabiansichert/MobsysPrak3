package de.hsas.inf.mobsys.praktikumsaufgabe_3;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private EditText emailInput, passwordInput, messageInput;
    private Button loginBtn, sendBtn;
    private LinearLayout chatContainer;
    private TextView statusView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        FirebaseAuth.getInstance().signOut();

        // Firebase init
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Views
        emailInput = findViewById(R.id.emailEditText);
        passwordInput = findViewById(R.id.passwordEditText);
        messageInput = findViewById(R.id.editTextMessage);
        chatContainer = findViewById(R.id.chatContainer);
        loginBtn = findViewById(R.id.loginButton);
        sendBtn = findViewById(R.id.buttonSend);
        statusView = findViewById(R.id.textViewStatus);

        disableChat();

        loginBtn.setOnClickListener(v -> {
            String email = emailInput.getText().toString().trim();
            String password = passwordInput.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty()) {
                statusView.setText("Bitte E-Mail und Passwort eingeben.");
                return;
            }

            auth.signInWithEmailAndPassword(email, password)
                    .addOnSuccessListener(result -> {
                        Toast.makeText(this, "Login erfolgreich", Toast.LENGTH_SHORT).show();
                        statusView.setText("");
                        hideLoginFields();
                        enableChat();
                        loadMessages();
                    })
                    .addOnFailureListener(e -> {
                        statusView.setText("Login fehlgeschlagen: " + e.getMessage());
                        disableChat();
                    });
        });

        sendBtn.setOnClickListener(v -> {
            FirebaseUser user = auth.getCurrentUser();
            if (user == null) {
                Toast.makeText(this, "Nicht eingeloggt.", Toast.LENGTH_SHORT).show();
                disableChat();
                return;
            }

            String message = messageInput.getText().toString().trim();
            if (message.isEmpty()) {
                Toast.makeText(this, "Nachricht leer.", Toast.LENGTH_SHORT).show();
                return;
            }

            Map<String, Object> messageData = new HashMap<>();
            messageData.put("text", message);
            messageData.put("timestamp", Timestamp.now());
            messageData.put("sender", user.getEmail() != null ? user.getEmail() : user.getUid());

            db.collection("chats").document("global").collection("messages")
                    .add(messageData)
                    .addOnSuccessListener(doc -> {
                        messageInput.setText("");
                        loadMessages();
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "Senden fehlgeschlagen: " + e.getMessage(), Toast.LENGTH_LONG).show());
        });
    }

    private void loadMessages() {
        CollectionReference messagesRef = db.collection("chats")
                .document("global")
                .collection("messages");

        messagesRef.orderBy("timestamp")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    chatContainer.removeAllViews(); // Clear previous messages

                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        String sender = doc.getString("sender");
                        String text = doc.getString("text");

                        if (sender != null && text != null) {
                            TextView messageView = new TextView(this);
                            messageView.setText(sender + ":\n" + text);
                            messageView.setTextSize(16);
                            messageView.setTextColor(getResources().getColor(android.R.color.black));
                            messageView.setPadding(24, 16, 24, 16);
                            messageView.setBackgroundColor(0xFFE0E0E0);

                            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.MATCH_PARENT,
                                    LinearLayout.LayoutParams.WRAP_CONTENT
                            );
                            params.setMargins(0, 0, 0, 16); // spacing between messages
                            messageView.setLayoutParams(params);

                            chatContainer.addView(messageView);
                        }
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Fehler beim Laden: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void enableChat() {
        messageInput.setEnabled(true);
        sendBtn.setEnabled(true);
    }

    private void disableChat() {
        messageInput.setEnabled(false);
        sendBtn.setEnabled(false);
    }

    private void hideLoginFields() {
        emailInput.setVisibility(View.GONE);
        passwordInput.setVisibility(View.GONE);
        loginBtn.setVisibility(View.GONE);
    }

    @Override
    protected void onStart() {
        super.onStart();
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser != null) {
            hideLoginFields();
            enableChat();
            loadMessages();
        } else {
            disableChat();
        }
    }
}
