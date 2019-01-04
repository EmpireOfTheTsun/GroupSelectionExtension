import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.Random;

public class GroupSelection {

	double coopGrowth = 0.018;
	double coopCons = 0.1;
	double selfGrowth = 0.02;
	double selfCons = 0.2;
	double deathRate = 0.1;
	int smallResources = 4;
	int smallGroupSize = 4;
	int largeResources = 50;
	int largeGroupSize = 40;
	int popSize = 4000;
	int noGenerations = 120;
	int timeSteps = 4;
	Random rand = new Random();
	ArrayList<String> fig1 = new ArrayList<String>(); //Lazy! Sorry if you're reading this.
	ArrayList<String> fig2 = new ArrayList<String>();

	
	public static void main(String[] args) throws IOException {
		new GroupSelection();
	}
	
	GroupSelection() throws IOException{
		ArrayList<Group> pool = createMigrantPool(popSize/4,popSize/4,popSize/4,popSize/4);
		for(int x=0; x<noGenerations; x++) {
			pool = reproduction(pool, x+1);
		}
    	File fout = new File("fig1.csv");
    	FileOutputStream fos = new FileOutputStream(fout);
    	BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos));
		for(String s : fig1) {
			bw.write(s);
			bw.newLine();
		}
		bw.close();
    	fout = new File("fig2.csv");
    	fos = new FileOutputStream(fout);
    	bw = new BufferedWriter(new OutputStreamWriter(fos));
		for(String s : fig2) {
			bw.write(s);
			bw.newLine();
		}
		bw.close();
		System.out.println("dun");
	}
	
	private ArrayList<Group> createMigrantPool(int maxSmallSelf, int maxSmallCoop, int maxLargeSelf, int maxLargeCoop) {
		int sum = maxSmallSelf+maxSmallCoop+maxLargeSelf+maxLargeCoop;
		if(sum != popSize) {
			double scaleFactor = (double) popSize / (double)sum;
			maxSmallSelf = (int) Math.round((double)maxSmallSelf * scaleFactor);
			maxSmallCoop = (int) Math.round((double)maxSmallCoop * scaleFactor);
			maxLargeSelf = (int) Math.round((double)maxLargeSelf * scaleFactor);
			maxLargeCoop = (int) Math.round((double)maxLargeCoop * scaleFactor);
			sum = maxSmallSelf+maxSmallCoop+maxLargeSelf+maxLargeCoop+0; //debugging
			while (sum < popSize) {
				int max = Math.max(maxSmallCoop, Math.max(maxLargeCoop, Math.max(maxSmallSelf, maxLargeSelf)));
				if(maxSmallCoop == max) {
					maxSmallCoop++;
				}
				else if(maxSmallSelf == max) {
					maxSmallSelf++;
				}
				else if(maxLargeSelf == max) {
					maxLargeSelf++;
				}
				else{
					maxLargeCoop++;
				}
				sum++;
				System.out.println("oof");
			}
		}
		ArrayList<Group> groupsList = new ArrayList<>();
		int smallSelf = 0;
		int smallCoop = 0;
		int largeSelf = 0;
		int largeCoop = 0;
		for (int x=0; x<sum; x++) {
			boolean valid = false;
			while(!valid) {
				valid = true;
				int decision = rand.nextInt(4);
				if(decision == 0) {
					if(maxSmallSelf == 0) {
						valid = false;
						continue;
					}					
					smallSelf++;
					maxSmallSelf--;
					if(smallSelf + smallCoop == smallGroupSize) {
						//add new group to list and reset waiting indivs
						groupsList.add(new Group(smallSelf, smallCoop));
						smallSelf = smallCoop = 0;
					}
				}
				else if(decision == 1) {
					if(maxSmallCoop == 0) {
						valid = false;
						continue;
					}
					smallCoop++;
					maxSmallCoop--;
					if(smallSelf + smallCoop == smallGroupSize) {
						groupsList.add(new Group(smallSelf, smallCoop));
						smallSelf = smallCoop = 0;
					}
				}
				else if (decision == 2) {
					if(maxLargeSelf == 0) {
						valid = false;
						continue;
					}
					largeSelf++;
					maxLargeSelf--;
					if(largeSelf + largeCoop == largeGroupSize) {
						groupsList.add(new Group(largeSelf, largeCoop));
						largeSelf = largeCoop = 0;
					}
				}
				else {
					if(maxLargeCoop == 0) {
						valid = false;
						continue;
					}
					largeCoop++;
					maxLargeCoop--;
					if(largeSelf + largeCoop == largeGroupSize) {
						groupsList.add(new Group(largeSelf, largeCoop));
						largeSelf = largeCoop = 0;
					}					
				}
			}
		}
		return groupsList;
	}
	
	public class Group{
		
		int selfPop; //no of selfish
		int coopPop; //no of co-op
		
		public Group(int self, int coop) {
			selfPop = self;
			coopPop = coop;
		}
		
	}
	
	private ArrayList<Group> reproduction(ArrayList<Group> pool, int round) {
		int poolSS = 0;
		int poolSC = 0;
		int poolLS = 0;
		int poolLC = 0;
		ArrayList<Double> selfishResourcesList = new ArrayList<Double>();
		ArrayList<Double> totalResourcesList = new ArrayList<Double>();

		for (Group group : pool) {
			int noSelfish = group.selfPop;
			int noCoop = group.coopPop;
			for (int t=0; t < timeSteps; t++) {		
				double noResources = getResources(noSelfish + noCoop); //this is constant for now (4 / 50)
				double selfishResourcesShare = calculateSelfishResourceShare(noSelfish, noCoop);
				double selfishResources = selfishResourcesShare * noResources;
				double coopResources = noResources - selfishResources;
				noSelfish = reproduce(noSelfish, selfishResources, selfCons);
				noCoop = reproduce(noCoop, coopResources, coopCons);
				//This USED to be done at last time step. Maybe better like that?
				if(t == timeSteps -1) {
					selfishResourcesList.add(selfishResources); //adds selfish % of resources at last timestamp. Could use t=0!
					totalResourcesList.add((double)noResources);
				}
			}
			if (group.coopPop+group.selfPop == largeGroupSize) {
				poolLS += noSelfish;
				poolLC += noCoop;
			}
			else {
				poolSS += noSelfish;
				poolSC += noCoop;
			}
		}
		System.out.println("Round "+round+": SS="+poolSS+" SC="+poolSC+" LS="+poolLS+" LC="+poolLC);
		double size = poolSS + poolSC + poolLS + poolLC;
		double largeGroup = poolLS + poolLC;
		double largeGroupPercent = largeGroup/(double)size;
		double selfishResources = 0.0;
		for(Double selfish : selfishResourcesList) {
		    selfishResources += selfish;
		}
		double totalResources = 0.0;
		for(Double total : totalResourcesList) {
		    totalResources += total;
		}
		double selfishResourcesPercent = selfishResources / totalResources; //% of resources used for 1st fig
		fig1.add(largeGroupPercent+","+selfishResourcesPercent); //% of large size for 1st fig
		fig2.add((double)poolSC/size+","+(double)poolLC/size+","+(double)poolSS/size+","+(double)poolLS/size); //% of pop for 2nd figure
		//System.out.println(poolSS/size+","+poolSC/size+","+poolLS/size+","+poolLC/size); //% of pop for 2nd figure
		return createMigrantPool(poolSS, poolSC, poolLS, poolLC);
	}
	
	//function (1) on pg4
	//returns % of resources that the selfish individuals will use.
	//to get co-op, do 1 - this
	private double calculateSelfishResourceShare(int noSelfish, int noCoop) {
		double numerator = (double)noSelfish * selfGrowth * selfCons;
		double denominator = numerator + ((double)noCoop * coopGrowth * coopCons);
		return numerator/denominator;
	}
	
	//function (2) on pg4
	private int reproduce(int currentPop, double resources, double cons) {
		int births = (int) Math.floor(resources/cons); //math.floor because can't make full indiv from 0.51% of resoure
		int deaths = (int) Math.round(deathRate * (double)currentPop);
		return currentPop + births - deaths;
	}
	
	private double getResources(int size) {
		if (size == largeGroupSize) {
			return largeResources;
		}
		else if (size == smallResources) {
			return smallResources;
		}
		else if (size > largeResources) {
			return getLargeResources(size);
		}
		else{
			return getSmallResources(size);
		}
	}

	//size = 4 * 2^k
	//resources = 4 * 2.1^k
	//solve for k to find resources
	//"per capita resources increases by 5% as population doubles"
	private double getSmallResources(double size) {
		double k = Math.log(size/4) / Math.log(2); //does log of base 2 of size/4
		double r = 4 * Math.pow(2.1, k);
		return r;
	}

	//size = 40 * 2^k
	//resources = 50 * 2.1^k
	//need separate because the values given in spec are inconsistent
	private double getLargeResources(double size) {
		double k = Math.log(size/40) / Math.log(2); //does log of base 2 of size/40
		double r = 50 * Math.pow(2.1, k);
		return r;
	}
	
}
