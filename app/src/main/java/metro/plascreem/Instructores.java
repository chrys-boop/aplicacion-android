package metro.plascreem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import metro.plascreem.databinding.ActivityInstructoresBinding;

public class Instructores extends AppCompatActivity {

    private ActivityInstructoresBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityInstructoresBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbarInstructores);

        binding.bottomNavigationInstructores.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = null;
            int itemId = item.getItemId();
            if (itemId == R.id.navigation_profile) {
                selectedFragment = new InstructorProfileFragment();
            } else if (itemId == R.id.navigation_upload_docs) {
                selectedFragment = new UploadDocumentsFragment();
            } else if (itemId == R.id.navigation_upload_media) {
                selectedFragment = new UploadMediaFragment();
            } else if (itemId == R.id.navigation_calendar) {
                selectedFragment = new CalendarFragment();
            }

            if (selectedFragment != null) {
                replaceFragment(selectedFragment, false, R.id.fragment_container_instructores);
            }
            return true;
        });

        binding.fabSendMessageInstructores.setOnClickListener(v -> {
            replaceFragment(new SendMessageFragment(), true, R.id.fragment_container_instructores);
        });

        if (savedInstanceState == null) {
            binding.bottomNavigationInstructores.setSelectedItemId(R.id.navigation_profile);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_options_menu, menu);

        // --- CORRECCIÓN DEFINITIVA ---
        // Ocultar los ítems que no son para el rol de Instructor
        MenuItem workerHistoryItem = menu.findItem(R.id.action_worker_history);
        if (workerHistoryItem != null) {
            workerHistoryItem.setVisible(false);
        }

        MenuItem manageUsersItem = menu.findItem(R.id.action_manage_users);
        if (manageUsersItem != null) {
            manageUsersItem.setVisible(false);
        }

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

    public void replaceFragment(Fragment fragment, boolean addToBackStack, int containerId) {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(containerId, fragment);
        if (addToBackStack) {
            transaction.addToBackStack(null);
        }
        transaction.commit();
    }
}
