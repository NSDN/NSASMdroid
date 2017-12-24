package cn.ac.nya.nsasmdroid;

import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import cn.ac.nya.nsasm.NSASM;
import cn.ac.nya.nsasm.Util;

public class MainActivity extends AppCompatActivity {

    private Typeface font;
    private TheHandler theHandler;
    private BottomNavigationView mNavigation;

    private View mInteractiveView;
    private View mEditorView;
    private View mAboutView;

    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()) {
                case R.id.navigation_home:
                    mInteractiveView.setVisibility(View.VISIBLE);
                    mEditorView.setVisibility(View.INVISIBLE);
                    mAboutView.setVisibility(View.INVISIBLE);
                    return true;
                case R.id.navigation_dashboard:
                    mInteractiveView.setVisibility(View.INVISIBLE);
                    mEditorView.setVisibility(View.VISIBLE);
                    mAboutView.setVisibility(View.INVISIBLE);
                    return true;
                case R.id.navigation_about:
                    mInteractiveView.setVisibility(View.INVISIBLE);
                    mEditorView.setVisibility(View.INVISIBLE);
                    mAboutView.setVisibility(View.VISIBLE);
                    return true;
            }
            return false;
        }

    };

    private static class TheHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            throw new RuntimeException("HO!");
        }
    }

    String buf;
    int lines = 1; NSASM.Result result;
    String[][] code = Util.getSegments("nop\n"); //ld func allowed
    NSASM nsasm = new NSASM(64, 32, 16, code);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        font = Typeface.createFromAsset(getAssets(), "fonts/consolas.ttf");
        theHandler = new TheHandler();
        mInteractiveView = findViewById(R.id.frag_interactive);
        mEditorView = findViewById(R.id.frag_editor);
        mAboutView = findViewById(R.id.frag_about);

        EditText output = (EditText) mInteractiveView.findViewById(R.id.textOutput);
        output.setTypeface(font);
        output.setKeyListener(null);

        EditText input = (EditText) mInteractiveView.findViewById(R.id.textInput);
        input.setTypeface(font);
        input.setOnEditorActionListener((view, i, keyEvent) -> {
            if (i == EditorInfo.IME_ACTION_DONE) {
                Util.print(lines + " >>> ");
                buf = input.getText().toString();
                Util.print(buf + '\n');
                if (buf.length() == 0) {
                    lines += 1;
                    input.setText(""); return true;
                }
                buf = Util.formatLine(buf);
                if (buf.contains("#")) {
                    Util.print("<" + buf + ">\n");
                    input.setText(""); return true;
                }

                result = nsasm.execute(buf);
                if (result == NSASM.Result.ERR) {
                    Util.print("\nNSASM running error!\n");
                    Util.print("At line " + lines + ": " + buf + "\n\n");
                } else if (result == NSASM.Result.ETC) {
                    output.setText(""); lines = 0; result = NSASM.Result.OK;

                    nsasm = new NSASM(64, 32, 16, code);
                    Util.print("NyaSama Assembly Script Module\n");
                    Util.print("Version: ");
                    Util.print(NSASM.version);
                    Util.print("\n\n");
                    Util.print("Now in console mode.\n\n");
                } else if (buf.startsWith("run") || buf.startsWith("call")) {
                    nsasm.run();
                }
                lines += 1;

                input.setText("");
                return true;
            }
            return false;
        });

        Util.print = (value) -> output.append(value.toString());
        Util.scan = () -> {
            final EditText editText = new EditText(this);
            AlertDialog dialog = new AlertDialog.Builder(MainActivity.this)
                    .setTitle(R.string.input_title)
                    .setMessage(R.string.input_msg)
                    .setView(editText)
                    .setPositiveButton(android.R.string.ok, null)
                    .setNegativeButton(android.R.string.cancel, null)
                    .create();

            dialog.setOnShowListener((dialogInterface) -> {
                dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                        .setOnClickListener((view) -> {
                            if (editText.getText().length() != 0) {
                                theHandler.sendMessage(theHandler.obtainMessage());
                                dialog.dismiss();
                            } else {
                                Toast.makeText(
                                        MainActivity.this,
                                        R.string.input_msg,
                                        Toast.LENGTH_SHORT
                                ).show();
                            }
                        });
                dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
                        .setOnClickListener((view) -> {
                            editText.setText("");
                            theHandler.sendMessage(theHandler.obtainMessage());
                            dialog.dismiss();
                        });
            });
            dialog.show();

            try {
                Looper.loop();
            } catch (Exception e) {
                System.err.println(e.getMessage());
            }

            return editText.getText().toString();
        };

        Util.print("NyaSama Assembly Script Module\n");
        Util.print("Version: ");
        Util.print(NSASM.version);
        Util.print("\n\n");
        Util.print("Now in console mode.\n\n");

        mNavigation = (BottomNavigationView) findViewById(R.id.navigation);
        mNavigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);

        EditText editor = (EditText) mEditorView.findViewById(R.id.textEditor);
        editor.setTypeface(font);
        editor.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if (i < charSequence.length()) {
                    int pos = editor.getSelectionStart(); char c = charSequence.charAt(i);
                    if (c == '\n' && pos != i) {
                        int start, end;
                        for (start = i - 1; start >= 0; start--) {
                            if (charSequence.charAt(start) == '\n')
                                break;
                        }
                        for (end = start + 1; end < pos; end++) {
                            if (charSequence.charAt(end) != ' ')
                                break;
                        }
                        String indent = "";
                        for (int k = 0; k < end - start - 1; k++) indent = indent.concat(" ");
                        editor.getText().insert(pos, indent);
                    } else {
                        switch (c) {
                            case '(': editor.getText().insert(pos, ")"); break;
                            case '<': editor.getText().insert(pos, ">"); break;
                            case '{': editor.getText().insert(pos, "}"); break;
                            case '[': editor.getText().insert(pos, "]"); break;
                            case '\'': editor.getText().insert(pos, "\'"); break;
                            case '\"': editor.getText().insert(pos, "\""); break;
                        }
                        editor.setSelection(pos);
                    }
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {
            }
        });
        Button btnTab = (Button) mEditorView.findViewById(R.id.buttonTab);
        btnTab.setOnClickListener((view) -> {
            int pos = editor.getSelectionStart();
            editor.getText().insert(pos, "    ");
        });
        Button btnRun = (Button) mEditorView.findViewById(R.id.buttonRun);
        btnRun.setOnClickListener((view) -> {
            output.setText("");
            String str = editor.getText().toString();
            if (str.isEmpty()) return;

            new Thread(() -> {
                Util.print("NyaSama Assembly Script Module\n");
                Util.print("Version: ");
                Util.print(NSASM.version);
                Util.print("\n\n");

                long now = System.nanoTime();
                Util.execute(str);
                long end = System.nanoTime();
                double ms = (double) (end - now) / 1e6;
                Util.print("This script took " +
                        Double.toString(ms) + "ms.\n\n");
            }).run();

            mNavigation.setSelectedItemId(R.id.navigation_home);
        });

        TextView textView = mAboutView.findViewById(R.id.app_ver);
        textView.setText(NSASM.version);

    }

}
