package org.hwyl.sexytopo.control.io.thirdparty.survex;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import androidx.documentfile.provider.DocumentFile;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;

public class EspecResolutionTest {

    private static DocumentFile mockFile(String name) {
        DocumentFile file = mock(DocumentFile.class);
        when(file.isFile()).thenReturn(true);
        when(file.isDirectory()).thenReturn(false);
        when(file.getName()).thenReturn(name);
        return file;
    }

    private static DocumentFile mockDirectory(DocumentFile... contents) {
        DocumentFile dir = mock(DocumentFile.class);
        when(dir.isDirectory()).thenReturn(true);
        when(dir.listFiles()).thenReturn(contents);
        return dir;
    }

    @Test
    public void testMatchedWhenEspecHasSameBaseName() {
        DocumentFile svx = mockFile("mycave.svx");
        DocumentFile espec = mockFile("mycave.espec");
        DocumentFile directory = mockDirectory(svx, espec);

        EspecResolution resolution = SurvexImporter.resolveEspec(svx, directory);

        Assert.assertTrue(resolution.isMatched());
        Assert.assertEquals("mycave.espec", resolution.getMatchedFile().getName());
    }

    @Test
    public void testUnmatchedWithOtherEspecFilesWhenNoNameMatch() {
        DocumentFile svx = mockFile("mycave.svx");
        DocumentFile other = mockFile("othercave.espec");
        DocumentFile directory = mockDirectory(svx, other);

        EspecResolution resolution = SurvexImporter.resolveEspec(svx, directory);

        Assert.assertTrue(resolution.isUnmatched());
        Assert.assertEquals(1, resolution.getOtherEspecFiles().size());
        Assert.assertEquals("othercave.espec", resolution.getOtherEspecFiles().get(0).getName());
    }

    @Test
    public void testUnmatchedWithEmptyListWhenNoEspecFilesAtAll() {
        DocumentFile svx = mockFile("mycave.svx");
        DocumentFile directory = mockDirectory(svx);

        EspecResolution resolution = SurvexImporter.resolveEspec(svx, directory);

        Assert.assertTrue(resolution.isUnmatched());
        Assert.assertTrue(resolution.getOtherEspecFiles().isEmpty());
    }

    @Test
    public void testMultipleUnmatchedEspecFilesAllReturned() {
        DocumentFile svx = mockFile("mycave.svx");
        DocumentFile espec1 = mockFile("other1.espec");
        DocumentFile espec2 = mockFile("other2.espec");
        DocumentFile directory = mockDirectory(svx, espec1, espec2);

        EspecResolution resolution = SurvexImporter.resolveEspec(svx, directory);

        Assert.assertTrue(resolution.isUnmatched());
        List<DocumentFile> others = resolution.getOtherEspecFiles();
        Assert.assertEquals(2, others.size());
    }

    @Test
    public void testMatchedFileIsNullWhenUnmatched() {
        DocumentFile svx = mockFile("mycave.svx");
        DocumentFile directory = mockDirectory(svx);

        EspecResolution resolution = SurvexImporter.resolveEspec(svx, directory);

        Assert.assertNull(resolution.getMatchedFile());
    }
}
