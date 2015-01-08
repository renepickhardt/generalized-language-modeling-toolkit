package de.glmtk.common;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
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

    protected Pattern parsePattern(Event event,
                                   Iterator<Event> iter) {
        String patternStr = parseScalar(event, iter);
        try {
            return Patterns.get(patternStr);
        } catch (IllegalArgumentException e) {
            throw new FileFormatException("Illegal pattern.");
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

    protected Set<Pattern> parseSetPattern(Event event,
            Iterator<Event> iter) {
        if (!event.is(ID.SequenceStart))
            throw new FileFormatException("Expected SequenceStart.");

        Set<Pattern> result = new TreeSet<>();
        event = iter.next();
        while (!event.is(ID.SequenceEnd)) {
            result.add(parsePattern(event, iter));
            event = iter.next();
        }
        return result;
    }

    protected Map<Pattern, Set<String>> parseMapPatternSetScalar(Event event,
            Iterator<Event> iter) {
        if (!event.is(ID.MappingStart))
            throw new FileFormatException("Expected MappingStart.");

        Map<Pattern, Set<String>> result = new TreeMap<>();
        event = iter.next();
        while (!event.is(ID.MappingEnd)) {
            Pattern pattern = parsePattern(event, iter);
            Set<String> scalars = parseSetScalar(iter.next(), iter);
            if (result.containsKey(pattern))
                throw new FileFormatException("Duplicate pattern in map.");
            result.put(pattern, scalars);
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

    protected void failOnDuplicate(Map<String, Boolean> keys,
                                   String key) {
        if (!keys.containsKey(key))
            throw new FileFormatException("Illegal key: " + key);
        if (keys.get(key))
            throw new FileFormatException("Duplicate key: " + key);
        keys.put(key, true);
    }
}
