# PatchWork

Clustering, sometimes called unsupervised learning, is one of the most fundamental step in understanding a dataset, aiming to discover the unknown nature of data through the separation of a finite dataset, with little or no ground truth, into a finite and discrete set of “natural,” hidden data structures. Given a set of n points in a multidimensional space, the purpose of clustering is to group them into several sets based on similarity measures and distance vectors.

Most clustering algorithms have a quadratic complexity with n, making then inadequate to analyze large amounts of data. In addition, many clustering algorithms are methods are inherently difficult to parallelize.

PatchWork is a novel clustering algorithm to address those issues. It is a mixture of density and grid-based clustering algorithm. It has linear complexity and near linear horizontal scalability. As a result, PatchWork can cluster a billion points in a few minutes only, a 40x improvement over Spark MLLib native implementation of the well-known K-Means.
