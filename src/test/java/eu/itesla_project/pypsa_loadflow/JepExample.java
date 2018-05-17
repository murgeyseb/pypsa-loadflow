/**
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package eu.itesla_project.pypsa_loadflow;

import jep.Jep;

public class JepExample {
    public static void main(String[] args) throws Exception {
        try (Jep jep = new Jep(false)) {
            jep.eval("from java.lang import System");
            jep.eval("s = 'Hello World'");
            jep.eval("System.out.println(s)");
            jep.eval("print(s)");
            jep.eval("print(s[1:-1])");
            jep.set("i", 10);
            jep.runScript(JepExample.class.getResource("/script.py").getFile());
        }
    }
}
