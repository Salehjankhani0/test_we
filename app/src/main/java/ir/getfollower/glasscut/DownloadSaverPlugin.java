package ir.getfollower.glasscut;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Base64;

import com.getcapacitor.JSObject;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;

import java.io.OutputStream;

@CapacitorPlugin(name = "DownloadSaver")
public class DownloadSaverPlugin extends Plugin {
    @PluginMethod
    public void save(PluginCall call) {
        String name = call.getString("name");
        String mime = call.getString("mime", "application/octet-stream");
        String data = call.getString("data");
        if (name == null || data == null) {
            call.reject("missing arguments");
            return;
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            call.reject("legacy-android");
            return;
        }
        try {
            byte[] bytes = Base64.decode(data, Base64.DEFAULT);
            ContentResolver resolver = getContext().getContentResolver();
            ContentValues values = new ContentValues();
            values.put(MediaStore.MediaColumns.DISPLAY_NAME, name);
            values.put(MediaStore.MediaColumns.MIME_TYPE, mime);
            values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);
            Uri uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
            if (uri == null) {
                call.reject("insert-failed");
                return;
            }
            try (OutputStream os = resolver.openOutputStream(uri)) {
                if (os == null) {
                    call.reject("stream-failed");
                    return;
                }
                os.write(bytes);
                os.flush();
            }
            JSObject ret = new JSObject();
            ret.put("uri", uri.toString());
            ret.put("name", name);
            call.resolve(ret);
        } catch (Exception e) {
            call.reject("save-failed: " + e.getMessage());
        }
    }
}
