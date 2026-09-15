package org.hwyl.sexytopo.control.io.translation;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import androidx.documentfile.provider.DocumentFile;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.hwyl.sexytopo.model.survey.Survey;
import org.junit.Assert;
import org.junit.Test;

public class FolderImporterTest {

    /** Minimal concrete subclass for testing the abstract base class behaviour. */
    private static class TestFolderImporter extends FolderImporter {

        private final List<DocumentFile> candidates;

        TestFolderImporter(List<DocumentFile> candidates) {
            this.candidates = candidates;
        }

        @Override
        public List<DocumentFile> getCandidateFiles(DocumentFile directory) {
            return candidates;
        }

        @Override
        public Survey toSurvey(
                android.content.Context context, DocumentFile file, DocumentFile directory) {
            return new Survey();
        }
    }

    private static DocumentFile mockDirectory() {
        DocumentFile dir = mock(DocumentFile.class);
        when(dir.isDirectory()).thenReturn(true);
        return dir;
    }

    private static DocumentFile mockFile() {
        DocumentFile file = mock(DocumentFile.class);
        when(file.isDirectory()).thenReturn(false);
        return file;
    }

    @Test
    public void testCanHandleDirectoryReturnsTrueWhenCandidatesExist() {
        DocumentFile candidate = mockFile();
        FolderImporter importer = new TestFolderImporter(Collections.singletonList(candidate));
        DocumentFile directory = mockDirectory();

        Assert.assertTrue(importer.canHandleFile(directory));
    }

    @Test
    public void testCanHandleDirectoryReturnsFalseWhenNoCandidates() {
        FolderImporter importer = new TestFolderImporter(Collections.<DocumentFile>emptyList());
        DocumentFile directory = mockDirectory();

        Assert.assertFalse(importer.canHandleFile(directory));
    }

    @Test
    public void testCanHandleReturnsFalseForFile() {
        DocumentFile candidate = mockFile();
        FolderImporter importer = new TestFolderImporter(Collections.singletonList(candidate));
        DocumentFile file = mockFile();

        Assert.assertFalse(importer.canHandleFile(file));
    }

    @Test
    public void testCanHandleReturnsFalseForNull() {
        FolderImporter importer = new TestFolderImporter(Collections.<DocumentFile>emptyList());

        Assert.assertFalse(importer.canHandleFile(null));
    }

    @Test
    public void testGetCandidateFilesReturnsAllCandidates() {
        DocumentFile a = mockFile();
        DocumentFile b = mockFile();
        FolderImporter importer = new TestFolderImporter(Arrays.asList(a, b));
        DocumentFile directory = mockDirectory();

        List<DocumentFile> result = importer.getCandidateFiles(directory);

        Assert.assertEquals(2, result.size());
    }
}
