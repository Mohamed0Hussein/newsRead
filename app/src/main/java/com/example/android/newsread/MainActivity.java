package com.example.android.newsread;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    SQLiteDatabase database;
    Intent intent;
    public class DownLoader extends AsyncTask<String,Void,String>{

        @Override
        protected String doInBackground(String... urls) {
            String res="";
            HttpURLConnection httpURLConnection = null;
            URL url;
            try {
                url = new URL(urls[0]);
                httpURLConnection = (HttpURLConnection) url.openConnection();
                InputStream inputStream = httpURLConnection.getInputStream();
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                int x = inputStreamReader.read();
                while (x != -1) {
                    res += (char) x;
                    x = inputStreamReader.read();
                }

                JSONArray jsonArray = new JSONArray(res);
                int noOfItems = 20;
                if(jsonArray.length()<20){
                    noOfItems = jsonArray.length();
                }
                database.execSQL("DELETE FROM Articles");
                for(int i=0;i<noOfItems;i++) {
                    String ArticleId = jsonArray.getString(i);
                    url = new URL("https://hacker-news.firebaseio.com/v0/item/" + ArticleId + ".json?print=pretty");
                    httpURLConnection = (HttpURLConnection) url.openConnection();
                    inputStream = httpURLConnection.getInputStream();
                    inputStreamReader = new InputStreamReader(inputStream);
                    int y = inputStreamReader.read();
                    String article = "";
                    while (y != -1) {
                        article += (char) y;
                        y = inputStreamReader.read();
                    }


                    JSONObject jsonObject = new JSONObject(article);
                    if(!jsonObject.isNull("title")&&!jsonObject.isNull("url")){
                        String articleTitle = jsonObject.getString("title");
                        String articleUrl = jsonObject.getString("url");
                        String articleId = jsonObject.getString("id");
                        url = new URL(articleUrl);
                        httpURLConnection = (HttpURLConnection) url.openConnection();
                        inputStream = httpURLConnection.getInputStream();
                        inputStreamReader = new InputStreamReader(inputStream);
                        x = inputStreamReader.read();
                        String articleContent = "";
                        while (x!=-1){
                            articleContent+=(char)x;
                            x = inputStreamReader.read();
                        }

                        String sql = "INSERT INTO Articles (articleId,title,ArticleContent) VALUES(?,?,?)";
                        SQLiteStatement statement = database.compileStatement(sql);
                        statement.bindString(1,articleId);
                        statement.bindString(2,articleTitle);
                        statement.bindString(3,articleContent);
                        statement.execute();

                    }
                }
            }catch (Exception e){e.printStackTrace();}
            return res;
        }

        @Override
        protected void onPostExecute(String s) {
            super.onPostExecute(s);
            updateListView();
        }
    }
    ListView lv;
    ArrayList<String> titles;
    ArrayList<String> contents;
    ArrayAdapter arrayAdapter;
    public void updateListView(){
        Cursor c = database.rawQuery("SELECT * FROM Articles",null);
        int Content = c.getColumnIndex("ArticleContent");
        int Title = c.getColumnIndex("title");
        if(c.moveToFirst()){
            do{
                titles.add(c.getString(Title));
                contents.add(c.getString(Content));
            }while (c.moveToNext());
        }
        arrayAdapter.notifyDataSetChanged();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            setContentView(R.layout.activity_main);
            lv = (ListView) findViewById(R.id.news);
            intent = new Intent(getApplicationContext(), ArticleActivity.class);
            titles = new ArrayList<String>();
            contents = new ArrayList<String>();
            arrayAdapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, titles);
            lv.setAdapter(arrayAdapter);
            lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                    intent.putExtra("content", contents.get(i));
                    startActivity(intent);
                }
            });
            database = this.openOrCreateDatabase("Articles",MODE_PRIVATE,null);
            database.execSQL("CREATE TABLE IF NOT EXISTS articles (id INTEGER PRIMARY KEY,articleId INTEGER,title VARCHAR,ArticleContent VARCHAR)");
            updateListView();
        }catch (Exception e){e.printStackTrace();}
        DownLoader downLoader = new DownLoader();
        try{
            downLoader.execute("https://hacker-news.firebaseio.com/v0/topstories.json?print=pretty");
        }catch (Exception e){e.printStackTrace();}
    }
}