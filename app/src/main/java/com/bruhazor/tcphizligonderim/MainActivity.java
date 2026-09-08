package com.bruhazor.tcphizligonderim;

import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends android.app.Activity {
    private static final int BG = Color.rgb(20, 22, 31);
    private static final int CARD = Color.rgb(32, 36, 48);
    private static final int FIELD = Color.rgb(17, 19, 26);
    private static final int PRIMARY = Color.rgb(80, 114, 255);
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final List<JSONObject> quickButtons = new ArrayList<>();
    private SharedPreferences prefs;
    private Socket manualSocket;
    private LinearLayout page;
    private TextView manualStatus;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().setStatusBarColor(Color.rgb(17, 19, 27));
        getWindow().setNavigationBarColor(Color.rgb(17, 19, 27));
        prefs = getSharedPreferences("tcp_hizli_gonderim", MODE_PRIVATE);
        loadButtons();
        showManual();
    }

    private void showManual() {
        page = basePage("TCP/IP Hızlı Gönderim", "Manuel bağlantı ile mesaj gönderin.", false);
        EditText ip = input("IP adresi", "127.0.0.1", false);
        EditText port = input("Port numarası", "5000", false); port.setInputType(InputType.TYPE_CLASS_NUMBER);
        page.addView(ip); page.addView(space(8)); page.addView(port); page.addView(space(14));
        LinearLayout row = new LinearLayout(this); row.setOrientation(LinearLayout.HORIZONTAL);
        Button open = button("TCP Aç", PRIMARY); Button close = button("TCP Kapat", Color.rgb(185, 78, 97));
        row.addView(open, weight(1)); row.addView(gap()); row.addView(close, weight(1)); page.addView(row); page.addView(space(16));
        EditText message = input("Gönderilecek TCP mesajı", "", true); message.setMinLines(6); page.addView(message); page.addView(space(14));
        Button send = button("Mesajı Gönder", PRIMARY); page.addView(send); page.addView(space(10));
        manualStatus = note("Hazır"); page.addView(manualStatus);
        open.setOnClickListener(v -> connectManual(ip.getText().toString(), port.getText().toString()));
        close.setOnClickListener(v -> closeManual());
        send.setOnClickListener(v -> sendManual(message.getText().toString()));
        setContentView(wrapScroll(page));
    }

    private void showQuick() {
        page = basePage("Hızlı Butonlar", "Sabit IP, port ve kodla tek tıklamayla gönderin.", true);
        Button create = button("+ Fonksiyon Butonu Oluştur", PRIMARY); create.setOnClickListener(v -> editDialog(-1)); page.addView(create); page.addView(space(18));
        if (quickButtons.isEmpty()) page.addView(note("Henüz hızlı buton oluşturulmadı."));
        for (int i = 0; i < quickButtons.size(); i++) page.addView(quickCard(i, quickButtons.get(i)));
        setContentView(wrapScroll(page));
    }

    private LinearLayout basePage(String title, String subtitle, boolean quickActive) {
        LinearLayout root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(dp(20), dp(20), dp(20), dp(26)); root.setBackgroundColor(BG);
        TextView h = new TextView(this); h.setText(title); h.setTextColor(Color.WHITE); h.setTextSize(27); h.setTypeface(Typeface.DEFAULT, Typeface.BOLD); root.addView(h);
        TextView sub = note(subtitle); root.addView(sub); root.addView(space(14));
        LinearLayout tabs = new LinearLayout(this); tabs.setOrientation(LinearLayout.HORIZONTAL);
        Button manual = button("Manuel", quickActive ? Color.rgb(48, 55, 75) : PRIMARY);
        Button quick = button("Hızlı Butonlar", quickActive ? PRIMARY : Color.rgb(48, 55, 75));
        manual.setOnClickListener(v -> showManual()); quick.setOnClickListener(v -> showQuick());
        tabs.addView(manual, weight(1)); tabs.addView(gap()); tabs.addView(quick, weight(1)); root.addView(tabs); root.addView(space(18)); return root;
    }

    private View quickCard(int index, JSONObject item) {
        LinearLayout card = new LinearLayout(this); card.setOrientation(LinearLayout.VERTICAL); card.setPadding(dp(15), dp(14), dp(15), dp(14)); card.setBackground(round(CARD, 16));
        TextView name = new TextView(this); name.setText(item.optString("name")); name.setTextColor(Color.WHITE); name.setTextSize(18); name.setTypeface(Typeface.DEFAULT, Typeface.BOLD); card.addView(name);
        TextView info = note(item.optJSONArray("ips").length() + " hedef  •  Port " + item.optInt("port") + "\nKod: " + item.optString("message")); card.addView(info); card.addView(space(8));
        LinearLayout row = new LinearLayout(this); row.setOrientation(LinearLayout.HORIZONTAL);
        Button send = button("Gönder", PRIMARY); Button edit = button("Düzenle", Color.rgb(52, 59, 80)); Button delete = button("Sil", Color.rgb(185, 78, 97));
        send.setOnClickListener(v -> sendQuick(item)); edit.setOnClickListener(v -> editDialog(index)); delete.setOnClickListener(v -> deleteButton(index));
        row.addView(send, weight(1)); row.addView(gap()); row.addView(edit, weight(1)); row.addView(gap()); row.addView(delete, weight(1)); card.addView(row);
        LinearLayout.LayoutParams p = full(); p.setMargins(0, 0, 0, dp(12)); card.setLayoutParams(p); return card;
    }

    private void editDialog(int index) {
        JSONObject old = index >= 0 ? quickButtons.get(index) : null;
        LinearLayout form = new LinearLayout(this); form.setOrientation(LinearLayout.VERTICAL); form.setPadding(dp(22), dp(6), dp(22), dp(6));
        EditText name = input("Buton adı", old == null ? "" : old.optString("name"), false);
        EditText ips = input("IP'ler: satır satır veya virgülle", old == null ? "127.0.0.1" : joinIps(old.optJSONArray("ips")), true); ips.setMinLines(3);
        EditText port = input("Port", old == null ? "5000" : String.valueOf(old.optInt("port")), false); port.setInputType(InputType.TYPE_CLASS_NUMBER);
        EditText message = input("Her basışta gönderilecek kod", old == null ? "" : old.optString("message"), false);
        form.addView(name); form.addView(space(9)); form.addView(ips); form.addView(space(9)); form.addView(port); form.addView(space(9)); form.addView(message);
        AlertDialog dialog = new AlertDialog.Builder(this).setTitle(index < 0 ? "Yeni Fonksiyon Butonu" : "Butonu Düzenle").setView(form).setNegativeButton("Vazgeç", null).setPositiveButton("Kaydet", null).create();
        dialog.setOnShowListener(x -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            List<String> targetIps = splitIps(ips.getText().toString()); int targetPort;
            try { targetPort = Integer.parseInt(port.getText().toString()); } catch (NumberFormatException e) { targetPort = 0; }
            if (name.getText().toString().trim().isEmpty() || targetIps.isEmpty() || message.getText().toString().isEmpty() || targetPort < 1 || targetPort > 65535) { toast("Buton adı, IP, port ve kod zorunludur."); return; }
            JSONObject saved = new JSONObject(); try { saved.put("name", name.getText().toString().trim()); saved.put("ips", new JSONArray(targetIps)); saved.put("port", targetPort); saved.put("message", message.getText().toString()); } catch (JSONException ignored) { }
            if (index < 0) quickButtons.add(saved); else quickButtons.set(index, saved); saveButtons(); dialog.dismiss(); showQuick();
        })); dialog.show();
    }

    private void deleteButton(int index) {
        new AlertDialog.Builder(this).setTitle("Butonu sil").setMessage("Bu hızlı buton silinsin mi?").setNegativeButton("Vazgeç", null).setPositiveButton("Sil", (d, w) -> { quickButtons.remove(index); saveButtons(); showQuick(); }).show();
    }

    private void connectManual(String ip, String portText) {
        executor.execute(() -> { try { int port = Integer.parseInt(portText); manualSocket = connect(ip.trim(), port); runOnUiThread(() -> manualStatus.setText("Manuel TCP bağlantısı açık.")); } catch (Exception e) { runOnUiThread(() -> manualStatus.setText("Bağlantı hatası: " + e.getMessage())); } });
    }
    private void closeManual() { try { if (manualSocket != null) manualSocket.close(); } catch (IOException ignored) { } manualSocket = null; manualStatus.setText("Manuel TCP bağlantısı kapalı."); }
    private void sendManual(String message) {
        if (message.isEmpty()) { toast("Gönderilecek mesajı yazın."); return; }
        executor.execute(() -> { try { if (manualSocket == null) throw new IOException("Önce TCP bağlantısını açın."); manualSocket.getOutputStream().write(message.getBytes(StandardCharsets.UTF_8)); manualSocket.getOutputStream().flush(); runOnUiThread(() -> manualStatus.setText("Mesaj gönderildi.")); } catch (Exception e) { runOnUiThread(() -> manualStatus.setText("Gönderim hatası: " + e.getMessage())); } });
    }
    private void sendQuick(JSONObject item) {
        executor.execute(() -> { int ok = 0; ArrayList<String> bad = new ArrayList<>(); for (String ip : splitIps(joinIps(item.optJSONArray("ips")))) { try (Socket s = connect(ip, item.optInt("port"))) { s.getOutputStream().write(item.optString("message").getBytes(StandardCharsets.UTF_8)); ok++; } catch (IOException e) { bad.add(ip); } } final int count = ok; runOnUiThread(() -> toast(count + " hedefe gönderildi" + (bad.isEmpty() ? "." : "; başarısız: " + String.join(", ", bad)))); });
    }
    private Socket connect(String ip, int port) throws IOException { Socket s = new Socket(); s.connect(new InetSocketAddress(ip, port), 5000); return s; }

    private void loadButtons() { try { JSONArray all = new JSONArray(prefs.getString("quick", "[]")); for (int i = 0; i < all.length(); i++) quickButtons.add(all.getJSONObject(i)); } catch (JSONException ignored) { } }
    private void saveButtons() { JSONArray all = new JSONArray(); for (JSONObject o : quickButtons) all.put(o); prefs.edit().putString("quick", all.toString()).apply(); }
    private String joinIps(JSONArray ips) { ArrayList<String> all = new ArrayList<>(); for (int i = 0; i < ips.length(); i++) all.add(ips.optString(i)); return String.join("\n", all); }
    private List<String> splitIps(String value) { ArrayList<String> all = new ArrayList<>(); for (String ip : value.replace(",", "\n").split("\\n")) if (!ip.trim().isEmpty()) all.add(ip.trim()); return all; }
    private ScrollView wrapScroll(View view) { ScrollView scroll = new ScrollView(this); scroll.setFillViewport(true); scroll.addView(view); return scroll; }
    private EditText input(String hint, String text, boolean multi) { EditText e = new EditText(this); e.setHint(hint); e.setText(text); e.setTextColor(Color.WHITE); e.setHintTextColor(Color.rgb(160, 170, 193)); e.setTextSize(16); e.setSingleLine(!multi); e.setPadding(dp(14), dp(10), dp(14), dp(10)); e.setBackground(round(FIELD, 12)); e.setLayoutParams(full()); return e; }
    private Button button(String text, int color) { Button b = new Button(this); b.setText(text); b.setAllCaps(false); b.setTextColor(Color.WHITE); b.setTextSize(15); b.setTypeface(Typeface.DEFAULT, Typeface.BOLD); b.setBackground(round(color, 13)); b.setPadding(dp(7), 0, dp(7), 0); b.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(52))); return b; }
    private TextView note(String text) { TextView t = new TextView(this); t.setText(text); t.setTextColor(Color.rgb(177, 188, 212)); t.setTextSize(14); t.setPadding(0, dp(5), 0, dp(5)); return t; }
    private View space(int height) { View v = new View(this); v.setLayoutParams(new LinearLayout.LayoutParams(1, dp(height))); return v; }
    private View gap() { View v = new View(this); v.setLayoutParams(new LinearLayout.LayoutParams(dp(8), 1)); return v; }
    private LinearLayout.LayoutParams full() { return new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT); }
    private LinearLayout.LayoutParams weight(int w) { return new LinearLayout.LayoutParams(0, dp(52), w); }
    private GradientDrawable round(int color, int radius) { GradientDrawable g = new GradientDrawable(); g.setColor(color); g.setCornerRadius(dp(radius)); return g; }
    private int dp(int n) { return (int) (n * getResources().getDisplayMetrics().density + .5f); }
    private void toast(String text) { Toast.makeText(this, text, Toast.LENGTH_LONG).show(); }
    @Override protected void onDestroy() { closeManual(); executor.shutdownNow(); super.onDestroy(); }
}
