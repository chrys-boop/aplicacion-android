package metro.plascreem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.Map;

import metro.plascreem.databinding.ActivityInstructoresBinding;

public class Instructores extends AppCompatActivity {

    private ActivityInstructoresBinding binding;
    private DatabaseManager databaseManager;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityInstructoresBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbarInstructores);

        mAuth = FirebaseAuth.getInstance();
        databaseManager = new DatabaseManager(this);

        loadInstructorData();

        binding.bottomNavigationInstructores.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            int itemId = item.getItemId();
            if (itemId == R.id.navigation_profile) {
                selectedFragment = new EditInstructorProfileFragment();
            } else if (itemId == R.id.navigation_upload_docs) {
                selectedFragment = new UploadDocumentsFragment();
            } else if (itemId == R.id.navigation_upload_media) {
                selectedFragment = new UploadMediaFragment();
            } else if (itemId == R.id.navigation_calendar) {
                selectedFragment = new CalendarFragment();
            }

            if (selectedFragment != null) {
                replaceFragment(selectedFragment, true, R.id.fragment_container_instructores);
            }
            return true;
        });

        binding.fabSendMessageInstructores.setOnClickListener(v -> {
            replaceFragment(new SendMessageFragment(), true, R.id.fragment_container_instructores);
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_options_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.action_change_password) {
            replaceFragment(new ChangePasswordFragment(), true, R.id.fragment_container_instructores);
            return true;
        } else if (itemId == R.id.action_update_email) {
            replaceFragment(new UpdateEmailFragment(), true, R.id.fragment_container_instructores);
            return true;
        } else if (itemId == R.id.action_settings) {
            replaceFragment(new SettingsFragment(), true, R.id.fragment_container_instructores);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadInstructorData();
    }

    private void loadInstructorData() {
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            String userId = currentUser.getUid();
            databaseManager.getUserDataMap(userId, new DatabaseManager.UserDataMapListener() {
                @Override
                public void onDataReceived(Map<String, Object> userData) {
                    if (userData != null) {
                        // Data will be used by the profile fragment, nothing to do here.
                    }
                }

                @Override
                public void onDataCancelled(String message) {
                    Toast.makeText(Instructores.this, "Error al cargar datos: " + message, Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void replaceFragment(Fragment fragment, boolean addToBackStack, int containerId) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(containerId, fragment);
        if (addToBackStack) {
            transaction.addToBackStack(null);
        }
        transaction.commit();
    }
}
