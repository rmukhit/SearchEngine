import java.util.Map;
import java.util.TreeMap;

/*
 * ArgumentMap Class that creates a key-value map of arguments of the program.
 */
public class ArgumentMap {
	
	private Map<String, String> arg;
	
	/** 
	 * Constructor that ads everything to map.
	 * @param args array of Strings 
	 */
	public ArgumentMap(String[] args) {
		arg = new TreeMap<String,String>();
		for(int i = 0; i <args.length; i++) {
			if (isFlag(args[i]) == true) {
				arg.put(args[i], null);
			} else if (isValue(args[i]) == true) {
				if (i == 0) {
				} else if (isFlag(args[i-1]) == true) {
					arg.put(args[i-1], args[i]);	
				}
			}
		}
	}
	
	/** 
	 * Function that returns boolean whether some key is in map
	 * @param key String
	 * @return boolean
	 */
	public boolean containsKey(String key) {
		return arg.containsKey(key);
	}
	
	/** 
	 * Getter that returns value from map
	 * @param key String
	 * @return String
	 */
	public String get(String key) {
		return arg.get(key);
	}	
	
	/** 
	 * Function that checks that argument is Flag (i.e -path, -index etc)
	 *
	 * @param arg
	 * @return boolean
	 */
	public static boolean isFlag(String arg) {
		if (arg == null || arg == "") {
			return false;
		}
		String firstLetter = String.valueOf(arg.charAt(0));
		if (arg.length() > 1 && "-".equals(firstLetter)) {
			if ((arg.charAt(1) > 32 && arg.charAt(1) <= 122)) {
				return true;
			}
		}
		return false; 
	}
	
	/** 
	 * Function that checks that argument is Value (i.e path, index etc)
	 *
	 * @param arg
	 * @return boolean
	 */	
	public static boolean isValue(String arg) {

		if (arg == null || arg == "" || arg == " ") {
			return false;
		}
		String firstLetter = String.valueOf(arg.charAt(0));
		if ("-".equals(firstLetter)) {
			return false;
		}
		return true; 
	}
	

}
