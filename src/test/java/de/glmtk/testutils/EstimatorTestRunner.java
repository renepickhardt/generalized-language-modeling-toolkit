package de.glmtk.testutils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.runner.Runner;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.junit.runners.Suite;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.Statement;

import de.glmtk.querying.EstimatorTest;
import de.glmtk.querying.ProbMode;
import de.glmtk.querying.estimator.Estimator;
import de.glmtk.querying.estimator.Estimators;

/**
 * Highly specialized JUnit-Test Runner, so we can have nice code, output and
 * eclipse JUnit view for {@link EstimatorTest}.
 *
 * This class is heavily based on JUnits {@link Parameterized} Runner and
 * behaves similarly.
 */
public class EstimatorTestRunner extends Suite {

    public static class EstimatorTestParameters {

        public Estimator estimator;

        public boolean continuationEstimator;

        public int condOrder;

        public int margOrder;

        public EstimatorTestParameters(
                Estimator estimator,
                boolean continuationEstimator,
                int condOrder,
                int margOrder) {
            this.estimator = estimator;
            this.continuationEstimator = continuationEstimator;
            this.condOrder = condOrder;
            this.margOrder = margOrder;
        }

    }

    public class TestRunnerForEstimator extends Suite {

        private class TestRunnerForProbMode extends BlockJUnit4ClassRunner {

            private ProbMode probMode;

            private int highestOrder;

            public TestRunnerForProbMode(
                    Class<?> type,
                    ProbMode probMode,
                    int highestOrder) throws InitializationError {
                super(type);
                this.probMode = probMode;
                this.highestOrder = highestOrder;
            }

            @Override
            public Object createTest() throws InstantiationException,
            IllegalAccessException, IllegalArgumentException,
            InvocationTargetException {
                return getTestClass().getOnlyConstructor().newInstance(
                        estimatorName, estimator, continuationEstimator,
                        probMode, highestOrder);
            }

            @Override
            protected String getName() {
                switch (probMode) {
                    case COND:
                        return "Conditional";
                    case MARG:
                        return "Marginal";
                    default:
                        throw new IllegalStateException();
                }
            }

            @Override
            protected String testName(FrameworkMethod method) {
                return TestRunnerForEstimator.this.getName() + " (" + getName()
                        + "): " + method.getName();
            }

            @Override
            protected void validateConstructor(List<Throwable> errors) {
                validateOnlyOneConstructor(errors);
                Constructor<?> constructor =
                        getTestClass().getJavaClass().getConstructors()[0];
                Class<?>[] types = constructor.getParameterTypes();
                if (types.length != 5 || !types[0].equals(String.class)
                        || !types[1].equals(Estimator.class)
                        || !types[2].equals(boolean.class)
                        || !types[3].equals(ProbMode.class)
                        || !types[4].equals(int.class)) {
                    errors.add(new Exception(
                            "Test class constructor should take exactly these arguments: (String, Estimator, boolean, ProbMode, integer)"));
                }
            }

            @Override
            protected Statement classBlock(RunNotifier notifier) {
                return childrenInvoker(notifier);
            }

            @Override
            protected Annotation[] getRunnerAnnotations() {
                return new Annotation[0];
            }

        }

        private Estimator estimator;

        private boolean continuationEstimator;

        private String estimatorName;

        private final List<TestRunnerForProbMode> runners;

        public TestRunnerForEstimator(
                Class<?> type,
                EstimatorTestParameters parameters) throws InitializationError {
            super(type, Collections.<Runner> emptyList());

            estimator = parameters.estimator;
            continuationEstimator = parameters.continuationEstimator;
            estimatorName = Estimators.getName(estimator);

            runners = new ArrayList<TestRunnerForProbMode>();
            createRunners(parameters);
        }

        @Override
        protected String getName() {
            return estimatorName;
        }

        @SuppressWarnings({
            "unchecked", "rawtypes"
        })
        @Override
        protected List<Runner> getChildren() {
            return (List) runners;
        }

        private void createRunners(EstimatorTestParameters parameters)
                throws InitializationError {
            if (parameters.condOrder != 0) {
                runners.add(new TestRunnerForProbMode(getTestClass()
                        .getJavaClass(), ProbMode.COND, parameters.condOrder));
            }
            if (parameters.margOrder != 0) {
                runners.add(new TestRunnerForProbMode(getTestClass()
                        .getJavaClass(), ProbMode.MARG, parameters.margOrder));
            }
        }

    }

    private List<TestRunnerForEstimator> runners;

    public EstimatorTestRunner(
            Class<?> type) throws Throwable {
        super(type, Collections.<Runner> emptyList());
        runners = new ArrayList<TestRunnerForEstimator>();
        createRunnersForParameters(allParameters());
    }

    @SuppressWarnings({
        "unchecked", "rawtypes"
    })
    @Override
    protected List<Runner> getChildren() {
        return (List) runners;
    }

    @SuppressWarnings("unchecked")
    private Iterable<EstimatorTestParameters> allParameters() throws Throwable {
        Object parameters = getParametersMethod().invokeExplosively(null);
        if (parameters instanceof Iterable<?>) {
            return (Iterable<EstimatorTestParameters>) parameters;
        }

        throw parametersMethodReturnedWrongType();
    }

    private FrameworkMethod getParametersMethod() throws Exception {
        List<FrameworkMethod> methods =
                getTestClass().getAnnotatedMethods(Parameters.class);
        for (FrameworkMethod method : methods) {
            if (method.isStatic() && method.isPublic()) {
                return method;
            }
        }

        throw new Exception("No public static parameters method on class "
                + getTestClass().getName());
    }

    private void createRunnersForParameters(
            Iterable<EstimatorTestParameters> allParameters)
                    throws InitializationError {
        for (EstimatorTestParameters parameters : allParameters) {
            runners.add(new TestRunnerForEstimator(getTestClass()
                    .getJavaClass(), parameters));
        }
    }

    private Exception parametersMethodReturnedWrongType() throws Exception {
        return new Exception(getTestClass().getName() + "."
                + getParametersMethod().getName()
                + "() must return an Iterable of "
                + EstimatorTestParameters.class.getSimpleName() + ".");
    }

}
