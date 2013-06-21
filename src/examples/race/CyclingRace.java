package race;

public class CyclingRace
{
	public static void main(String[] args)
	{
		Cyclist[] cyclists = new Cyclist[3];
		
		Cyclist cl = null;
		
		// insert raw data for cyclists
		
		cl = new Cyclist();
		cl.idnum = 2;
		cl.time = 3725;
		cl.bonus = 5;
		cyclists[0] = cl;
		
		cl = new Cyclist();
		cl.idnum = 56;
		cl.time = 3569;
		cl.bonus = 10;
		cyclists[1] = cl;
		
		cl = new Cyclist();
		cl.idnum = 123;
		cl.time = 3766;
		cl.bonus = 50;
		cyclists[2] = cl;
		
		
		Cyclist[] results = new Cyclist[3];
		
		int diff = 0;
		
		// compute results of the competition
        for (int i = 0; i < cyclists.length; ++i) {
			diff = cyclists[i].time - cyclists[i].bonus;

            for (int j = cyclists.length - 1; j >= diff; --j) {
                results[j + 1] = results[j];
            }

			results[diff] = cyclists[i];
		}

        int[] diffs = new int[3];
		
		Cyclist bestCL = results[0];
		int bestTime = bestCL.time - bestCL.bonus;
        diffs[0] = bestTime;
		for (int i = 1; i < results.length; ++i) {
			cl = results[i];
			diff = cl.time - cl.bonus - bestTime;
            diffs[i] = diff;
		}
	}
}

class Cyclist 
{
	public int idnum;
	public int time;
	public int bonus;
}
