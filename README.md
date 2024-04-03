# topKMining

This is the implementation of the TTFE algorithm. Some related literature articles and algorithms are also included. 

## Literature

The development of top-$k$ mining algorithms of the branch of the TTFE. 

## SPMF

A set of algorithms forked from the SPMF platform. 

## THUI

The implementation of the original THUI algorithm, abstracted from the SPMF. 

## THUFI

The implementation of mining top-$k$ high threat and frequency itemsets based on the original THUI. 

This is not an accurate algorithm since it is implemented by directly replacing the utility values with the FU values. 

## TFUI

The improved implementation of the THUFI algorithm with file configures. 

It supports the alpha and the beta values directly set in the database file. 

## TTFE

The implementation of mining top-$k$ high threat and frequency event sets. 

It supports super-parameters directly set in the database file. 

It uses better data structure and sorting algorithms. 

It has more friendly debugging procedures. 

### TTFE_v1

This is an accurate algorithm. 

It will also compute the top-$k$ event sets in each transaction. 

### TTFE_v2

This is a fuzzy algorithm using the tree data structure. 

It has better performance due to the node pruning. 
