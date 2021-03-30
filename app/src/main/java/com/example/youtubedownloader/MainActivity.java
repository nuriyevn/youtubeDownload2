package com.example.youtubedownloader;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.StrictMode;
import android.util.Log;

import com.github.kiulian.downloader.OnYoutubeDownloadListener;
import com.github.kiulian.downloader.YoutubeDownloader;
import com.github.kiulian.downloader.YoutubeException;
import com.github.kiulian.downloader.model.Extension;
import com.github.kiulian.downloader.model.VideoDetails;
import com.github.kiulian.downloader.model.YoutubeVideo;
import com.github.kiulian.downloader.model.formats.AudioVideoFormat;
import com.github.kiulian.downloader.model.formats.Format;
import com.github.kiulian.downloader.model.formats.VideoFormat;
import com.github.kiulian.downloader.model.playlist.YoutubePlaylist;
import com.github.kiulian.downloader.model.quality.VideoQuality;
import com.github.kiulian.downloader.model.subtitles.Subtitles;
import com.github.kiulian.downloader.model.subtitles.SubtitlesInfo;
import com.github.kiulian.downloader.parser.Parser;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "main";


    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1) {

            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED)

                    Log.i(TAG, "WRITE_EXTERNAL_STORAGE  == PERMISSION_GRANTED");
                //locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
                //new DownloadFileFromURL().execute(file_url);
            }
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        if (Build.VERSION.SDK_INT >= 23) {
            if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Permission is granted");
                //new DownloadFileFromURL().execute(file_url);
            } else {

                ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
            }
        } else {
            //new DownloadFileFromURL().execute(file_url);
        }








// init downloader
        YoutubeDownloader downloader = new YoutubeDownloader();

// you can easly implement or extend default parsing logic
       // YoutubeDownloader downloader = new YoutubeDownloader(new Parser());
// downloader configurations
        downloader.setParserRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/72.0.3626.121 Safari/537.36");
        downloader.setParserRetryOnFailure(1);

// parsing data
        String videoId = "VyPeZdPgI4E"; // for url https://www.youtube.com/watch?v=abc12345
        YoutubeVideo video = null;
        try {
            video = downloader.getVideo(videoId);
        } catch (YoutubeException e) {
            e.printStackTrace();
        }

// video details
        VideoDetails details = video.details();
        System.out.println(details.title());

        System.out.println(details.viewCount());
        details.thumbnails().forEach(image -> System.out.println("Thumbnail: " + image));

// get videos with audio
        List<AudioVideoFormat> videoWithAudioFormats = video.videoWithAudioFormats();
        videoWithAudioFormats.forEach(it -> {
            System.out.println(it.audioQuality() + " : " + it.url());
        });



// filtering only video formats
        List<VideoFormat> videoFormats = video.findVideoWithQuality(VideoQuality.hd720);
        videoFormats.forEach(it -> {
            System.out.println(it.videoQuality() + " : " + it.url());
        });

// itags can be found here - https://gist.github.com/sidneys/7095afe4da4ae58694d128b1034e01e2
        Format formatByItag;
        formatByItag = video.findFormatByItag(136);
        if (formatByItag != null) {
            System.out.println(formatByItag.url());
        }

        File outputDir = new File(Environment
                .getExternalStorageDirectory().toString() + "/my_videos");
        Format format = videoFormats.get(0);

// sync downloading
        /*try {
            File file = video.download(format, outputDir);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (YoutubeException e) {
            e.printStackTrace();
        }*/



// async downloading with callback
        try {
            Future<File> future = video.downloadAsync(videoFormats.get(0), outputDir, new OnYoutubeDownloadListener() {
                @Override
                public void onDownloading(int progress) {
                     System.out.printf("Downloaded %d%%\n", progress);
                }

                @Override
                public void onFinished(File file) {
                    System.out.println("Finished file: " + file);
                }

                @Override
                public void onError(Throwable throwable) {
                    System.out.println("Error: " + throwable.getLocalizedMessage());
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        } catch (YoutubeException e) {
            e.printStackTrace();
        }
/*
// async downloading without callback
        Future<File> future = null;
        try {
            future = video.downloadAsync(format, outputDir);
        } catch (YoutubeException.LiveVideoException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        File file = null;
        try {
            file = future.get(5, TimeUnit.SECONDS);
        } catch (ExecutionException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (TimeoutException e) {
            e.printStackTrace();
        }

// cancel downloading
        future.cancel(true); // true is required to interrupt downloading thread

// live videos and streams
        if (video.details().isLive()) {
            System.out.println("Live Stream HLS URL: " + video.details().liveUrl());
        }

// naming
// by default file name will be same as video title on youtube,
// but you can specify output file name
        File myAwesomeFile = video.download(format, outputDir, "myAwesomeName");
        System.out.println(file.getName()); // myAwesomeName.mp4
// if file with such name already exits sufix will be added myAwesomeFile(1).mp4
// you may disable this feature by passing overwrite flag
        File myAwesomeFile = video.download(format, outputDir, "myAwesomeName", true);

// subtitles
// you can get subtitles from video captions if you have already parsed video meta
        List<SubtitlesInfo> subtitles = video.subtitles(); // NOTE: includes auto-generated
// if you don't need video meta, but just subtitles use this instead
        List<SubtitlesInfo> subtitles = downloader.getVideoSubtitles(videoId); // NOTE: does not include auto-generated

        for (SubtitlesInfo info : subtitles) {
            Subtitles subtitles = info.getSubtitles()
                    .formatTo(Extension.JSON3)
                    .translateTo("uk"); // // NOTE: subtitle translation supported only for "subtitles from captions"
            // sync download
            String subtitlesData = subtitles.download();
            // async download
            Future<String> subtitlesFuture = subtitles.downloadAsync();
            // to download using external download manager
            String downloadUrl = subtitles.getDownloadUrl();
        }


// playlists

// parsing data
        String playlistId = "abc12345"; // for url https://www.youtube.com/playlist?list=abc12345
        YoutubePlaylist playlist = downloader.getPlaylist(playlistId);

// playlist details
        PlaylistDetails details = playlist.details();
        System.out.println(details.title());
...
        System.out.println(details.videoCount());

// get video details
        PlaylistVideoDetails videoDetails = playlist.videos().get(0);
        System.out.println(videoDetails.title());
...
        System.out.println(videoDetails.index());

// get video
        YoutubeVideo video = downloader.getVideo(videoDetails.videoId());
*/


    }
}