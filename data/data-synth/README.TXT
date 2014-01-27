Synthetic Dynamic Co-Clustering Datasets
---------------------------------------------------

This archive contains four synthetic datasets that have been generated for evaluating dynamic co-clustering. Each dataset contains 10 time step matrices, containing 1,000 objects and 1,500 features, assigned to 8 embedded clusters of objects and features.

Each dataset contains a different level of volatility, where certain proportion of cluster memberships are swapped between successive time steps:
- set1/ : 10% of object and feature cluster memberships swapped.
- set2/ : 20% of object and feature cluster memberships swapped.
- set3/ : 30% of object and feature cluster memberships swapped.
- set4/ : 30% of object and feature cluster memberships swapped.

The datasets are provided in sparse matrix form. The data has already been pre-processed and divided into time steps. Each time step graph is stored using the following files:
    *.mtx: Sparse feature-object matrix in Matrix Market format.
    *.fids: List of features in present in each step, with each line corresponding to a row (feature) of the corresponding feature-object matrix.
    *.ids: List of object identifiers for each step, with each line corresponding to a column (object) of the corresponding feature-object matrix.
    *.clist: A list of grounth truth object clustering annotations for each step, with each line corresponding to a column (object) of the corresponding feature-object matrix. 
    *.fclist: A list of grounth truth feature clustering annotations for each step, with each line corresponding to a row (feature) of the corresponding feature-object matrix. 

This archive is for academic use only and is not for redistribution.

For more details visit:

http://mlg.ucd.ie/sdcc.html

Or contact derek.greene@ucd.ie

