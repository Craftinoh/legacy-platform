package it.legacynetwork.chickenwars.generator;

import it.legacynetwork.chickenwars.model.ResourceType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Scheduler unico della partita, indipendente da Bukkit. */
public final class GeneratorService {
    private final GeneratorSchedule schedule;private final GeneratorDropSink sink;
    private final Map<String,GeneratorState> states=new LinkedHashMap<String,GeneratorState>();
    private boolean running;
    public GeneratorService(GeneratorSchedule schedule,GeneratorDropSink sink){if(schedule==null||sink==null)throw new IllegalArgumentException("Servizio generatori incompleto");this.schedule=schedule;this.sink=sink;}
    public void add(GeneratorState state){if(running)throw new IllegalStateException("Generatori gia' avviati");if(state==null)throw new IllegalArgumentException("Generatore mancante");if(states.containsKey(state.getId()))throw new IllegalArgumentException("ID generatore duplicato: "+state.getId());states.put(state.getId(),state);}
    public void start(long tick){if(running)return;running=true;for(GeneratorState state:states.values())state.start(tick,schedule);}
    public int tick(long tick){if(!running)return 0;int drops=0;for(GeneratorState state:states.values()){int amount=state.poll(tick,schedule);if(amount>0&&sink.drop(state,amount))drops++;}return drops;}
    public void setTier(ResourceType type,int level,long tick){for(GeneratorState state:states.values())if(state.getDefinition().getType()==type)state.setLevel(level,tick,schedule);}
    public void setTeamTier(String teamId,int level,long tick){for(GeneratorState state:states.values())if(teamId!=null&&teamId.equals(state.getDefinition().getTeamId()))state.setLevel(level,tick,schedule);}
    public void stop(){if(!running)return;running=false;for(GeneratorState state:states.values())state.stop();sink.cleanup();}
    public void clear(){stop();states.clear();}
    public boolean isRunning(){return running;}
    public List<GeneratorState> states(){return Collections.unmodifiableList(new ArrayList<GeneratorState>(states.values()));}
}
