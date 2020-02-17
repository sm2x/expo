package expo.modules.videothumbnails;

import android.content.Context;
import android.graphics.Bitmap;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.webkit.URLUtil;

import org.unimodules.core.ExportedModule;
import org.unimodules.core.ModuleRegistry;
import org.unimodules.core.Promise;
import org.unimodules.core.arguments.ReadableArguments;
import org.unimodules.core.interfaces.ExpoMethod;
import org.unimodules.core.utilities.FileUtilities;
import org.unimodules.interfaces.filesystem.FilePermissionModuleInterface;
import org.unimodules.interfaces.filesystem.Permission;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class VideoThumbnailsModule extends ExportedModule {
  private static final String TAG = "ExpoVideoThumbnails";
  private static final String ERROR_TAG = "E_VIDEO_THUMBNAILS";
  private static String ERR_COULD_NOT_GET_THUMBNAIL = "ERR_COULD_NOT_GET_THUMBNAIL";

  private static final String KEY_QUALITY = "quality";
  private static final String KEY_TIME = "time";
  private static final String KEY_HEADERS = "headers";

  private ModuleRegistry mModuleRegistry;
  private File mCacheDir;

  public VideoThumbnailsModule(Context context) {
    super(context);
    mCacheDir = context.getCacheDir();
  }

  @Override
  public String getName() {
    return TAG;
  }

  @Override
  public void onCreate(ModuleRegistry moduleRegistry) {
    mModuleRegistry = moduleRegistry;
  }

  private static class GetThumbnailAsyncTask extends AsyncTask<Void, Void, Bitmap> {
    private String mSourceFilename;
    private ReadableArguments mVideoOptions;
    private File mCacheDir;
    Exception mError;

    GetThumbnailAsyncTask(String sourceFilename, ReadableArguments videoOptions, File cacheDir) {
      mSourceFilename = sourceFilename;
      mVideoOptions = videoOptions;
      mCacheDir = cacheDir;
    }

    @Override
    protected final Bitmap doInBackground(Void... voids) {
      long time = mVideoOptions.getInt(KEY_TIME, 0) * 1000;
      Map headers = mVideoOptions.getMap(KEY_HEADERS, new HashMap<String, String>());
      MediaMetadataRetriever retriever = new MediaMetadataRetriever();
      if (URLUtil.isFileUrl(mSourceFilename)) {
        retriever.setDataSource(Uri.decode(mSourceFilename).replace("file://", ""));
        return retriever.getFrameAtTime(time, MediaMetadataRetriever.OPTION_CLOSEST_SYNC);
      } else {
        File downloadedFile = download(Uri.decode(mSourceFilename), headers);
        if (downloadedFile == null) {
          return null;
        }
        retriever.setDataSource(downloadedFile.getAbsolutePath());
        Bitmap thumbnail = retriever.getFrameAtTime(time, MediaMetadataRetriever.OPTION_CLOSEST_SYNC);
        downloadedFile.delete();
        return thumbnail;
      }
    }

    private File download(String url, Map headers) {
      File output = new File(mCacheDir + File.separator + UUID.randomUUID().toString());
      HttpURLConnection httpURLConnection = null;
      try {
        httpURLConnection = (HttpURLConnection) new URL(url).openConnection();

        // add headers
        for (Object key : headers.keySet()) {
          httpURLConnection.setRequestProperty(key.toString(), headers.get(key).toString());
        }

        // get response and save it as a file
        try (DataInputStream dataInputStream = new DataInputStream(httpURLConnection.getInputStream());
             FileOutputStream fileOutputStream = new FileOutputStream(output)) {
          byte[] buffer = new byte[1024];
          int length;

          while ((length = dataInputStream.read(buffer)) > 0) {
            fileOutputStream.write(buffer, 0, length);
          }
        }

        return output;
      } catch (IOException | SecurityException e) {
        mError = e;
        return null;
      } finally {
        if (httpURLConnection != null) {
          httpURLConnection.disconnect();
        }
      }
    }
  }

  private boolean isAllowedToRead(String url) {
    if (mModuleRegistry != null) {
      FilePermissionModuleInterface permissionModuleInterface = mModuleRegistry.getModule(FilePermissionModuleInterface.class);
      if (permissionModuleInterface != null) {
        return permissionModuleInterface.getPathPermissions(getContext(), url).contains(Permission.READ);
      }
    }
    return true;
  }


  @ExpoMethod
  public void getThumbnail(String sourceFilename, final ReadableArguments videoOptions, final Promise promise) {
    if (URLUtil.isFileUrl(sourceFilename) && !isAllowedToRead(Uri.decode(sourceFilename).replace("file://", ""))) {
      promise.reject(ERROR_TAG, "Can't read file");
      return;
    }

    GetThumbnailAsyncTask getThumbnailAsyncTask = new GetThumbnailAsyncTask(sourceFilename, videoOptions, getContext().getCacheDir()) {
      @Override
      protected void onPostExecute(Bitmap thumbnail) {
        if (thumbnail == null || mError != null) {
          promise.reject(ERR_COULD_NOT_GET_THUMBNAIL, "Could not get thumbnail.", mError);
          return;
        }
        try {
          String path = FileUtilities.generateOutputPath(mCacheDir, "VideoThumbnails", "jpg");
          OutputStream outputStream = new FileOutputStream(path);
          thumbnail.compress(Bitmap.CompressFormat.JPEG, (int) (videoOptions.getDouble(KEY_QUALITY, 1) * 100), outputStream);
          outputStream.flush();
          outputStream.close();
          Bundle response = new Bundle();
          response.putString("uri", Uri.fromFile(new File(path)).toString());
          response.putInt("width", thumbnail.getWidth());
          response.putInt("height", thumbnail.getHeight());
          promise.resolve(response);
        } catch (IOException ex) {
          promise.reject(ERROR_TAG, ex);
        }
      }
    };
    getThumbnailAsyncTask.execute();
  }
}
