package net.golbarg.skillassessment.ui.about;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;

import net.golbarg.skillassessment.BuildConfig;
import net.golbarg.skillassessment.R;
import net.golbarg.skillassessment.ads.AdManager;
import net.golbarg.skillassessment.billing.BillingManager;
import net.golbarg.skillassessment.databinding.FragmentAboutBinding;
import net.golbarg.skillassessment.databinding.ViewSettingRowBinding;
import net.golbarg.skillassessment.databinding.ViewSettingSwitchRowBinding;
import net.golbarg.skillassessment.db.BackupManager;
import net.golbarg.skillassessment.db.ContentUpdater;
import net.golbarg.skillassessment.reminder.ReminderScheduler;
import net.golbarg.skillassessment.util.AppLinks;
import net.golbarg.skillassessment.util.Async;
import net.golbarg.skillassessment.util.Feedback;
import net.golbarg.skillassessment.util.GoalPicker;
import net.golbarg.skillassessment.util.Prefs;
import net.golbarg.skillassessment.util.UiUtils;
import net.golbarg.skillassessment.widget.DailyQuestionWidget;

import java.time.LocalDate;
import java.util.Calendar;

/** The settings tab: preferences, reminders, backups, content updates and links. */
public class AboutFragment extends Fragment {
    private static final String TAG = "AboutFragment";

    private static final int[] THEME_MODES = {
            AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM, AppCompatDelegate.MODE_NIGHT_NO, AppCompatDelegate.MODE_NIGHT_YES};
    private static final int[] THEME_LABELS = {R.string.theme_system, R.string.theme_light, R.string.theme_dark};

