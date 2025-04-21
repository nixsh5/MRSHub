package com.example.mrshub;

import android.app.DownloadManager;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.OpenableColumns;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.firebase.ui.database.FirebaseRecyclerOptions;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MainActivity2landingdrop extends AppCompatActivity implements ResourceAdapter.ResourceClickListener {

    private static final int REQUEST_CODE_SELECT_FILE = 101;
    private Uri selectedFileUri;

    private BottomSheetDialog currentUploadDialog;
    private TextInputEditText currentEtFileName;

    private FirebaseStorage storage;
    private FirebaseDatabase database;
    private DatabaseReference resourcesRef;
    private FirebaseAuth auth;
    private RecyclerView recyclerView;
    private ResourceAdapter adapter;
    private BottomSheetDialog uploadDialog;
    private TextInputEditText etFileName, etDescription;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2landingdrop);

        // Set up the toolbar for menu
        Toolbar toolbar = findViewById(R.id.topAppBar);
        setSupportActionBar(toolbar);

        storage = FirebaseStorage.getInstance();
        database = FirebaseDatabase.getInstance();
        resourcesRef = database.getReference("resources");
        auth = FirebaseAuth.getInstance();

        // Check if user is logged in
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        setupRecyclerView();

        FloatingActionButton fabUpload = findViewById(R.id.fabUpload);
        fabUpload.setOnClickListener(v -> showUploadDialog());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_logout) {
            FirebaseAuth.getInstance().signOut();

            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void setupRecyclerView() {
        recyclerView = findViewById(R.id.recyclerViewResources);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        Query query = resourcesRef.orderByChild("uploadTimestamp");

        FirebaseRecyclerOptions<Resource> options = new FirebaseRecyclerOptions.Builder<Resource>()
                .setQuery(query, snapshot -> {
                    Resource resource = snapshot.getValue(Resource.class);
                    if (resource != null) {
                        resource.setFileId(snapshot.getKey());
                    }
                    return resource;
                })
                .build();

        adapter = new ResourceAdapter(options, this);
        recyclerView.setAdapter(adapter);

        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                FloatingActionButton fab = findViewById(R.id.fabUpload);
                if (dy > 10 && fab.isShown()) {
                    fab.hide();
                } else if (dy < -10 && !fab.isShown()) {
                    fab.show();
                }
                if (!recyclerView.canScrollVertically(-1)) {
                    fab.show();
                }
            }
        });
    }

    private void showUploadDialog() {
        selectedFileUri = null;

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_upload_file, null);
        BottomSheetDialog uploadDialog = new BottomSheetDialog(this);
        uploadDialog.setContentView(dialogView);

        TextInputEditText etFileName = dialogView.findViewById(R.id.etFileName);
        TextInputEditText etDescription = dialogView.findViewById(R.id.etDescription);
        MaterialButton btnSelectFile = dialogView.findViewById(R.id.btnSelectFile);
        MaterialButton btnUpload = dialogView.findViewById(R.id.btnUpload);

        btnSelectFile.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("*/*");
            startActivityForResult(intent, REQUEST_CODE_SELECT_FILE);
        });

        btnUpload.setOnClickListener(v -> {
            String fileName = etFileName.getText().toString().trim();
            String description = etDescription.getText().toString().trim();

            if (selectedFileUri == null) {
                Toast.makeText(this, "Please select a file first", Toast.LENGTH_SHORT).show();
                return;
            }
            if (fileName.isEmpty()) {
                etFileName.setError("File name is required");
                return;
            }

            uploadFile(selectedFileUri, fileName, description);
            uploadDialog.dismiss();
        });

        uploadDialog.show();

        this.currentUploadDialog = uploadDialog;
        this.currentEtFileName = etFileName;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_CODE_SELECT_FILE && resultCode == RESULT_OK && data != null) {
            selectedFileUri = data.getData();
            String fileName = getFileNameFromUri(selectedFileUri);

            if (currentEtFileName != null && fileName != null) {
                currentEtFileName.setText(fileName);
            }
        }
    }

    private String getFileNameFromUri(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (nameIndex >= 0) {
                        result = cursor.getString(nameIndex);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result;
    }

    private void uploadFile(Uri fileUri, String fileName, String description) {
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setTitle("Uploading...");
        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progressDialog.setMax(100);
        progressDialog.setCancelable(false);
        progressDialog.show();

        String fileExtension = getFileExtension(fileUri);
        String uniqueFileName = UUID.randomUUID().toString() + (fileExtension != null ? "." + fileExtension : "");
        StorageReference fileRef = storage.getReference("resources/" + uniqueFileName);

        fileRef.putFile(fileUri)
                .addOnSuccessListener(taskSnapshot -> fileRef.getDownloadUrl().addOnSuccessListener(uri -> {
                    Map<String, Object> resourceData = new HashMap<>();
                    resourceData.put("fileName", fileName);
                    resourceData.put("description", description);
                    resourceData.put("downloadUrl", uri.toString());
                    resourceData.put("uploadedBy", auth.getCurrentUser().getEmail());
                    resourceData.put("uploadTimestamp", System.currentTimeMillis() * -1);
                    resourceData.put("fileType", fileExtension != null ? fileExtension : "unknown");

                    resourcesRef.push().setValue(resourceData)
                            .addOnSuccessListener(aVoid -> {
                                progressDialog.dismiss();
                                Toast.makeText(MainActivity2landingdrop.this, "Upload successful", Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e -> {
                                progressDialog.dismiss();
                                Toast.makeText(MainActivity2landingdrop.this, "Failed to save metadata: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                }))
                .addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    Toast.makeText(this, "Upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                })
                .addOnProgressListener(snapshot -> {
                    double progress = (100.0 * snapshot.getBytesTransferred()) / snapshot.getTotalByteCount();
                    progressDialog.setProgress((int) progress);
                });
    }

    private String getFileExtension(Uri uri) {
        ContentResolver contentResolver = getContentResolver();
        MimeTypeMap mimeTypeMap = MimeTypeMap.getSingleton();
        String contentType = contentResolver.getType(uri);
        return contentType != null ? mimeTypeMap.getExtensionFromMimeType(contentType) : null;
    }

    @Override
    public void onDownloadClick(Resource resource) {
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(resource.getDownloadUrl()));
        request.setTitle(resource.getFileName());
        request.setDescription("Downloading file...");
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, resource.getFileName());

        DownloadManager downloadManager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
        downloadManager.enqueue(request);

        Toast.makeText(this, "Download started", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onItemClick(Resource resource) {
        String fileType = resource.getFileType();
        String url = resource.getDownloadUrl();

        if (fileType == null || url == null) {
            Toast.makeText(this, "Preview not available.", Toast.LENGTH_SHORT).show();
            return;
        }

        fileType = fileType.toLowerCase();

        if (fileType.equals("jpg") || fileType.equals("jpeg") || fileType.equals("png") || fileType.equals("gif")) {
            showImagePreviewDialog(url);
        } else if (fileType.equals("pdf")) {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(Uri.parse(url), "application/pdf");
            intent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
            try {
                startActivity(intent);
            } catch (Exception e) {
                Toast.makeText(this, "No PDF viewer found.", Toast.LENGTH_SHORT).show();
            }
        } else if (fileType.equals("docx") || fileType.equals("doc")) {
            openDocFileFromUrl(url, resource.getFileName(), fileType);
        } else {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(url));
            intent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
            try {
                startActivity(intent);
            } catch (Exception e) {
                Toast.makeText(this, "No app found to preview this file.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void showImagePreviewDialog(String imageUrl) {
        View view = getLayoutInflater().inflate(R.layout.dialog_preview_image, null);
        ImageView imageView = view.findViewById(R.id.imagePreview);

        Glide.with(this)
                .load(imageUrl)
                .placeholder(android.R.drawable.progress_indeterminate_horizontal)
                .error(android.R.drawable.stat_notify_error)
                .into(imageView);

        BottomSheetDialog dialog = new BottomSheetDialog(this);
        dialog.setContentView(view);
        dialog.show();
    }

    // DOC/DOCX preview as temp file in app cache using FileProvider
    private void openDocFileFromUrl(String url, String fileName, String fileType) {
        new Thread(() -> {
            try {
                // Download the file to cache
                URL fileUrl = new URL(url);
                HttpURLConnection connection = (HttpURLConnection) fileUrl.openConnection();
                connection.connect();
                InputStream input = connection.getInputStream();
                File tempFile = new File(getCacheDir(), fileName);
                FileOutputStream output = new FileOutputStream(tempFile);
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = input.read(buffer)) != -1) {
                    output.write(buffer, 0, bytesRead);
                }
                output.close();
                input.close();

                // Get content URI using FileProvider
                Uri contentUri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", tempFile);

                // Determine MIME type
                String mimeType = fileType.equals("docx") ?
                        "application/vnd.openxmlformats-officedocument.wordprocessingml.document" :
                        "application/msword";

                // Launch intent to open the file
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(contentUri, mimeType);
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_ACTIVITY_NO_HISTORY);

                runOnUiThread(() -> {
                    try {
                        startActivity(intent);
                    } catch (Exception e) {
                        Toast.makeText(this, "No DOC/DOCX viewer found. Please install Microsoft Word or Google Docs.", Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, "Failed to open file: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    // DELETE LOGIC: Only uploader can delete
    @Override
    public void onDeleteClick(Resource resource) {
        String currentUserEmail = FirebaseAuth.getInstance().getCurrentUser().getEmail();
        String uploaderEmail = resource.getUploadedBy();

        if (currentUserEmail != null && currentUserEmail.equals(uploaderEmail)) {
            deleteResource(resource);
        } else {
            Toast.makeText(this, "You can only delete files you uploaded.", Toast.LENGTH_SHORT).show();
        }
    }

    public void deleteResource(Resource resource) {
        String fileId = resource.getFileId();
        String downloadUrl = resource.getDownloadUrl();

        // 1. Delete metadata from Realtime Database
        DatabaseReference dbRef = FirebaseDatabase.getInstance().getReference("resources").child(fileId);
        dbRef.removeValue().addOnSuccessListener(aVoid -> {
            // 2. Delete file from Storage
            String uniqueFileName = extractFileNameFromUrl(downloadUrl);
            StorageReference fileRef = FirebaseStorage.getInstance().getReference("resources").child(uniqueFileName);
            fileRef.delete()
                    .addOnSuccessListener(aVoid2 ->
                            Toast.makeText(this, "File deleted", Toast.LENGTH_SHORT).show())
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "File deleted successfully" , Toast.LENGTH_SHORT).show());
        }).addOnFailureListener(e ->
                Toast.makeText(this, "Database delete failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    // Helper to extract file name from download URL
    private String extractFileNameFromUrl(String downloadUrl) {
        if (downloadUrl == null) return "";
        int start = downloadUrl.indexOf("/resources/") + "/resources/".length();
        int end = downloadUrl.indexOf("?", start);
        if (start >= 0 && end > start) {
            return downloadUrl.substring(start, end);
        } else if (start >= 0) {
            return downloadUrl.substring(start);
        }
        return "";
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (adapter != null) {
            adapter.startListening();
        }
    }

    @Override
    protected void onDestroy() {
        if (adapter != null) {
            adapter.stopListening();
        }
        super.onDestroy();
    }
}
