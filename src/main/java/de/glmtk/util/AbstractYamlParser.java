package de.glmtk.util;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.yaml.snakeyaml.events.Event;
import org.yaml.snakeyaml.events.Event.ID;
import org.yaml.snakeyaml.events.MappingStartEvent;
import org.yaml.snakeyaml.events.ScalarEvent;

import de.glmtk.exceptions.FileFormatException;

public abstract class AbstractYamlParser {
    protected void parseBegining(Event event,
                                 Iterator<Event> iter,
                                 String expectedTag) {
        if (!event.is(ID.StreamStart))
            throw new FileFormatException("Expected Stream Start.");

        event = iter.next();
        if (!event.is(ID.DocumentStart))
            throw new FileFormatException("Expected DocumentStart.");

        event = iter.next();
        if (!event.is(ID.MappingStart))
            throw new FileFormatException("Expected MappingStart.");
        String tag = ((MappingStartEvent) event).getTag();
        if (tag == null || !tag.equals(expectedTag))
            throw new FileFormatException("Expected file to start with tag.");
    }

    protected void parseEnding(Event event,
                               Iterator<Event> iter) {
        if (!event.is(ID.MappingEnd))
            throw new FileFormatException("Expected MappingEnd.");

        event = iter.next();
        if (!event.is(ID.DocumentEnd))
            throw new FileFormatException("Expected DocumentEnd.");

        event = iter.next();
        if (!event.is(ID.StreamEnd))
            throw new FileFormatException("Expected StreamEnd.");
    }

    protected String parseScalar(Event event,
                                 @SuppressWarnings("unused") Iterator<Event> iter) {
        if (!event.is(ID.Scalar))
            throw new FileFormatException("Expected ScalarEvent.");
        ScalarEvent scalarEvent = (ScalarEvent) event;
        String result = scalarEvent.getValue();
        if (result.equals("null"))
            result = null;
        return result;
    }

    protected boolean parseBoolean(Event event,
                                   Iterator<Event> iter) {
        String booleanStr = parseScalar(event, iter);
        return Boolean.valueOf(booleanStr);
    }

    protected int parseInt(Event event,
                           Iterator<Event> iter) {
        String intStr = parseScalar(event, iter);
        try {
            return Integer.parseInt(intStr);
        } catch (NumberFormatException e) {
            throw new FileFormatException("Illegal int.");
        }
    }

    protected long parseLong(Event event,
                             Iterator<Event> iter) {
        String longStr = parseScalar(event, iter);
        try {
            return Long.parseLong(longStr);
        } catch (NumberFormatException e) {
            throw new FileFormatException("Illegal long.");
        }
    }

    protected Path parsePath(Event event,
                             Iterator<Event> iter) {
        String pathStr = parseScalar(event, iter);
        try {
            return Paths.get(pathStr);
        } catch (InvalidPathException e) {
            throw new FileFormatException("Illegal path.");
        }
    }

    protected Set<String> parseSetScalar(Event event,
                                         Iterator<Event> iter) {
        if (!event.is(ID.SequenceStart))
            throw new FileFormatException("Expected SequenceStart.");

        Set<String> result = new TreeSet<>();
        event = iter.next();
        while (!event.is(ID.SequenceEnd)) {
            result.add(parseScalar(event, iter));
            event = iter.next();
        }
        return result;
    }

    protected Map<String, Boolean> createValidKeysMap(String... keys) {
        Map<String, Boolean> result = new HashMap<>();
        for (String key : keys)
            result.put(key, false);
        return result;
    }

    protected void registerKey(Map<String, Boolean> keys,
                               String key) {
        if (!keys.containsKey(key))
            throw new FileFormatException("Illegal key: " + key);
        if (keys.get(key))
            throw new FileFormatException("Duplicate key: " + key);
        keys.put(key, true);
    }
}
