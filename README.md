social-graph-cluster
====================

Implementation of the GraphScope algorithm for clustering evolving graphs. Detailed description of the algorithm is 
available on: http://dl.acm.org/citation.cfm?id=1281266. The graphs are stored in matrices so the implementation uses 
the EJML library (https://code.google.com/p/efficient-java-matrix-library/) to handle the required calculations.

Data folder contains the synthetic and real datasets used to test the algorithm. The datasets are described briefly in
the following lines.
- data-synth folder contains four synthetic datasets. Each dataset contains 10 time step matrices, containing 1,000 
objects and 1,500 features, assigned to 8 embedded clusters of objects and features. The dataset are available online
(http://mlg.ucd.ie/dscc.html). More details can be found in the readme file inside data-synth folder and in the paper
that describes the datasets (http://www.csi.ucd.ie/files/ucd-csi-2011-08.pdf). It's worth to mention that for the 
algorithm evaluation has been used static graphs also. The BenchmarkGenerator class contains three implementations of
static binary graph benchmarks.
- delicious.2006.summer and delicious.2007.autum contain the real data used for the algorithm evaluation. The data 
correspond to the bookmark-tag correlation that has been recorded during the summer of 2006 and autum of 2007. Each
snapshot contains 1,500 tags and 3,000 bookmarks and coves a 7-day period. These data come from the delicious dataset
descibed here :http://www.dai-labor.de/en/publication/359 . Note that we keep the 1,500 most frequent tags and 3,000
most frequent bookmarks.

For more information or support, contact: sot.beis@gmail.com
