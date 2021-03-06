package org.genshin.emojidexandroid;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.ViewFlipper;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initEmojidexEditor();
        setShareButtonIcon();
        getIntentData();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId())
        {
            case R.id.action_settings:
                openSettings(null);
                return true;
        }
        return false;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        if (viewFlipper.getCurrentView() == findViewById(R.id.emoji_layout))
        {
            outState.putCharSequence("text", emojiEditText.getText());
            outState.putString("view", "center");
        }
        else if (viewFlipper.getCurrentView() == findViewById(R.id.text_layout))
        {
            outState.putCharSequence("text", textEditText.getText());
            outState.putString("view", "right");
        }
        else
        {
            outState.putCharSequence("text", emojiHalfEditText.getText());
            outState.putString("view", "left");
        }
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        String nowView = savedInstanceState.getString("view");

        if (nowView.equals("center"))
        {
            emojiEditText.setText(savedInstanceState.getCharSequence("text"));
        }
        else if (nowView.equals("right"))
        {
            viewFlipper.showNext();
            textEditText.setText(savedInstanceState.getCharSequence("text"));
        }
        else
        {
            viewFlipper.showPrevious();
            emojiHalfEditText.setText(savedInstanceState.getCharSequence("text"));
            textHalfEditText.setText(deEmojify(savedInstanceState.getCharSequence("text")));
        }
    }


    /**
     * Emojidex
     */
    private EmojidexEditor emojidex = null;
    private EmojiDataManager emojiDataManager;
    private List<EmojiData> emojiDataList;

    private EditText emojiEditText;
    private EditText textEditText;
    private EditText emojiHalfEditText;
    private EditText textHalfEditText;
    private CustomTextWatcher emojiTextWatcher;
    private CustomTextWatcher textTextWatcher;
    private CustomTextWatcher emojiHalfTextWatcher;
    private CustomTextWatcher textHalfTextWatcher;
    private ViewFlipper viewFlipper;

    private boolean realTime;

    private void initEmojidexEditor()
    {
        if (emojidex == null)
            emojidex = new EmojidexEditor(getApplicationContext());

        emojiDataManager = emojidex.getEmojiDataManager();
        emojiDataList = emojiDataManager.getCategorizedList("all");

        emojiEditText = (EditText)findViewById(R.id.emoji_edittext);
        textEditText = (EditText) findViewById(R.id.text_edittext);
        emojiHalfEditText = (EditText)findViewById(R.id.emoji_half_edittext);
        textHalfEditText = (EditText)findViewById(R.id.text_half_edittext);

        // detects input
        emojiTextWatcher = new CustomTextWatcher(emojiEditText.getId(), emojiEditText);
        textTextWatcher = new CustomTextWatcher(textEditText.getId(), textEditText);
        emojiHalfTextWatcher = new CustomTextWatcher(emojiHalfEditText.getId(), emojiHalfEditText);
        textHalfTextWatcher = new CustomTextWatcher(textHalfEditText.getId(), textHalfEditText);

        emojiEditText.addTextChangedListener(emojiTextWatcher);
        textEditText.addTextChangedListener(textTextWatcher);
        emojiHalfEditText.addTextChangedListener(emojiHalfTextWatcher);
        emojiHalfEditText.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                emojiHalfEditText.removeTextChangedListener(emojiHalfTextWatcher);
                emojiHalfEditText.addTextChangedListener(emojiHalfTextWatcher);
                textHalfEditText.removeTextChangedListener(textHalfTextWatcher);
                return false;
            }
        });
        textHalfEditText.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                textHalfEditText.removeTextChangedListener(textHalfTextWatcher);
                textHalfEditText.addTextChangedListener(textHalfTextWatcher);
                emojiHalfEditText.removeTextChangedListener(emojiHalfTextWatcher);
                return false;
            }
        });

        // set viewFlipper action
        viewFlipper = (ViewFlipper)findViewById(R.id.viewFlipper_editor);
        viewFlipper.setOnTouchListener(new FlickTouchListener());
        // set default view
        viewFlipper.showNext();
    }

    private CharSequence emojify(final CharSequence cs)
    {
        return emojidex.emojify(cs);
    }

    private CharSequence deEmojify(final CharSequence cs)
    {
        return emojidex.deEmojify(cs);
    }

    private CharSequence toUnicodeString(final CharSequence cs)
    {
        return emojidex.toUnicodeString(cs);
    }

    /**
     * share
     * @param v
     */
    public void shareData(View v)
    {
        String data = setShareData();
        createList(data);
    }

    /**
     * share to last selected application
     * @param v
     */
    public void shareDataLastSelected(View v)
    {
        String packageName = FileOperation.loadPreferences(getApplicationContext(), FileOperation.SHARE);
        if (packageName.equals(""))
            shareData(v);
        else
        {
            // set share data
            String data = setShareData();

            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/plain");
            intent.setPackage(packageName);
            intent.putExtra(Intent.EXTRA_TEXT, data);
            startActivity(intent);
        }
    }

    /**
     * set share data
     * @return data
     */
    private String setShareData()
    {
        String text;
        if (viewFlipper.getCurrentView() == findViewById(R.id.emoji_layout))
        {
            text = toUnicodeString(emojiEditText.getText()).toString();
        }
        else if (viewFlipper.getCurrentView() == findViewById(R.id.text_layout))
        {
            text = textEditText.getText().toString();
        }
        else
        {
            text = toUnicodeString(emojiHalfEditText.getText()).toString();
        }

        return  text;
    }

    public void moveViewToRight(View v)
    {
        // right edge of the screen
        if (viewFlipper.getCurrentView() == findViewById(R.id.text_layout))
            return;

        // move emoji
        if (viewFlipper.getCurrentView() == findViewById(R.id.emoji_text_layout))
        {
            emojiEditText.setText(emojiHalfEditText.getText());
        }
        // move text and toUnicodeString
        else
        {
            textEditText.setText(toUnicodeString(emojiEditText.getText()));
        }

        viewFlipper.setInAnimation(AnimationUtils.loadAnimation(getApplicationContext(), R.anim.right_in));
        viewFlipper.setOutAnimation(AnimationUtils.loadAnimation(getApplicationContext(), R.anim.left_out));
        viewFlipper.showNext();
}

    public void moveViewToLeft(View v)
    {
        // left edge of the screen
        if (viewFlipper.getCurrentView() == findViewById(R.id.emoji_text_layout))
            return;

        // move text and emojify
        if (viewFlipper.getCurrentView() == findViewById(R.id.text_layout))
        {
            emojiEditText.setText(emojify(deEmojify(textEditText.getText())));
        }
        // move emoji and deEmojify
        else
        {
            emojiHalfEditText.setText(emojiEditText.getText());
            textHalfEditText.setText(deEmojify(emojiEditText.getText()));
        }

        viewFlipper.setInAnimation(AnimationUtils.loadAnimation(getApplicationContext(), R.anim.left_in));
        viewFlipper.setOutAnimation(AnimationUtils.loadAnimation(getApplicationContext(), R.anim.right_out));
        viewFlipper.showPrevious();
    }

    private class FlickTouchListener implements View.OnTouchListener
    {
        private float lastTouchX;
        private float currentX;

        @Override
        public boolean onTouch(View view, MotionEvent event)
        {
            switch (event.getAction())
            {
                case MotionEvent.ACTION_DOWN:
                    lastTouchX = event.getX();
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    currentX = event.getX();
                    if (lastTouchX + 30 < currentX)
                        moveViewToLeft(null);
                    if (lastTouchX > currentX + 30)
                        moveViewToRight(null);
                    break;
            }
            return true;
        }
    }

    private class CustomTextWatcher implements TextWatcher
    {
        private int inputEditTextId;
        private EditText editText;

        public CustomTextWatcher(int id, EditText editText)
        {
            inputEditTextId = id;
            this.editText = editText;
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) { }

        @Override
        public void afterTextChanged(Editable s)
        {
            // exclude while converting the Japanese
            final Object[] spans = s.getSpans(0, s.length(), Object.class);
            for(Object span : spans)
                if(span.getClass().getName().equals("android.view.inputmethod.ComposingText"))
                    return;

            int oldPos = editText.getSelectionStart();
            int oldTextLength = editText.getText().length();

            switch (inputEditTextId)
            {
                // convert to emoji in emojiEditText
                case R.id.emoji_edittext :
                    if (realTime)
                    {
                        editText.removeTextChangedListener(emojiTextWatcher);
                        editText.setText(emojify(deEmojify(editText.getText())));
                        editText.addTextChangedListener(emojiTextWatcher);
                    }
                    break;

                // convert to unicode in emojiEditText
                case R.id.text_edittext :
                    if (realTime)
                    {
                        editText.removeTextChangedListener(textTextWatcher);
                        editText.setText(toUnicodeString(editText.getText()));
                        editText.addTextChangedListener(textTextWatcher);
                    }
                    break;

                // conversion while input emoji
                case R.id.emoji_half_edittext :
                    editText.removeTextChangedListener(emojiHalfTextWatcher);
                    editText.setText(emojify(s));
                    textHalfEditText.setText(deEmojify(s));
                    editText.addTextChangedListener(emojiHalfTextWatcher);
                    break;

                // conversion while input text
                case R.id.text_half_edittext :
                    editText.removeTextChangedListener(textHalfTextWatcher);
                    emojiHalfEditText.setText(emojify(s));
                    editText.setText(deEmojify(s));
                    editText.addTextChangedListener(textHalfTextWatcher);
                    break;
            }

            // adjustment cursor position
            int addTextLength = editText.getText().length() - oldTextLength;
            int newPos = oldPos + addTextLength;
            if (newPos > editText.getText().length())
                newPos = editText.getText().length();
            else if (newPos < 0)
                newPos = 0;
            editText.setSelection(newPos);

            // load image
            ArrayList<String> emojiNames = new ArrayList<String>();
            emojiNames = emojidex.getEmojiNames();
            if (emojiNames != null)
            {
                for (String emojiName : emojiNames)
                    loadImage(emojiName);
            }
        }
    }

    /**
     * open settings view
     * @param v
     */
    public void openSettings(View v)
    {
        String text;

        Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
        startActivity(intent);
    }

    /**
     * create application list for sharing.
     * @param data
     */
    private void createList(final String data)
    {
        // get destination application list
        PackageManager packageManager = getPackageManager();
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        final List<ResolveInfo> appInfo = packageManager.queryIntentActivities(intent, 0);

        // create listView
        ListView listView = new ListView(this);
        listView.setAdapter(new appInfoAdapter(this, R.layout.applicationlist_view, appInfo));

        // create dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getResources().getString(R.string.share));
        builder.setView(listView);
        builder.setPositiveButton(R.string.cancel, null);
        final AlertDialog dialog = builder.show();

        // when click a listView's item
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                dialog.dismiss();

                ResolveInfo info = appInfo.get(position);
                Intent intent = new Intent(Intent.ACTION_SEND);
                intent.setType("text/plain");
                intent.setPackage(info.activityInfo.packageName);
                intent.putExtra(Intent.EXTRA_TEXT, data);
                startActivity(intent);

                // save settings and set icon
                FileOperation.savePreferences(getApplicationContext(), info.activityInfo.packageName, FileOperation.SHARE);
                setShareButtonIcon();
            }
        });
    }

    /**
     * application list adapter for sharing.
     */
    private class appInfoAdapter extends ArrayAdapter<ResolveInfo>
    {
        private LayoutInflater inflater;
        private int layout;

        public appInfoAdapter(Context context, int resource, List<ResolveInfo> objects) {
            super(context, resource, objects);

            inflater = (LayoutInflater)context.getSystemService(LAYOUT_INFLATER_SERVICE);
            layout = resource;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view = convertView;
            if (convertView == null)
                view = this.inflater.inflate(this.layout, null);

            // set application icon & name
            PackageManager packageManager = getPackageManager();
            ResolveInfo info = getItem(position);
            ImageView icon = (ImageView)view.findViewById(R.id.application_list_icon);
            icon.setImageDrawable(info.loadIcon(packageManager));
            TextView textView = (TextView)view.findViewById(R.id.application_list_name);
            textView.setText(info.loadLabel(packageManager));
            return view;
        }
    }

    /**
     * set the last selected application's icon to share button
     */
    private void setShareButtonIcon()
    {
        ImageButton button = (ImageButton)findViewById(R.id.last_share_button);

        // set image
        String packageName = FileOperation.loadPreferences(getApplicationContext(), FileOperation.SHARE);
        if (packageName.equals(""))
        {
            // default
            button.setImageResource(android.R.drawable.ic_menu_send);
        }
        else
        {
            boolean set = false;
            PackageManager packageManager = getPackageManager();
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/plain");
            List<ResolveInfo> appInfo = packageManager.queryIntentActivities(intent, 0);
            for (ResolveInfo info : appInfo)
            {
                // get application's icon
                if (info.activityInfo.packageName.equals(packageName))
                {
                    button.setImageDrawable(info.loadIcon(packageManager));
                    set = true;
                    break;
                }
            }
            // set default icon when could not get icon
            if (!set)
                button.setImageResource(android.R.drawable.ic_menu_send);
        }
    }

    /**
     * load image
     * @param name emoji name
     */
    private void loadImage(String name)
    {
        EmojiData emoji = emojiDataManager.getEmojiData(name);
        ImageView blank = (ImageView)findViewById(R.id.blank);
        blank.setImageDrawable(emoji.getIcon());
    }

    @Override
    protected void onResume()
    {
        super.onResume();

        loadRealTimeConvertSettings();
        CharSequence text = emojiEditText.getText();
        if (text != null)
            emojiEditText.setText(text);
    }

    /**
     * Whether to the real-time conversion
     */
    private void loadRealTimeConvertSettings()
    {
        if (FileOperation.loadPreferences(getApplicationContext(), FileOperation.REALTIME).equals("false"))
            realTime = false;
        else
            realTime = true;
    }

    /**
     * When sent other application's text(intent).
     */
    private void getIntentData()
    {
        Intent intent = getIntent();
        String action = intent.getAction();
        String type = intent.getType();

        if (action.equals(Intent.ACTION_SEND) && type != null) {
            if (type.equals("text/plain")) {
                emojiEditText.setText(intent.getStringExtra(Intent.EXTRA_TEXT));
            }
        }
    }
}