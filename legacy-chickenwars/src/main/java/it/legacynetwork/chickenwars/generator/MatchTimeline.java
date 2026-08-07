package it.legacynetwork.chickenwars.generator;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Applica anche fasi saltate dopo lag, una sola volta e nell'ordine corretto. */
public final class MatchTimeline {
    private MatchPhaseSchedule schedule;private final Set<String> applied=new HashSet<String>();
    public MatchTimeline(MatchPhaseSchedule schedule){this.schedule=schedule;}
    public List<MatchPhaseDefinition> poll(int elapsedSeconds){List<MatchPhaseDefinition> due=new ArrayList<MatchPhaseDefinition>();for(MatchPhaseDefinition phase:schedule.getPhases())if(phase.getAtSecond()<=elapsedSeconds&&applied.add(phase.getId()))due.add(phase);return due;}
    public MatchPhaseDefinition next(int elapsedSeconds){for(MatchPhaseDefinition phase:schedule.getPhases())if(phase.getAtSecond()>elapsedSeconds&&!applied.contains(phase.getId()))return phase;return null;}
    public void reload(MatchPhaseSchedule replacement){if(replacement==null)throw new IllegalArgumentException("Timeline mancante");schedule=replacement;}
    public void reset(){applied.clear();}
}
