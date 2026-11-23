package com.novaai.mobile;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
import android.widget.*;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {
    private LinearLayout messages;
    private EditText input;
    private ScrollView scroll;
    private TextView status;
    private Button send;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private JSONArray conversation = new JSONArray();
    private static final String PREFS = "nova_settings";
    private static final String KEY = "gemini_api_key";
    private static final String MODEL = "gemini-2.5-flash-lite";

    private int dp(float v) { return (int)(v * getResources().getDisplayMetrics().density + 0.5f); }

    private GradientDrawable shape(int color, int radius) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(radius));
        return d;
    }

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(Color.rgb(13, 10, 24));
        getWindow().setNavigationBarColor(Color.rgb(13, 10, 24));
        buildUi();
        restoreHistory();
        updateStatus();
        if (messages.getChildCount() == 0) {
            addBubble("Welcome to Nova AI! Add your Gemini API key in Settings to start chatting with real AI.", false);
            saveHistory();
        }
    }

    private void buildUi() {
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(16), dp(12), dp(16), dp(12));
        page.setBackgroundColor(Color.rgb(13, 10, 24));
        setContentView(page);

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        TextView title = new TextView(this);
        title.setText("NOVA AI");
        title.setTextColor(Color.WHITE);
        title.setTextSize(22);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        header.addView(title, new LinearLayout.LayoutParams(0, -2, 1));

        Button settings = new Button(this);
        settings.setText("Settings");
        settings.setTextSize(11);
        settings.setTextColor(Color.WHITE);
        settings.setBackground(shape(Color.rgb(55, 37, 91), 18));
        header.addView(settings);
        Button newChat = new Button(this);
        newChat.setText("New chat");
        newChat.setTextSize(11);
        newChat.setTextColor(Color.WHITE);
        newChat.setBackground(shape(Color.rgb(55, 37, 91), 18));
        header.addView(newChat);
        page.addView(header);

        status = new TextView(this);
        status.setTextColor(Color.rgb(174, 145, 255));
        status.setTextSize(10);
        status.setPadding(0, dp(10), 0, dp(10));
        page.addView(status);

        scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        messages = new LinearLayout(this);
        messages.setOrientation(LinearLayout.VERTICAL);
        messages.setPadding(0, dp(8), 0, dp(12));
        scroll.addView(messages);
        page.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));

        LinearLayout composer = new LinearLayout(this);
        composer.setGravity(Gravity.CENTER_VERTICAL);
        composer.setPadding(dp(8), dp(6), dp(6), dp(6));
        composer.setBackground(shape(Color.rgb(31, 25, 46), 24));
        input = new EditText(this);
        input.setSingleLine(false);
        input.setMaxLines(4);
        input.setTextSize(14);
        input.setHint("Message Nova...");
        input.setHintTextColor(Color.rgb(160, 150, 177));
        input.setTextColor(Color.WHITE);
        input.setBackgroundColor(Color.TRANSPARENT);
        input.setPadding(dp(10), dp(8), dp(8), dp(8));
        composer.addView(input, new LinearLayout.LayoutParams(0, -2, 1));

        send = new Button(this);
        send.setText("➤");
        send.setTextSize(18);
        send.setTextColor(Color.WHITE);
        send.setBackground(shape(Color.rgb(125, 75, 230), 22));
        composer.addView(send, new LinearLayout.LayoutParams(dp(48), dp(48)));
        page.addView(composer, new LinearLayout.LayoutParams(-1, -2));

        TextView foot = new TextView(this);
        foot.setText("Nova AI • Gemini powered");
        foot.setTextColor(Color.rgb(112, 103, 130));
        foot.setGravity(Gravity.CENTER);
        foot.setTextSize(10);
        foot.setPadding(0, dp(8), 0, 0);
        page.addView(foot);

        send.setOnClickListener(v -> sendMessage());
        input.setOnEditorActionListener((v, actionId, event) -> { sendMessage(); return true; });
        settings.setOnClickListener(v -> showKeyDialog());
        newChat.setOnClickListener(v -> {
            conversation = new JSONArray();
            messages.removeAllViews();
            saveHistory();
            addBubble("New conversation started. What would you like to ask?", false);
            saveHistory();
        });
    }

    private void updateStatus() {
        boolean ready = !getSharedPreferences(PREFS, MODE_PRIVATE).getString(KEY, "").trim().isEmpty();
        status.setText(ready ? "● GEMINI CONNECTED • " + MODEL : "● SETUP REQUIRED • Add your Gemini API key in Settings");
        status.setTextColor(ready ? Color.rgb(128, 230, 170) : Color.rgb(255, 205, 117));
    }

    private void showKeyDialog() {
        EditText field = new EditText(this);
        field.setSingleLine(true);
        field.setHint("Paste Gemini API key");
        field.setInputType( android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        field.setText(getSharedPreferences(PREFS, MODE_PRIVATE).getString(KEY, ""));
        new AlertDialog.Builder(this)
            .setTitle("Nova AI setup")
            .setMessage("Enter your Gemini API key. The key is saved on this device only. Do not share your APK with a personal key inside it.")
            .setView(field)
            .setPositiveButton("Save", (d, w) -> {
                String value = field.getText().toString().trim();
                getSharedPreferences(PREFS, MODE_PRIVATE).edit().putString(KEY, value).apply();
                updateStatus();
                Toast.makeText(this, value.isEmpty() ? "API key cleared" : "API key saved on this device", Toast.LENGTH_SHORT).show();
            })
            .setNeutralButton("Clear key", (d, w) -> {
                getSharedPreferences(PREFS, MODE_PRIVATE).edit().remove(KEY).apply();
                updateStatus();
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void addBubble(String text, boolean user) {
        TextView bubble = new TextView(this);
        bubble.setText(text);
        bubble.setTextColor(Color.WHITE);
        bubble.setTextSize(14);
        bubble.setLineSpacing(dp(3), 1.0f);
        bubble.setPadding(dp(14), dp(11), dp(14), dp(11));
        bubble.setBackground(shape(user ? Color.rgb(104, 62, 190) : Color.rgb(39, 32, 55), 18));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            Math.min(getResources().getDisplayMetrics().widthPixels - dp(70), dp(310)), -2);
        lp.gravity = user ? Gravity.RIGHT : Gravity.LEFT;
        lp.setMargins(0, dp(5), 0, dp(5));
        messages.addView(bubble, lp);
        scroll.post(() -> scroll.fullScroll(View.FOCUS_DOWN));
    }

    private void sendMessage() {
        String message = input.getText().toString().trim();
        if (message.isEmpty()) return;
        String apiKey = getSharedPreferences(PREFS, MODE_PRIVATE).getString(KEY, "").trim();
        if (apiKey.isEmpty()) {
            showKeyDialog();
            Toast.makeText(this, "Add your Gemini API key first", Toast.LENGTH_LONG).show();
            return;
        }
        input.setText("");
        ((InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE))
            .hideSoftInputFromWindow(input.getWindowToken(), 0);
        addBubble(message, true);
        JSONObject userPart = new JSONObject();
        try {
            userPart.put("role", "user");
            userPart.put("parts", new JSONArray().put(new JSONObject().put("text", message)));
            conversation.put(userPart);
        } catch (Exception ignored) {}
        saveHistory();
        send.setEnabled(false);
        status.setText("● NOVA IS THINKING...");
        executor.execute(() -> {
            try {
                String answer = callGemini(apiKey, conversation);
                JSONObject modelPart = new JSONObject();
                modelPart.put("role", "model");
                modelPart.put("parts", new JSONArray().put(new JSONObject().put("text", answer)));
                conversation.put(modelPart);
                runOnUiThread(() -> {
                    addBubble(answer, false);
                    saveHistory();
                    send.setEnabled(true);
                    updateStatus();
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    addBubble("Sorry, I couldn't get a response. " + safeError(e.getMessage()), false);
                    send.setEnabled(true);
                    updateStatus();
                });
            }
        });
    }

    private String callGemini(String key, JSONArray history) throws Exception {
        URL url = new URL("https://generativelanguage.googleapis.com/v1beta/models/" + MODEL + ":generateContent?key=" + key);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setConnectTimeout(20000);
        conn.setReadTimeout(45000);
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");

        JSONObject body = new JSONObject();
        body.put("contents", history);
        JSONObject generation = new JSONObject();
        generation.put("temperature", 0.7);
        generation.put("maxOutputTokens", 1024);
        body.put("generationConfig", generation);

        try (OutputStream os = conn.getOutputStream()) {
            os.write(body.toString().getBytes(StandardCharsets.UTF_8));
        }
        int code = conn.getResponseCode();
        InputStream stream = code >= 200 && code < 300 ? conn.getInputStream() : conn.getErrorStream();
        StringBuilder response = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) response.append(line);
        }
        conn.disconnect();

        JSONObject result = new JSONObject(response.toString());
        if (code < 200 || code >= 300) {
            String msg = result.optJSONObject("error") != null ?
                result.getJSONObject("error").optString("message", "HTTP " + code) : "HTTP " + code;
            throw new Exception(msg);
        }
        JSONArray candidates = result.optJSONArray("candidates");
        if (candidates == null || candidates.length() == 0) throw new Exception("The AI returned no answer. Please try again.");
        JSONObject content = candidates.getJSONObject(0).getJSONObject("content");
        JSONArray parts = content.getJSONArray("parts");
        StringBuilder answer = new StringBuilder();
        for (int i = 0; i < parts.length(); i++) {
            String t = parts.getJSONObject(i).optString("text", "");
            if (!t.isEmpty()) {
                if (answer.length() > 0) answer.append("\n");
                answer.append(t);
            }
        }
        if (answer.length() == 0) throw new Exception("The AI returned an empty answer.");
        return answer.toString();
    }

    private String safeError(String message) {
        if (message == null) return "Check your internet connection and API key.";
        if (message.length() > 220) message = message.substring(0, 220) + "...";
        return message;
    }

    private void saveHistory() {
        try {
            JSONArray saved = new JSONArray();
            for (int i = 0; i < conversation.length(); i++) saved.put(conversation.getJSONObject(i));
            getSharedPreferences(PREFS, MODE_PRIVATE).edit().putString("history", saved.toString()).apply();
        } catch (Exception ignored) {}
    }

    private void restoreHistory() {
        try {
            conversation = new JSONArray(getSharedPreferences(PREFS, MODE_PRIVATE).getString("history", "[]"));
            for (int i = 0; i < conversation.length(); i++) {
                JSONObject item = conversation.getJSONObject(i);
                String role = item.optString("role");
                JSONArray parts = item.optJSONArray("parts");
                if (parts != null && parts.length() > 0) {
                    String text = parts.getJSONObject(0).optString("text", "");
                    addBubble(text, "user".equals(role));
                }
            }
        } catch (Exception ignored) { conversation = new JSONArray(); }
    }

    @Override protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
}
