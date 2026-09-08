package com.bruhazor.tcphizligonderim;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Canvas;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Paint;
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
import java.security.MessageDigest;
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
        page = basePage("TCP/IP for PJLINK", "Manuel bağlantı ile mesaj gönderin.", "manual");
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
        page = basePage("Hızlı Butonlar", "Sabit IP, port ve kodla tek tıklamayla gönderin.", "quick");
        Button create = button("+ Fonksiyon Butonu Oluştur", PRIMARY); create.setOnClickListener(v -> editDialog(-1)); page.addView(create); page.addView(space(18));
        if (quickButtons.isEmpty()) page.addView(note("Henüz hızlı buton oluşturulmadı."));
        for (int i = 0; i < quickButtons.size(); i++) page.addView(quickCard(i, quickButtons.get(i)));
        setContentView(wrapScroll(page));
    }

    private void showPjlink() {
        page = basePage("PJLink Komutları", "Projektörünüzü uygulama içinden yönetin.", "pjlink");
        page.addView(new ProjectorArt(this), new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(105)));
        TextView ipLabel = note("PJLink bağlantısı"); ipLabel.setTypeface(Typeface.DEFAULT, Typeface.BOLD); page.addView(ipLabel);
        EditText ip = input("Projektör IP adresi", prefs.getString("pj_ip", ""), false);
        EditText password = input("PJLink parolası (yoksa boş bırakın)", prefs.getString("pj_password", ""), false); password.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        TextView status = note("Hazır • Standart PJLink portu 4352");
        page.addView(ip); page.addView(space(8)); page.addView(password); page.addView(space(8)); page.addView(status); page.addView(space(12));
        page.addView(note("Komuta dokunmadan önce IP ve gerekirse parolayı girin. “Gönder” bağlantıyı test eder, komutu yollar ve yanıtı gösterir.")); page.addView(space(9));
        String[][] commands = {
                {"Aç", "%1POWR 1"}, {"Kapat / Bekleme", "%1POWR 0"}, {"Güç Durumu", "%1POWR ?"},
                {"HDMI 1", "%1INPT 31"}, {"HDMI 2", "%1INPT 32"}, {"PC / RGB", "%1INPT 11"},
                {"Video", "%1INPT 21"}, {"Giriş Durumu", "%1INPT ?"},
                {"Görüntü Karart", "%1AVMT 11"}, {"Görüntüyü Aç", "%1AVMT 10"},
                {"Ses Kapat", "%1AVMT 21"}, {"Sesi Aç", "%1AVMT 20"},
                {"AV Karart", "%1AVMT 31"}, {"AV Karartmayı Aç", "%1AVMT 30"},
                {"Hata Durumu", "%1ERST ?"}, {"Lamba Durumu", "%1LAMP ?"},
                {"Giriş Listesi", "%1INST ?"}, {"Cihaz Adı", "%1NAME ?"},
                {"Üretici", "%1INF1 ?"}, {"Model", "%1INF2 ?"}, {"PJLink Sınıfı", "%1CLSS ?"}
        };
        for (String[] command : commands) page.addView(pjlinkCard(command[0], command[1], ip, password, status));
        setContentView(wrapScroll(page));
    }

    private View pjlinkCard(String label, String command, EditText ip, EditText password, TextView status) {
        LinearLayout card = new LinearLayout(this); card.setOrientation(LinearLayout.VERTICAL); card.setPadding(dp(14), dp(11), dp(14), dp(11)); card.setBackground(round(CARD, 14));
        TextView name = new TextView(this); name.setText(label); name.setTextColor(Color.WHITE); name.setTextSize(17); name.setTypeface(Typeface.DEFAULT, Typeface.BOLD); card.addView(name);
        TextView code = note(command + "  ↵ CR"); code.setTextColor(Color.rgb(170, 190, 255)); card.addView(code);
        LinearLayout row = new LinearLayout(this); row.setOrientation(LinearLayout.HORIZONTAL);
        Button copy = button("Kopyala", Color.rgb(52, 59, 80)); Button send = button("Gönder", PRIMARY);
        copy.setOnClickListener(v -> { ((ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE)).setPrimaryClip(ClipData.newPlainText("PJLink komutu", command + "\r")); toast("Komut CR sonlandırıcısıyla kopyalandı."); });
        send.setOnClickListener(v -> { prefs.edit().putString("pj_ip", ip.getText().toString().trim()).putString("pj_password", password.getText().toString()).apply(); sendPjlink(ip.getText().toString().trim(), password.getText().toString(), command, status); });
        row.addView(copy, weight(1)); row.addView(gap()); row.addView(send, weight(1)); card.addView(row);
        LinearLayout.LayoutParams p = full(); p.setMargins(0, 0, 0, dp(10)); card.setLayoutParams(p); return card;
    }

    private LinearLayout basePage(String title, String subtitle, String active) {
        LinearLayout root = new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(dp(20), dp(20), dp(20), dp(26)); root.setBackgroundColor(BG);
        TextView h = new TextView(this); h.setText(title); h.setTextColor(Color.WHITE); h.setTextSize(27); h.setTypeface(Typeface.DEFAULT, Typeface.BOLD); root.addView(h);
        TextView sub = note(subtitle); root.addView(sub); root.addView(space(14));
        LinearLayout tabs = new LinearLayout(this); tabs.setOrientation(LinearLayout.HORIZONTAL);
        Button manual = button("Manuel", active.equals("manual") ? PRIMARY : Color.rgb(48, 55, 75));
        Button quick = button("Hızlı", active.equals("quick") ? PRIMARY : Color.rgb(48, 55, 75));
        Button pjlink = button("PJLink", active.equals("pjlink") ? PRIMARY : Color.rgb(48, 55, 75));
        manual.setOnClickListener(v -> showManual()); quick.setOnClickListener(v -> showQuick()); pjlink.setOnClickListener(v -> showPjlink());
        tabs.addView(manual, weight(1)); tabs.addView(gap()); tabs.addView(quick, weight(1)); tabs.addView(gap()); tabs.addView(pjlink, weight(1)); root.addView(tabs); root.addView(space(18)); return root;
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
    private void sendPjlink(String ip, String password, String command, TextView status) {
        if (ip.isEmpty()) { toast("Projektör IP adresini girin."); return; }
        status.setText("Bağlanıyor: " + ip + ":4352");
        executor.execute(() -> {
            try (Socket s = connect(ip, 4352)) {
                s.setSoTimeout(5000);
                String greeting = readLine(s);
                if (!greeting.startsWith("PJLINK")) throw new IOException("PJLink karşılama mesajı alınamadı.");
                String outgoing = command;
                if (greeting.startsWith("PJLINK 1")) {
                    String[] parts = greeting.split(" ");
                    if (parts.length < 3 || password.isEmpty()) throw new IOException("Projektör parola istiyor.");
                    outgoing = md5(parts[2] + password) + command;
                }
                s.getOutputStream().write((outgoing + "\r").getBytes(StandardCharsets.US_ASCII));
                s.getOutputStream().flush();
                String response = readLine(s);
                runOnUiThread(() -> status.setText("Bağlantı başarılı • Yanıt: " + response));
            } catch (Exception e) {
                runOnUiThread(() -> status.setText("Bağlantı başarısız • " + e.getMessage()));
            }
        });
    }
    private String readLine(Socket socket) throws IOException {
        StringBuilder line = new StringBuilder(); int value;
        while ((value = socket.getInputStream().read()) != -1) { if (value == '\r' || value == '\n') break; line.append((char) value); }
        if (line.length() == 0 && value == -1) throw new IOException("Projektör yanıt vermedi.");
        return line.toString();
    }
    private String md5(String value) throws Exception {
        byte[] bytes = MessageDigest.getInstance("MD5").digest(value.getBytes(StandardCharsets.US_ASCII));
        StringBuilder output = new StringBuilder(); for (byte b : bytes) output.append(String.format("%02x", b)); return output.toString();
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

    private static class ProjectorArt extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        ProjectorArt(Context context) { super(context); }
        @Override protected void onDraw(Canvas canvas) {
            super.onDraw(canvas); float w = getWidth(), h = getHeight();
            paint.setColor(Color.rgb(38, 44, 60)); canvas.drawRoundRect(w * .13f, h * .22f, w * .87f, h * .76f, h * .16f, h * .16f, paint);
            paint.setColor(Color.rgb(80, 114, 255)); canvas.drawCircle(w * .70f, h * .49f, h * .18f, paint);
            paint.setColor(Color.rgb(16, 18, 26)); canvas.drawCircle(w * .70f, h * .49f, h * .10f, paint);
            paint.setColor(Color.rgb(128, 145, 205)); canvas.drawCircle(w * .30f, h * .49f, h * .045f, paint); canvas.drawCircle(w * .42f, h * .49f, h * .045f, paint);
            paint.setColor(Color.rgb(80, 114, 255)); canvas.drawRoundRect(w * .22f, h * .79f, w * .34f, h * .84f, h * .02f, h * .02f, paint); canvas.drawRoundRect(w * .66f, h * .79f, w * .78f, h * .84f, h * .02f, h * .02f, paint);
        }
    }
}
