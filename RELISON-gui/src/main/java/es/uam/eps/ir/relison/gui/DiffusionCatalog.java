/*
 *  Copyright (C) 2023 Information Retrieval Group at Universidad Autónoma
 *  de Madrid, http://ir.ii.uam.es
 *
 *  This Source Code Form is subject to the terms of the Mozilla Public
 *  License, v. 2.0. If a copy of the MPL was not distributed with this
 *  file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package es.uam.eps.ir.relison.gui;

import es.uam.eps.ir.relison.grid.diffusion.expiration.ExpirationMechanismIdentifiers;
import es.uam.eps.ir.relison.grid.diffusion.filter.FilterIdentifiers;
import es.uam.eps.ir.relison.grid.diffusion.metrics.MetricIdentifiers;
import es.uam.eps.ir.relison.grid.diffusion.propagation.PropagationMechanismIdentifiers;
import es.uam.eps.ir.relison.grid.diffusion.protocol.ProtocolIdentifiers;
import es.uam.eps.ir.relison.grid.diffusion.selection.SelectionMechanismIdentifiers;
import es.uam.eps.ir.relison.grid.diffusion.sight.SightMechanismIdentifiers;
import es.uam.eps.ir.relison.grid.diffusion.stop.StopConditionIdentifiers;
import es.uam.eps.ir.relison.grid.diffusion.update.UpdateMechanismIdentifiers;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Describes the information-diffusion elements the GUI exposes (protocols, the five custom-protocol mechanisms,
 * stop conditions, filters and metrics), together with their tunable parameters.
 *
 * <p>Each parameter carries both a UI control type (so the frontend can render it like any other metric parameter)
 * and the RELISON "basic type" string ({@code int/double/boolean/string/orientation/long}) used to encode it into
 * the YAML-shaped configuration map that {@code SimulationParameterReader} consumes.</p>
 *
 * @author Javier Sanz-Cruzado (javier.sanz-cruzado@uam.es)
 */
public final class DiffusionCatalog
{
    /** A tunable parameter of a diffusion element. */
    public static final class DiffParam
    {
        /** The configuration key (e.g. {@code numOwn}). */
        public final String name;
        /** The label shown in the UI. */
        public final String label;
        /** The UI control type: {@code int/double/bool/string/orientation}. */
        public final String control;
        /** The RELISON basic type used when encoding the value: {@code int/double/boolean/string/orientation/long}. */
        public final String type;
        /** The default value. */
        public final Object def;
        /** For orientation parameters, the allowed values; {@code null} otherwise. */
        public final List<String> options;

        DiffParam(String name, String label, String control, String type, Object def, List<String> options)
        {
            this.name = name;
            this.label = label;
            this.control = control;
            this.type = type;
            this.def = def;
            this.options = options;
        }

        Map<String, Object> toJson()
        {
            Map<String, Object> json = new LinkedHashMap<>();
            json.put("name", name);
            json.put("label", label);
            json.put("type", control);
            json.put("default", def);
            if (options != null) json.put("options", options);
            return json;
        }
    }

    /** A single exposed diffusion element. */
    public static final class Element
    {
        public final String id;
        public final String label;
        public final String group;
        public final List<DiffParam> params;
        /** Whether the element needs uploaded information/user feature data to run. */
        public final boolean needsFeatures;

        Element(String id, String label, String group, boolean needsFeatures, List<DiffParam> params)
        {
            this.id = id;
            this.label = label;
            this.group = group;
            this.needsFeatures = needsFeatures;
            this.params = params;
        }

        Map<String, Object> toJson()
        {
            Map<String, Object> json = new LinkedHashMap<>();
            json.put("id", id);
            json.put("label", label);
            json.put("group", group);
            json.put("needsFeatures", needsFeatures);
            List<Map<String, Object>> ps = new ArrayList<>();
            for (DiffParam p : params) ps.add(p.toJson());
            json.put("params", ps);
            return json;
        }
    }

