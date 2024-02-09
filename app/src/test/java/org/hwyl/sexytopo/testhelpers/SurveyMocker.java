package org.hwyl.sexytopo.testhelpers;

import android.net.Uri;

import androidx.documentfile.provider.DocumentFile;

import org.hwyl.sexytopo.model.survey.Survey;
import org.mockito.Mockito;

public class SurveyMocker {
    public static void mockSurveyUri(Survey survey, String uri) {
        Uri mockUri = Mockito.mock(Uri.class);
        Mockito.when(mockUri.toString()).thenReturn(uri);
        DocumentFile mockDocumentFile = Mockito.mock(DocumentFile.class);
        Mockito.when(mockDocumentFile.getUri()).thenReturn(mockUri);
        Mockito.when(mockDocumentFile.getName()).thenReturn(uri);
        survey.setDirectory(mockDocumentFile);
    }
}
