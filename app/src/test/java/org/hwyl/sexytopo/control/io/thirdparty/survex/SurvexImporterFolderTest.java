package org.hwyl.sexytopo.control.io.thirdparty.survex;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.content.Context;
import androidx.documentfile.provider.DocumentFile;
import java.util.Arrays;
import java.util.Collections;
import org.hwyl.sexytopo.R;
import org.hwyl.sexytopo.control.io.translation.FakeImportChooser;
import org.hwyl.sexytopo.control.io.translation.RecordingImportCallback;
import org.junit.Assert;
import org.junit.Test;

public class SurvexImporterFolderTest {

    private final SurvexImporter importer = new SurvexImporter();

    private static DocumentFile mockFile(String name) {
        DocumentFile file = mock(DocumentFile.class);
        when(file.isFile()).thenReturn(true);
        when(file.isDirectory()).thenReturn(false);
        when(file.getName()).thenReturn(name);
        return file;
    }

    private static DocumentFile mockDirectory(DocumentFile... contents) {
        DocumentFile dir = mock(DocumentFile.class);
        when(dir.isFile()).thenReturn(false);
        when(dir.isDirectory()).thenReturn(true);
        when(dir.getName()).thenReturn("export");
        when(dir.listFiles()).thenReturn(contents);
        return dir;
    }

    private static Context mockContext() {
        Context context = mock(Context.class);
        when(context.getString(R.string.import_espec_none)).thenReturn("None");
        return context;
    }

    @Test
    public void testCanHandleSvxFile() {
        Assert.assertTrue(importer.canHandleFile(mockFile("cave.svx")));
    }

    @Test
    public void testCannotHandleOtherFile() {
        Assert.assertFalse(importer.canHandleFile(mockFile("cave.th")));
    }

    @Test
    public void testCanHandleFolderContainingSvx() {
        Assert.assertTrue(
                importer.canHandleFile(
                        mockDirectory(mockFile("cave.svx"), mockFile("cave.espec"))));
    }

    @Test
    public void testCannotHandleFolderWithoutSvx() {
        Assert.assertFalse(importer.canHandleFile(mockDirectory(mockFile("cave.th"))));
    }

    @Test
    public void testFindMatchingEspecBySameBaseName() {
        DocumentFile espec = mockFile("cave.espec");
        DocumentFile match =
                SurvexImporter.findMatchingEspec(
                        mockFile("cave.svx"), Arrays.asList(mockFile("other.espec"), espec));
        Assert.assertSame(espec, match);
    }

    @Test
    public void testFindMatchingEspecReturnsNullWithoutMatch() {
        Assert.assertNull(
                SurvexImporter.findMatchingEspec(
                        mockFile("cave.svx"), Collections.singletonList(mockFile("other.espec"))));
    }

    @Test
    public void testSeveralSvxFilesAsksWhichOne() {
        DocumentFile directory = mockDirectory(mockFile("a.svx"), mockFile("b.svx"));
        FakeImportChooser chooser = new FakeImportChooser(1);

        importer.importSurvey(mockContext(), directory, chooser, new RecordingImportCallback());

        Assert.assertEquals(R.string.import_choose_svx_file, (int) chooser.titles.get(0));
        Assert.assertEquals(Arrays.asList("a.svx", "b.svx"), chooser.optionsAsked.get(0));
    }

    @Test
    public void testSingleSvxWithMatchingEspecAsksNothing() {
        DocumentFile directory = mockDirectory(mockFile("cave.svx"), mockFile("cave.espec"));
        FakeImportChooser chooser = new FakeImportChooser(0);

        importer.importSurvey(mockContext(), directory, chooser, new RecordingImportCallback());

        Assert.assertTrue(chooser.titles.isEmpty());
    }

    @Test
    public void testSingleSvxWithoutEspecAsksNothing() {
        DocumentFile directory = mockDirectory(mockFile("cave.svx"));
        FakeImportChooser chooser = new FakeImportChooser(0);

        importer.importSurvey(mockContext(), directory, chooser, new RecordingImportCallback());

        Assert.assertTrue(chooser.titles.isEmpty());
    }

    @Test
    public void testUnmatchedEspecOffersEspecFilesAndNone() {
        DocumentFile directory = mockDirectory(mockFile("cave.svx"), mockFile("other.espec"));
        FakeImportChooser chooser = new FakeImportChooser(1);

        importer.importSurvey(mockContext(), directory, chooser, new RecordingImportCallback());

        Assert.assertEquals(R.string.import_choose_espec, (int) chooser.titles.get(0));
        Assert.assertEquals(Arrays.asList("other.espec", "None"), chooser.optionsAsked.get(0));
    }
}
