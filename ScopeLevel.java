package miniJava.AbstractSyntaxTrees;

import java.util.ArrayList;

public class ScopeLevel {
	public ArrayList<Attribute> entry;
	public int scope;
	
	public ScopeLevel(int scope){
		this.scope = scope;
		entry = new ArrayList<Attribute>();
	}
	
	public void enterToScope(Attribute a){
		entry.add(a);
	}
	
	public boolean checkInScope(Attribute att){
		for (Attribute a : entry) {
			if(a.id.equals(att.id)){
				return true;
			}
		}
		return false;
	}
}
