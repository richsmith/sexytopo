package org.hwyl.sexytopo.control.io.translation;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.content.Context;
import androidx.documentfile.provider.DocumentFile;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.hwyl.sexytopo.R;
import org.hwyl.sexytopo.model.survey.Survey;
import org.junit.Assert;
import org.junit.Test;

public class ImportManagerTest {

    private static class StubImporter extends Importer {

        private final String name;
        private final boolean canHandle;
        final Survey survey = new Survey();

        StubImporter(String name, boolean canHandle) {
            this.name = name;
            this.canHandle = canHandle;
        }

        @Override
        public Survey toSurvey(Context context, DocumentFile file) {
            return survey;
        }

        @Override
        public boolean canHandleFile(DocumentFile file) {
            return canHandle;
        }

        @Override
        public String getName() {
            return name;
        }
    }

    private final Context context = mock(Context.class);
    private final DocumentFile file = mock(DocumentFile.class);

    @Test
    public void testFailsWhenNothingRecognisesTheFile() {
        FakeImportChooser chooser = new FakeImportChooser(0);
        RecordingImportCallback callback = new RecordingImportCallback();

        ImportManager.importSurvey(
                context,
                file,
                chooser,
                callback,
                Collections.singletonList(new StubImporter("A", false)));

        Assert.assertNotNull(callback.exception);
        Assert.assertNull(callback.survey);
        Assert.assertTrue(chooser.titles.isEmpty());
    }

    @Test
    public void testSingleMatchImportsWithoutAsking() {
        StubImporter wanted = new StubImporter("B", true);
        FakeImportChooser chooser = new FakeImportChooser(0);
        RecordingImportCallback callback = new RecordingImportCallback();

        ImportManager.importSurvey(
                context,
                file,
                chooser,
                callback,
                Arrays.asList(new StubImporter("A", false), wanted));

        Assert.assertSame(wanted.survey, callback.survey);
        Assert.assertTrue(chooser.titles.isEmpty());
    }

    @Test
    public void testSeveralMatchesAsksWhichFormat() {
        StubImporter first = new StubImporter("Therion", true);
        StubImporter second = new StubImporter("Survex", true);
        FakeImportChooser chooser = new FakeImportChooser(1);
        RecordingImportCallback callback = new RecordingImportCallback();

        ImportManager.importSurvey(context, file, chooser, callback, Arrays.asList(first, second));

        Assert.assertEquals(
                Collections.singletonList(R.string.import_choose_format), chooser.titles);
        Assert.assertEquals(Arrays.asList("Therion", "Survex"), chooser.optionsAsked.get(0));
        Assert.assertSame(second.survey, callback.survey);
    }

    @Test
    public void testFindFilesMatchesExtensionOnly() {
        DocumentFile svx = mockFile("cave.svx");
        DocumentFile backup = mockFile("cave.svx.bak");
        DocumentFile noDot = mockFile("notsvx");
        DocumentFile subdirectory = mock(DocumentFile.class);
        when(subdirectory.isFile()).thenReturn(false);
        when(subdirectory.getName()).thenReturn("sub.svx");
        DocumentFile directory = mock(DocumentFile.class);
        when(directory.listFiles())
                .thenReturn(new DocumentFile[] {svx, backup, noDot, subdirectory});

        List<DocumentFile> found = Importer.findFiles(directory, "svx");

        Assert.assertEquals(Collections.singletonList(svx), found);
    }

    private static DocumentFile mockFile(String name) {
        DocumentFile file = mock(DocumentFile.class);
        when(file.isFile()).thenReturn(true);
        when(file.getName()).thenReturn(name);
        return file;
    }
}
