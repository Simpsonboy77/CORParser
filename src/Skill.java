public class Skill {


    String name = "";
    SkillParser.tierEnum tier;
    String archetype = "";
    String focus = "";
    String type = "";
    int expcost = 0;
    int steamCost = 0 ;
    int aetherCost = 0;
    String Prerequisites = "";
    String multiPurchase = "";
    String decsription = "";

    public Skill()
    {
    }
    public void setName(String name){
        this.name = name;
    }
    public void setTier(SkillParser.tierEnum tier){
        this.tier = tier;
    }
    public void setArchetype(String archetype){
        this.archetype = archetype;
    }
    public void setFocus(String focus){
        this.focus = focus;
    }
    public void setExpcost(int cost){
        this.expcost = cost;
    }

}