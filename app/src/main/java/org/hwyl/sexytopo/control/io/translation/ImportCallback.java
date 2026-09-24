package org.hwyl.sexytopo.control.io.translation;

import org.hwyl.sexytopo.model.survey.Survey;

/** Receives the outcome of an import, which may arrive after the user has answered questions. */
public interface ImportCallback {

    void onImported(Survey survey);

    void onImportFailed(Exception exception);
}
