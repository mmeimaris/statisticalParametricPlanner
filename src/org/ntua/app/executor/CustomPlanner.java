package org.ntua.app.executor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.TreeMap;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.sparql.core.BasicPattern;
import com.hp.hpl.jena.sparql.core.TriplePath;
import com.hp.hpl.jena.sparql.syntax.Element;
import com.hp.hpl.jena.sparql.syntax.ElementGroup;
import com.hp.hpl.jena.sparql.syntax.ElementPathBlock;

public class CustomPlanner {

	Query query ;	
	ElementPathBlock triplePathBlock ;
	public HashMap<Triple, Integer> tripleIndex = new HashMap<>();
	public HashMap<Integer, Triple> reverseTripleIndex = new HashMap<>();
	public HashMap<LinkedHashSet<Integer>, HashSet<Node>> existingNodes = new HashMap<>();
	public HashMap<Node, HashSet<Triple>> nodeTripleIndex = new HashMap<>();
	public HashMap<Node, Integer> nodeIndex;
	public HashMap<Node, Integer> existingNodesCount = new HashMap<Node, Integer>();

	public OntModel model;

	public CustomPlanner(Query q, OntModel model){
		this.query = q;
		this.model = model;
	}
	public void createQuery(){

		Element pattern = query.getQueryPattern();

		ElementGroup g = (ElementGroup) pattern ;

		triplePathBlock = (ElementPathBlock) g.getElements().get(0);

		nodeIndex = new HashMap<>();

		int nextIndex = 0, nextNodeIndex = 0;

		for(TriplePath triplePath : triplePathBlock.getPattern().getList()){

			Triple triple = triplePath.asTriple();	
			//System.out.println("\nTriple: " + triple);
			HashSet<Triple> tripleSetA = new HashSet<Triple>();
			tripleSetA.add(triple);			
			if(nodeTripleIndex.containsKey(triple.getSubject())) {
				HashSet<Triple> newSet = new HashSet<Triple>();
				newSet.addAll(nodeTripleIndex.get(triple.getSubject()));
				newSet.add(triple);
				nodeTripleIndex.put(triple.getSubject(), newSet);
				//nodeTripleIndex.get(triple.getSubject()).add(triple);
			}

			else nodeTripleIndex.put(triple.getSubject(), tripleSetA);

			if(nodeTripleIndex.containsKey(triple.getPredicate())) 
			{
				HashSet<Triple> newSet = new HashSet<Triple>();
				newSet.addAll(nodeTripleIndex.get(triple.getPredicate()));
				newSet.add(triple);
				nodeTripleIndex.put(triple.getPredicate(), newSet);
				//nodeTripleIndex.get(triple.getPredicate()).add(triple);
			}

			else nodeTripleIndex.put(triple.getPredicate(), tripleSetA);

			if(nodeTripleIndex.containsKey(triple.getObject())) 
			{
				HashSet<Triple> newSet = new HashSet<Triple>();
				newSet.addAll(nodeTripleIndex.get(triple.getObject()));
				newSet.add(triple);
				nodeTripleIndex.put(triple.getObject(), newSet);
				//nodeTripleIndex.get(triple.getObject()).add(triple);
			}

			else nodeTripleIndex.put(triple.getObject(), tripleSetA);

			//System.out.println("Node triple index:" + nodeTripleIndex);
			tripleIndex.put(triple, nextIndex);
			reverseTripleIndex.put(nextIndex, triple);

			nextIndex++;
			if(!nodeIndex.containsKey(triple.getSubject()))
				nodeIndex.put(triple.getSubject(), nextNodeIndex++);

			if(!nodeIndex.containsKey(triple.getPredicate()))
				nodeIndex.put(triple.getPredicate(), nextNodeIndex++);

			if(!nodeIndex.containsKey(triple.getObject()))
				nodeIndex.put(triple.getObject(), nextNodeIndex++);

		}
	}

	public BasicPattern createBP(ArrayList<Integer> finalSet){

		BasicPattern newBp = new BasicPattern();

		for(Integer i : finalSet){			 	
			newBp.add(reverseTripleIndex.get(i-1));			 		
		}

		return newBp;
	}

