/**
 * Created by MuhammadGumilang on 23/09/2016.
 */
import com.google.gson.Gson;
import spark.ModelAndView;
import spark.Request;
import spark.Spark;
import spark.template.velocity.VelocityTemplateEngine;
import java.util.HashMap;
import java.util.Map;

import static spark.Spark.*;

public class Main {
    public static ModelAndView prepareMV(Graph oldGraph, Graph newGraph, int iteration, String algo) {
        Map<String, Object> model = new HashMap<>();
        Gson gson = new Gson();
        String oldJson = gson.toJson(oldGraph);
        String newJson = gson.toJson(newGraph);

        model.put("oldData", oldJson);
        model.put("data", newJson);
        model.put("iteration", iteration);
        model.put("conflicts", newGraph.getConflicts());
        model.put("conflictsTS", newGraph.getConflictsTS());
        model.put("eff", newGraph.getEffectivity());
        model.put("effTS", newGraph.getEffectivityTS());
        model.put("algorithm", algo);
        return new ModelAndView(model, "index.vm");
    }

    public static ModelAndView prepareHC(Graph initGraph) {
        HillClimbing hc = new HillClimbing(initGraph);
        hc.run();
        Graph hcGraph = hc.getGraph();

        return prepareMV(initGraph, hcGraph, hc.getIteration(), "Hill Climbing");
    }

    public static ModelAndView prepareSA(Request req, Graph initGraph) {
        double temp = !req.queryParams("temp").equals("") ? Double.parseDouble(req.queryParams("temp")) : 10000;
        double decr = !req.queryParams("decr").equals("") ? Double.parseDouble(req.queryParams("decr")) : 0.97;
        SimulatedAnnealing sa = new SimulatedAnnealing(initGraph, temp, decr);
        sa.run();
        Graph saGraph = sa.getGraph();

        return prepareMV(initGraph, saGraph, sa.getIteration(), "Simulated Annealing");
    }

    public static ModelAndView prepareGA(Request req, Graph initGraph) {
        boolean best = !req.queryParams("best").equals("") || (req.queryParams("best").equals("T"));
        boolean steady = req.queryParams("steady").equals("") || (req.queryParams("steady").equals("T"));
        int iteration = !req.queryParams("iteration").equals("") ? Integer.parseInt(req.queryParams("iteration")) : 10;
        int chrom = !req.queryParams("chrom").equals("") ? Integer.parseInt(req.queryParams("chrom")) : 50;

        DNA dna = new DNA(initGraph, chrom, true);
        GeneticAlgorithm ga = new GeneticAlgorithm(best, steady, "uniform", "roulette");

        int generationCount = 0;
        while (dna.getFittestChromosome().getFitness() < dna.getFittestChromosome().getMaxFitness() && generationCount < iteration) {
            generationCount++;
            System.out.println("Generation: " + generationCount + " Fittest: " + dna.getFittestChromosome().getFitness());
            dna = ga.evolve(dna);
        }
        generationCount++;

        return prepareMV(initGraph, dna.getFittestChromosome().getGraph(), generationCount, "Genetic Algorithm");
    }

    public static ModelAndView prepareInit(Graph initGraph) {
        return prepareMV(initGraph, initGraph, 0, "Random Initialize");
    }

    public static void main(String[] args) {
        Spark.staticFileLocation("/public");
        Graph initGraph = new Graph("input.txt");
        initGraph.randomInitialize();

        get("/", (req, res) -> {
            return prepareInit(initGraph);
        }, new VelocityTemplateEngine());

        get("/HC", (req, res) -> {
            return prepareHC(initGraph);
        }, new VelocityTemplateEngine());

        get("/SA", (req, res) -> {
            return prepareSA(req, initGraph);
        }, new VelocityTemplateEngine());

        get("/GA", (req, res) -> {
            return prepareGA(req, initGraph);
        }, new VelocityTemplateEngine());

        post("/alter", (req, res) -> {
            Gson gson = new Gson();
            Graph graph = gson.fromJson(req.body(), Graph.class);
            return ("{ \"conflicts\": " + graph.getConflicts()
                    + ", \"conflictsTS\": " + graph.getConflictsTS()
                    + ", \"eff\": " + graph.getEffectivity()
                    + ", \"effTS\": " + graph.getEffectivityTS()
                    + "}");
        });
    }
}
