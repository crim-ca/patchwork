# PatchWork

Clustering, sometimes called unsupervised learning, is one of the most fundamental step in understanding a dataset, aiming to discover the unknown nature of data through the separation of a finite dataset, with little or no ground truth, into a finite and discrete set of “natural,” hidden data structures. Given a set of n points in a multidimensional space, the purpose of clustering is to group them into several sets based on similarity measures and distance vectors.

Most clustering algorithms have a quadratic complexity with n, making then inadequate to analyze large amounts of data. In addition, many clustering algorithms are methods are inherently difficult to parallelize.

PatchWork is a novel clustering algorithm to address those issues. It is a mixture of density and grid-based clustering algorithm. It has linear complexity and near linear horizontal scalability. As a result, PatchWork can cluster a billion points in a few minutes only, a 40x improvement over Spark MLLib native implementation of the well-known K-Means.

## How to run the demo

At the root of the project:
``./runPatchwork.sh``

The demo reads data from a sample file, runs PatchWork to define the clusters, displays the clusters for each data point, then a summary of the clusters. The demo runs in a few seconds. If everything runs smoothly, you will see something like:

```
-----------------------------------------
number of points : 7826
number of clusters : 5
-----------------------------------------
   cluster 1 has 1370 cells
   cluster 6 has 689 cells
   cluster 14 has 193 cells
   cluster 19 has 117 cells
   cluster 25 has 299 cells
-----------------------------------------
size of epsilon : [0.2,0.2]
min pts in each cell : 1
time of training : 13450 ms
-----------------------------------------
```

Tests were executed using a Cloudera CDH 5.4.0 featuring Spark 1.3.0.

## How to compile

By default, you need a running local Spark server. If you want to run the algorithm on a cluster (e.g. on a YARN cluster), you can edit the ``SparkContext`` and the ``runPatchwork.sh`` script and recompile the project. ``sbt`` is required to compile the project.

To compile (at the root of the project): ``./build.sh``

## How to cite PatchWork

Frank Gouineau, Tom Landry and Thomas Triplet (2016) **PatchWork, a Scalable Density-Grid Clustering Algorithm**. In Proc. *31th ACM Symposium On Applied Computing*, Data-Mining track, Pisa, Italia (accepted)

## Contact

For all questions, contact me at thomas.triplet@crim.ca.
