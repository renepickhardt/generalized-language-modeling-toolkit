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

package de.glmtk.testutil;

import static de.glmtk.output.Output.enableOutputFormatting;
import static de.glmtk.util.LoggingHelper.LOGGING_HELPER;

import org.apache.logging.log4j.core.appender.ConsoleAppender.Target;
import org.junit.BeforeClass;

import de.glmtk.GlmtkPaths;

public class LoggingTest {
    @BeforeClass
    public static void setUpLogging() {
        LOGGING_HELPER.addFileAppender(GlmtkPaths.LOG_DIR.resolve("test"),
                "FileTest", false);
        LOGGING_HELPER.addConsoleAppender(Target.SYSTEM_OUT);
    }

    @BeforeClass
    public static void setUpOutput() {
        enableOutputFormatting();
    }
}
