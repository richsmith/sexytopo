package org.hwyl.sexytopo.control.io.thirdparty.survex;

import android.content.Context;

import androidx.documentfile.provider.DocumentFile;

import org.hwyl.sexytopo.control.io.IoUtils;
import org.hwyl.sexytopo.control.io.thirdparty.survextherion.SurvexTherionImporter;
import org.hwyl.sexytopo.control.io.translation.Importer;
import org.hwyl.sexytopo.model.survey.Survey;

import java.util.Map;


public class SurvexImporter extends Importer {

    public Survey toSurvey(Context context, DocumentFile file) throws Exception {
        Survey survey = new Survey();
        String text = IoUtils.slurpFile(context, file);
        
        // Parse passage data first to extract station comments
        Map<String, String> passageComments = SurvexTherionImporter.parsePassageData(text, true);
        
        // Parse centreline data
        SurvexTherionImporter.parseCentreline(text, survey);
        
        // Merge passage comments with station comments
        SurvexTherionImporter.mergePassageComments(survey, passageComments);
        
        return survey;
    }


    @Override
    public boolean canHandleFile(DocumentFile file) {
        return file.getName().endsWith(".svx");
    }

}
