package it.legacynetwork.chickenwars.generator;

import it.legacynetwork.chickenwars.model.ResourceType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.Sound;

/** Timeline configurabile, ordinata e validata una sola volta. */
public final class MatchPhaseSchedule {
    private final List<MatchPhaseDefinition> phases;
    public MatchPhaseSchedule(List<MatchPhaseDefinition> phases){List<MatchPhaseDefinition> copy=new ArrayList<MatchPhaseDefinition>(phases==null?Collections.<MatchPhaseDefinition>emptyList():phases);Set<String> ids=new HashSet<String>();int previous=-1;for(MatchPhaseDefinition phase:copy){if(!ids.add(phase.getId()))throw new IllegalArgumentException("Fase duplicata: "+phase.getId());if(phase.getAtSecond()<=previous)throw new IllegalArgumentException("Tempi fase duplicati o errati");previous=phase.getAtSecond();}this.phases=Collections.unmodifiableList(copy);}
    public static MatchPhaseSchedule fromSection(ConfigurationSection section){List<MatchPhaseDefinition> result=new ArrayList<MatchPhaseDefinition>();if(section!=null)for(String id:section.getKeys(false)){ConfigurationSection phase=section.getConfigurationSection(id);if(phase==null)continue;ResourceType resource=ResourceType.fromString(phase.getString("resource"));boolean collapse=phase.getBoolean("royal-collapse",false);if(resource==null&&!collapse)throw new IllegalArgumentException("Fase senza effetto: "+id);String sound=phase.getString("sound","NOTE_PLING").trim().toUpperCase(java.util.Locale.ROOT);try{Sound.valueOf(sound);}catch(IllegalArgumentException invalid){throw new IllegalArgumentException("Suono fase non valido: "+id,invalid);}result.add(new MatchPhaseDefinition(id,phase.getInt("at-seconds",-1),resource,phase.getInt("tier",resource==null?0:1),collapse,phase.getString("message-key"),sound));}return new MatchPhaseSchedule(result);}
    public List<MatchPhaseDefinition> getPhases(){return phases;}
}
