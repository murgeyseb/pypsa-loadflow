/**
 * Copyright (c) 2018, RTE (http://www.rte-france.com)
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */

/**
 * Concrete implementation of loadflow computation that uses PyPSA core engine
 *
 * PyPSA is a python based framework that provides many functionalities, including
 * linear and non linear powerflow, optimal powerflow, PTDF computation...
 *
 * The interface between java and python is made using Jep (Java Embedded Python)
 *
 * @see <a href="https://github.com/PyPSA/PyPSA">PyPSA repository on GitHub</a>
 * @see <a href="https://github.com/ninia/jep">Jep repository on GitHub</a>
 *
 * @author Sebastien Murgey {@literal <sebastien.murgey at rte-france.com>}
 */
package eu.itesla_project.pypsa_loadflow;
