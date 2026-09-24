package org.hwyl.sexytopo.control.io.translation;

import java.util.ArrayList;
import java.util.List;

/** Records what it was asked and answers with a fixed choice. */
public class FakeImportChooser implements ImportChooser {

    public final List<Integer> titles = new ArrayList<>();
    public final List<List<String>> optionsAsked = new ArrayList<>();
    private final int answer;

    public FakeImportChooser(int answer) {
        this.answer = answer;
    }

    @Override
    public void choose(int titleRes, List<String> options, OnChosen onChosen) {
        titles.add(titleRes);
        optionsAsked.add(options);
        onChosen.onChosen(answer);
    }
}
