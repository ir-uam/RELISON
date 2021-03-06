Diffusion protocol summary
==================================================
The RELISON framework allows the execution of many different information diffusion protocols. Some of them are pre-defined (following some previous research works). You can find the list of protocols in the following link:

.. toctree::
   :maxdepth: 1

   protocols/protocols

However, in order to make the diffusion module configurable, it is also possible to define custom diffusion protocols, built from their fundamental elements, as described in the Simulation description section. The framework already defines some of this parts, which can be found below:

.. toctree::
   :maxdepth: 1

   protocols/expiration
   protocols/propagation
   protocols/selection
   protocols/sight
   protocols/update

In addition, it is necessary to provide, for each simulation, two additional elements: a set of filters to preprocess the input data for the simulations, and a stop condition, which determines when the simulation ends. We list them below:

.. toctree::
   :maxdepth: 1

   protocols/stop
   protocols/filters