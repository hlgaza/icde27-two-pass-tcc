# Bypassing the Memory Limit of Temporal Connected Component Computation

![License: GPL v3](https://img.shields.io/badge/License-GPLv3-blue.svg)
![Java Version](https://img.shields.io/badge/Java-24-orange?logo=openjdk)

## Overview

This repository contains the source and analysis codebase for the paper "Bypassing the Memory Limit of Temporal Connected Component Computation".
The two-pass traversal algorithm presents an end-to-end solution for finding the largest
temporal connected component (TCC) in a graph. At a glance, finding the largest TCC comprises three steps:
1. Compute the temporal reachability (TR) of all vertices
2. Construct a reachability graph by identifying mutually reachable vertices
3. Compute the maximum clique in the reachability graph, where the clique is the largest TCC.

Baseline algorithms materialize all-pairs TR results, which exhaust memory on large temporal graphs, before even
reaching the theoretically hard step of finding the maximum clique. 
To this end, our proposed solution utilizes a two-pass temporal breadth-first search (TBFS)
algorithm to find mutually reachable vertices on-the-fly without full TR materialization, and leverages dynamic sparse graph structures to incrementally construct the inherently sparse reachability graph.
By doing so, our solution finds the largest TCC in large temporal graphs, previously unachievable with
baseline algorithms.

## Repository Structure

| File/Directory          | Description                                                      |
|-------------------------|------------------------------------------------------------------|
| tcc/src/main/java/      | Source code containing the proposed and baseline algorithms |                                               |
| tcc/src/main/resources/ | Configuration file for logging                                   |
| scripts/                | Scripts to help with automation                                  |

### Main Classes
From the source root directory, you can find the following main classes:

#### Time Instant
1. `org/dfpl/tcc/pcsr/analysis/TwoPassTccInstantApp1.java`: (our proposed solution) the analysis class for the two-pass TCC algorithm
1. `org/dfpl/tcc/adjlist/analysis/ReversedTccInstantApp1.java`: (baseline) the analysis class for the reversed-time scan algorithm with adjacency list
1. `org/dfpl/tcc/adjset/analysis/ReversedTccInstantApp1.java`: (baseline) the analysis class for the reversed-time scan algorithm with adjacency set
1. `org/dfpl/tcc/csr/analysis/TimeCentricInstantApp1.java`: (baseline) the analysis class for the time-centric algorithm analysis

##### Ablation
1. `org/dfpl/tcc/adjlist/analysis/TwoPassTccInstantApp1.java`: (our proposed solution) the analysis class for the two-pass TCC algorithm with adjacency list
1. `org/dfpl/tcc/adjset/analysis/TwoPassTccInstantApp1.java`: (our proposed solution) the analysis class for the two-pass TCC algorithm with adjacency list
1. `org/dfpl/tcc/csr/analysis/TwoPassTccInstantApp1.java`: (our proposed solution) the analysis class for the two-pass TCC algorithm with growing CSR

#### Time Period
1. `org/dfpl/tcc/pcsr/analysis/TwoPassTccPeriodApp1.java`: (our proposed solution) the analysis class for the two-pass TCC algorithm
1. `org/dfpl/tcc/adjlist/analysis/ReversedTccPeriodApp1.java`: (baseline) the analysis class for the reversed-time scan algorithm with adjacency list
1. `org/dfpl/tcc/adjset/analysis/ReversedTccPeriodApp1.java`: (baseline) the analysis class for the reversed-time scan algorithm with adjacency set
1. `org/dfpl/tcc/csr/analysis/TimeCentricPeriodApp1.java`: (baseline) the analysis class for the time-centric algorithm analysis

## Specifications

- [Java 24](https://www.oracle.com/java/technologies/javase/jdk24-archive-downloads.html)
- [Maven 3.8.4](https://maven.apache.org/install.html) if compiling the source code

## Compilation

1. Clone the repository: `git clone https://github.com/hlgaza/icde27-two-pass-tcc.git`
2. Go to the content directory: `cd icde27-two-pass-tcc/tcc`
3. Package the specialized JARs: `mvn clean package`
4. Find the executable JARs in the `target/` directory:
    - `tcc-1.0-two-pass-*.jar` (Two-pass traversal)
    - `tcc-1.0-reversed-*.jar` (Reversed-time scan)
    - `tcc-1.0-tc-*.jar` (Time-centric)

Alternatively, you can create JAR files within [IntelliJ](https://www.jetbrains.com/help/idea/compiling-applications.html#package_into_jar)
or [Eclipse](https://www.eclipse.org/documentation/).

## Usage

### Quick Start
To use the bitcoin_alpha dataset and the two-pass traversal algorithm, run the following commands:
```bash
# Create a workspace
mkdir tcc_ws/
cd tcc_ws

# Download the JAR
mkdir jars/
cd jars
wget https://github.com/hlgaza/icde27-two-pass-tcc/releases/download/v1.0/tcc-1.0-two-pass-pcsr-instant.jar
cd ../

# Download the dataset
mkdir datasets/
cd datasets/
wget https://github.com/hlgaza/icde27-two-pass-tcc/releases/download/v1.0/bitcoin_alpha.tar.gz
tar -xzvf bitcoin_alpha.tar.gz
cd ../

# Run the algorithm
/usr/bin/time -v java -Xmx16g \
   -Ddirected=true \
   -Dvertices=datasets/bitcoin_alpha/vertices.csv \
   -Dstream=datasets/bitcoin_alpha/graph.csv \
   -jar jars/tcc-1.0-two-pass-pcsr-instant.jar
```

### Command Line Interface (CLI)

After obtaining the JAR of the desired algorithm (through [compiling](#compilation) or [downloading](https://github.com/hlgaza/two-tbfs-tcc/releases)),
run it using `java -jar`. For large datasets, ensure you allocate sufficient heap memory (e.g., 16
GB).
Here is an example command:

```bash
java -Xmx16g \
     -Djava.util.logging.config.file=/path/to/logging.properties \
     -Ddirected=true \
     -Dvertices=/path/to/dataset/vertices.csv \
     -Dstream=/path/to/dataset/graph.csv \
     -jar /path/to/jar/tcc-*.jar
```

| Parameter                       | Description                                                                                                   |
|---------------------------------|---------------------------------------------------------------------------------------------------------------|
| -Djava.util.logging.config.file | (optional) Path to the logging configuration file. A sample `logging.properties` can be found in `resources/` |
| -Ddirected                      | (optional, default=`true`) Set to `true` for directed graphs                                                  |
| -Dvertices                      | Path to the vertices file                                                                                     |
| -Dstream                        | Path to the graph file                                                                                        |

**Note:** To measure maximum resident set size (RSS), prefix the command with `/usr/bin/time -v`.

### IntelliJ IDEA or Eclipse

Alternatively, you can run the main analysis classes without creating JAR files 
within [IntelliJ IDEA](https://www.jetbrains.com/idea/download) or [Eclipse](https://www.eclipse.org/downloads/) by
indicating the parameters
in the VM options. 

### Docker
To avoid host environment dependencies (like Java version mismatches),
you can run the entire pipeline inside a containerized environment:
```bash
docker build -t tcc_app:latest .
docker run -it --name tcc_app tcc_app:latest

cd datasets/
wget https://github.com/hlgaza/icde27-two-pass-tcc/releases/download/v1.0/bitcoin_alpha.tar.gz
tar -xzvf bitcoin_alpha.tar.gz
cd ../

/usr/bin/time -v java -Xmx16g \
   -Ddirected=true \
   -Dvertices=datasets/bitcoin_alpha/vertices.csv \
   -Dstream=datasets/bitcoin_alpha/graph.csv \
   -jar jars/tcc-1.0-two-pass-pcsr-instant.jar
```

### Scripts

The `scripts/run.sh` is a bash script for running the algorithms on real-world and synthetic datasets.

The scripts assume the following file structure from a base directory:

```plain
two-tbfs-tcc/
├── scripts
├── tcc
│   └── target
│       ├── tcc-1.0-reversed-*.jar
│       ├── tcc-1.0-tc-*.jar
│       └── tcc-1.0-two-pass-*.jar
output/
├── bitcoin_alpha
├── erdos_reyni
datasets/
├── bitcoin_alpha
│   ├── graph.csv
│   └── vertices.csv
└── erdos_reyni
   └── n10000
       ├── p50
       │   ├── graph.csv
       │   └── vertices.csv
       └── p80
           ├── graph.csv
           └── vertices.csv
```

#### Steps

1. Compile the JARs.
1. Download sample datasets (see [Datasets](#datasets)).
1. Extract the datasets to the `datasets/` directory.
1. Indicate the desired algorithm and dataset in the `run.sh` scripts.
1. Run the script: `sh run.sh`.
   - **Note:** The script uses `nohup` to run the algorithm in the background.
   It also creates the `output/` directory following the structure of the input.
1. View the log in the `output/` directory.

## Datasets

### Format

The JAR files expect two input files in CSV format:

1. `vertices.csv`: vertex_name,vertex_id

```plain
#vertex,index
2306,0
2305,1
```

2. `graph.csv`: out_vertex_name,in_vertex_name,timestamp

Time Instant:
```plain
#out,in,timestamp
1,401,1289192400
9,969,1289192400
9,270,1289192400
```

Time Period:
```plain
#out,in,timestamp,timestamp
1,401,1289192400,1289192410
9,969,1289192400,1289192420
9,270,1289192400,1289192405
```

### Sample Datasets

- Real-world: [bitcoin_alpha](https://github.com/hlgaza/icde27-two-pass-tcc/releases/download/v1.0/bitcoin_alpha.tar.gz)
- Random graph: [erdos_reyni](https://github.com/hlgaza/icde27-two-pass-tcc/releases/download/v1.0/erdos_reyni_n10k.tar.gz)

