package net.golbarg.skillassessment.ui.about;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import net.golbarg.skillassessment.R;

import java.util.Calendar;

import mehdi.sakout.aboutpage.AboutPage;
import mehdi.sakout.aboutpage.Element;

public class AboutFragment extends Fragment {
    Context context;
    LinearLayout linearLayoutAbout;

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_about, container, false);
        context = root.getContext();

        linearLayoutAbout = root.findViewById(R.id.linear_layout_about);

        Element elementDonate = new Element("Donate", R.drawable.ic_heart);
        elementDonate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(context, "Donate", Toast.LENGTH_SHORT).show();
            }
        });


        View aboutPage = new AboutPage(context)
                .isRTL(false)
                .setDescription("This is the about description of app")
                .addItem(new Element().setTitle(context.getString(R.string.version)).setIconDrawable(R.drawable.ic_info))
                .addGroup("Help us to improve")
                .addItem(elementDonate)
                .addGroup(context.getString(R.string.connect_with_us))
                .addEmail("contact@golbarg.net")
                .addFacebook("golbargnet")
                .addYoutube("UCooKZ969-pMyYN0WAbOUaAg")
                .addPlayStore("net.golbarg.skillassessment")
                .addWebsite("golbarg.net")
                .addItem(getCopyRightsElement())
                .create();

        linearLayoutAbout.addView(aboutPage);

        return root;
    }

    Element getCopyRightsElement() {
        Element copyRightsElement = new Element();
        final String copyrights = "CopyRights ©" + Calendar.getInstance().get(Calendar.YEAR);
        copyRightsElement.setTitle(copyrights);
        copyRightsElement.setIconTint(mehdi.sakout.aboutpage.R.color.about_item_icon_color);
        copyRightsElement.setIconNightTint(android.R.color.white);
        copyRightsElement.setGravity(Gravity.CENTER);
        copyRightsElement.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("AboutFragment", "onClick:yup ");
            }
        });
        return copyRightsElement;
    }
}