    private FragmentAboutBinding binding;
    private ActivityResultLauncher<String> notificationPermission;
    private ActivityResultLauncher<String> createBackup;
    private ActivityResultLauncher<String[]> openBackup;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        notificationPermission = registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
            if (granted) pickReminderTime();
            else if (binding != null) UiUtils.snackbar(binding.getRoot(), R.string.notifications_denied, Snackbar.LENGTH_LONG).show();
        });
        createBackup = registerForActivityResult(new ActivityResultContracts.CreateDocument("application/json"), uri -> {
            if (uri == null) return;
            Context app = requireContext().getApplicationContext();
            Async.run(this, () -> {
                try {
                    BackupManager.export(app, uri);
                    return true;
                } catch (Exception e) {
                    Log.e(TAG, "Backup failed", e);
                    return false;
                }
            }, ok -> {
                if (binding != null) UiUtils.snackbar(binding.getRoot(), Boolean.TRUE.equals(ok) ? R.string.backup_saved : R.string.backup_export_failed, Snackbar.LENGTH_LONG).show();
            });
        });
        openBackup = registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> {
            if (uri == null || binding == null) return;
            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.backup_import_confirm_title)
                    .setMessage(R.string.backup_import_confirm_message)
                    .setNegativeButton(R.string.cancel, null)
                    .setPositiveButton(R.string.backup_restore, (d, w) -> restoreBackup(uri))
                    .show();
        });
    }

    private void restoreBackup(Uri uri) {
        Context app = requireContext().getApplicationContext();
        Async.run(this, () -> {
            try {
                BackupManager.restore(app, uri);
                return true;
            } catch (Exception e) {
                Log.e(TAG, "Restore failed", e);
                return false;
            }
        }, ok -> {
            if (Boolean.TRUE.equals(ok)) DailyQuestionWidget.refresh(app);
            if (binding != null) UiUtils.snackbar(binding.getRoot(), Boolean.TRUE.equals(ok) ? R.string.backup_restored : R.string.backup_failed, Snackbar.LENGTH_LONG).show();
        });
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentAboutBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        UiUtils.applySystemBarPadding(binding.root, true, false);
        binding.txtVersion.setText(getString(R.string.version_format, BuildConfig.VERSION_NAME));
        binding.txtCopyright.setText(getString(R.string.copyright, Calendar.getInstance().get(Calendar.YEAR)));

        row(binding.rowTheme, R.drawable.ic_contrast, getString(R.string.theme), themeLabel(), v -> chooseTheme());
        bindGoalRow();
        toggle(binding.rowFontSize, R.drawable.ic_format_size, R.string.font_size, R.string.font_size_desc,
                Prefs.isLargeCodeFont(requireContext()), on -> Prefs.setLargeCodeFont(requireContext(), on));
        toggle(binding.rowSound, R.drawable.ic_volume, R.string.sound_effects, R.string.sound_effects_desc,
                Prefs.isSoundEnabled(requireContext()), on -> {
                    Prefs.setSoundEnabled(requireContext(), on);
                    if (on) Feedback.play(Feedback.Sound.CORRECT);
                });
        toggle(binding.rowVibration, R.drawable.ic_vibration, R.string.vibration, R.string.vibration_desc,
                Prefs.isVibrationEnabled(requireContext()), on -> {
                    Prefs.setVibrationEnabled(requireContext(), on);
                    if (on) Feedback.haptic(binding.getRoot(), Feedback.Haptic.SUCCESS);
                });
        bindReminder();
        binding.rowReminder.getRoot().setOnClickListener(v -> onReminderClicked());

        BillingManager billing = BillingManager.get(requireContext());
        billing.getState().observe(getViewLifecycleOwner(), this::bindPremium);
        row(binding.rowRestore, R.drawable.ic_replay, getString(R.string.premium_restore), null, v -> billing.restore(owned -> {
            if (binding == null) return;
            UiUtils.snackbar(binding.getRoot(), owned ? R.string.premium_restored : R.string.premium_not_found, Snackbar.LENGTH_SHORT).show();
        }));

        bindPrivacyRow();

        row(binding.rowBackup, R.drawable.ic_upload, getString(R.string.backup_export), getString(R.string.backup_export_desc),
                v -> createBackup.launch("skill-assessment-backup-" + LocalDate.now() + ".json"));
        row(binding.rowRestoreBackup, R.drawable.ic_download, getString(R.string.backup_import), getString(R.string.backup_import_desc),
                v -> openBackup.launch(new String[]{"application/json", "application/octet-stream", "text/plain"}));
        bindContentRow();

        row(binding.rowRate, R.drawable.ic_star, getString(R.string.rate_app), getString(R.string.rate_app_desc), v -> rate());
        row(binding.rowShare, R.drawable.ic_share, getString(R.string.share_app), null, v -> share());
        row(binding.rowFeedback, R.drawable.ic_mail, getString(R.string.contact_us), AppLinks.EMAIL, v -> email());
        row(binding.rowWebsite, R.drawable.ic_public, getString(R.string.website), "golbarg.net", v -> open(AppLinks.WEBSITE));
    }

    private void row(ViewSettingRowBinding row, @DrawableRes int icon, String title, @Nullable String subtitle, View.OnClickListener click) {
        row.imgIcon.setImageResource(icon);
        row.txtTitle.setText(title);
        row.txtSubtitle.setVisibility(subtitle == null ? View.GONE : View.VISIBLE);
        row.txtSubtitle.setText(subtitle);
        row.getRoot().setOnClickListener(click);
    }

    private void bindPremium(BillingManager.State state) {
        if (binding == null || state == null) return;
        String subtitle = state.premium ? getString(R.string.premium_active_desc) : getString(R.string.premium_desc);
        String title = state.premium ? getString(R.string.premium_active)
                : state.price != null ? getString(R.string.premium_buy, state.price) : getString(R.string.premium_title);
        row(binding.rowPremium, R.drawable.ic_crown, title, subtitle, v -> {
            if (BillingManager.isPremium(requireContext())) return;
            BillingManager.get(requireContext()).purchase(requireActivity(), success -> {
                if (binding == null) return;
                if (success) {
                    Feedback.play(Feedback.Sound.UNLOCK);
                    UiUtils.snackbar(binding.getRoot(), R.string.premium_thanks, Snackbar.LENGTH_LONG).show();
                } else if (!BillingManager.isPremium(requireContext())) {
                    BillingManager.State current = BillingManager.get(requireContext()).getState().getValue();
                    if (current == null || !current.available) {
                        UiUtils.snackbar(binding.getRoot(), R.string.premium_unavailable, Snackbar.LENGTH_LONG).show();
                    }
                }
            });
        });
        // While Premium isn't sold, only past buyers see the section (as "Premium active").
        boolean showSection = BillingManager.FOR_SALE || state.premium;
        binding.txtPremiumHeader.setVisibility(showSection ? View.VISIBLE : View.GONE);
        binding.cardPremium.setVisibility(showSection ? View.VISIBLE : View.GONE);
        binding.rowRestore.getRoot().setVisibility(BillingManager.FOR_SALE && !state.premium ? View.VISIBLE : View.GONE);
    }

    private void bindGoalRow() {
        row(binding.rowGoal, R.drawable.ic_flag, getString(R.string.daily_goal),
                GoalPicker.label(requireContext(), Prefs.getDailyGoal(requireContext())),
                v -> GoalPicker.show(requireContext(), () -> {
                    if (binding != null) bindGoalRow();
                }));
    }

    private void bindContentRow() {
        long last = ContentUpdater.getLastCheck(requireContext());
        String subtitle = last > 0 ? getString(R.string.content_updates_desc, UiUtils.formatDate(last)) : getString(R.string.content_updates_never);
        row(binding.rowContentUpdates, R.drawable.ic_sync, getString(R.string.content_updates), subtitle, v -> checkContent());
    }

    private void checkContent() {
        binding.rowContentUpdates.txtSubtitle.setVisibility(View.VISIBLE);
        binding.rowContentUpdates.txtSubtitle.setText(R.string.content_checking);
        binding.rowContentUpdates.getRoot().setEnabled(false);
        Context app = requireContext().getApplicationContext();
        Async.run(this, () -> ContentUpdater.check(app), status -> {
            if (binding == null) return;
            binding.rowContentUpdates.getRoot().setEnabled(true);
            bindContentRow();
            int message = status == ContentUpdater.Status.UPDATED ? R.string.content_updated
                    : status == ContentUpdater.Status.UP_TO_DATE ? R.string.content_up_to_date : R.string.content_failed;
            UiUtils.snackbar(binding.getRoot(), message, Snackbar.LENGTH_LONG).show();
        });
    }

    private void bindPrivacyRow() {
        boolean required = AdManager.isPrivacyOptionsRequired(requireContext());
        binding.rowPrivacy.getRoot().setVisibility(required ? View.VISIBLE : View.GONE);
        row(binding.rowPrivacy, R.drawable.ic_privacy, getString(R.string.ad_privacy), getString(R.string.ad_privacy_desc),
                v -> AdManager.showPrivacyOptions(requireActivity(), null));
    }

    private void bindReminder() {
        boolean on = ReminderScheduler.isEnabled(requireContext());
        binding.rowReminder.imgIcon.setImageResource(R.drawable.ic_notifications);
        binding.rowReminder.txtTitle.setText(R.string.reminder);
        binding.rowReminder.txtSubtitle.setText(on ? getString(R.string.reminder_at, formatTime(
                ReminderScheduler.getHour(requireContext()), ReminderScheduler.getMinute(requireContext()))) : getString(R.string.reminder_off));
        binding.rowReminder.switchToggle.setChecked(on);
        binding.rowReminder.getRoot().setContentDescription(getString(R.string.reminder));
    }

    private void onReminderClicked() {
        if (ReminderScheduler.isEnabled(requireContext())) {
            new MaterialAlertDialogBuilder(requireContext())
                    .setTitle(R.string.reminder)
                    .setItems(new CharSequence[]{getString(R.string.reminder_pick_time), getString(R.string.reminder_turn_off)}, (d, which) -> {
                        if (which == 0) {
                            pickReminderTime();
                        } else {
                            ReminderScheduler.disable(requireContext());
                            bindReminder();
                        }
                    })
                    .show();
            return;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS);
        } else {
            pickReminderTime();
        }
    }

    private void pickReminderTime() {
        MaterialTimePicker picker = new MaterialTimePicker.Builder()
                .setTitleText(R.string.reminder_pick_time)
                .setTimeFormat(DateFormat.is24HourFormat(requireContext()) ? TimeFormat.CLOCK_24H : TimeFormat.CLOCK_12H)
                .setHour(ReminderScheduler.getHour(requireContext()))
                .setMinute(ReminderScheduler.getMinute(requireContext()))
                .build();
        picker.addOnPositiveButtonClickListener(v -> {
            ReminderScheduler.enable(requireContext(), picker.getHour(), picker.getMinute());
            if (binding != null) bindReminder();
        });
        picker.show(getChildFragmentManager(), "reminder_time");
    }

    private String formatTime(int hour, int minute) {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.HOUR_OF_DAY, hour);
        c.set(Calendar.MINUTE, minute);
        return DateFormat.getTimeFormat(requireContext()).format(c.getTime());
    }

    private interface OnToggle {
        void onToggle(boolean on);
    }

    private void toggle(ViewSettingSwitchRowBinding row, @DrawableRes int icon, int title, int subtitle, boolean checked, OnToggle onToggle) {
        row.imgIcon.setImageResource(icon);
        row.txtTitle.setText(title);
        row.txtSubtitle.setText(subtitle);
        row.switchToggle.setChecked(checked);
        row.getRoot().setContentDescription(getString(title));
        row.getRoot().setOnClickListener(v -> {
            boolean on = !row.switchToggle.isChecked();
            row.switchToggle.setChecked(on);
            onToggle.onToggle(on);
        });
    }

    private String themeLabel() {
        int mode = Prefs.getThemeMode(requireContext());
        for (int i = 0; i < THEME_MODES.length; i++) if (THEME_MODES[i] == mode) return getString(THEME_LABELS[i]);
        return getString(R.string.theme_system);
    }

    private void chooseTheme() {
        int current = Prefs.getThemeMode(requireContext());
        int checked = 0;
        String[] labels = new String[THEME_LABELS.length];
        for (int i = 0; i < THEME_LABELS.length; i++) {
            labels[i] = getString(THEME_LABELS[i]);
            if (THEME_MODES[i] == current) checked = i;
        }
        new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.theme)
                .setSingleChoiceItems(labels, checked, (dialog, which) -> {
                    dialog.dismiss();
                    Prefs.setThemeMode(requireContext(), THEME_MODES[which]);
                })
                .show();
    }

    private void rate() {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(AppLinks.MARKET_URI)));
        } catch (ActivityNotFoundException e) {
            open(AppLinks.PLAY_URL);
        }
    }

    private void share() {
        Intent send = new Intent(Intent.ACTION_SEND)
                .setType("text/plain")
                .putExtra(Intent.EXTRA_SUBJECT, getString(R.string.app_name))
                .putExtra(Intent.EXTRA_TEXT, getString(R.string.share_app_message, AppLinks.PLAY_URL));
        startSafely(Intent.createChooser(send, getString(R.string.share_app)));
    }

    private void email() {
        Intent mail = new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:" + AppLinks.EMAIL))
                .putExtra(Intent.EXTRA_SUBJECT, getString(R.string.app_name) + " " + BuildConfig.VERSION_NAME);
        startSafely(mail);
    }

    private void open(String url) {
        startSafely(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
    }

    private void startSafely(Intent intent) {
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Toast.makeText(requireContext(), R.string.no_app_to_open, Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
