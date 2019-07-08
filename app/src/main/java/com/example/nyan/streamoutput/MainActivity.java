package com.example.nyan.streamoutput;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.MediaController;
import android.widget.Toast;
import android.widget.VideoView;

import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.*;
import org.apache.http.HttpResponseFactory;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;


import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {

    final Context context = this;
    private Pattern pattern;
    private Matcher matcher;
    private String IP_address;
    String IP_address_pattern = "^([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
            "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
            "([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\." +
            "([01]?\\d\\d?|2[0-4]\\d|25[0-5])$";
    private String login;
    private String password;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final EditText textIP = findViewById(R.id.textIP);
        final Button buttonSetConnection = findViewById(R.id.buttonSetConnect);
        //кнопка для получения snapshot
        final Button buttonSnapshot = findViewById(R.id.buttonSnapshot);
        buttonSnapshot.setVisibility(View.INVISIBLE);

        //Button buttonTurnOn = findViewById(R.id.buttonTurnOn);

        //получение видеопотока
            buttonSetConnection.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    IP_address = textIP.getText().toString();

                    boolean validate = validateIP_address(IP_address);
                    if(!validate){
                        Toast toast = Toast.makeText(context,"Неверный ip-адрес",Toast.LENGTH_SHORT);
                        toast.setGravity(Gravity.CENTER,0,0);
                        toast.show();
                    }
                    else {
                        LayoutInflater layoutInflater = LayoutInflater.from(context);
                        View authView = layoutInflater.inflate(R.layout.toast_authorization, null);

                        AlertDialog.Builder mDialogBuilder = new AlertDialog.Builder(context);
                        mDialogBuilder.setView(authView);
                        final EditText textLogin = authView.findViewById(R.id.textLogin);
                        final EditText textPassword = authView.findViewById(R.id.textPassword);

                        mDialogBuilder.setCancelable(false).setPositiveButton("Ок", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                buttonSnapshot.setVisibility(View.VISIBLE);
                                login = textLogin.getText().toString();
                                password = textPassword.getText().toString();
                                StringBuilder stringURI;
                                if (login.equals("") && password.equals("")) {
                                    stringURI = new StringBuilder();
                                    stringURI.append("rtsp://").append(IP_address).append("/axis-media/media.amp");
                                } else {
                                    stringURI = new StringBuilder();
                                    stringURI.append("rtsp://").append(login)
                                            .append(":").append(password)
                                            .append("@").append(IP_address).append("/axis-media/media.amp");
                                }
                                VideoView myVideoView = findViewById(R.id.videoView);
                                myVideoView.setVideoURI(Uri.parse(stringURI.toString()));
                                myVideoView.start();
                            }
                        }).setNegativeButton("Отмена", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                dialogInterface.cancel();
                            }
                        });
                        AlertDialog alertDialog = mDialogBuilder.create();
                        alertDialog.show();
                    }
                }
            });
//        //транслирует видео
//        //вывод с общей камеры
//        //String rtspPath = "rtsp://46.0.199.87/axis-media/media.amp";
//        //вывод с нашей камеры
//        String rtspPath = "rtsp://root:root@169.254.15.78/axis-media/media.amp";
//        //String rtspPath = "rtsp://root:root@169.254.14.71:554/axis-media/media.amp";
//
//        VideoView myVideoView = findViewById(R.id.videoView);
//        myVideoView.setVideoURI(Uri.parse(rtspPath));
//        //myVideoView.setMediaController(new MediaController(this));
//        //myVideoView.requestFocus();
//        myVideoView.start();

        //получение скриншота
        buttonSnapshot.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                IP_address = textIP.getText().toString();
                StringBuilder stringURL = new StringBuilder();
                if (login.equals("") && password.equals("")) {
                    stringURL.append("http://").append(IP_address).append("/jpg/image.jpg");
                } else {
                    stringURL.append("http://").append(login).append(":").append(password).append("@")
                            .append(IP_address).append("/jpg/image.jpg");
                }
                new DownloadImageTask((ImageView) findViewById(R.id.imageView)).execute(stringURL.toString());
            }
        });

//        //включение лампочки(разрабатывается)
//        buttonTurnOn.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                //включает лампочку
//                //String http = "http://root:root@192.168.0.101/axis-cgi/io/output.cgi?action=1:/";
//                //гаснет через 500мс, можно будет попробовать строку сразу пихать в execute
//                String http = "http://root:root@192.168.0.101/axis-cgi/io/output.cgi?action=1:/500"+"\\";
//                new RequestTask().execute(http);
//            }
//        });
    }

    //поток для включения лампочки
    private class RequestTask extends AsyncTask<String, String, String>{
        @Override
        protected String doInBackground(String... uri) {
            HttpClient httpclient = new DefaultHttpClient();
            HttpResponse response;
            String responseString = null;
            try {
                response = httpclient.execute(new HttpGet(uri[0]));
                StatusLine statusLine = response.getStatusLine();
                if(statusLine.getStatusCode() == HttpStatus.SC_OK){
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    response.getEntity().writeTo(out);
                    responseString = out.toString();
                    out.close();
                } else{
                    //закрываем соединение
                    response.getEntity().getContent().close();
                    throw new IOException(statusLine.getReasonPhrase());
                }
            } catch (ClientProtocolException e) {
                //TODO Handle problems..
            } catch (IOException e) {
                //TODO Handle problems..
            }
            return responseString;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            //Do anything with response..
        }
    }
    //поток для получения скриншота
    private class DownloadImageTask extends AsyncTask<String,Void,Bitmap> {
        ImageView bmImage;

        public DownloadImageTask(ImageView bmImage){
            this.bmImage = bmImage;
        }
        @Override
        protected Bitmap doInBackground(String... strings) {
            String urlDisplay = strings[0];
            Bitmap bitmap = null;
            try {
                InputStream in = new URL(urlDisplay).openStream();
                bitmap = BitmapFactory.decodeStream(in);
            } catch (Exception e) {
                Log.e("Ошибка передачи", e.getMessage());
                e.printStackTrace();
            }
            return bitmap;
        }
        protected void onPostExecute(Bitmap result) {
            bmImage.setImageBitmap(result);
        }
    }
    //проверка ip-адреса на корректность
    private boolean validateIP_address(final String textIP){
        pattern = Pattern.compile(IP_address_pattern);
        matcher = pattern.matcher(textIP);
        return matcher.matches();
    }
}