    private static final Map<String, Element> PROTOCOLS = new LinkedHashMap<>();
    private static final Map<String, Element> SELECTION = new LinkedHashMap<>();
    private static final Map<String, Element> EXPIRATION = new LinkedHashMap<>();
    private static final Map<String, Element> PROPAGATION = new LinkedHashMap<>();
    private static final Map<String, Element> UPDATE = new LinkedHashMap<>();
    private static final Map<String, Element> SIGHT = new LinkedHashMap<>();
    private static final Map<String, Element> STOP = new LinkedHashMap<>();
    private static final Map<String, Element> FILTERS = new LinkedHashMap<>();
    private static final Map<String, Element> METRICS = new LinkedHashMap<>();

    static
    {
        // ---- Preset protocols ----
        protocol(ProtocolIdentifiers.SIMPLE, "Simple", numOwn(), numRec());
        protocol(ProtocolIdentifiers.INDEPCASCADE, "Independent cascade model", prob(0.1), numOwn());
        protocol(ProtocolIdentifiers.PUSH, "Push", numOwn(), numRec(), waitTime());
        protocol(ProtocolIdentifiers.PULL, "Pull", numOwn(), numRec(), waitTime());
        protocol(ProtocolIdentifiers.RUMORSPREADING, "Rumor spreading", numOwn(), numRec(), waitTime());
        protocol(ProtocolIdentifiers.BIDIRRUMORSPREADING, "Bidirectional rumor spreading", numOwn(), numRec(), waitTime());
        protocol(ProtocolIdentifiers.THRESHOLD, "Proportion threshold", numOwn(), numRec(), real("threshold", "Threshold", 0.5));
        protocol(ProtocolIdentifiers.COUNTTHRESHOLD, "Count threshold", numOwn(), integer("threshold", "Threshold", 1));
        protocol(ProtocolIdentifiers.TEMPORAL, "Temporal", bool("pure", "Pure (only real timestamps)", false));

        // ---- Custom-protocol mechanisms ----
        // Selection.
        select(SelectionMechanismIdentifiers.ONLYOWN, "Only own", numOwn(), numRepr());
        select(SelectionMechanismIdentifiers.COUNT, "Count", numOwn(), numRec(), numRepr());
        select(SelectionMechanismIdentifiers.ICM, "Independent cascade model", numOwn(), prob(0.1), numRepr());
        select(SelectionMechanismIdentifiers.COUNTTHRESHOLD, "Count threshold", numOwn(), integer("threshold", "Threshold", 1), numRepr());
        select(SelectionMechanismIdentifiers.PROPORTIONTHRESHOLD, "Proportion threshold", numOwn(), real("threshold", "Threshold", 0.5), numRepr(), orientation());
        select(SelectionMechanismIdentifiers.PUSHPULL, "Push-pull", numOwn());
        select(SelectionMechanismIdentifiers.TIMESTAMPORDERED, "Timestamp-ordered", numOwn(), numRec(), numRepr());
        select(SelectionMechanismIdentifiers.PURETIMESTAMP, "Pure timestamp-based");
        select(SelectionMechanismIdentifiers.LOOSETIMESTAMP, "Loose timestamp-based");

        // Expiration.
        expir(ExpirationMechanismIdentifiers.INFINITETIME, "Infinite time");
        expir(ExpirationMechanismIdentifiers.ALLNOTPROP, "All not propagated");
        expir(ExpirationMechanismIdentifiers.TIMED, "Timed", longp("max-time", "Max. time", 86400L));
        expir(ExpirationMechanismIdentifiers.EXPDECAY, "Exponential decay", real("half-life", "Half-life", 1.0));

        // Propagation.
        propag(PropagationMechanismIdentifiers.ALLNEIGHS, "All neighbors", orientation());
        propag(PropagationMechanismIdentifiers.PUSH, "Push", waitTime(), orientation());
        propag(PropagationMechanismIdentifiers.PULL, "Pull", waitTime(), orientation());
        propag(PropagationMechanismIdentifiers.PUSHPULL, "Push-pull", waitTime(), orientation());

        // Update.
        update(UpdateMechanismIdentifiers.NEWEST, "Newest");
        update(UpdateMechanismIdentifiers.OLDEST, "Oldest");
        update(UpdateMechanismIdentifiers.MERGER, "Merger");

        // Sight.
        sight(SightMechanismIdentifiers.ALLSIGHT, "All sight");
        sight(SightMechanismIdentifiers.ALLNOTPROPAGATED, "All not propagated");
        sight(SightMechanismIdentifiers.ALLNOTDISCARDED, "All not discarded");
        sight(SightMechanismIdentifiers.ALLNOTDISCARDEDNOTPROPAGATED, "All not discarded nor propagated");
        sight(SightMechanismIdentifiers.COUNT, "Count", integer("numSight", "Pieces seen per iteration", 10));

        // ---- Stop conditions ----
        stop(StopConditionIdentifiers.NUMITER, "Number of iterations", integer("numIter", "Iterations", 50));
        stop(StopConditionIdentifiers.NOMORENEW, "No more new information");
        stop(StopConditionIdentifiers.NOMOREPROP, "No more propagated information");
        stop(StopConditionIdentifiers.TOTALPROP, "Total propagated reached", longp("propagated", "Total pieces", 100L));
        stop(StopConditionIdentifiers.NOMORETIME, "No more timestamps");
        stop(StopConditionIdentifiers.MAXTIME, "Max. timestamp", longp("maxTimestamp", "Max. timestamp", 0L));
        stop(StopConditionIdentifiers.NOMORETIMENORINFO, "No more timestamps nor propagation");

        // ---- Filters ----
        filter(FilterIdentifiers.BASIC, "Basic (no filtering)", false);
        filter(FilterIdentifiers.NUMPIECES, "Limit pieces per user", false, integer("numPieces", "Max. pieces", 10));
        filter(FilterIdentifiers.ONLYREPR, "Only repropagated pieces", false);
        filter(FilterIdentifiers.CREATOR, "Only pieces with a creator", false);

        // ---- Metrics (structural ones need no features; the feature ones do) ----
        metric(MetricIdentifiers.SPEED, "Speed", "Information pieces", false);
        metric(MetricIdentifiers.USERSPEED, "Information count", "Information pieces", false);
        metric(MetricIdentifiers.INFOGINI, "Information Gini complement", "Information pieces", false);
        metric(MetricIdentifiers.USERRECALL, "Creator recall", "Creators", false);
        metric(MetricIdentifiers.USERGLOBALGINI, "Global creator Gini complement", "Creators", false, unique());
        metric(MetricIdentifiers.USERINDIVGINI, "Individual creator Gini complement", "Creators", false, unique());
        metric(MetricIdentifiers.USERGLOBALENTROPY, "Global creator entropy", "Creators", false, unique());
        metric(MetricIdentifiers.USERINDIVENTROPY, "Individual creator entropy", "Creators", false, unique());
        metric(MetricIdentifiers.REALPROPRECALL, "Individual real propagated recall", "Information pieces", true);
        metric(MetricIdentifiers.GLOBALREALPROPRECALL, "Global real propagated recall", "Information pieces", true);
        metric(MetricIdentifiers.RECALL, "Feature recall", "Features", true, feature(), userFeature());
        metric(MetricIdentifiers.GINI, "Individual feature Gini complement", "Features", true, feature(), userFeature(), unique());
        metric(MetricIdentifiers.GLOBALGINI, "Global feature Gini complement", "Features", true, feature(), userFeature(), unique());
        metric(MetricIdentifiers.ENTROPY, "Feature entropy", "Features", true, feature(), userFeature(), unique());
        metric(MetricIdentifiers.GLOBALENTROPY, "Global feature entropy", "Features", true, feature(), userFeature(), unique());
        metric(MetricIdentifiers.GLOBALKLD, "Global feature KLD", "Features", true, feature(), userFeature(), unique());
    }

