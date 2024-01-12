package technology.rocketjump.undermount.mapgen.generators;

import java.util.Random;

public class SimplexNoise {

	SimplexNoiseOctave[] octaves;
	double[] frequencys;
	double[] amplitudes;

	int largestFeature;
	double persistence;

	public SimplexNoise(int largestFeature, double persistence, Random random){
		this.largestFeature=largestFeature;
		this.persistence=persistence;

		//recieves a number (eg 128) and calculates what power of 2 it is (eg 2^7)
		int numberOfOctaves=(int)Math.ceil(Math.log10(largestFeature)/Math.log10(2));

		octaves=new SimplexNoiseOctave[numberOfOctaves];
		frequencys=new double[numberOfOctaves];
		amplitudes=new double[numberOfOctaves];

		for(int i=0;i<numberOfOctaves;i++){
			octaves[i]=new SimplexNoiseOctave(random);

			frequencys[i] = Math.pow(2,i);
			amplitudes[i] = Math.pow(persistence,octaves.length-i);
		}
	}


	public double getNoise(int x, int y){
		double result=0;

		for(int i=0;i<octaves.length;i++){
			//double frequency = Math.pow(2,i);
			//double amplitude = Math.pow(persistence,octaves.length-i);

			result=result+octaves[i].noise(x/frequencys[i], y/frequencys[i])* amplitudes[i];
		}
		return result;
	}

	public double getNoise(int x,int y, int z){
		double result=0;

		for(int i=0;i<octaves.length;i++){
			double frequency = Math.pow(2,i);
			double amplitude = Math.pow(persistence,octaves.length-i);

			result=result+octaves[i].noise(x/frequency, y/frequency,z/frequency)* amplitude;
		}
		return result;
	}
}
