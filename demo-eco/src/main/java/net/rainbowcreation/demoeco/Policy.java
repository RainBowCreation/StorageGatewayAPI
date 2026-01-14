package net.rainbowcreation.demoeco;

import net.rainbowcreation.storage.api.annotations.EnableQuery;
import net.rainbowcreation.storage.api.annotations.QLQuery;

import java.util.HashSet;
import java.util.Set;

@QLQuery(namespace = "trpolicy", typeName = "TranslationPolicy")
public class Policy {
    public enum Mode { ALL, ONLY, EXCEPT }

    public String ownerId;
    @EnableQuery public Mode mode = Mode.ALL;
    public Set<String> cats = new HashSet<>();
    public Policy() {}
}