package com.example.ui;

import android.app.Activity;
import android.os.Bundle;
import android.text.format.Formatter;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import java.util.ArrayList;
import download.http.core.Http;
import download.http.listener.JsonReaderListCallback;
import download.otherFileLoader.db.DownFileManager;
import download.otherFileLoader.listener.DownloadListener;
import download.otherFileLoader.request.DownFile;
import download.otherFileLoader.util.ToastUtils;

public class FileDownloadActivity extends Activity implements View.OnClickListener{


    DownFileManager mDownloadManager;

    Button pauseall;


    private ListView mDownloadLsv;
    private DownloadAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_filedownload);
        mDownloadManager = DownFileManager.getInstance(this);

        pauseall = (Button) findViewById(R.id.pauseall);
        pauseall.setOnClickListener(this);

        mDownloadLsv = (ListView) findViewById(R.id.mDownloadLsv);
        mDownloadLsv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

            }
        });

        String url = "http://api.stay4it.com/v1/public/core/?service=downloader.applist";
        Http.with(this).url(url).callback(new JsonReaderListCallback<AppEntry>("data") {
            @Override
            public ArrayList<AppEntry> onPost(ArrayList<AppEntry> appEntries) {
                for (int i = 0; i < appEntries.size(); i++) {

                    DownFile downFile = DownFileManager.getInstance(FileDownloadActivity.this).initData
                            (appEntries.get(i).url, null);
                    if (downFile != null){
                        appEntries.get(i).downLength = downFile.downLength;
                        appEntries.get(i).totalLength = downFile.totalLength;
                        appEntries.get(i).state = downFile.state;
                    }
                }
                return super.onPost(appEntries);
            }

            @Override
            public void onSuccess(ArrayList<AppEntry> result) {
                Log.e("test", "" + result.size());

                adapter = new DownloadAdapter(result);
                mDownloadLsv.setAdapter(adapter);

                for (final AppEntry entry:result
                     ) {
                    if (entry.state == DownFile.DownloadStatus.DOWNLOADING || entry.state == DownFile.DownloadStatus.WAITING || entry.state == DownFile.DownloadStatus.ERROR){
                        DownFileManager.getInstance(FileDownloadActivity.this).download(entry.url,getDownloadListener(entry));
                    }
                }
            }
        }).get();
    }
    Boolean isVisiable = false;

    @Override
    protected void onPause() {
        super.onPause();
        isVisiable = false;
    }

    @Override
    protected void onResume() {
        super.onResume();
        isVisiable = true;
    }


    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.pauseall:
                if (pauseall.getText().equals("pauseall")){
                    pauseall.setText("recoverall");
                    mDownloadManager.pauseAll();
                }else {
                    pauseall.setText("pauseall");
                    mDownloadManager.recoverAll();
                }
                break;
        }

    }
    class DownloadAdapter extends BaseAdapter {

        public ArrayList<AppEntry> applist;
        public DownloadAdapter(ArrayList<AppEntry> list){
            this.applist = list;
        }

        private ViewHolder holder;

        @Override
        public int getCount() {
            return applist != null ? applist.size() : 0;
        }

        @Override
        public Object getItem(int position) {
            return applist.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null || convertView.getTag() == null) {
                convertView = LayoutInflater.from(FileDownloadActivity.this).inflate(R.layout.activity_applist_item, null);
                holder = new ViewHolder();
                holder.mDownloadBtn = (Button) convertView.findViewById(R.id.mDownloadBtn);
                holder.mDownloadLabel = (TextView) convertView.findViewById(R.id.mDownloadLabel);
                holder.mDownloadStatusLabel = (TextView) convertView.findViewById(R.id.mDownloadStatusLabel);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }
            final AppEntry entry = applist.get(position);



            holder.mDownloadLabel.setText(entry.name + "  " + entry.size + "\n" + entry.desc);

            holder.mDownloadStatusLabel.setText(entry.state + "\n"
                    + Formatter.formatShortFileSize(getApplicationContext(), entry.downLength)
                    + "/" + Formatter.formatShortFileSize(getApplicationContext(), entry.totalLength));
            holder.mDownloadBtn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (entry.state != DownFile.DownloadStatus.DOWNLOADING && entry.state != DownFile.DownloadStatus.FINISH && entry.state != DownFile.DownloadStatus.WAITING) {
                        DownFileManager.getInstance(FileDownloadActivity.this).download(entry.url,getDownloadListener(entry));
                    } else if (entry.state == DownFile.DownloadStatus.FINISH) {
                        //完成
                    } else if (entry.state == DownFile.DownloadStatus.DOWNLOADING || entry.state == DownFile.DownloadStatus.WAITING) {
                        DownFileManager.getInstance(FileDownloadActivity.this).pause(entry.url);
                    }
                }
            });
            return convertView;
        }
    }
    public DownloadListener getDownloadListener(final AppEntry entry){

        return new DownloadListener() {
            @Override
            public void success(String path) {
                entry.state = DownFile.DownloadStatus.FINISH;
                adapter.notifyDataSetChanged();
                ToastUtils.showToast(FileDownloadActivity.this,"已完成"+path);
            }

            @Override
            public void progress(int currentLen, int totalLen) {
                if (!isVisiable){
                    return;
                }
                entry.downLength = currentLen;
                entry.totalLength = totalLen;
                entry.state = DownFile.DownloadStatus.DOWNLOADING;
                adapter.notifyDataSetChanged();
            }

            @Override
            public void error() {
                entry.state = DownFile.DownloadStatus.ERROR;
                adapter.notifyDataSetChanged();
            }

            @Override
            public void waiting() {
                entry.state = DownFile.DownloadStatus.WAITING;
                adapter.notifyDataSetChanged();
            }

            @Override
            public void pause() {
                entry.state = DownFile.DownloadStatus.PAUSE;
                adapter.notifyDataSetChanged();
            }

            @Override
            public void cancel() {
                entry.state = DownFile.DownloadStatus.CANCEL;
                adapter.notifyDataSetChanged();
            }
        };
    }

    static class ViewHolder {
        TextView mDownloadLabel;
        TextView mDownloadStatusLabel;
        Button mDownloadBtn;
    }

}
