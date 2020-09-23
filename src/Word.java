/*
 * Class Word 
 */
public class Word implements Comparable<Word> {
	
	//private variables
	private int frequency;
	private int position;
	private String location;
	
	/**
	 * Constructor that build object from parameters
	 * @param int frequency - how many times word appeared in file
	 * @param int position - on which position it firstly appeared
	 * @param String location - path to a file where word appeared
	 */
	public Word (int frequency, int position, String location) {
		this.frequency = frequency;
		this.position = position;
		this.location = location;
	}
	
	/**
	 * Getter that returns number of apperances in file
	 * @return int
	 */
	public int getFrequency() {
		return this.frequency;
	}
	
	/**
	 * Getter that returns position of a word in a file
	 * @return int
	 */
	public int getPosition() {
		return this.position;
	}
	
	/**
	 * Getter that returns path to a file where word located
	 * @return String
	 */
	public String getLocation() {
		return this.location;
	}
	
	/**
	 * Method that overrides compareTo function according to project description
	 * @param Word other - the word with which we compare this.word
	 * @return int
	 */
	@Override
	public int compareTo(Word other) {
		int comparison;
		
		if (this.frequency == other.frequency) {
			comparison = 0;
		} else if (this.frequency < other.frequency){
			comparison = 1;
		} else {
			comparison = -1;
		}
		
	    if (comparison != 0) {
    			return comparison;
    		} else {
    			comparison = Integer.compare(this.position, other.position);
    		    if (comparison != 0) {
    	    			return comparison;
    	    		} else {
    	    			comparison = this.location.compareTo(other.location);
    	    			return comparison;
    	    		}
    		}

	}
	
}
