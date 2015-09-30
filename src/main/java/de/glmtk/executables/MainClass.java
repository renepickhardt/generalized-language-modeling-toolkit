/*
 * Generalized Language Modeling Toolkit (GLMTK)
 *
 * Copyright (C) 2014-2015 Lukas Schmelzeisen
 *
 * GLMTK is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * GLMTK is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * GLMTK. If not, see <http://www.gnu.org/licenses/>.
 *
 * See the AUTHORS file for contributors.
 */

package de.glmtk.executables;

import java.util.Arrays;


/**
 * GLMTK entry point for all executables.
 */
public class MainClass {
    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            throw new IllegalArgumentException("No main class specified.");
        }

        String mainClass = args[0];
        String[] newArgs = Arrays.copyOfRange(args, 1, args.length);
        switch (mainClass) {
            case "glmtk":
                GlmtkExecutable.main(newArgs);
                break;

            case "glmtk-delunk":
                GlmtkDelUnk.main(newArgs);
                break;

            case "glmtk-exp-setup":
                GlmtkExpSetupExecutable.main(newArgs);
                break;

            case "glmtk-exp-estimatortime":
                GlmtkExpEstimatorTimeExecutable.main(newArgs);
                break;

            case "glmtk-exp-argmaxcompare":
                GlmtkExpArgmaxCompare.main(newArgs);
                break;

            case "glmtk-autocompletion-demo":
                GlmtkAutocompletionDemo.main(newArgs);
                break;

            default:
                // TODO: when can this happen? if it happens is the error
                // message helpful?
                throw new IllegalArgumentException(String
                    .format("Unkown main class specified: '%s'.", mainClass));
        }
    }
}
