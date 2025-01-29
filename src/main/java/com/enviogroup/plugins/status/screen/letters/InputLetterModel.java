package com.enviogroup.plugins.status.screen.letters;

import com.enviogroup.plugins.status.screen.OrganisationModel;

public class InputLetterModel extends BaseLetterModel {
    private static final String INPUT_LETTER_TYPE = "Входящее письмо";

    @Override
    public String getLetterType() {
        return INPUT_LETTER_TYPE;
    }


}