    private DiffusionCatalog()
    {
    }

    /* ----------------------------- builders ----------------------------- */

    private static void protocol(String id, String label, DiffParam... params) { PROTOCOLS.put(id, new Element(id, label, "Protocol", false, List.of(params))); }
    private static void select(String id, String label, DiffParam... params) { SELECTION.put(id, new Element(id, label, "Selection", false, List.of(params))); }
    private static void expir(String id, String label, DiffParam... params) { EXPIRATION.put(id, new Element(id, label, "Expiration", false, List.of(params))); }
    private static void propag(String id, String label, DiffParam... params) { PROPAGATION.put(id, new Element(id, label, "Propagation", false, List.of(params))); }
    private static void update(String id, String label, DiffParam... params) { UPDATE.put(id, new Element(id, label, "Update", false, List.of(params))); }
    private static void sight(String id, String label, DiffParam... params) { SIGHT.put(id, new Element(id, label, "Sight", false, List.of(params))); }
    private static void stop(String id, String label, DiffParam... params) { STOP.put(id, new Element(id, label, "Stop", false, List.of(params))); }
    private static void filter(String id, String label, boolean needsFeat, DiffParam... params) { FILTERS.put(id, new Element(id, label, "Filter", needsFeat, List.of(params))); }
    private static void metric(String id, String label, String group, boolean needsFeat, DiffParam... params) { METRICS.put(id, new Element(id, label, group, needsFeat, List.of(params))); }

