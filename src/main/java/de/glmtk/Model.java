package de.glmtk;

import java.util.HashMap;
import java.util.Map;

import de.glmtk.smoothing.estimator.Estimator;
import de.glmtk.smoothing.estimator.Estimators;

/**
 * is being used for parmater -m by calling the programm
 * So it can be used for help and log messages but also
 * to controll which model / smoothing method should be
 * applied for the calculation
 * 
 */
public enum Model {

    MAXIMUM_LIKELIHOOD("MLE", "Maximum Likelihood", Estimators.MLE),

    KNESER_NEY("KN", "Kneser Ney", null),

    MODIFIED_KNESER_NEY("MKN", "Modified Kneser Ney",
            Estimators.MODIFIED_KNESER_NEY_ESIMATOR),

    GENERALIZED_LANGUAGE_MODEL("GLM", "Generalized Language Model", null);

    private String abbreviation;

    private String name;

    private Estimator estimator;

    private Model(
            String abbreviation,
            String name,
            Estimator estimator) {
        this.abbreviation = abbreviation;
        this.name = name;
        this.estimator = estimator;
    }

    public String getAbbreviation() {
        return abbreviation;
    }

    public String getName() {
        return name;
    }

    public Estimator getEstimator() {
        if (estimator == null) {
            throw new UnsupportedOperationException();
        }
        return estimator;
    }

    private static final Map<String, Model> abbreviationToModel;
    static {
        Map<String, Model> m = new HashMap<String, Model>();
        for (Model model : Model.values()) {
            m.put(model.abbreviation, model);
        }
        abbreviationToModel = m;
    }

    /**
     * Returns null on fail.
     */
    public static Model fromAbbreviation(String abbreviation) {
        return abbreviationToModel.get(abbreviation);
    }
}
