# PyPSA powerflow integration

## Requirements
In order to build PyPSA powerflow integration, you need the following environment available:
  * JDK *(1.8 or greater)*
  * Maven
  * [powsybl-core](https://github.com/powsybl/powsybl-core)
  * Python (only tested on 2.7)

## Install
To build PyPSA powerflow integration, just do the following:

```
$> git clone https://github.com/murgeyseb/pypsa-loadflow.git
$> mvn clean install
```

It is then possible to use it as any other dependency in a Maven project.

## Runtime requirements
To be able to use PyPSA, you need to have it installed in your pythons packages.
PyPSA loadflow integration uses a modified version of PyPSA that includes a relaxation coefficient on
Newton Raphson algorithm. It is available on GitHub:
```
$> git clone https://github.com/murgeyseb/PyPSA.git
```

To install it just do the following:
```
$> git checkout addRelaxationCoeffOnNewtonRaphson
$> python setup.py install
```

Alternatively, if you want to install it only in your user's local directory:
```
$> python setup.py install --user
```

You will also need to get Jep:
```
$> JAVA_HOME=<your_JDK_directory> pip install jep
```

Alternatively, if you want to install it only in your user's local directory:
```
$> JAVA_HOME=<your_JDK_directory> pip install --user jep
```

In runtime, you will also need to put the jep installation directory in your LD_LIBRARY_PATH variable, and put your python library in LD_PRELOAD variable:
```
export LD_PRELOAD=/usr/lib64/libpython2.7.so:$LD_PRELOAD
export LD_LIBRARY_PATH=/usr/lib/python2.7/site-packages/jep:$LD_LIBRARY_PATH
```
