package com.suneku.amadeus;

import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

public class UpdateActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_update);

        Button checkButton = (Button) findViewById(R.id.check_update);
        final Button downloadButton = (Button) findViewById(R.id.download);

        final TextView updateDescription = (TextView) findViewById(R.id.update_description);
        final String url = "https://suneku.ru/check_update.php";

        checkButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateDescription.setText("Checking for updates...");

                new CheckForUpdate(updateDescription, downloadButton)
                        .execute(url);
            }
        });

    }

    private class CheckForUpdate extends AsyncTask<String, Void, String> {

        private TextView updateDesc;
        private Button downloadButton;

        CheckForUpdate(TextView updateDesc, Button downloadButton) {
            this.downloadButton = downloadButton;
            this.updateDesc = updateDesc;
        }

        @Override
        protected String doInBackground(String... link) {
            String response = "";
            try {
                URL url = new URL(link[0]);
                HttpsURLConnection con = (HttpsURLConnection) url.openConnection();

                con.setRequestMethod("POST");
                con.setRequestProperty("User-Agent", "Mozilla/5.0");
                con.setRequestProperty("Accept-Language", "en-US,en;q=0.5");

                String appVersion = BuildConfig.VERSION_NAME;

                String urlParameters = "mobile=yes&appversion=" + appVersion;

                con.setDoOutput(true);
                con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                OutputStream wr = new BufferedOutputStream(con.getOutputStream());
                wr.write(urlParameters.getBytes("UTF-8"));
                wr.flush();
                wr.close();

                BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    response += inputLine;
                }
                in.close();

            } catch (Exception e) {
                this.cancel(true);
            } finally {
                return response;
            }
        }

        @Override
        protected void onCancelled() {
            updateDesc.setText("No updates were found."); // FIXME: or some other error occurred...
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);

            try {
                final JSONObject jObject = new JSONObject(s);
                String desc = jObject.getString("description");
                updateDesc.setText(desc);
                downloadButton.setEnabled(true);
                downloadButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        try {
                            Intent app = new Intent(Intent.ACTION_VIEW, Uri.parse(jObject.getString("link")));
                            startActivity(app);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                });
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

}
