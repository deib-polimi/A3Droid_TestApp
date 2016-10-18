package it.polimi.greenhouse.a3.roles;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import a3.a3droid.A3SupervisorRole;
import a3.a3droid.TimerInterface;

public abstract class SupervisorRole extends A3SupervisorRole implements TimerInterface {
	
	protected Map<String, Map<Integer, Set<String[]>>> launchedGroups;
	
	public SupervisorRole() {
		launchedGroups = new ConcurrentHashMap<String, Map<Integer, Set<String[]>>>();
	}
	
	protected void addGroupMember(String type, Integer experimentId, String uuid, String name) {
		if(launchedGroups.containsKey(type))
			if(launchedGroups.get(type).containsKey(experimentId))
				launchedGroups.get(type).get(experimentId).add(new String[]{uuid, name});	
			else{
				launchedGroups.get(type).put(experimentId, Collections.synchronizedSet(new HashSet<String[]>()));
				launchedGroups.get(type).get(experimentId).add(new String[]{uuid, name});						
			}
		else{
			Map<Integer, Set<String[]>> newGroup = new ConcurrentHashMap<Integer, Set<String[]>>();
			Set<String[]> experiments = Collections.synchronizedSet(new HashSet<String[]>());
			experiments.add(new String[]{uuid, name});					
			newGroup.put(experimentId, experiments);
			launchedGroups.put(type, newGroup);
		}
		
	}
	
	
	protected void removeGroupMember(String uuid){
		for(String type : launchedGroups.keySet())			
			for(int i : launchedGroups.get(type).keySet())
				for(String [] id : launchedGroups.get(type).get(i))
					if(id[0].equals(uuid)){
						launchedGroups.get(type).get(i).remove(id);						
						return;
					}
	}
	
	protected int groupSize(String type){
		int size = 0;
		for(int i : launchedGroups.get(type).keySet())
			size += launchedGroups.get(type).get(i).size();
		return size;
	}
	
	protected String retrieveGroupMemberUuid(String name){
		for(String gType : launchedGroups.keySet())
			for(int i : launchedGroups.get(gType).keySet())
				for(String [] id : launchedGroups.get(gType).get(i))
					if(id[1].equals(name))
						return id[0];
		return null;
	}

}
