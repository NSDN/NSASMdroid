package cn.ac.nya.nsasmdroid;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v7.app.AppCompatActivity;
import android.view.KeyEvent;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import cn.ac.nya.nsasm.NSASM;
import cn.ac.nya.nsasm.Util;

public class MainActivity extends AppCompatActivity {

    BottomNavigationView mNavigation;

    private View mInteractiveView;
    private View mEditorView;
    private View mAboutView;

    private boolean inputState = false;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mInteractiveView = findViewById(R.id.frag_interactive);
        mEditorView = findViewById(R.id.frag_editor);
        mAboutView = findViewById(R.id.frag_about);

        EditText output = (EditText) mInteractiveView.findViewById(R.id.textOutput);
        EditText input = (EditText) mInteractiveView.findViewById(R.id.textInput);
        input.setOnKeyListener((view, i, keyEvent) -> {
            if (keyEvent.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
                inputState = true;
            }
            return true;
        });

        Util.print = (value) -> output.append(value.toString());
        Util.scan = () -> {
            while (!inputState) Thread.yield();
            inputState = false;
            return input.getText().toString();
        };

        mNavigation = (BottomNavigationView) findViewById(R.id.navigation);
        mNavigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);

        Button button = (Button) mEditorView.findViewById(R.id.buttonRun);
        EditText text = (EditText) mEditorView.findViewById(R.id.textEditor);
        button.setOnClickListener((view) -> {
            output.setText("");
            String str = text.getText().toString();
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

    }

}
