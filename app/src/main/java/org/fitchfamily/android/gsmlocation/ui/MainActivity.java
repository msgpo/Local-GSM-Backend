package org.fitchfamily.android.gsmlocation.ui;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.view.View;

import com.mikepenz.google_material_typeface_library.GoogleMaterial;
import com.mikepenz.materialdrawer.Drawer;
import com.mikepenz.materialdrawer.DrawerBuilder;
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IDrawerItem;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.Extra;
import org.androidannotations.annotations.InstanceState;
import org.androidannotations.annotations.ViewById;
import org.fitchfamily.android.gsmlocation.Config;
import org.fitchfamily.android.gsmlocation.R;
import org.fitchfamily.android.gsmlocation.ui.database.UpdateDatabaseFragment;
import org.fitchfamily.android.gsmlocation.ui.database.UpdateDatabaseFragment_;
import org.fitchfamily.android.gsmlocation.ui.settings.AdvancedSettingsFragment_;
import org.fitchfamily.android.gsmlocation.ui.settings.SettingsFragment_;

@EActivity(R.layout.activity_main)
public class MainActivity extends AppCompatActivity implements UpdateDatabaseFragment.Listener {
    private static final int SETTINGS = 1;

    private static final int DATABASE = 2;

    private static final int LIBRARIES = 3;

    private static final int SETTINGS_ADVANCED = 4;

    private static final int ABOUT = 5;

    @Extra
    protected Action action;

    @InstanceState
    protected Bundle drawerState;

    @ViewById
    protected Toolbar toolbar;

    private Drawer drawer;

    @AfterViews
    protected void init() {
        toolbar.setTitle(R.string.app_name);

        drawer = new DrawerBuilder()
                .withActivity(this)
                .withSavedInstance(drawerState)
                .withToolbar(toolbar)
                .addDrawerItems(
                        new PrimaryDrawerItem()
                                .withName(R.string.fragment_update_database_title)
                                .withIcon(GoogleMaterial.Icon.gmd_folder)
                                .withIdentifier(DATABASE),

                        new PrimaryDrawerItem()
                                .withName(R.string.fragment_settings_title)
                                .withIcon(GoogleMaterial.Icon.gmd_settings)
                                .withIdentifier(SETTINGS),

                        new PrimaryDrawerItem()
                                .withName(R.string.fragment_settings_advanced_title)
                                .withIcon(GoogleMaterial.Icon.gmd_settings_applications)
                                .withIdentifier(SETTINGS_ADVANCED)
                )
                .addStickyDrawerItems(
                        new PrimaryDrawerItem()
                                .withName(R.string.activity_main_about)
                                .withIcon(GoogleMaterial.Icon.gmd_info)
                                .withSelectable(false)
                                .withIdentifier(ABOUT)
                )
                .withOnDrawerItemClickListener(new Drawer.OnDrawerItemClickListener() {
                    @Override
                    public boolean onItemClick(View view, int position, IDrawerItem drawerItem) {
                        if (drawerItem != null) {
                            final int id = drawerItem.getIdentifier();

                            if (id == SETTINGS) {
                                setFragment(new SettingsFragment_());
                            } else if (id == DATABASE) {
                                setFragment(new UpdateDatabaseFragment_());
                             } else if (id == SETTINGS_ADVANCED) {
                                setFragment(new AdvancedSettingsFragment_());
                            } else if (id == ABOUT) {
                                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(Config.ABOUT_URL)));
                            }
                        }
                        return false;
                    }
                })
                .withFireOnInitialOnClick(drawerState == null)
                .build();

        updateTitle();
        if(action == Action.request_permission) {
            drawer.setSelection(SETTINGS);
        }

    }

    private void setFragment(Fragment fragment) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.container, fragment)
                .commit();

        updateTitle();
    }

    @Override
    public void openSettings() {
        drawer.setSelection(SETTINGS);
    }

    private void updateTitle() {
        IDrawerItem item = drawer == null ? null : drawer.getDrawerItem(drawer.getCurrentSelection());

        if (item instanceof PrimaryDrawerItem) {
            toolbar.setSubtitle(((PrimaryDrawerItem) item).getName().getText(this));
        } else {
            toolbar.setSubtitle(null);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        drawer.saveInstanceState(drawerState = new Bundle());
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onBackPressed() {
        if(drawer != null && drawer.isDrawerOpen()) {
            drawer.closeDrawer();
        } else {
            super.onBackPressed();
        }
    }

    public enum Action {
        request_permission
    }
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }
}
