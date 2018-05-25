"""
PyPSA sensitivity script

Use global inputs coming from calling scripts (could be from
Java via Jep or other sources) to load a network, do a sensitivity
computation and save results in an output file
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

def run_dc_sensitivity(network, sensitivity_factors):
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
        converged, n_iter, sensitivity_values = run_dc_sensitivity(network, sensitivity_factors)
    else:
        converged, n_iter = run_ac_loadflow(network)
