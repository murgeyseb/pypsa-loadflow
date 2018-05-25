"""

 Copyright (c) 2018, RTE (http://www.rte-france.com)
 This Source Code Form is subject to the terms of the Mozilla Public
 License, v. 2.0. If a copy of the MPL was not distributed with this
 file, You can obtain one at http://mozilla.org/MPL/2.0/.

"""
"""
PyPSA loadflow script

Use global inputs coming from calling scripts (could be from
Java via Jep or other sources) to load a network, do a power flow
simulation and save results in an output file
"""

import pypsa

# Script inputs
global debug
global dc_mode
global relaxation_coeff
global use_seed
global input_directory
global output_directory
global compute_lpf
# End of script inputs

def slack_distribution(network):
    return

def run_dc_loadflow(network):
    network.lpf()
    converged = 'True'
    n_iter = 1
    return converged, n_iter


def run_ac_loadflow(network):
    if use_seed:
        network.lpf()
    result = network.pf(use_seed=use_seed, relaxation_coeff=relaxation_coeff)
    subnetwork_num = 0
    snapshot_num = 0
    converged = result.converged.iloc[subnetwork_num][snapshot_num]
    n_iter = result.n_iter.iloc[subnetwork_num][snapshot_num]
    return converged, n_iter

if __name__ == '__main__':
    network = pypsa.Network(import_name=input_directory)
    if debug:
        network.consistency_check()

    if dc_mode:
        converged, n_iter = run_dc_loadflow(network)
    else:
        converged, n_iter = run_ac_loadflow(network)

    if converged:
        network.export_to_csv_folder(output_directory)
