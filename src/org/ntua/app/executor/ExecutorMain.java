package org.ntua.app.executor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.ontology.OntModelSpec;
import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.sparql.algebra.Algebra;
import com.hp.hpl.jena.sparql.algebra.Op;
import com.hp.hpl.jena.sparql.algebra.op.OpBGP;
import com.hp.hpl.jena.sparql.algebra.op.OpProject;
import com.hp.hpl.jena.sparql.core.Var;
import com.hp.hpl.jena.sparql.engine.QueryIterator;
import com.hp.hpl.jena.sparql.engine.ResultSetStream;
import com.hp.hpl.jena.tdb.TDBFactory;

public class ExecutorMain {

	public static void main(String[] args) {
		
		//Point Jena to the the directory of the database
		Dataset dataset = TDBFactory.createDataset("c:/TDB/TDB");
	    
		//Put the dataset in the Model object
		Model model = dataset.getDefaultModel();

		/*
		 * briskw to montelo
		 */
//		OntModelSpec ontModelSpec = OntModelSpec.OWL_MEM_TRANS_INF;
		OntModelSpec ontModelSpec = OntModelSpec.OWL_MEM;
		OntModel ontModel = ModelFactory.createOntologyModel(ontModelSpec, model);
	    
	    //Print the total number of triples
	    System.out.println(model.size());
	    
	    //Put query string here
	    String queryString = //"PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> PREFIX ub: <http://swat.cse.lehigh.edu/onto/univ-bench.owl#> SELECT ?X ?Y ?Z WHERE {?X rdf:type ub:GraduateStudent . ?Y rdf:type ub:University . ?Z rdf:type ub:Department . ?X ub:memberOf ?Z . ?Z ub:subOrganizationOf ?Y . ?X ub:undergraduateDegreeFrom ?Y}";
				"PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#> PREFIX ub: <http://www.lehigh.edu/~zhp2/2004/0401/univ-bench.owl#> SELECT ?X ?Y ?Z WHERE {?X rdf:type ub:Student . ?Y rdf:type ub:Faculty . ?Z rdf:type ub:Course . ?X ub:advisor ?Y . ?Y ub:teacherOf ?Z . ?X ub:takesCourse ?Z}";



		//example ordering of query with 6 triple patterns
		//try the same query with 3,5,6,4,1,2 and 1,2,3,4,5,6 to see the difference in time (second one will be very slow)
		//Instead of ArrayList<Arrays.asList(......)>, this should contain the order object computed in line 51.
		//However, if you want to test orderings manually, use the code as follows:
		Query q = QueryFactory.create(queryString);
		CustomPlanner plan = new CustomPlanner(q, ontModel); //pernaw ston planner query kai montelo
		plan.createQuery();
		List<Var> vars = new ArrayList<>() ;
		vars = q.getProjectVars();
		Op newOp = new OpBGP(plan.createBP(new ArrayList<Integer>(Arrays.asList(new Integer[]{3,5,6,4,1,2})))) ;
		OpProject newopp = new OpProject(newOp, vars);
		System.out.println("Triple order : " + newopp.toString());
		long start = System.nanoTime();
		testQuery(newopp, model, q);
		long end = System.nanoTime();
		System.out.println("Elapsed time: " + ((end-start) / 1000000000.0) + " sec");




	    //this will have to contain the order of the triple patterns. Please fill the findPlan() method in CustomPlanner
       	List<Integer> order = plan.findPlan(); //briskei th seira mesw selectivity

		for (Integer integer : order) {
			System.out.println(integer);
		}

		ArrayList<Integer> finalSet = new ArrayList<Integer>(order);
		
		newOp = new OpBGP(plan.createBP(finalSet));
		
		newopp = new OpProject(newOp, vars);
		
		System.out.println("Triple order : " + newopp.toString());
		
		start = System.nanoTime();
		
		testQuery(newopp, model, q);
		
		end = System.nanoTime();
		
		System.out.println("Elapsed time: " + ((end-start) / 1000000000.0) + " sec");

		//Close the dataset to avoid leaks
	    dataset.close();
	
	}
	
	public static void testQuery(Op op, Model model, Query q){
				 
		
		QueryIterator qIter2 = Algebra.exec(op, model.getGraph()) ;
		
		List<Var> vars = new ArrayList<>() ;
		List<String> varNames = new ArrayList<String>();
		vars = q.getProjectVars();
	        	        	     
		for(Var var : vars){
			varNames.add(var.getName());
		}
		ResultSet rs = new ResultSetStream(varNames, model, qIter2);
	   		
		qIter2 = Algebra.exec(op, model.getGraph()) ;		    
		rs = new ResultSetStream(varNames, model, qIter2);		       
		while(rs.hasNext()){
		
			rs.next();
		        	
		}		 				
	     
	    return ;
	     
	  }

}
