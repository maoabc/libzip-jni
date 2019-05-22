package mao.archive.libzip;

import androidx.annotation.Keep;

/**
 * Created by mao on 17-6-17.
 */

@Keep
public interface ProgressListener {
    @Keep
    void onProgressing(int percent);
}
