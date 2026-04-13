package org.hwyl.sexytopo.control.io.share;

import android.content.Context;
import android.net.Uri;
import org.hwyl.sexytopo.model.survey.Survey;

public interface Shareable {
    String getShareTypeName(Context context);

    String getMimeType();

    Uri buildShareUri(Context context, Survey survey) throws Exception;
}
