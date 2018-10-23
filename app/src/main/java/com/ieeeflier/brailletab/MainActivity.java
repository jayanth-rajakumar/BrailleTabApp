package com.ieeeflier.brailletab;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Environment;
import android.os.Message;
import android.speech.tts.TextToSpeech;
import android.os.StrictMode;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.Spinner;
import android.widget.Toast;
import android.app.Activity;
import java.io.InputStreamReader;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.InputStream;
import java.net.URL;
import org.json.JSONArray;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.os.Bundle;
import org.json.JSONException;
import org.json.JSONObject;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.List;
import android.widget.TextView;
import java.io.File;
import java.io.FileReader;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;


public class MainActivity extends AppCompatActivity {

    public static String notif_text="DefaultText",notif_desc="DefaultDesc",notif_pack="DefaultPack";

    List title_list=new ArrayList();
    List desc_list=new ArrayList();
    Integer list_ctr=0,list_size=0;
    Integer eBook_num=-1,ebook_index=-1,music_num=-1,music_index=-1;
    Timer timernav = new Timer();
    MyTask tasknav = new MyTask();
    File[] files;
    Integer music_toggle=0;
    File[] files2;
    MediaPlayer mediaPlayer = new MediaPlayer();
    TextToSpeech t1;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();

        StrictMode.setThreadPolicy(policy);

        //Update textViews for notifications every 2 seconds from the static public variables notif_text,notif_desc and notif_pack, which are updated from the NLService.java class.
        timernav.schedule(tasknav, 0, 2000);

        try{
            String path = Environment.getExternalStorageDirectory().toString()+"/Download/ebooks";
            File directory = new File(path);
            files = directory.listFiles();
            eBook_num=files.length;

            String path2 = Environment.getExternalStorageDirectory().toString()+"/Download/Music";
            File directory2 = new File(path2);
            files2 = directory2.listFiles();
            music_num=files2.length;
        }catch(Exception e){
            e.printStackTrace();
        }

        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        t1=new TextToSpeech(getApplicationContext(), new TextToSpeech.OnInitListener() {
            @Override
            public void onInit(int status) {
                if(status != TextToSpeech.ERROR) {
                    t1.setLanguage(Locale.US);
                }
            }
        });


    }

    public void SpeakOut(String S)
    {

        t1.speak(S, TextToSpeech.QUEUE_FLUSH, null);
    }

    public void GETNewsJSON(View view) {
        //Refer https://newsapi.org
        //Spinner s1 loads its values from the String-array called 'sources' in res/values/strings.xml
        //SpeakOut("Valar Morghulis");
        Spinner s1 = (Spinner)findViewById(R.id.spinner);

        String urlstr="https://newsapi.org/v1/articles?source=" + s1.getSelectedItem().toString() + "&apiKey=d3da03ce937e449d9193c52543693e95";
        String response = null;
        try
        {
            URL url=new URL(urlstr);

            //Adapted from https://www.androidhive.info/2012/01/android-json-parsing-tutorial/
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            conn.setRequestMethod("GET");

            InputStream in = new BufferedInputStream(conn.getInputStream());
            response = convertStreamToString(in);


            JSONObject jsonObj = new JSONObject(response);
            JSONArray articles = jsonObj.getJSONArray("articles");

            list_size=0;
            list_ctr=0;
            title_list.clear();
            desc_list.clear();
            for (int i = 0; i < articles.length(); i++) {
                JSONObject c = articles.getJSONObject(i);
                String title = c.getString("title");
                String desc = c.getString("description");
                title_list.add(title);
                desc_list.add(desc);
                list_size++;

            }

            TextView tv1 = (TextView)findViewById(R.id.textView2);
            tv1.setText(title_list.get(list_ctr).toString());
            TextView tv2 = (TextView)findViewById(R.id.textView);
            tv2.setText(desc_list.get(list_ctr).toString());
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }



    }

    private String convertStreamToString(InputStream is) {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();

        String line;
        try {
            while ((line = reader.readLine()) != null) {
                sb.append(line).append('\n');
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                is.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return sb.toString();
    }

    public void next_article(View view) {
        list_ctr++;
        if(list_ctr==list_size)
        {
            list_ctr=0;
        }
        TextView tv1 = (TextView)findViewById(R.id.textView2);
        tv1.setText(title_list.get(list_ctr).toString());
        TextView tv2 = (TextView)findViewById(R.id.textView);
        tv2.setText(desc_list.get(list_ctr).toString());

    }

    public void call_fn(View view) {
        Intent callIntent = new Intent(Intent.ACTION_CALL);
        callIntent.setData(Uri.parse("tel:29709021"));

        if (ActivityCompat.checkSelfPermission(MainActivity.this,
                Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        startActivity(callIntent);
    }

    public void ebook_fn(View view) {
        TextView tv4 = (TextView)findViewById(R.id.textView4);

        tv4.setMovementMethod(new ScrollingMovementMethod());

        ebook_index++;
        if(ebook_index==eBook_num)
            ebook_index=0;

        try{
            String data = getTextFileData(files[ebook_index].getName());
            tv4.setText((data));
        }catch(Exception e){
            e.printStackTrace();
        }



    }

    //Source: http://code2care.org/2015/read-text-file-from-sd-card-android-programming/
    public String getTextFileData(String fileName) {
        File sdCardDir = Environment.getExternalStorageDirectory();
        File txtFile = new File(sdCardDir, "Download/ebooks/" + fileName);

        StringBuilder text = new StringBuilder();
        try {
            BufferedReader reader = new BufferedReader(new FileReader(txtFile));
            String line;
            while ((line = reader.readLine()) != null) {
                text.append(line + '\n');
            }
            reader.close();
        } catch (Exception e) {

            e.printStackTrace();
        }

        return text.toString();

    }


    public void music_fn(View view) {

        if(music_toggle ==0)
        {
            try
            {
                music_index++;
                if(music_index==music_num)
                    music_index=0;

                Uri myUri =Uri.fromFile(new File(Environment.getExternalStorageDirectory().getPath() + "/download/Music/" + files2[music_index].getName()));


                mediaPlayer.setDataSource(getApplicationContext(), myUri);
                mediaPlayer.prepare();
                mediaPlayer.start();
                music_toggle=1;
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }
        else
        {
            mediaPlayer.stop();
            mediaPlayer.reset();
            music_toggle=0;
            music_fn(this.findViewById(android.R.id.content));
        }


    }

    public void stop_music_fn(View view) {

        mediaPlayer.stop();
        mediaPlayer.reset();
    }

    class MyTask extends TimerTask {
        @Override
        public void run() {

            runOnUiThread(new Runnable() {

                @Override
                public void run() {
                    TextView tv1 = (TextView)findViewById(R.id.textView5);
                    TextView tv2 = (TextView)findViewById(R.id.textView7);
                    TextView tv3 = (TextView)findViewById(R.id.textView6);
                    tv1.setText(notif_text);
                    tv2.setText(notif_desc);
                    tv3.setText(notif_pack);



                }
            });
        }

    }

}
