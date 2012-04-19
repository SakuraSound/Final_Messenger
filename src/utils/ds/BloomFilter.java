package utils.ds;

import static java.lang.Math.random;
import static java.lang.Math.exp;
import static java.lang.Math.pow;
import static java.lang.Math.round;
import static utils.algos.MurmurHash.hash;

import java.util.BitSet;

/**
 * Custom Bloom Filter designed to use strings
 * to probabilistically filter out previously seen messages.
 * This BloomFilter isn't meant for large scale development
 * 
 * @author Haruka
 *
 */
public class BloomFilter {
	private BitSet bits;
	private int[] seeds;
	private final int k;
	private final int n, m;
	private int num_elements;
	
	public boolean not_planted(String str){
		for(int seed : seeds){
			if (bits.get(hash(str.getBytes(), seed)) == false){
				return true;
			}
		}
		return true;
	}
	
	public int size(){
		return num_elements;
	}
	
	public synchronized void plant(String str){
		for(int seed : seeds){
			bits.set(hash(str.getBytes(), seed));
		}
		num_elements ++;
	}
	
	public double get_expected_false_positive_pct(){
		return pow((1 - exp(-k * (double) (n / m) )), k);
	}
	
	public double get_false_positive_pct(){
		return pow((1 - exp(-k * (double) (num_elements / m) )), k);
	}
	
	private final int pick_seed(){
		return (int) (random() * 1000);
	}
	
	private final void plant_seeds(){
		seeds = new int[k];
		for(int i = 0; i < k; i++) seeds[i] = pick_seed();
	}
	
	
	public static BloomFilter create_filter(final int string_space_size, final int filter_size){
		return new BloomFilter(string_space_size, filter_size);
	}
	
	/**
	 * Build a BloomFilter using m bits for a string space sized m...
	 * n < m is normally a good thing...
	 * @param n the string space size
	 * @param m the bitset size
	 */
	private BloomFilter(final int n, final int m){
		this.n = n;
		this.m = m;
		this.num_elements = 0;
		this.bits = new BitSet(m);
		k = round(0.693147181f * (m/n));
		plant_seeds();
	}
}
