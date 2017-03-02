We added Jena libs 2.12 in our classpath
We downloaded the TDB files with a preloaded LUBM dataset from here: https://www.dropbox.com/s/58wfe2yyww1vh1y/TDB.rar?dl=0
We pointed Jena to the  directory of the database
We Put the dataset in the Model object
We found the model
We found the total number of triples
We put in the code the given query(<http://www.w3.org/1999/02/22-rdf-syntax-ns#> PREFIX ub: <http://www.lehigh.edu/~zhp2/2004/0401/univ-bench.owl#> SELECT ?X ?Y ?Z WHERE {?X rdf:type ub:Student . ?Y rdf:type ub:Faculty . ?Z rdf:type ub:Course . ?X ub:advisor ?Y . ?Y ub:teacherOf ?Z . ?X ub:takesCourse ?Z}";)
send to the planner  the query and the model
In findplan() at the customplanner we implemented our proposed algorithm for finding the best execution order of triples
((
findplan() calculates the R(the total number of resources of the RDF database)
It calculates the selectivity of each subject according to if is bound or unbound
it calculates the tp(the number of triples matching predicate p) for each predicate
It calculates the selectivity of each predicate according to if is bound or unbound
It calculate the frequency of class (p, oc)  for each object
It calculates the selectivity of each object according to if is bound or unbound
find the selectivity of each triple with the type:
0.33 * subjectSelectivity + 0.33 * predicateSelectivity + 0.33 * objectSelectivity
))
With TreeMap we found the ascending sequence of  triples according to selectivity
 We found the descending sequence of  triples with putting them in a pile
At executormain we put the findplan() in a list which finds the sequence of triples according to selectivity
We ran it for the LUBM queries
We compare the elapsed time of the proposed sequence(given query) with that which stems from of our algorithm