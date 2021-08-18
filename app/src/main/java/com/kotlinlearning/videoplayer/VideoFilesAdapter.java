package com.kotlinlearning.videoplayer;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Bundle;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.io.File;
import java.util.ArrayList;

public class VideoFilesAdapter extends RecyclerView.Adapter<VideoFilesAdapter.ViewHolder> {

    private ArrayList<MediaFiles> videoList;
    private Context context;
    BottomSheetDialog bottomSheetDialog;

    public VideoFilesAdapter(ArrayList<MediaFiles> videoList, Context context) {
        this.videoList = videoList;
        this.context = context;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.video_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VideoFilesAdapter.ViewHolder holder, @SuppressLint("RecyclerView") int position) {

        holder.videoName.setText(videoList.get(position).getDisplayName());
        String size=videoList.get(position).getSize();
        holder.videoSize.setText(android.text.format.Formatter.formatFileSize(context,
                Long.parseLong(size)));
        double milliSeconds = Double.parseDouble(videoList.get(position).getDuration());
        holder.videoDuration.setText(timeConversion((long) milliSeconds));

        Glide.with(context).load(new File(videoList.get(position).getPath())).into(holder.thumbnail);//check



        holder.menu_more.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                bottomSheetDialog=new BottomSheetDialog(context,R.style.BottomSheetTheme);
                View bsview= LayoutInflater.from(context).inflate(R.layout.video_bs_layout,
                        v.findViewById(R.id.bottom_sheet));
                bsview.findViewById(R.id.bs_play).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        holder.itemView.performClick();
                        bottomSheetDialog.dismiss();
                    }
                });
                bsview.findViewById(R.id.bs_rename).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        AlertDialog.Builder alertdialog = new AlertDialog.Builder(context);
                        alertdialog.setTitle("Rename to");
                        EditText editText = new EditText(context);
                        String path =videoList.get(position).getPath();
                        final File file =new File(path);
                        String videoname = file.getName();
                        videoname = videoname.substring(0,videoname.lastIndexOf("."));
                        editText.setText(videoname);
                        alertdialog.setView(editText);
                        editText.requestFocus();

                        alertdialog.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                String onlyPath = file.getParentFile().getAbsolutePath();
                                String ext = file.getAbsolutePath();
                                ext = ext.substring(ext.lastIndexOf("."));
                                String newPath = onlyPath+"/"+editText.getText().toString()+ext;
                                File newFile = new File(newPath);
                                boolean rename = file.renameTo(newFile);
                                if (rename) {
                                    ContentResolver resolver = context.getApplicationContext().getContentResolver();
                                    resolver.delete(MediaStore.Files.getContentUri("external"),
                                            MediaStore.MediaColumns.DATA+"=?",new String[]
                                                    {file.getAbsolutePath()});
                                                     Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                                                     intent.setData(Uri.fromFile(newFile));
                                                     context.getApplicationContext().sendBroadcast(intent);
                                                     notifyDataSetChanged();
                                                     Toast.makeText(context,"Video Renamed",Toast.LENGTH_SHORT).show();

                                    SystemClock.sleep(200);
                                    ((Activity) context).recreate();

                                }

                                else{
                                    Toast.makeText(context, "Process Failed", Toast.LENGTH_SHORT).show();
                                }

                            }
                        });
                        alertdialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                dialogInterface.dismiss();
                            }
                        });
                        alertdialog.create().show();
                        bottomSheetDialog.dismiss();
                    }
                });


                bsview.findViewById(R.id.bs_share).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Uri uri=Uri.parse(videoList.get(position).getPath());
                        Intent shareIntent= new Intent(Intent.ACTION_SEND);
                        shareIntent.setType("video/*");
                        shareIntent.putExtra(Intent.EXTRA_STREAM,uri);
                        context.startActivity(Intent.createChooser(shareIntent,"Share Video via"));
                        bottomSheetDialog.dismiss();
                    }
                });


                bsview.findViewById(R.id.bs_delete).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        AlertDialog.Builder alertDialog=new AlertDialog.Builder(context);
                        alertDialog.setTitle("Delete");
                        alertDialog.setMessage("Do you want to delete this video");
                        alertDialog.setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                Uri contentUri= ContentUris
                                        .withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                                                Long.parseLong(videoList.get(position).getId()));
                                File file=new File(videoList.get(position).getPath());
                                boolean delete=file.delete();
                                if(delete){
                                    context.getContentResolver().delete(contentUri,null,null);
                                    videoList.remove(position);
                                    notifyItemRemoved(position);
                                    notifyItemRangeChanged(position,videoList.size());
                                    Toast.makeText(context,"Deleted",Toast.LENGTH_SHORT).show();
                                }else {
                                    Toast.makeText(context,"can't delete",Toast.LENGTH_SHORT).show();
                                }

                            }
                        });
                        alertDialog.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        });
                        alertDialog.show();
                        bottomSheetDialog.dismiss();
                    }
                });

                bsview.findViewById(R.id.bs_properties).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        AlertDialog.Builder alertdailog = new AlertDialog.Builder(context);
                        alertdailog.setTitle("Properties");

                        String one = "File: "+videoList.get(position).getDisplayName();
                        String path = videoList.get(position).getPath();
                        int indexOfPath = path.lastIndexOf("/");
                        String two = "Path :"+path.substring(0,indexOfPath);
                        String three = "Size :"+android.text.format.Formatter.formatFileSize(context, Long.parseLong(videoList.get(position).getSize()));
                        String four = "leength :" + timeConversion((long) milliSeconds);
                        String nameWithFormat = videoList.get(position).getDisplayName();
                        int index = nameWithFormat.lastIndexOf(".");
                        String format = nameWithFormat.substring(index+1);
                        String five = "Format :"+format;
                        MediaMetadataRetriever mediaMetadataRetriever = new MediaMetadataRetriever();
                        mediaMetadataRetriever.setDataSource(videoList.get(position).getPath());
                        String height = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT);
                        String width = mediaMetadataRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH);
                        String six = "Resolution :"+width+"x"+height;


                        alertdailog.setMessage(one+"\n\n"+two+"\n\n"+three+"\n\n"+four+"\n\n"+five+"\n\n"+six);
                        alertdailog.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                dialogInterface.dismiss();
                            }
                        });
                        alertdailog.show();
                        bottomSheetDialog.dismiss();
 ;
                    }
                });


                bottomSheetDialog.setContentView(bsview);
                bottomSheetDialog.show();
            }
        });
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent=new Intent(context,VideoPlayerActivity.class);
                intent.putExtra("position",position);
                intent.putExtra("video_title",videoList.get(position).getDisplayName());
                Bundle bundle=new Bundle();
                bundle.putParcelableArrayList("videoArrayList",videoList);
                intent.putExtras(bundle);
                context.startActivity(intent);
            }
        });
    }

    @Override
    public int getItemCount() {
        return videoList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        ImageView thumbnail,menu_more;
        TextView videoName,videoSize,videoDuration;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            thumbnail=itemView.findViewById(R.id.thumbnail);
            menu_more=itemView.findViewById(R.id.video_menu_more);
            videoName=itemView.findViewById(R.id.video_name);
            videoSize=itemView.findViewById(R.id.video_size);
            videoDuration=itemView.findViewById(R.id.video_duration);
        }
    }
    public String timeConversion(long value){
        String videoTime;
        int duration = (int) value;
        int hrs=(duration/3600000); //milli sec to hours
        int mns=(duration/60000)%60000; //milli sec to minutes
        int scs=(duration%60000/1000); //milli sec to sec
        if (hrs>0){
            videoTime=String.format("%02d:%02d:%02d",hrs,mns,scs);
        }
        else {
            videoTime=String.format("%02d:%02d",mns,scs);
        }
        return videoTime;
    }

}
