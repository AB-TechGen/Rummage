package dev.rummage.net;

/** Structured messages retain the server-established sender identity through every transport. */
public record ChatEvent(Kind kind,String playerId,String name,String text,String frame) {
    public enum Kind { NOTICE, CHAT, ADVANCEMENT }
    public static ChatEvent notice(String text){return new ChatEvent(Kind.NOTICE,"","",text,"");}
    public static ChatEvent chat(String id,String name,String text){return new ChatEvent(Kind.CHAT,id,name,text,"");}
    public static ChatEvent advancement(String id,String name,String frame,String title){return new ChatEvent(Kind.ADVANCEMENT,id,name,title,frame);}
    public String plainText(){return switch(kind){case NOTICE->text;case CHAT->"<"+name+"> "+text;case ADVANCEMENT->name+" earned ["+text+"]";};}
}
