# Distributed Subtrajectory Join (DSJ)

An open source implementation of the solution proposed in [1].

Joining trajectory datasets is a significant operation in mobility data analytics and the cornerstone of various methods that aim to extract knowledge out of them. In the era of Big Data, the production of mobility data has become massive and, consequently, performing such an operation in a centralized way is not feasible. Here, we address the problem of Distributed Subtrajectory Join (DSJ) processing by utilizing the MapReduce programming model.
The problem that we address is as follows: given two sets of trajectories (or a single set and its mirror in the case of self-join), identify all pairs of maximal ``portions'' of trajectories (or else, subtrajectories) that move close in time and space w.r.t. a spatial threshold ε<sub>sp</sub> and a temporal tolerance ε<sub>t</sub>, for at least some time duration δt.

## Implementation Details
This is a MapReduce solution in Java that has been implemented and tested against Hadoop 2.7.2. The only external library used here is [cts](https://github.com/orbisgis/cts) for coordinate transformation.

## Input Data
The input is an hdfs directory containing csv files (comma delimited) of the form <obj_id, traj_id, t, lon, lat>, where 
* obj_id and traj_id are integers (traj_id might be ommited if not available)
* t is an integer corresponding to the unix timestamp
* lon and lat are the coordinates in WGS84

## Preprocessing
For each dataset that is going to be used a [preprocessing step](https://github.com/DataStories-UniPi/Distributed-Subtrajectory-Join/blob/master/src/DSJ/PreprocessDriver.java) must take place before the first run of DSJ.
### Input Parameters
The input parameters of this preprocessing step are the following:
* hostname --> is a string representing the hostname (e.g. localhost)
* dfs_port --> is a string representing the hdfs port
* rm_port -->  is a string representing the YARN resource manager port
* workspace_dir --> is a string corresponding to the HDFS path of your workspace;
* raw_dir --> is a string corresponding to the relative HDFS path (in relation with the workspace_dir) where the raw input data are stored
* prep_dir --> is a string corresponding to the relative HDFS path (in relation with the workspace_dir) where the intermediate preprocessed output data will be stored
* input_dir --> is a string corresponding to the relative HDFS path (in relation with the workspace_dir) where the final preprocessed output data will be stored
* sample_freq --> is a double precision number corresponding to the probability with which a key will be chosen, during the sampling phase of the repartitioning and index building (refer to [1] for more details)
* max_nof_samples =--> is an integer corresponding to the total number of samples to obtain from all selected splits, during the sampling phase of the repartitioning and index building (refer to [1] for more details)
* maxCellPtsPrcnt -->  is a double precision number corresponding to the maximum percent of points (in relation to the total number of points) per cell of the quadtree index (refer to [1] for more details)

Example --> yarn jar Preprocess.jar "hdfs://localhost" ":9000" ":8050" "/workspace_dir" "/raw" "/prep" "/input" sample_freq max_nof_samples maxCellPtsPrcnt

## DSJ
[DSJ](https://github.com/DataStories-UniPi/Distributed-Subtrajectory-Join/blob/master/src/DSJ/DSJDriver.java) can be run multiple times with different parameters once the preprocessing step has taken place.

### Input Parameters
The input parameters of DSJ are the following:
* hostname --> is a string representing the hostname (e.g. localhost)
* dfs_port --> is a string representing the hdfs port
* rm_port -->  is a string representing the YARN resource manager port
* workspace_dir --> is a string corresponding to the HDFS path of your workspace;
* input_dir --> is a string corresponding to the relative HDFS path (in relation with the workspace_dir) where the input of DSJ is stored
* output_dir --> is a string corresponding to the relative HDFS path (in relation with the workspace_dir) where the output of DSJ will be stored
* nof_reducers --> is an integer corresponding to the number of reducers
* e_sp_method --> can be either 1 or 2 corresponding to the two alternative distance range methods available. 1 is for pure euclidean distance and 2 is for percentage of the quadtree cell that the point belongs to (refer to [1] for more details)
* epsilon_sp --> is an integer number corresponding to the euclidean distance if e_sp_method = 1 and a a double precision number corresponding to the percentage of the quadtree cell if e_sp_method = 2
* epsilon_t  --> is an integer number corresponding to ε<sub>t</sub> in seconds
* dt --> is an integer number corresponding to δt in seconds
* job_description --> is a string corresponding to the name of the current run (this is helpfull when multiple runs with different parameters take place)

Example --> yarn jar DSJ.jar "hdfs://localhost" ":9000" ":8050" /workspace_dir" "/input" "/join_output" nof_reducers e_sp_method epsilon_sp epsilon_t dt DSJ_run_1

## References
1. P. Tampakis, C. Doulkeridis, N. Pelekis, and Y. Theodoridis. “Distributed Subtrajectory Join on Massive Datasets”. In:ACM Trans. Spatial AlgorithmsSyst.6.2 (2019)
