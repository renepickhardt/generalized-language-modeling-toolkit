package de.glmtk;

import java.util.HashMap;
import java.util.Map;

import de.glmtk.querying.estimator.Estimator;
import de.glmtk.querying.estimator.Estimators;

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

    MODIFIED_KNESER_NEY("MKN", "Modified Kneser Ney", Estimators.MOD_KNESER_NEY),

    MODIFIED_KNESERY_NEY_ABS("MKNA", "Modified Kneser Ney Abs",
            Estimators.MOD_KNESER_NEY_ABS),

    GENERALIZED_LANGUAGE_MODEL("GLM", "Generalized Language Model",
            Estimators.GLM),

    GENERALIZED_LANGUAGE_MODEL_ABS("GLMA", "Generalized Language Model Abs",
            Estimators.GLM_ABS);

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

    private static final Map<String, Model> ABBREVIATION_TO_MODEL;
    static {
        Map<String, Model> m = new HashMap<String, Model>();
        for (Model model : Model.values()) {
            m.put(model.abbreviation, model);
        }
        ABBREVIATION_TO_MODEL = m;
    }

    /**
     * Returns {@code null} on fail.
     */
    public static Model fromAbbreviation(String abbreviation) {
        return ABBREVIATION_TO_MODEL.get(abbreviation);
    }
}