	public List<Integer> findPlan(){

		Element pattern = query.getQueryPattern();
		ElementGroup g = (ElementGroup) pattern ;
		triplePathBlock = (ElementPathBlock) g.getElements().get(0);

		Stack<Integer> order = new Stack<Integer>(); //h seira poy 8a mpoyn oi tripletes
		
		HashMap<Double, Integer> hashMap = new HashMap<Double, Integer>(); //zeygaria apo selectivity kai ari8mo tripletas
		
		int tripleNo = 1; //se poio ari8mo tripletas eimai - px 1h

		//this will iterate all triple patterns.
		for(TriplePath triplePath : triplePathBlock.getPattern().getList()){

			Triple triple = triplePath.asTriple();	

			System.out.println("\nTriple: " + triple);

			String q ="SELECT ?p (COUNT(*) as ?count) WHERE {?s ?p ?o} GROUP BY ?p";
			Query queryObj = QueryFactory.create(q);
			double tp = 0;
			double predicateSelectivity;
			try (QueryExecution qexec = QueryExecutionFactory.create(queryObj, model)) {
				ResultSet results = qexec.execSelect();
				for ( ; results.hasNext() ; )
				{
					QuerySolution soln = results.nextSolution();
					RDFNode rdfNode = soln.get("?p");
					if (rdfNode.toString().equals(triple.getPredicate().toString())) { //einai to p toy query to p ths tripletas
						Integer count = soln.getLiteral("count").getInt(); 
						System.out.println("p: " + rdfNode.toString());
						System.out.println("count: " + count.toString());
						tp = count.intValue();
					}
				}
				System.out.println("tp: " + tp);
				if (triple.getPredicate().isVariable()) {
					predicateSelectivity = 1 - tp / model.size();
				} else {
					predicateSelectivity = tp / model.size();
				}
			}
			System.out.println("predicate selectivity: " + predicateSelectivity);

			double r = model.listResourcesWithProperty(null).toList().size();
			System.out.println("r: " + r);
			double subjectSelectivity;
			if (triple.getSubject().isVariable()) {
				subjectSelectivity = 1 - 1 / r;
			} else {
				subjectSelectivity = 1 / r;
			}
			System.out.println("subject selectivity: " + subjectSelectivity);

			/*
			 * briskw ola ta bounded predicates
			 */
			q ="SELECT ?o WHERE {?s ?p ?o FILTER ( bound (?p) ) }";
			queryObj = QueryFactory.create(q);
			ArrayList <RDFNode> bounded = new ArrayList<RDFNode>(); //bounded predicates
			try (QueryExecution qexec = QueryExecutionFactory.create(queryObj, model)) {
				ResultSet results = qexec.execSelect();
				for ( ; results.hasNext() ; )
				{
					QuerySolution soln = results.nextSolution();
					RDFNode rdfNode = soln.get("?p");
					bounded.add(rdfNode);
				}
			}
			double objectSelectivity;
			if (bounded.contains(triple.getPredicate())) {
				System.out.println("bounded");
				q ="SELECT ?p ?o WHERE {?s ?p ?o}";
				queryObj = QueryFactory.create(q);
				try (QueryExecution qexec = QueryExecutionFactory.create(queryObj, model)) {
					int occurrence = 0;
					int total = 0;
					ResultSet results = qexec.execSelect();
					for ( ; results.hasNext() ; )
					{
						QuerySolution soln = results.nextSolution();
						RDFNode rdfNodeP = soln.get("?p");
						RDFNode rdfNodeO = soln.get("?o");
						if (rdfNodeP.toString().equals(triple.getPredicate().toString())) {
							if (rdfNodeO.toString().equals(triple.getObject().toString())) {
								occurrence = occurrence + 1;
							}
							total++;
						}
					}
					int frequency = occurrence / total;
					objectSelectivity = frequency / tp;
				}
			} else {
				System.out.println("unbounded");
				q ="SELECT ?o WHERE {?s ?p ?o}";
				queryObj = QueryFactory.create(q);
				try (QueryExecution qexec = QueryExecutionFactory.create(queryObj, model)) {
					double occurrence = 0;
					double total = 0;
					ResultSet results = qexec.execSelect();
					for ( ; results.hasNext() ; )
					{
						QuerySolution soln = results.nextSolution();
						RDFNode rdfNodeO = soln.get("?o");
						if (rdfNodeO.toString().equals(triple.getObject().toString())) {
							occurrence = occurrence + 1;
						}
						total++;
					}
					double frequency = occurrence / total;
					objectSelectivity = frequency / tp;
				}
			}
			System.out.println("object selectivity: " + objectSelectivity);

			double selectivity = 0.33 * subjectSelectivity + 0.33 * predicateSelectivity + 0.33 * objectSelectivity;
			System.out.println("selectivity: " + selectivity);
			
			hashMap.put(new Double(selectivity), new Integer(tripleNo));
			tripleNo++;
		} //end for

		Map<Double, Integer> sortedMap = new TreeMap<Double, Integer>(hashMap); //sto TreeMap mpainoyn me au3oysa seira ws pros to selectivity
		for (Double key : sortedMap.keySet()) {
			order.push(hashMap.get(key)); //ta bazw se stoiba gia na paw apo ay3oysa se f8inoysa seira
		}
		return order;
	}

}
