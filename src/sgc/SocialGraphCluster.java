package sgc;

import java.io.IOException;

import sgc.eval.EvolvingExperimenter;
import sgc.eval.StaticExperimenter;
     
public class SocialGraphCluster {
		
	public static void main(String[] args) throws IOException {
		
		StaticExperimenter staticExperimenter = new StaticExperimenter();
		EvolvingExperimenter evolvingExperimenter = new EvolvingExperimenter();

		//Choose one of the above experiment
		
		staticExperimenter.experiment1();
//		staticExperimenter.experiment2();
//		staticExperimenter.experiment3();
//		
//		evolvingExperimenter.synthDatasetExp();
//		evolvingExperimenter.deliciousExp();
		
		System.out.println("Experiment Finished!");
	}

}
