/**
 * 
 */
package de.uni_leipzig.simba.benchmarker;


/**
 * @author sherif
 *
 */
public class ModifierFactory {

	public static final String ABBREVIATION_MODIFIER = "abbreviation";
	public static final String ACRONYM_MODIFIER = "acronym";
	public static final String MERGE_MODIFIER = "merge";
	public static final String MISSPELING_MODIFIER = "misspelling";
	public static final String PERMUTATION_MODIFIER = "permutation";
	public static final String SPLIT_MODIFIER = "split";
	public static final String SYNONYM_MODIFIER = "synoym";

	public static Modifier getModifier(String name) {
		System.out.println("Getting Modifier with name "+name);

		if(name.equalsIgnoreCase(ABBREVIATION_MODIFIER))
			return new AbbreviationModifier();
		if(name.equalsIgnoreCase(ACRONYM_MODIFIER))
			return new AcronymModifier();
		if (name.equalsIgnoreCase(MERGE_MODIFIER))
			return new MergeModifier();
		if (name.equalsIgnoreCase(MISSPELING_MODIFIER))
			return new MisspellingModifier();
		if (name.equalsIgnoreCase(PERMUTATION_MODIFIER))
			return new PermutationModifier();
		if (name.equalsIgnoreCase(SPLIT_MODIFIER))
			return new SplitModifier();
		if (name.equalsIgnoreCase(SYNONYM_MODIFIER))
			return new SynonymModifier();
		
		System.out.println("Sorry, The Modifier " + name + " is not yet implemented ... Exit with error");
		System.exit(1);
		return null;
	}
}
