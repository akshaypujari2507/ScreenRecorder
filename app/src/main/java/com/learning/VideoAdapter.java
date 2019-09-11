package com.learning;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.AsyncTask;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.io.Console;
import java.io.File;
import java.util.ArrayList;

public class VideoAdapter extends RecyclerView.Adapter<VideoAdapter.MyViewHolder> {

    ArrayList<File> files;
    Context context;

    public VideoAdapter(ArrayList<File> files, Context context) {
        this.files = files;
        this.context = context;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new MyViewHolder(LayoutInflater.from(context).inflate(R.layout.video_list_item, parent));
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, final int position) {
        new VideoAsyncTask(holder.thumbnail).execute(files.get(position).getPath());
        holder.title.setText(files.get(position).getName());
        holder.play.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(Uri.parse(files.get(position).getPath()), "video/mp4");
                context.startActivity(intent);
            }
        });
        holder.delete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                files.get(position).delete();
                files.remove(files.get(position));
                notifyDataSetChanged();
            }
        });
    }

    @Override
    public int getItemCount() {
        return files.size();
    }

    public class MyViewHolder extends RecyclerView.ViewHolder {
        ImageView play, delete, thumbnail;
        TextView title;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.video_title);
            play = itemView.findViewById(R.id.play_video);
            delete = itemView.findViewById(R.id.delete);
            thumbnail = itemView.findViewById(R.id.video_thumbnail);
        }
    }

    public class VideoAsyncTask extends AsyncTask<String, Void, Bitmap> {

        ImageView imageView;

        public VideoAsyncTask(ImageView imageView) {
            this.imageView = imageView;
        }

        @Override
        protected Bitmap doInBackground(String... strings) {
            try {

                MediaMetadataRetriever mediaMetadataRetriever = new MediaMetadataRetriever();
                mediaMetadataRetriever.setDataSource(strings[0]);
                return mediaMetadataRetriever.getFrameAtTime(2000, MediaMetadataRetriever.OPTION_CLOSEST_SYNC);
            } catch (Exception e) {
                System.out.println("Exception :" + e);
                return null;
            }

        }

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            super.onPostExecute(bitmap);
            if (bitmap == null) {
                imageView.setImageBitmap(BitmapFactory.decodeResource(context.getResources(), R.mipmap.ic_launcher));
            } else {
                imageView.setImageBitmap(bitmap);
            }
        }
    }

}
