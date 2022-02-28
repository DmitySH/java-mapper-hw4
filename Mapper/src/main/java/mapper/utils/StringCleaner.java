package mapper.utils;

import mapper.exceptions.ExportMapperException;
import mapper.interfaces.Cleaner;

import java.util.HashSet;
import java.util.Set;

public class StringCleaner implements Cleaner {
    private static final Set<CharSequence> banned = new HashSet<>(Set.of("\uFFF0", "\uFFF1",
            "\uFFF2", "\uFFF3", "\uFFF4"));

    @Override
    public String cleanString(String str) {
        for (CharSequence banChar : banned) {
            if (str.contains(banChar)) {
                throw new ExportMapperException("String has incorrect symbol " + banChar);
            }
        }

        return str.replace('\"', '\uFFF0').replace('{', '\uFFF1')
                .replace('}', '\uFFF2').replace('[', '\uFFF3')
                .replace(']', '\uFFF4');
    }

    @Override
    public String recoverString(String str) {
        return str.replace('\uFFF0', '\"').replace('\uFFF1', '{')
                .replace('\uFFF2', '}').replace('\uFFF3', '[')
                .replace('\uFFF4', ']');
    }
}
