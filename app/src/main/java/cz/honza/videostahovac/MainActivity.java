package cz.honza.videostahovac;

import android.app.Activity;
import android.app.DownloadManager;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import org.json.JSONObject;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;

public class MainActivity extends Activity {
    private EditText urlInput, apiInput;
    private Spinner qualitySpinner;
    private Button downloadButton;
    private ProgressBar progress;
    private TextView status;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private static final String DEFAULT_API = "https://api.cobalt.tools/";
    private boolean busy = false;
    private long activeDownloadId = -1;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        buildUi();
    }

    private void buildUi() {
        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(20);
        root.setPadding(pad, dp(28), pad, pad);
        scroll.addView(root);

        TextView title = new TextView(this);
        title.setText("Video Stahovač");
        title.setTextSize(28);
        title.setTextColor(0xFF172554);
        title.setTypeface(null, 1);
        root.addView(title);

        TextView subtitle = new TextView(this);
        subtitle.setText("Vlož odkaz, vyber kvalitu a stáhni video do telefonu.");
        subtitle.setTextSize(15);
        subtitle.setPadding(0, dp(6), 0, dp(20));
        root.addView(subtitle);

        label(root, "URL videa");
        urlInput = new EditText(this);
        urlInput.setSingleLine(true);
        urlInput.setHint("https://…");
        urlInput.setInputType( android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_URI);
        root.addView(urlInput, matchWrap());

        label(root, "Kvalita videa");
        qualitySpinner = new Spinner(this);
        String[] qualities = {"Nejlepší dostupná", "2160p (4K)", "1440p", "1080p", "720p", "480p", "360p", "Pouze zvuk"};
        qualitySpinner.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, qualities));
        root.addView(qualitySpinner, matchWrap());

        label(root, "Cobalt API endpoint (pokročilé)");
        apiInput = new EditText(this);
        apiInput.setSingleLine(true);
        apiInput.setText(DEFAULT_API);
        apiInput.setHint("https://adresa-kompatibilni-instance/");
        apiInput.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_URI);
        root.addView(apiInput, matchWrap());

        downloadButton = new Button(this);
        downloadButton.setText("STÁHNOUT VIDEO");
        LinearLayout.LayoutParams bp = matchWrap();
        bp.topMargin = dp(18);
        root.addView(downloadButton, bp);

        progress = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        progress.setIndeterminate(true);
        progress.setVisibility(View.GONE);
        LinearLayout.LayoutParams pp = matchWrap();
        pp.topMargin = dp(12);
        root.addView(progress, pp);

        status = new TextView(this);
        status.setText("Připraveno.");
        status.setTextSize(14);
        status.setPadding(0, dp(10), 0, 0);
        root.addView(status);

        TextView note = new TextView(this);
        note.setText("\nPoznámka: podpora YouTube a Facebooku závisí na dostupnosti API a konkrétním odkazu. Stahuj pouze obsah, ke kterému máš oprávnění.");
        note.setTextSize(12);
        note.setTextColor(0xFF64748B);
        note.setPadding(0, dp(22), 0, 0);
        root.addView(note);

        downloadButton.setOnClickListener(v -> startResolve());
        setContentView(scroll);
    }

    private void label(LinearLayout root, String text) {
        TextView t = new TextView(this);
        t.setText(text);
        t.setTextSize(14);
        t.setTypeface(null, 1);
        t.setPadding(0, dp(12), 0, dp(4));
        root.addView(t);
    }

    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(-1, -2);
    }
    private int dp(int v) { return (int)(v * getResources().getDisplayMetrics().density + 0.5f); }

    private void startResolve() {
        if (busy) return;
        String link = urlInput.getText().toString().trim();
        String api = apiInput.getText().toString().trim();
        if (!(link.startsWith("https://") || link.startsWith("http://"))) {
            status.setText("Vlož platný odkaz začínající http:// nebo https://.");
            return;
        }
        if (!(api.startsWith("https://") || api.startsWith("http://"))) {
            status.setText("Zkontroluj adresu API endpointu.");
            return;
        }
        ((InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE))
                .hideSoftInputFromWindow(urlInput.getWindowToken(), 0);
        busy = true;
        downloadButton.setEnabled(false);
        progress.setVisibility(View.VISIBLE);
        status.setText("Zpracovávám odkaz…");
        new Thread(() -> {
            try {
                JSONObject body = new JSONObject();
                body.put("url", link);
                body.put("videoQuality", selectedQuality());
                body.put("downloadMode", qualitySpinner.getSelectedItemPosition() == 7 ? "audio" : "auto");
                body.put("filenameStyle", "basic");

                String endpoint = api.endsWith("/") ? api : api + "/";
                HttpURLConnection conn = (HttpURLConnection)new URL(endpoint).openConnection();
                conn.setRequestMethod("POST");
                conn.setConnectTimeout(15000);
                conn.setReadTimeout(45000);
                conn.setDoOutput(true);
                conn.setRequestProperty("Accept", "application/json");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("User-Agent", "VideoStahovacAndroid/1.0");
                try (OutputStream out = conn.getOutputStream()) {
                    out.write(body.toString().getBytes("UTF-8"));
                }
                int code = conn.getResponseCode();
                InputStream stream = (code >= 200 && code < 400) ? conn.getInputStream() : conn.getErrorStream();
                String response = readAll(stream);
                conn.disconnect();
                if (code < 200 || code >= 300) throw new Exception("API odpovědělo HTTP " + code + ". " + shortText(response));
                JSONObject json = new JSONObject(response);
                String apiStatus = json.optString("status", "");
                if (!"tunnel".equals(apiStatus) && !"redirect".equals(apiStatus)) {
                    String msg = json.optString("text", json.optString("error", "Tento odkaz API nepodporuje nebo vyžaduje jiný postup."));
                    throw new Exception(msg);
                }
                String fileUrl = json.optString("url", "");
                if (fileUrl.isEmpty()) throw new Exception("API nevrátilo adresu souboru.");
                String filename = json.optString("filename", "video-stazene");
                handler.post(() -> enqueueDownload(fileUrl, filename));
            } catch (Exception e) {
                handler.post(() -> {
                    status.setText("Stažení se nepodařilo: " + (e.getMessage() == null ? "neznámá chyba" : e.getMessage()));
                    resetBusy();
                });
            }
        }).start();
    }

    private String selectedQuality() {
        switch (qualitySpinner.getSelectedItemPosition()) {
            case 1: return "2160";
            case 2: return "1440";
            case 3: return "1080";
            case 4: return "720";
            case 5: return "480";
            case 6: return "360";
            default: return "max";
        }
    }

    private void enqueueDownload(String fileUrl, String filename) {
        try {
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(fileUrl));
            request.setTitle(filename);
            request.setDescription("Video Stahovač");
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            request.setAllowedOverMetered(true);
            request.setAllowedOverRoaming(false);
            request.setDestinationInExternalPublicDir(android.os.Environment.DIRECTORY_DOWNLOADS, safeFilename(filename));
            DownloadManager dm = (DownloadManager)getSystemService(DOWNLOAD_SERVICE);
            activeDownloadId = dm.enqueue(request);
            progress.setIndeterminate(false);
            progress.setMax(100);
            progress.setProgress(0);
            status.setText("Stahování probíhá… Soubor se ukládá do složky Stažené.");
            pollDownloadProgress();
        } catch (Exception e) {
            status.setText("Nepodařilo se zahájit stahování: " + e.getMessage());
        }
        resetBusy();
    }

    private void pollDownloadProgress() {
        if (activeDownloadId < 0) return;
        DownloadManager dm = (DownloadManager)getSystemService(DOWNLOAD_SERVICE);
        DownloadManager.Query q = new DownloadManager.Query().setFilterById(activeDownloadId);
        android.database.Cursor c = dm.query(q);
        if (c == null) return;
        try {
            if (!c.moveToFirst()) {
                progress.setVisibility(View.GONE);
                return;
            }
            int statusCol = c.getColumnIndex(DownloadManager.COLUMN_STATUS);
            int totalCol = c.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES);
            int soFarCol = c.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR);
            int st = c.getInt(statusCol);
            long total = c.getLong(totalCol);
            long soFar = c.getLong(soFarCol);
            if (total > 0) progress.setProgress((int)(soFar * 100 / total));
            if (st == DownloadManager.STATUS_SUCCESSFUL) {
                progress.setProgress(100);
                status.setText("Hotovo! Video najdeš ve složce Stažené. ✅");
                activeDownloadId = -1;
                progress.postDelayed(() -> progress.setVisibility(View.GONE), 2500);
            } else if (st == DownloadManager.STATUS_FAILED) {
                status.setText("Stahování selhalo. Zkontroluj připojení nebo zkus odkaz znovu.");
                activeDownloadId = -1;
                progress.setVisibility(View.GONE);
            } else {
                if (st == DownloadManager.STATUS_PENDING) status.setText("Stahování čeká…");
                else if (st == DownloadManager.STATUS_RUNNING) {
                    status.setText(total > 0 ? "Stahování: " + (soFar * 100 / total) + " %" : "Stahování probíhá…");
                }
                handler.postDelayed(this::pollDownloadProgress, 700);
            }
        } finally {
            c.close();
        }
    }

    private String safeFilename(String name) {
        String s = name.replaceAll("[\\\\/:*?\"<>|]", "_").trim();
        if (s.isEmpty()) s = "video-stazene";
        return s.length() > 120 ? s.substring(0, 120) : s;
    }

    private String readAll(InputStream in) throws IOException {
        if (in == null) return "";
        try (BufferedReader r = new BufferedReader(new InputStreamReader(in, "UTF-8"))) {
            StringBuilder b = new StringBuilder();
            String line;
            while ((line = r.readLine()) != null) b.append(line);
            return b.toString();
        }
    }
    private String shortText(String s) { return s.length() > 240 ? s.substring(0, 240) : s; }
    private void resetBusy() {
        busy = false;
        downloadButton.setEnabled(true);
        progress.setVisibility(View.GONE);
    }
}
