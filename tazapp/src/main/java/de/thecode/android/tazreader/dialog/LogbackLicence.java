package de.thecode.android.tazreader.dialog;

import de.mateware.dialog.licences.StandardLicence;

/**
 * Created by mate on 18.11.2016.
 */

public class LogbackLicence extends StandardLicence {

    public LogbackLicence() {
        super("Logback Android", "Copyright (C) 1999-2014, QOS.ch. All rights reserved. ", null);
        setLicenceText("This program and the accompanying materials are dual-licensed under " +
                "either the terms of the Eclipse Public License v1.0 as published by " +
                "the Eclipse Foundation\n" +
                "\n" +
                "  or (per the licensee's choosing)\n" +
                "\n" +
                "under the terms of the GNU Lesser General Public License version 2.1 " +
                "as published by the Free Software Foundation.");
    }
}