    private static DiffParam integer(String name, String label, int def) { return new DiffParam(name, label, "int", "int", def, null); }
    private static DiffParam longp(String name, String label, long def) { return new DiffParam(name, label, "int", "long", def, null); }
    private static DiffParam real(String name, String label, double def) { return new DiffParam(name, label, "double", "double", def, null); }
    private static DiffParam bool(String name, String label, boolean def) { return new DiffParam(name, label, "bool", "boolean", def, null); }
    private static DiffParam string(String name, String label, String def) { return new DiffParam(name, label, "string", "string", def, null); }
    private static DiffParam orientation() { return new DiffParam("orientation", "Orientation", "orientation", "orientation", "OUT", List.of("OUT", "IN", "UND", "MUTUAL")); }

    private static DiffParam numOwn() { return integer("numOwn", "Own pieces / iteration", 1); }
    private static DiffParam numRec() { return integer("numRec", "Received pieces / iteration", 1); }
    private static DiffParam numRepr() { return integer("numRepr", "Repropagated pieces / iteration", 0); }
    private static DiffParam waitTime() { return integer("waitTime", "Wait time", 1); }
    private static DiffParam prob(double def) { return real("prob", "Probability", def); }
    private static DiffParam unique() { return bool("unique", "Count each piece once", true); }
    private static DiffParam feature() { return string("feature", "Feature name", ""); }
    private static DiffParam userFeature() { return bool("userFeature", "User feature (off = info feature)", false); }

    /* ----------------------------- accessors ---------------------------- */

    public static Map<String, Element> protocols() { return PROTOCOLS; }
    public static Map<String, Element> selection() { return SELECTION; }
    public static Map<String, Element> expiration() { return EXPIRATION; }
    public static Map<String, Element> propagation() { return PROPAGATION; }
    public static Map<String, Element> update() { return UPDATE; }
    public static Map<String, Element> sight() { return SIGHT; }
    public static Map<String, Element> stop() { return STOP; }
    public static Map<String, Element> filters() { return FILTERS; }
    public static Map<String, Element> metrics() { return METRICS; }

    /** @return the element of the given family, or {@code null}. */
    public static Element find(String family, String id)
    {
        Map<String, Element> fam = switch (family)
        {
            case "protocol" -> PROTOCOLS;
            case "selection" -> SELECTION;
            case "expiration" -> EXPIRATION;
            case "propagation" -> PROPAGATION;
            case "update" -> UPDATE;
            case "sight" -> SIGHT;
            case "stop" -> STOP;
            case "filter" -> FILTERS;
            case "metric" -> METRICS;
            default -> null;
        };
        return fam == null ? null : fam.get(id);
    }

    /** Builds the catalog payload keyed by family. */
    public static Map<String, Object> toJson()
    {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("protocol", familyJson(PROTOCOLS));
        out.put("selection", familyJson(SELECTION));
        out.put("expiration", familyJson(EXPIRATION));
        out.put("propagation", familyJson(PROPAGATION));
        out.put("update", familyJson(UPDATE));
        out.put("sight", familyJson(SIGHT));
        out.put("stop", familyJson(STOP));
        out.put("filter", familyJson(FILTERS));
        out.put("metric", familyJson(METRICS));
        return out;
    }

    private static List<Map<String, Object>> familyJson(Map<String, Element> family)
    {
        List<Map<String, Object>> list = new ArrayList<>();
        for (Element e : family.values()) list.add(e.toJson());
        return list;
    }
}
