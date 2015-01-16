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

package de.glmtk.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.error.Mark;
import org.yaml.snakeyaml.events.Event;
import org.yaml.snakeyaml.events.Event.ID;
import org.yaml.snakeyaml.events.MappingStartEvent;
import org.yaml.snakeyaml.events.ScalarEvent;

import de.glmtk.Constants;
import de.glmtk.exceptions.FileFormatException;

public abstract class AbstractYamlParser {
    protected Path file;
    protected String fileType;
    protected Event event;
    protected Iterator<Event> iter;
    protected Mark mark;

    public AbstractYamlParser(Path file,
                              String fileType) {
        this.file = file;
        this.fileType = fileType;
        event = null;
        iter = null;
    }

    protected abstract void parse();

    public final void run() throws IOException {
        Yaml yaml = new Yaml();
        try (BufferedReader reader = Files.newBufferedReader(file,
                Constants.CHARSET)) {
            event = null;
            iter = yaml.parse(reader).iterator();
            parse();
        }
    }

    protected final void nextEvent() {
        event = iter.next();
        mark = event.getStartMark();
    }

    protected void parseBegining(String expectedTag) {
        nextEvent();
        assertEventIsId(ID.StreamStart);

        nextEvent();
        assertEventIsId(ID.DocumentStart);

        nextEvent();
        assertEventIsId(ID.MappingStart);
        String tag = ((MappingStartEvent) event).getTag();
        if (tag == null || !tag.equals(expectedTag))
            throw newFileFormatException("%s file needs to start with a '%s'.",
                                         StringUtils.capitalize(fileType), expectedTag);
    }

    protected void parseEnding() {
        assertEventIsId(ID.MappingEnd);

        nextEvent();
        assertEventIsId(ID.DocumentEnd);

        nextEvent();
        assertEventIsId(ID.StreamEnd);
    }

    protected String parseScalar() {
        assertEventIsId(ID.Scalar);
        ScalarEvent scalarEvent = (ScalarEvent) event;
        String result = scalarEvent.getValue();
        if (result.equals("null"))
            result = null;
        return result;
    }

    protected boolean parseBoolean() {
        String booleanStr = parseScalar();
        if (booleanStr.equals("true"))
            return true;
        else if (booleanStr.equals("false"))
            return false;
        throw newFileFormatException(
                                     "Illegal boolean value: '%s'. Valid are only 'true' or 'false'.",
                                     booleanStr);
    }

    protected int parseInt() {
        String intStr = parseScalar();
        try {
            return Integer.parseInt(intStr);
        } catch (NumberFormatException e) {
            throw newFileFormatException("Illegal integer number: '%s'.",
                                         intStr);
        }
    }

    protected long parseLong() {
        String longStr = parseScalar();
        try {
            return Long.parseLong(longStr);
        } catch (NumberFormatException e) {
            throw newFileFormatException("Illegal long number: '%s'.", longStr);
        }
    }

    protected Path parsePath() {
        String pathStr = parseScalar();
        try {
            return Paths.get(pathStr);
        } catch (InvalidPathException e) {
            throw newFileFormatException("Illegal path: '%s'.", pathStr);
        }
    }

    protected Set<String> parseSetScalar() {
        assertEventIsId(ID.SequenceStart);

        Set<String> result = new TreeSet<>();
        nextEvent();
        while (!event.is(ID.SequenceEnd)) {
            String scalar = parseScalar();
            if (result.contains(scalar))
                throw newFileFormatException(
                                             "Set contains value multiple times: '%s'.", scalar);
            result.add(scalar);
            nextEvent();
        }
        return result;
    }

    protected Map<String, Boolean> createValidKeysMap(String... keys) {
        Map<String, Boolean> result = new LinkedHashMap<>();
        for (String key : keys)
            result.put(key, false);
        return result;
    }

    protected void registerKey(Map<String, Boolean> keys,
                               String key) {
        if (!keys.containsKey(key)) {
            List<String> possible = new ArrayList<>();
            for (Entry<String, Boolean> entry : keys.entrySet())
                if (!entry.getValue())
                    possible.add(entry.getKey());
            throw newFileFormatException(
                                         "Illegal key '%s'. Possibles keys for this position are: '%s'.",
                                         key, StringUtils.join(possible, "', '"));
        }
        if (keys.get(key))
            throw newFileFormatException("Duplicate key '%s'.", key);
        keys.put(key, true);
    }

    protected final void assertEventIsId(ID expected) {
        if (!event.is(expected)) {
            ID actual = null;
            for (ID id : ID.values())
                if (event.is(id)) {
                    actual = id;
                    break;
                }

            if (actual == ID.Alias)
                throw newFileFormatException("Aliasing is not supported yet.");

            throw newFileFormatException(
                                         "Illegal '%s', expected '%s' instead.", idString(actual),
                                         idString(expected));
        }
    }

    private String idString(ID id) {
        StringBuilder result = new StringBuilder();
        for (char c : id.toString().toCharArray())
            if (Character.isUpperCase(c))
                result.append(' ').append(Character.toLowerCase(c));
            else
                result.append(c);
        return result.toString().substring(1);
    }

    protected final FileFormatException newFileFormatException(String message,
                                                               Object... params) {
        // If in first line, use end mark instead because of the tag at
        // beginning of file
        if (mark.getLine() == 0)
            mark = event.getEndMark();
        throw new FileFormatException(file, fileType, mark, message, params);
    }
}
